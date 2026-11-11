"""
Asynchronous Network Server for LAN Gamepad.
Hosts high-performance UDP input datagram receiver and TCP pairing connection manager.
Implements deadman switch watchdog, sequence ordering, and stats collection.
"""
import asyncio
import json
import logging
import time
from typing import Optional, Callable
from config import DEFAULT_UDP_PORT, DEFAULT_TCP_PORT, DEADMAN_TIMEOUT_SEC
import protocol
from controller_state import ControllerState
from gamepad_backend import GamepadBackend
from pairing import PairingManager

logger = logging.getLogger("LAN_Gamepad.Network")


class ServerStats:
    def __init__(self):
        self.packets_received: int = 0
        self.packets_dropped_stale: int = 0
        self.packets_invalid: int = 0
        self.last_packet_time: float = 0.0
        self.current_packet_rate: float = 0.0
        self._rate_window_count: int = 0
        self._last_rate_calc: float = time.time()
        self.last_seen_sequence: int = 0
        self.connected_device: str = "None"
        self.connected_ip: str = "None"

    def record_packet(self, seq: int) -> None:
        self.packets_received += 1
        self._rate_window_count += 1
        self.last_packet_time = time.time()
        self.last_seen_sequence = seq

        now = time.time()
        elapsed = now - self._last_rate_calc
        if elapsed >= 1.0:
            self.current_packet_rate = self._rate_window_count / elapsed
            self._rate_window_count = 0
            self._last_rate_calc = now


class UdpInputProtocol(asyncio.DatagramProtocol):
    """
    Non-blocking asyncio datagram protocol for low-latency gamepad input streaming.
    """

    def __init__(
        self,
        backend: GamepadBackend,
        pairing_mgr: PairingManager,
        stats: ServerStats,
        on_state_update: Optional[Callable[[ControllerState], None]] = None
    ):
        self.backend = backend
        self.pairing_mgr = pairing_mgr
        self.stats = stats
        self.on_state_update = on_state_update
        self.transport = None
        self._last_sequence: int = -1

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data: bytes, addr):
        client_ip, client_port = addr

        # Look up paired session for this client
        session = self.pairing_mgr.get_session_by_ip(client_ip)
        session_key = session.session_token if session else None

        # Verify and unpack packet
        success, parsed, err = protocol.unpack_input_packet(data, session_key)
        if not success:
            self.stats.packets_invalid += 1
            logger.debug(f"Invalid packet from {client_ip}: {err}")
            return

        seq = parsed["sequence"]

        # Discard duplicate or out-of-order packets
        if self._last_sequence >= 0:
            # Wrap-safe sequence comparison
            diff = (seq - self._last_sequence) & 0xFFFFFFFF
            if diff == 0 or diff > 0x7FFFFFFF:
                self.stats.packets_dropped_stale += 1
                return

        self._last_sequence = seq
        self.stats.record_packet(seq)

        # Update controller state
        state = ControllerState(
            buttons=parsed["buttons"],
            stick_lx=parsed["stick_lx"],
            stick_ly=parsed["stick_ly"],
            stick_rx=parsed["stick_rx"],
            stick_ry=parsed["stick_ry"],
            trigger_l=parsed["trigger_l"],
            trigger_r=parsed["trigger_r"],
            dpad_x=parsed["dpad_x"],
            dpad_y=parsed["dpad_y"],
        )

        self.backend.update_controller(state)
        if self.on_state_update:
            self.on_state_update(state)


class NetworkServer:
    def __init__(
        self,
        backend: GamepadBackend,
        pairing_mgr: PairingManager,
        udp_port: int = DEFAULT_UDP_PORT,
        tcp_port: int = DEFAULT_TCP_PORT,
        on_state_change: Optional[Callable[[ControllerState], None]] = None
    ):
        self.backend = backend
        self.pairing_mgr = pairing_mgr
        self.udp_port = udp_port
        self.tcp_port = tcp_port
        self.on_state_change = on_state_change
        self.stats = ServerStats()
        self._running = False
        self._udp_transport = None
        self._tcp_server = None
        self._deadman_task = None

    async def start(self) -> None:
        self._running = True
        loop = asyncio.get_running_loop()

        # Start UDP receiver
        self._udp_transport, _ = await loop.create_datagram_endpoint(
            lambda: UdpInputProtocol(
                backend=self.backend,
                pairing_mgr=self.pairing_mgr,
                stats=self.stats,
                on_state_update=self.on_state_change
            ),
            local_addr=("0.0.0.0", self.udp_port),
        )
        logger.info(f"UDP input socket bound to 0.0.0.0:{self.udp_port}")

        # Start TCP pairing server
        self._tcp_server = await asyncio.start_server(
            self._handle_tcp_client, "0.0.0.0", self.tcp_port
        )
        logger.info(f"TCP pairing server listening on 0.0.0.0:{self.tcp_port}")

        # Start deadman watchdog background loop
        self._deadman_task = asyncio.create_task(self._deadman_watchdog_loop())

    async def _handle_tcp_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        peer = writer.get_extra_info("peername")
        client_ip = peer[0] if peer else "Unknown"
        logger.info(f"New TCP connection from {client_ip}")

        try:
            while self._running:
                line = await reader.readline()
                if not line:
                    break

                try:
                    msg = json.loads(line.decode("utf-8").strip())
                except json.JSONDecodeError:
                    continue

                msg_type = msg.get("type")

                if msg_type == "PAIR_REQUEST":
                    pin = str(msg.get("pin", ""))
                    device_name = str(msg.get("device_name", "Android Gamepad"))

                    session = self.pairing_mgr.verify_and_create_session(
                        client_ip=client_ip, device_name=device_name, pin_entered=pin
                    )

                    if session:
                        self.stats.connected_device = device_name
                        self.stats.connected_ip = client_ip
                        response = {
                            "type": "PAIR_SUCCESS",
                            "session_id": session.session_id,
                            "session_token": session.session_token.hex(),
                            "udp_port": self.udp_port,
                            "deadman_timeout_ms": int(DEADMAN_TIMEOUT_SEC * 1000)
                        }
                    else:
                        response = {
                            "type": "PAIR_REJECTED",
                            "reason": "INVALID_PIN"
                        }

                    writer.write((json.dumps(response) + "\n").encode("utf-8"))
                    await writer.drain()

                elif msg_type == "PING":
                    writer.write((json.dumps({"type": "PONG", "time": time.time()}) + "\n").encode("utf-8"))
                    await writer.drain()

                elif msg_type == "DISCONNECT":
                    logger.info(f"Client {client_ip} sent explicit DISCONNECT.")
                    self.backend.release_all()
                    break

        except Exception as exc:
            logger.debug(f"TCP client connection closed: {exc}")
        finally:
            writer.close()
            try:
                await writer.wait_closed()
            except Exception:
                pass

    async def _deadman_watchdog_loop(self) -> None:
        """
        Continuously checks if input packets have ceased for > 250ms.
        Immediately neutralizes controller to prevent stuck character actions.
        """
        neutral_applied = True
        while self._running:
            await asyncio.sleep(0.05)  # 50ms polling resolution
            if self.stats.packets_received == 0:
                continue

            idle_duration = time.time() - self.stats.last_packet_time
            if idle_duration > DEADMAN_TIMEOUT_SEC:
                if not neutral_applied:
                    logger.debug(f"Deadman watchdog fired: {idle_duration*1000:.1f}ms without packet. Neutralizing inputs.")
                    self.backend.release_all()
                    if self.on_state_change:
                        self.on_state_change(ControllerState.neutral())
                    neutral_applied = True
            else:
                neutral_applied = False

    async def stop(self) -> None:
        self._running = False
        if self._deadman_task:
            self._deadman_task.cancel()
        if self._udp_transport:
            self._udp_transport.close()
        if self._tcp_server:
            self._tcp_server.close()
            await self._tcp_server.wait_closed()
        self.backend.release_all()
        logger.info("Network server stopped cleanly.")

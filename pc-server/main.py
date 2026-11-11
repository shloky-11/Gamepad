"""
LAN Gamepad Windows PC Server — Main Entry Point.
Launches the asynchronous UDP receiver, TCP pairing listener, mDNS discovery,
virtual Xbox 360 controller backend, and terminal status monitor.
"""
import asyncio
import logging
import sys
import os
import time
from typing import Dict, Any

from config import DEFAULT_UDP_PORT, DEFAULT_TCP_PORT, DEFAULT_WEB_PORT
from gamepad_backend import create_best_backend
from pairing import PairingManager
from discovery import DiscoveryBroadcaster, get_local_ip
from network_server import NetworkServer
from controller_state import ControllerState
from web_dashboard import WebCompanionServer

# Setup clean console logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S"
)
logger = logging.getLogger("LAN_Gamepad")


class LanGamepadApplication:
    def __init__(self):
        self.local_ip = get_local_ip()
        self.pairing_mgr = PairingManager()
        self.backend, self.backend_info = create_best_backend()
        self.current_state = ControllerState.neutral()
        self.discovery = DiscoveryBroadcaster(tcp_port=DEFAULT_TCP_PORT, udp_port=DEFAULT_UDP_PORT)

        self.net_server = NetworkServer(
            backend=self.backend,
            pairing_mgr=self.pairing_mgr,
            udp_port=DEFAULT_UDP_PORT,
            tcp_port=DEFAULT_TCP_PORT,
            on_state_change=self._on_state_change
        )

        self.web_server = WebCompanionServer(
            get_status_callback=self.get_telemetry_snapshot,
            port=DEFAULT_WEB_PORT
        )

    def _on_state_change(self, state: ControllerState):
        self.current_state = state

    def get_telemetry_snapshot(self) -> Dict[str, Any]:
        return {
            "local_ip": self.local_ip,
            "tcp_port": DEFAULT_TCP_PORT,
            "udp_port": DEFAULT_UDP_PORT,
            "pin": self.pairing_mgr.get_current_pin(),
            "backend_name": self.backend.get_name(),
            "is_real_hardware": self.backend.is_real_hardware(),
            "stats": {
                "packets_received": self.net_server.stats.packets_received,
                "packets_dropped_stale": self.net_server.stats.packets_dropped_stale,
                "packets_invalid": self.net_server.stats.packets_invalid,
                "current_packet_rate": round(self.net_server.stats.current_packet_rate, 1),
                "connected_device": self.net_server.stats.connected_device,
                "connected_ip": self.net_server.stats.connected_ip,
            },
            "controller_state": self.current_state.to_dict(),
        }

    def print_banner(self):
        pin = self.pairing_mgr.get_current_pin()
        print("\n" + "=" * 64)
        print("           LAN GAMEPAD SERVER — WINDOWS CONTROLLER HUB           ")
        print("=" * 64)
        print(f" Status:             RUNNING")
        print(f" Local Wi-Fi IP:     {self.local_ip}")
        print(f" TCP Pairing Port:   {DEFAULT_TCP_PORT}")
        print(f" UDP Input Port:     {DEFAULT_UDP_PORT}")
        print(f" Web Companion:      http://localhost:{DEFAULT_WEB_PORT}")
        print("-" * 64)
        print(f" Pairing PIN:        >>> {pin} <<< (Enter on phone app)")
        print(f" Connected Device:   {self.net_server.stats.connected_device}")
        print(f" Controller Backend: {self.backend.get_name()}")
        print("-" * 64)
        if not self.backend.is_real_hardware():
            print(" [!] NOTE: Running in Simulator Mode.")
            print("     To activate real Windows Xbox 360 controller output:")
            print("     1. Install ViGEmBus: https://github.com/nefarius/ViGEmBus/releases")
            print("     2. Run: pip install vgamepad")
        else:
            print(" [*] SUCCESS: Virtual Xbox 360 controller exposed to Windows!")
            print("     Verify in Windows: Press Win+R, type 'joy.cpl' and hit Enter.")
        print("=" * 64)
        print(" Commands: [r]eset controller | [p]in regen | [q]uit server\n")

    async def terminal_monitor_loop(self):
        """Displays live input throughput and controller summary."""
        while True:
            await asyncio.sleep(2.0)
            stats = self.net_server.stats
            if stats.packets_received > 0:
                print(
                    f"\r[Live Stats] Pkts: {stats.packets_received:,} | "
                    f"Rate: {stats.current_packet_rate:.1f} Hz | "
                    f"Stale: {stats.packets_dropped_stale} | "
                    f"{self.current_state.summary()}",
                    end="",
                    flush=True
                )

    async def run(self):
        # Start networking
        await self.net_server.start()
        # Start web dashboard
        await self.web_server.start()
        # Start mDNS broadcaster
        self.discovery.start()

        self.print_banner()

        # Run terminal monitor alongside
        monitor_task = asyncio.create_task(self.terminal_monitor_loop())

        try:
            while True:
                await asyncio.sleep(1.0)
        except (KeyboardInterrupt, asyncio.CancelledError):
            print("\nShutting down LAN Gamepad Server...")
        finally:
            monitor_task.cancel()
            self.discovery.stop()
            await self.web_server.stop()
            await self.net_server.stop()
            self.backend.disconnect()
            print("Server cleanly stopped.")


def main():
    app = LanGamepadApplication()
    try:
        asyncio.run(app.run())
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()

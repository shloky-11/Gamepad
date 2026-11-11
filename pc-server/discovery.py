"""
mDNS / DNS-SD Service Discovery broadcaster for LAN Gamepad.
Advertises the PC server on the local Wi-Fi network using standard zeroconf.
"""
import socket
import logging
from typing import Optional
from config import MDNS_SERVICE_TYPE, MDNS_SERVICE_NAME, DEFAULT_TCP_PORT, DEFAULT_UDP_PORT

logger = logging.getLogger("LAN_Gamepad.Discovery")


def get_local_ip() -> str:
    """Detects the primary local LAN IP address of this machine."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Does not actually transmit packets, just resolves route
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip


class DiscoveryBroadcaster:
    def __init__(self, tcp_port: int = DEFAULT_TCP_PORT, udp_port: int = DEFAULT_UDP_PORT):
        self.tcp_port = tcp_port
        self.udp_port = udp_port
        self.local_ip = get_local_ip()
        self._zeroconf = None
        self._service_info = None

    def start(self) -> bool:
        try:
            from zeroconf import Zeroconf, ServiceInfo

            hostname = socket.gethostname()
            service_name = f"{hostname} - {MDNS_SERVICE_NAME}.{MDNS_SERVICE_TYPE}"

            properties = {
                b"tcp_port": str(self.tcp_port).encode("utf-8"),
                b"udp_port": str(self.udp_port).encode("utf-8"),
                b"version": b"1",
                b"hostname": hostname.encode("utf-8"),
            }

            self._service_info = ServiceInfo(
                type_=MDNS_SERVICE_TYPE,
                name=service_name,
                addresses=[socket.inet_aton(self.local_ip)],
                port=self.tcp_port,
                properties=properties,
                server=f"{hostname}.local.",
            )

            self._zeroconf = Zeroconf()
            self._zeroconf.register_service(self._service_info)
            logger.info(f"mDNS Service announced: {service_name} on {self.local_ip}:{self.tcp_port}")
            return True
        except ImportError:
            logger.warning("zeroconf library not installed. Install via 'pip install zeroconf' for automatic discovery.")
            return False
        except Exception as exc:
            logger.warning(f"Could not announce mDNS service: {exc}. Manual IP entry remains available.")
            return False

    def stop(self) -> None:
        if self._zeroconf and self._service_info:
            try:
                self._zeroconf.unregister_service(self._service_info)
                self._zeroconf.close()
                logger.info("mDNS Service unregistered.")
            except Exception as e:
                logger.debug(f"Error closing zeroconf: {e}")
            finally:
                self._zeroconf = None
                self._service_info = None

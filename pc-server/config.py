"""
Configuration settings for LAN Gamepad Windows Server.
"""
import os

# Networking Ports
DEFAULT_UDP_PORT = 42100
DEFAULT_TCP_PORT = 42101
DEFAULT_WEB_PORT = 42102

# Magic signature and version
PROTOCOL_MAGIC = 0x4C475044  # b'LGPD'
PROTOCOL_VERSION = 1

# Deadman switch safety: automatically release all buttons and center sticks
# if no packet received within this time (in seconds)
DEADMAN_TIMEOUT_SEC = 0.250  # 250 milliseconds

# Service Discovery (mDNS / DNS-SD)
MDNS_SERVICE_TYPE = "_langamepad._tcp.local."
MDNS_SERVICE_NAME = "LAN Gamepad Server"

# Pairing
PIN_EXPIRY_SECONDS = 600  # 10 minutes
MAX_PIN_ATTEMPTS = 5

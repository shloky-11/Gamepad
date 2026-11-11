"""
Binary UDP Protocol implementation for LAN Gamepad.
Handles exact 32-byte packet encoding, decoding, validation, and HMAC authentication.
"""
import struct
import hmac
import hashlib
from typing import Optional, Tuple
from config import PROTOCOL_MAGIC, PROTOCOL_VERSION

# Button Bitmask constants matching Xbox 360 controller
BTN_A = 1 << 0
BTN_B = 1 << 1
BTN_X = 1 << 2
BTN_Y = 1 << 3
BTN_LB = 1 << 4
BTN_RB = 1 << 5
BTN_BACK = 1 << 6
BTN_START = 1 << 7
BTN_GUIDE = 1 << 8
BTN_L3 = 1 << 9
BTN_R3 = 1 << 10
BTN_DPAD_UP = 1 << 11
BTN_DPAD_DOWN = 1 << 12
BTN_DPAD_LEFT = 1 << 13
BTN_DPAD_RIGHT = 1 << 14

PACKET_STRUCT_FORMAT = ">IBBHIIhhhhBBbbI"
PACKET_SIZE = struct.calcsize(PACKET_STRUCT_FORMAT)  # exactly 32 bytes

assert PACKET_SIZE == 32, f"Packet size must be exactly 32 bytes, got {PACKET_SIZE}"


def compute_auth_tag(session_key: bytes, packet_prefix_28_bytes: bytes) -> int:
    """
    Computes a 4-byte truncated HMAC-SHA256 authentication tag for the packet payload.
    """
    if not session_key:
        return 0
    h = hmac.new(session_key, packet_prefix_28_bytes, hashlib.sha256).digest()
    return struct.unpack(">I", h[:4])[0]


def pack_input_packet(
    sequence: int,
    buttons: int,
    stick_lx: int,
    stick_ly: int,
    stick_rx: int,
    stick_ry: int,
    trigger_l: int,
    trigger_r: int,
    dpad_x: int,
    dpad_y: int,
    session_key: bytes = b"",
    flags: int = 0
) -> bytes:
    """
    Packs controller state into a 32-byte binary datagram.
    """
    # Clamp input ranges
    stick_lx = max(-32768, min(32767, int(stick_lx)))
    stick_ly = max(-32768, min(32767, int(stick_ly)))
    stick_rx = max(-32768, min(32767, int(stick_rx)))
    stick_ry = max(-32768, min(32767, int(stick_ry)))
    trigger_l = max(0, min(255, int(trigger_l)))
    trigger_r = max(0, min(255, int(trigger_r)))
    dpad_x = max(-1, min(1, int(dpad_x)))
    dpad_y = max(-1, min(1, int(dpad_y)))

    # Pack the first 28 bytes without auth_tag
    header_and_body = struct.pack(
        ">IBBHIIhhhhBBbb",
        PROTOCOL_MAGIC,
        PROTOCOL_VERSION,
        flags,
        0,  # reserved
        sequence & 0xFFFFFFFF,
        buttons & 0xFFFFFFFF,
        stick_lx,
        stick_ly,
        stick_rx,
        stick_ry,
        trigger_l,
        trigger_r,
        dpad_x,
        dpad_y,
    )

    auth_tag = compute_auth_tag(session_key, header_and_body) if session_key else 0
    return header_and_body + struct.pack(">I", auth_tag)


def unpack_input_packet(
    data: bytes,
    session_key: Optional[bytes] = None
) -> Tuple[bool, Optional[dict], str]:
    """
    Validates and unpacks a 32-byte UDP packet.
    Returns (success, parsed_dict, error_message).
    """
    if len(data) != PACKET_SIZE:
        return False, None, f"Invalid packet length: {len(data)} != {PACKET_SIZE}"

    magic, version, flags, reserved, seq, btns, lx, ly, rx, ry, tl, tr, dx, dy, auth_tag = struct.unpack(
        PACKET_STRUCT_FORMAT, data
    )

    if magic != PROTOCOL_MAGIC:
        return False, None, f"Invalid magic 0x{magic:08X} (expected 0x{PROTOCOL_MAGIC:08X})"

    if version != PROTOCOL_VERSION:
        return False, None, f"Unsupported protocol version {version} (expected {PROTOCOL_VERSION})"

    # Validate HMAC if session_key is configured
    if session_key:
        expected_tag = compute_auth_tag(session_key, data[:28])
        if auth_tag != expected_tag:
            return False, None, f"HMAC authentication failed (got 0x{auth_tag:08X}, expected 0x{expected_tag:08X})"

    parsed = {
        "sequence": seq,
        "flags": flags,
        "buttons": btns,
        "stick_lx": lx,
        "stick_ly": ly,
        "stick_rx": rx,
        "stick_ry": ry,
        "trigger_l": tl,
        "trigger_r": tr,
        "dpad_x": dx,
        "dpad_y": dy,
        "auth_tag": auth_tag,
    }
    return True, parsed, ""

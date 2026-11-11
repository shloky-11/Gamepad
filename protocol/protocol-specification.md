# LAN Gamepad Protocol Specification v1.0

## 1. Architecture Overview

LAN Gamepad uses a dual-protocol hybrid network design over standard local IEEE 802.11 Wi-Fi networks:

- **TCP Channel (Port 42101)**: Reliable control plane used for:
  - Service discovery handshake
  - Pairing request & 6-digit PIN verification
  - Ephemeral HMAC session secret negotiation (`session_token`)
  - Connection teardown / unpairing
- **UDP Channel (Port 42100)**: Low-latency data plane used for:
  - Unidirectional, connectionless controller state snapshot streaming (60 Hz to 120 Hz)
  - Sub-millisecond serialization / deserialization
  - Monotonic sequence tracking and duplicate/out-of-order rejection
  - Microsecond HMAC authenticity verification

```
+------------------+                                +-------------------+
|                  |   TCP:42101 Handshake & Pairing |                   |
|   Android App    | <=============================> | Windows PC Server |
|                  |      Exchange Session Key       |                   |
|                  |                                 |                   |
|                  |   UDP:42100 Gamepad Snapshot    |                   |
|                  | ------------------------------> | [ViGEm Virtual]   |
+------------------+     (32 bytes @ 60-120 Hz)      | [Xbox Gamepad ]   |
                                                     +-------------------+
```

---

## 2. UDP Input Datagram Format

Every UDP packet is a fixed **32-byte binary datagram**. All multi-byte integers are serialized in **Big-Endian (Network Byte Order)**.

### Binary Layout (32 Bytes Total)

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Magic ('LGPD')                          | (0-3)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|    Version    |     Flags     |            Reserved           | (4-7)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        Sequence Number                        | (8-11)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        Button Bitmask                         | (12-15)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|       Left Stick X (int16)    |      Left Stick Y (int16)     | (16-19)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|       Right Stick X (int16)   |      Right Stick Y (int16)    | (20-23)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Trigger L    |  Trigger R    |    Dpad X     |    Dpad Y     | (24-27)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                  Authentication Tag (HMAC-SHA256)             | (28-31)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### Field Definitions

| Byte Offset | Field Name | Type | Description |
|---|---|---|---|
| `0..3` | `magic` | `uint32_t` | Constant `0x4C475044` (ASCII `"LGPD"`). Rejects random network traffic. |
| `4` | `version` | `uint8_t` | Must equal `0x01`. |
| `5` | `flags` | `uint8_t` | Bit 0: Heartbeat flag; Bit 1: Rumble feedback request. |
| `6..7` | `reserved` | `uint16_t` | Reserved for future expansion (must be `0x0000`). |
| `8..11` | `sequence` | `uint32_t` | Monotonically increasing counter per packet sent. Packets with `sequence <= last_seen_sequence` are dropped. |
| `12..15` | `buttons` | `uint32_t` | Bitmask of digital button states (1 = Pressed, 0 = Released). |
| `16..17` | `stick_lx` | `int16_t` | Left analog stick horizontal axis ($-32768$ Left to $+32767$ Right). |
| `18..19` | `stick_ly` | `int16_t` | Left analog stick vertical axis ($-32768$ Down to $+32767$ Up). |
| `20..21` | `stick_rx` | `int16_t` | Right analog stick horizontal axis ($-32768$ Left to $+32767$ Right). |
| `22..23` | `stick_ry` | `int16_t` | Right analog stick vertical axis ($-32768$ Down to $+32767$ Up). |
| `24` | `trigger_l` | `uint8_t` | Left trigger analog pressure ($0$ = Released to $255$ = Fully Depressed). |
| `25` | `trigger_r` | `uint8_t` | Right trigger analog pressure ($0$ = Released to $255$ = Fully Depressed). |
| `26` | `dpad_x` | `int8_t` | D-pad horizontal ($-1$ = Left, $0$ = Neutral, $+1$ = Right). |
| `27` | `dpad_y` | `int8_t` | D-pad vertical ($-1$ = Down, $0$ = Neutral, $+1$ = Up). |
| `28..31` | `auth_tag` | `uint32_t` | First 4 bytes of `HMAC-SHA256(session_key, packet_bytes[0..27])`. Prevents spoofing. |

---

## 3. Button Bitmask Mapping

The 32-bit `buttons` field maps directly to Xbox 360 controller digital switches:

| Bit Position | Hex Mask | Xbox Controller Button |
|---|---|---|
| Bit 0 | `0x00000001` | **A** (South action button) |
| Bit 1 | `0x00000002` | **B** (East action button) |
| Bit 2 | `0x00000004` | **X** (West action button) |
| Bit 3 | `0x00000008` | **Y** (North action button) |
| Bit 4 | `0x00000010` | **LB** (Left Bumper) |
| Bit 5 | `0x00000020` | **RB** (Right Bumper) |
| Bit 6 | `0x00000040` | **BACK** / View button |
| Bit 7 | `0x00000080` | **START** / Menu button |
| Bit 8 | `0x00000100` | **GUIDE** / Xbox button |
| Bit 9 | `0x00000200` | **L3** (Left Stick click-in) |
| Bit 10 | `0x00000400` | **R3** (Right Stick click-in) |
| Bit 11 | `0x00000800` | **DPAD_UP** (Digital mirror of `dpad_y > 0`) |
| Bit 12 | `0x00001000` | **DPAD_DOWN** (Digital mirror of `dpad_y < 0`) |
| Bit 13 | `0x00002000` | **DPAD_LEFT** (Digital mirror of `dpad_x < 0`) |
| Bit 14 | `0x00004000` | **DPAD_RIGHT** (Digital mirror of `dpad_x > 0`) |
| Bits 15..31 | `0xFFFF8000` | Reserved |

---

## 4. TCP Pairing & Session Negotiation Flow

TCP port `42101` accepts newline-delimited JSON payloads (`\n` terminated):

### Step 1: Client Pair Request
```json
{
  "type": "PAIR_REQUEST",
  "device_name": "Google Pixel 8",
  "client_version": 1,
  "pin": "492018",
  "client_nonce": "a7f3e829bc0192e4"
}
```

### Step 2: Server Response
If PIN matches:
```json
{
  "type": "PAIR_SUCCESS",
  "server_name": "DESKTOP-GAMING",
  "session_id": "sess_89b21f90",
  "session_token": "f4b931920acb9e84711928374a58b21c",
  "udp_port": 42100,
  "keepalive_interval_ms": 1000,
  "deadman_timeout_ms": 250
}
```
If PIN is incorrect:
```json
{
  "type": "PAIR_REJECTED",
  "reason": "INVALID_PIN",
  "attempts_remaining": 2
}
```

### Step 3: Keepalive Ping/Pong (TCP or UDP)
Client transmits periodic snapshot or ping every 500ms when idle.
Server responds with:
```json
{
  "type": "PONG",
  "server_time_ms": 1741700100200
}
```

---

## 5. Security & Safety Guarantees

### Deadman Watchdog Switch
- The Windows PC server runs an asynchronous timer for the active controller.
- If **250 milliseconds** elapse without receiving any valid, authenticated UDP packet:
  1. All buttons are immediately released (`buttons = 0`).
  2. Analog sticks are returned to center position (`stick_lx = 0, stick_ly = 0, stick_rx = 0, stick_ry = 0`).
  3. Analog triggers are released (`trigger_l = 0, trigger_r = 0`).
  4. The virtual controller driver is updated with this neutral state.

### Replay & Out-of-Order Packet Defense
- Each UDP packet carries a 32-bit sequence number.
- Any packet with `sequence <= highest_seen_sequence` is discarded without waking the virtual driver.
- Wrap-around handling: standard modular arithmetic comparison `(sequence - last_sequence) < 0x80000000`.

### Local Authentication
- Every packet includes an HMAC-SHA256 authentication tag derived from the ephemeral session secret agreed during TCP pairing.
- Stray packets from other devices on the LAN cannot hijack or inject controller inputs.

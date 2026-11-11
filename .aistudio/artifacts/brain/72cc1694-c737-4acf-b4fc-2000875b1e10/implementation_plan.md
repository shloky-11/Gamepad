# LAN Gamepad — Android to Windows Wireless Controller

Transform any Android smartphone into a low-latency, wireless physical-style Xbox 360 controller for Windows 10/11 over local Wi-Fi, featuring genuine multi-touch analog sticks, triggers, zero-cloud local discovery, and real virtual gamepad emulation.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed through Phase 1 clarification:
> - **Virtual Controller Backend**: `vgamepad` (virtual Xbox 360 controller) as the primary backend, with automatic fallback to an integrated interactive controller simulator and verification guide for `joy.cpl`.
> - **Visual Styling**: Xbox Carbon Black aesthetic with luminous accent illumination, ergonomic tactile button geometry, and landscape full-screen touch targets.
> - **PC Companion Interface**: Dual-mode presentation: high-speed async Terminal CLI with live telemetry alongside an embedded local Web Companion & Diagnostics Dashboard.

- **Confirmed Decision 1**: Dual-stack local network architecture utilizing UDP for sub-millisecond continuous input snapshots (60–120 Hz) and TCP for TLS/AES-authenticated session pairing and configuration.
- **Confirmed Decision 2**: Zero-dependency fallback so the system remains fully operational and inspectable even before Windows kernel drivers (`ViGEmBus`) are installed.
- **Open Choice / Default**: Android target SDK 34 (Android 14) with backward compatibility down to Android 8.0 (API 26), ensuring broad device compatibility.

---

## 1. Overview & Core Concept

**LAN Gamepad** solves the problem of playing PC games without requiring a physical Xbox or PlayStation controller. By establishing a direct, local Wi-Fi link between an Android device and a Windows PC, the system streams high-resolution controller states directly into Windows's native game input pipeline.

### Target Persona & Scenarios
- **PC Gamers on the Go**: Traveling or couch-gaming without carrying bulky hardware controllers.
- **Local Co-op / Extra Controller**: Instantly creating a Player 2 or Player 4 controller for party games (e.g., Overcooked, Rocket League, FIFA).
- **Steering / Precision D-Pad**: Fine-grained analog steering or twin-stick shooting directly from smartphone touchscreens.

### Key Value
- **Zero Internet Requirement**: Complete privacy and maximum responsiveness over the local Wi-Fi subnet without third-party accounts or cloud relays.
- **True Gamepad Emulation**: Recognised by Windows games as a native XInput / DirectInput device via `vgamepad`/ViGEmBus.
- **Failsafe Safety**: 250ms deadman auto-neutral reset prevents stuck inputs upon packet loss or Wi-Fi disconnection.

---

## 2. User Experience & Visual Design

### Android Gamepad Visuals (Xbox Carbon Black)
- **Palette**:
  - Base Chassis: Deep matte charcoal `#121316` and obsidian `#0A0B0D`
  - Accents: Xbox luminous green `#107C10` and `#22C55E` for active states and thumbstick highlights
  - Action Buttons: Authentic Xbox ABXY palette (A: Emerald `#22C55E`, B: Crimson `#EF4444`, X: Cobalt `#3B82F6`, Y: Amber `#F59E0B`)
- **Left Side Controls**:
  - Full 360° virtual analog stick with proportional thumb knob, deadzone visualization, and L3 click-in
  - Ergonomic 4-way D-Pad with diagonal quadrant deadzones
- **Right Side Controls**:
  - Authentic 4-button diamond ABXY cluster with generous touch radiuses ($\ge 56\text{px}$)
  - Full 360° right analog stick with R3 click-in
- **Shoulders & Triggers**:
  - Top bumpers (LB, RB) and progressive analog triggers (LT, RT) with real-time fill level indicators
- **Center Controls**:
  - View (Back), Menu (Start), and Xbox Guide / Sync button
  - Unobtrusive status bar displaying Wi-Fi RSSI, target IP, transmission rate (Hz), and round-trip ping

### PC Companion Server & Web Dashboard
- **Rich Terminal CLI**:
  - Live ASCII telemetry, listening ports, pairing PIN, connected client IP, packet rate counter, and controller backend status
- **Web Companion Dashboard**:
  - Real-time 3D/2D visualizer mirroring current gamepad inputs
  - Visual driver status indicator (`ViGEmBus installed` / `Simulator Active`)
  - One-click copy instructions for Windows Firewall rules and `joy.cpl` testing
  - Pair code QR generator for rapid phone scanning

---

## 3. Key Product Decisions & Trade-Offs

### 1. Networking: UDP Snapshot vs. TCP Event Streams
- **Chosen Approach**: Full 32-byte UDP state snapshots sent at 60–120 Hz, accompanied by a 4-byte sequence number and truncated HMAC-SHA256 session token.
- **Why**: TCP head-of-line blocking creates catastrophic latency spikes during minor Wi-Fi jitter. UDP ensures that a dropped packet is immediately superseded by the latest state with zero retry delays.
- **Alternatives Considered**: WebSockets (unnecessary protocol overhead and TCP jitter) and raw Bluetooth (complex driver pairing on Windows).

### 2. Virtual Gamepad Backend: `vgamepad` with ViGEmBus & Simulator Fallback
- **Chosen Approach**: Python `vgamepad` targeting the official Nefarius ViGEmBus driver, coupled with an automatic fallback simulator.
- **Why**: ViGEmBus remains the de-facto standard for Windows XInput controller emulation. While the driver reached feature completion, its API is rock-solid and universally recognized by modern Windows games.
- **Alternatives Considered**: VIIPER (requires complex USB/IP setup) and vJoy (lacks native XInput Xbox 360 identity).

### 3. Safety Deadman Switch
- **Chosen Approach**: 250ms hardware neutral timeout on the PC server.
- **Why**: Prevents characters from endlessly running or firing if the Android phone goes to sleep, runs out of battery, or drops network frames.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                 Android Application (Kotlin)                │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │  Touch Input Engine   │       │  NsdManager (mDNS)    │  │
│  │  - Pointer Matrix     │       │  - Auto-Discovery     │  │
│  │  - Multi-touch ABXY   │       │  - TCP Handshake      │  │
│  │  - Dual Sticks (L3/R3)│       │  - Keystore Security  │  │
│  └───────────┬───────────┘       └───────────┬───────────┘  │
│              │                               │              │
│              ▼                               ▼              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         Binary UDP Packet Encoder (32 Bytes)          │  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               │
                Local Wi-Fi UDP (Port 42100)
                Local Wi-Fi TCP (Port 42101)
                               │
┌──────────────────────────────┼──────────────────────────────┐
│                              ▼                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │        Windows Companion Server (Python 3.12+)        │  │
│  │  - Async UDP Receiver (Non-blocking asyncio)          │  │
│  │  - Sequence Validator & HMAC Session Guard            │  │
│  │  - 250ms Deadman Failsafe Watchdog                    │  │
│  └───────────┬───────────────────────────────┬───────────┘  │
│              │                               │              │
│              ▼                               ▼              │
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │ Virtual Gamepad Layer │       │ Local Management Hub  │  │
│  │ - vgamepad / ViGEm    │       │ - Rich CLI Telemetry  │  │
│  │ - Xbox 360 Controller │       │ - HTTP/WS Live Web UI │  │
│  └───────────┬───────────┘       └───────────────────────┘  │
└──────────────┼──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│     Windows OS & PC Games (XInput / joy.cpl / Steam)        │
└─────────────────────────────────────────────────────────────┘
```

### Binary UDP Protocol Specification (32-Byte Payload)

| Offset | Field | Type | Description |
|---|---|---|---|
| 0–3 | `MAGIC` | `uint32_be` | Constant `0x4C475044` (`"LGPD"`) |
| 4 | `VERSION` | `uint8` | Protocol version (`0x01`) |
| 5–7 | `RESERVED` | `uint8[3]` | Padding / future flags |
| 8–11 | `SEQUENCE` | `uint32_be` | Monotonically incrementing packet counter |
| 12–15 | `BUTTONS` | `uint32_be` | Bitmask for ABXY, Bumpers, Sticks, Back, Start |
| 16–17 | `STICK_LX` | `int16_be` | Left Stick X ($-32768$ to $+32767$) |
| 18–19 | `STICK_LY` | `int16_be` | Left Stick Y ($-32768$ to $+32767$, inverted for Y-up) |
| 20–21 | `STICK_RX` | `int16_be` | Right Stick X ($-32768$ to $+32767$) |
| 22–23 | `STICK_RY` | `int16_be` | Right Stick Y ($-32768$ to $+32767$) |
| 24 | `TRIGGER_L`| `uint8` | Left Trigger analog pressure ($0$ to $255$) |
| 25 | `TRIGGER_R`| `uint8` | Right Trigger analog pressure ($0$ to $255$) |
| 26 | `DPAD_X` | `int8` | D-pad horizontal ($-1, 0, +1$) |
| 27 | `DPAD_Y` | `int8` | D-pad vertical ($-1, 0, +1$) |
| 28–31 | `AUTH_TAG` | `uint32_be` | Truncated HMAC-SHA256 of packet using session key |

---

## 5. Deliverables & Execution Plan

1. **Protocol & Specification**:
   - `protocol/protocol-specification.md`: Full endianness, bit positions, TCP handshake, cryptographic session tokens, and timing specifications.
2. **PC Companion Server (`pc-server/`)**:
   - `main.py`: Dual-mode server launch (CLI + Web companion).
   - `protocol.py`: High-performance binary struct packing/unpacking and HMAC validation.
   - `network_server.py`: Async UDP datagram protocol and TCP pairing listener.
   - `discovery.py`: Zeroconf mDNS advertiser (`_langamepad._tcp.local.`).
   - `gamepad_backend.py`: Abstract `GamepadBackend` interface with `VgamepadBackend` and `SimulatorBackend`.
   - `pairing.py`: Secure 6-digit PIN and crypto session key generator.
   - `requirements.txt` & `start_server.bat`: Single-click launch and dependency installer.
3. **Android Application (`android-app/`)**:
   - Full Gradle project (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, wrapper).
   - `AndroidManifest.xml` with zero dangerous permissions (only `INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`).
   - `GamepadTouchSurface.kt`: Zero-allocation multi-touch gesture matrix supporting simultaneous triggers, dual joysticks, and ABXY chords.
   - `UdpClient.kt` & `TcpPairingClient.kt`: Non-blocking coroutine-based networking.
   - `DiscoveryManager.kt`: Native `NsdManager` listener.
   - `SettingsDataStore.kt`: Deadzone, sensitivity, and layout preferences.
4. **Interactive Web Companion & Testing Suite**:
   - Interactive controller simulator, live packet inspector, driver installation assistant, and joy.cpl verification guide.
5. **Documentation & Tests**:
   - Beginner-friendly `README.md` with step-by-step installation, firewall configuration, and troubleshooting.
   - Automated test suite verifying packet packing/unpacking parity between Kotlin and Python.

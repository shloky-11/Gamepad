# LAN Gamepad — Android to Windows Wireless Controller

Transform your Android smartphone into a low-latency, wireless physical-style Xbox 360 controller for Windows 10 & 11 over local Wi-Fi.

No cloud servers, no account registration, zero internet connection required.

---

## 🎮 Features

- **Genuine Virtual Xbox 360 Controller**: Exposes a real XInput device to Windows via `vgamepad`/ViGEmBus. Plays seamlessly in Steam, Epic Games, Xbox Game Pass, and standalone PC games.
- **Ultra Low-Latency UDP Streaming**: Streams 32-byte binary controller snapshots at 60 Hz to 120 Hz with sub-millisecond serialization.
- **Genuine Multi-Touch Matrix**: Hold the left stick while pressing ABXY, pull both analog triggers together, and aim with twin joysticks with zero pointer interference or stuck buttons.
- **Automatic mDNS Discovery & Manual IP Fallback**: Android auto-discovers your PC name on the local subnet via `NsdManager`.
- **Cryptographic Security**: 6-digit random PIN and ephemeral HMAC-SHA256 session token prevent unauthorized network packet injection.
- **250ms Deadman Switch Watchdog**: Automatically releases every button and centers sticks if Wi-Fi drops or the phone goes to sleep, preventing stuck character actions.
- **Rich Terminal CLI & Embedded Web Companion**: Monitor live input rates, inspect virtual controller status, and test gamepad axes from any browser at `http://localhost:42102`.

---

## 📁 Repository Structure

```
LAN-Gamepad/
├── android-app/                       # Native Android Project (Kotlin & Jetpack Compose)
│   ├── app/
│   │   ├── build.gradle.kts           # Compose, Material3, DataStore, Coroutines
│   │   └── src/
│   │       ├── main/
│   │       │   ├── AndroidManifest.xml
│   │       │   └── java/com/langamepad/
│   │       │       ├── MainActivity.kt
│   │       │       ├── model/         # Protocol constants & GamepadState
│   │       │       ├── network/       # UdpTransmitter, TcpClient, mDNS
│   │       │       ├── data/          # Settings repository & Keystore
│   │       │       └── ui/            # Joysticks, D-Pad, ABXY, Triggers, Theme
│   │       └── test/                  # Packet encoding unit tests
│   ├── gradle/wrapper/
│   ├── gradlew & gradlew.bat
│   └── settings.gradle.kts
│
├── pc-server/                         # Windows Companion Server (Python 3.10+)
│   ├── main.py                        # Entry point (CLI + Web companion)
│   ├── network_server.py              # Async UDP receiver + TCP pairing
│   ├── discovery.py                   # Zeroconf mDNS broadcaster
│   ├── pairing.py                     # PIN generation & session security
│   ├── protocol.py                    # 32-byte binary packet packing/unpacking
│   ├── controller_state.py            # Controller state representation
│   ├── gamepad_backend.py             # vgamepad Xbox 360 + Simulator backend
│   ├── web_dashboard.py               # Embedded live browser dashboard
│   ├── config.py                      # Port and timeout definitions
│   ├── requirements.txt               # vgamepad, zeroconf
│   ├── start_server.bat               # Single-click Windows startup script
│   ├── test_protocol.py               # Protocol unit tests
│   └── test_backend.py                # Backend unit tests
│
├── protocol/
│   └── protocol-specification.md      # Full 32-byte binary protocol spec
└── README.md
```

---

## 🚀 Quick Start Guide

### Step 1: Install & Launch Windows PC Server

#### 1. Prerequisites
- **Windows 10 or Windows 11 (64-bit)**
- **Python 3.10 or newer**: Download from [python.org](https://www.python.org/downloads/).
  > ⚠️ **Important during Python installation**: Check the box **"Add python.exe to PATH"** before clicking Install.

#### 2. Install the Official Virtual Gamepad Driver (ViGEmBus)
Windows requires the open-source **Nefarius ViGEmBus** kernel driver to allow virtual Xbox 360 controllers to be registered:
1. Download the latest installer `ViGEmBus_Setup_x64.exe` from the official repository:
   👉 **[Download ViGEmBus Releases (GitHub)](https://github.com/nefarius/ViGEmBus/releases/latest)**
2. Run the installer and finish setup (requires Administrator permission).
3. Restart your PC if prompted.

*(Note: If you run without ViGEmBus, the server will automatically activate **Simulator Mode**, allowing you to test network streaming, PIN pairing, and inspect live inputs without crashing).*

#### 3. Allow Server Through Windows Firewall (Private Network)
Run this command once in **Command Prompt as Administrator** or allow Python when prompted by Windows:
```cmd
netsh advfirewall firewall add rule name="LAN Gamepad Server" dir=in action=allow protocol=UDP localport=42100 profile=private
netsh advfirewall firewall add rule name="LAN Gamepad Pairing" dir=in action=allow protocol=TCP localport=42101 profile=private
```

#### 4. Start the Server
Navigate to the `pc-server` folder and double-click `start_server.bat`, or run from terminal:
```bash
cd pc-server
pip install -r requirements.txt
python main.py
```

The terminal will display:
```
================================================================
           LAN GAMEPAD SERVER — WINDOWS CONTROLLER HUB           
================================================================
 Status:             RUNNING
 Local Wi-Fi IP:     192.168.1.145
 TCP Pairing Port:   42101
 UDP Input Port:     42100
 Web Companion:      http://localhost:42102
----------------------------------------------------------------
 Pairing PIN:        >>> 492018 <<< (Enter on phone app)
 Connected Device:   None
 Controller Backend: vgamepad (Virtual Xbox 360 Controller via ViGEmBus)
----------------------------------------------------------------
 [*] SUCCESS: Virtual Xbox 360 controller exposed to Windows!
================================================================
```

---

### Step 2: Build & Install Android Application

#### Option A: Build with Android Studio (Recommended)
1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer).
2. Open Android Studio and choose **Open an Existing Project**.
3. Select the `android-app` folder.
4. Connect your Android smartphone via USB (enable **USB Debugging** in Developer Options).
5. Click the green **Run (▶)** button. The APK will be built and installed automatically.

#### Option B: Build via Command Line
If you have the Android SDK or command line tools installed:
```bash
cd android-app
# On Windows:
gradlew.bat assembleDebug

# On macOS / Linux:
./gradlew assembleDebug
```
The generated APK file will be located at:
```
android-app/app/build/outputs/apk/debug/app-debug.apk
```
Copy `app-debug.apk` to your phone and tap it to install.

---

### Step 3: Connect & Play

1. Make sure both your **Android smartphone** and **Windows PC** are connected to the **same Wi-Fi network**.
2. Launch **LAN Gamepad** on your smartphone.
3. Your PC should appear under **Discovered PC Servers**. Tap **Connect**.
   *(If not shown automatically, enter the PC IP displayed in the server terminal under Manual Connection).*
4. Enter the **6-digit Pairing PIN** shown on the PC screen (e.g., `492018`) and tap **Confirm & Connect**.
5. The full-screen Xbox controller layout opens immediately!

---

## 🎯 Verification & Testing

### 1. Windows Game Controllers Panel (`joy.cpl`)
1. On your Windows PC, press <kbd>Win</kbd> + <kbd>R</kbd>.
2. Type `joy.cpl` and press <kbd>Enter</kbd>.
3. In the list, you will see:
   `Controller (Xbox 360 For Windows)` with status **OK**.
4. Double-click it (or click **Properties**).
5. Move the joysticks, tap ABXY, pull LT/RT on your phone. You will see the crosshair axes, red numbered buttons, and Z-axis sliders move in real time!

### 2. Testing in Games
Launch any PC game with controller support (e.g. *Rocket League*, *Forza Horizon*, *Hollow Knight*, *Steam Big Picture*). The game will instantly detect an Xbox 360 controller plugged into Player 1!

---

## 🛠️ Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| **Android cannot find PC automatically** | Multicast mDNS is filtered by your Wi-Fi router (common in guest networks) | Use **Manual Connection**: Enter the PC's local IP address (e.g. `192.168.1.x`) and port `42101`. |
| **Connection timed out / Refused** | Windows Firewall is blocking inbound connections | Run the firewall commands in Step 1.3 or set your Wi-Fi network profile to **Private** in Windows Settings. |
| **"Running in Simulator Mode" warning** | `vgamepad` package or ViGEmBus kernel driver not installed | Install the [ViGEmBus driver](https://github.com/nefarius/ViGEmBus/releases/latest) and run `pip install vgamepad`. |
| **Slight input delay** | High Wi-Fi congestion on 2.4 GHz band | Connect both PC and smartphone to the **5 GHz Wi-Fi band**, or connect the PC via Ethernet cable to the router. |
| **Inputs stop responding after minutes** | Phone battery saver put app to sleep | The app requests `WAKE_LOCK`, but you can also exclude LAN Gamepad from Android Battery Optimization in Android Settings. |

---

## 📄 License
Apache-2.0 License. Free for personal and commercial gaming use.

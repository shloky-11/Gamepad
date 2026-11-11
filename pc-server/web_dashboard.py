"""
Embedded Web Companion Dashboard for LAN Gamepad Server.
Zero extra dependencies — uses Python standard library asyncio HTTP server.
Serves a responsive live dashboard to visualize controller inputs, telemetry, and pairing QR codes.
"""
import asyncio
import json
import logging
from typing import Dict, Any, Callable
from controller_state import ControllerState
from gamepad_backend import GamepadBackend
from pairing import PairingManager
from network_server import ServerStats

logger = logging.getLogger("LAN_Gamepad.WebDashboard")

HTML_DASHBOARD = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>LAN Gamepad — PC Server Companion</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #090a0c;
      --card: #121418;
      --border: #1e2229;
      --xbox-green: #107C10;
      --xbox-glow: #22c55e;
      --text: #f3f4f6;
      --muted: #9ca3af;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: var(--bg);
      color: var(--text);
      font-family: 'Plus Jakarta Sans', sans-serif;
      padding: 24px;
      min-height: 100vh;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--border);
      margin-bottom: 24px;
    }
    .brand {
      font-size: 20px;
      font-weight: 800;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .brand-badge {
      background: var(--xbox-green);
      color: white;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-family: 'JetBrains Mono', monospace;
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .card {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 20px;
    }
    .card-title {
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--muted);
      margin-bottom: 8px;
    }
    .metric-val {
      font-size: 28px;
      font-weight: 700;
      font-family: 'JetBrains Mono', monospace;
      color: #fff;
    }
    .pin-display {
      font-size: 36px;
      font-weight: 800;
      letter-spacing: 6px;
      color: var(--xbox-glow);
      font-family: 'JetBrains Mono', monospace;
    }
    .controller-view {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 16px;
      padding: 28px;
      margin-top: 16px;
    }
    .sticks-row {
      display: flex;
      justify-content: space-around;
      align-items: center;
      flex-wrap: wrap;
      gap: 24px;
      margin-top: 20px;
    }
    .stick-box {
      width: 140px;
      height: 140px;
      border-radius: 50%;
      background: #181b22;
      border: 2px solid var(--border);
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .stick-thumb {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: #232733;
      border: 2px solid var(--xbox-green);
      position: absolute;
      transition: transform 0.04s ease-out;
      box-shadow: 0 0 12px rgba(16,124,16,0.3);
    }
    .btn-cluster {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-top: 12px;
    }
    .btn-pill {
      padding: 6px 14px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      background: #1c202a;
      border: 1px solid var(--border);
      color: var(--muted);
      transition: all 0.1s;
    }
    .btn-pill.active {
      background: var(--xbox-green);
      color: white;
      border-color: var(--xbox-glow);
      box-shadow: 0 0 10px rgba(34,197,94,0.4);
    }
    .btn-pill.btn-a.active { background: #16a34a; }
    .btn-pill.btn-b.active { background: #dc2626; }
    .btn-pill.btn-x.active { background: #2563eb; }
    .btn-pill.btn-y.active { background: #d97706; }
    .status-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--xbox-glow);
      margin-right: 6px;
    }
    .guide-banner {
      background: #141b16;
      border: 1px solid #1c3621;
      padding: 16px;
      border-radius: 8px;
      font-size: 13px;
      line-height: 1.6;
      color: #86efac;
      margin-top: 20px;
    }
  </style>
</head>
<body>
  <div class="header">
    <div class="brand">
      <span class="status-dot"></span> LAN Gamepad Server
      <span class="brand-badge">PORT 42100</span>
    </div>
    <div style="font-family:'JetBrains Mono'; font-size:13px; color:var(--muted);" id="uptime-text">Live Polling</div>
  </div>

  <div class="grid">
    <div class="card">
      <div class="card-title">Pairing PIN (Enter on Phone)</div>
      <div class="pin-display" id="pin-val">------</div>
      <div style="font-size:12px; color:var(--muted); margin-top:8px;">Valid for private Wi-Fi connection</div>
    </div>
    <div class="card">
      <div class="card-title">Input Transmission Rate</div>
      <div class="metric-val" id="hz-val">0.0 <span style="font-size:16px; color:var(--muted);">Hz</span></div>
      <div style="font-size:12px; color:var(--muted); margin-top:8px;" id="packets-text">0 total packets received</div>
    </div>
    <div class="card">
      <div class="card-title">Connected Smartphone</div>
      <div class="metric-val" style="font-size:20px;" id="device-val">Searching...</div>
      <div style="font-size:12px; color:var(--muted); margin-top:8px;" id="ip-val">IP: Waiting for device</div>
    </div>
    <div class="card">
      <div class="card-title">Windows Gamepad Backend</div>
      <div class="metric-val" style="font-size:16px; color:#4ade80;" id="backend-val">Loading...</div>
      <div style="font-size:12px; color:var(--muted); margin-top:8px;">Native DirectInput / XInput</div>
    </div>
  </div>

  <div class="controller-view">
    <h3 style="font-size:16px; margin-bottom:12px;">Live Controller Telemetry</h3>
    <div class="btn-cluster">
      <div class="btn-pill btn-a" id="btn-a">A</div>
      <div class="btn-pill btn-b" id="btn-b">B</div>
      <div class="btn-pill btn-x" id="btn-x">X</div>
      <div class="btn-pill btn-y" id="btn-y">Y</div>
      <div class="btn-pill" id="btn-lb">LB</div>
      <div class="btn-pill" id="btn-rb">RB</div>
      <div class="btn-pill" id="btn-back">BACK</div>
      <div class="btn-pill" id="btn-start">START</div>
      <div class="btn-pill" id="btn-l3">L3 (Left Stick)</div>
      <div class="btn-pill" id="btn-r3">R3 (Right Stick)</div>
      <div class="btn-pill" id="dpad-up">DPAD UP</div>
      <div class="btn-pill" id="dpad-down">DPAD DOWN</div>
      <div class="btn-pill" id="dpad-left">DPAD LEFT</div>
      <div class="btn-pill" id="dpad-right">DPAD RIGHT</div>
    </div>

    <div class="sticks-row">
      <div style="text-align:center;">
        <div style="font-size:12px; color:var(--muted); margin-bottom:8px;">Left Analog Stick (L3)</div>
        <div class="stick-box">
          <div class="stick-thumb" id="stick-l-thumb"></div>
        </div>
        <div style="font-family:'JetBrains Mono'; font-size:11px; margin-top:8px; color:var(--muted);" id="stick-l-coords">X:0 Y:0</div>
      </div>
      <div style="text-align:center;">
        <div style="font-size:12px; color:var(--muted); margin-bottom:8px;">Left Trigger (LT)</div>
        <div style="width:120px; height:12px; background:#181b22; border-radius:6px; overflow:hidden; border:1px solid var(--border); margin:12px auto;">
          <div id="trigger-l-bar" style="width:0%; height:100%; background:var(--xbox-glow); transition:width 0.05s;"></div>
        </div>
        <div style="font-family:'JetBrains Mono'; font-size:11px; color:var(--muted);" id="trigger-l-val">0 / 255</div>
      </div>
      <div style="text-align:center;">
        <div style="font-size:12px; color:var(--muted); margin-bottom:8px;">Right Trigger (RT)</div>
        <div style="width:120px; height:12px; background:#181b22; border-radius:6px; overflow:hidden; border:1px solid var(--border); margin:12px auto;">
          <div id="trigger-r-bar" style="width:0%; height:100%; background:var(--xbox-glow); transition:width 0.05s;"></div>
        </div>
        <div style="font-family:'JetBrains Mono'; font-size:11px; color:var(--muted);" id="trigger-r-val">0 / 255</div>
      </div>
      <div style="text-align:center;">
        <div style="font-size:12px; color:var(--muted); margin-bottom:8px;">Right Analog Stick (R3)</div>
        <div class="stick-box">
          <div class="stick-thumb" id="stick-r-thumb"></div>
        </div>
        <div style="font-family:'JetBrains Mono'; font-size:11px; margin-top:8px; color:var(--muted);" id="stick-r-coords">X:0 Y:0</div>
      </div>
    </div>
  </div>

  <div class="guide-banner">
    <strong>Windows Verification Instructions:</strong> Press <kbd style="background:#222; padding:2px 6px; border-radius:4px;">Win + R</kbd>, type <code>joy.cpl</code> and press Enter. You should see <em>"Controller (Xbox 360 For Windows)"</em> listed under Game Controllers. Click <strong>Properties</strong> to verify all axes and buttons respond to your smartphone touches.
  </div>

  <script>
    async function updateStatus() {
      try {
        const res = await fetch('/api/status');
        if (!res.ok) return;
        const d = await res.json();
        
        document.getElementById('pin-val').textContent = d.pin || '------';
        document.getElementById('hz-val').innerHTML = `${(d.stats.current_packet_rate || 0).toFixed(1)} <span style="font-size:16px; color:var(--muted);">Hz</span>`;
        document.getElementById('packets-text').textContent = `${d.stats.packets_received} packets received (${d.stats.packets_dropped_stale} stale dropped)`;
        document.getElementById('device-val').textContent = d.stats.connected_device || 'Waiting for phone...';
        document.getElementById('ip-val').textContent = `IP: ${d.stats.connected_ip || 'None'}`;
        document.getElementById('backend-val').textContent = d.backend_name;

        // Controller buttons
        const s = d.controller_state;
        const toggle = (id, active) => {
          const el = document.getElementById(id);
          if (el) el.classList.toggle('active', !!active);
        };
        toggle('btn-a', s.btn_a);
        toggle('btn-b', s.btn_b);
        toggle('btn-x', s.btn_x);
        toggle('btn-y', s.btn_y);
        toggle('btn-lb', s.btn_lb);
        toggle('btn-rb', s.btn_rb);
        toggle('btn-back', s.btn_back);
        toggle('btn-start', s.btn_start);
        toggle('btn-l3', s.btn_l3);
        toggle('btn-r3', s.btn_r3);
        toggle('dpad-up', s.dpad_up);
        toggle('dpad-down', s.dpad_down);
        toggle('dpad-left', s.dpad_left);
        toggle('dpad-right', s.dpad_right);

        // Joysticks (-32768 to 32767 mapped to -45px to 45px)
        const lxPx = (s.stick_lx / 32767) * 45;
        const lyPx = (-s.stick_ly / 32767) * 45; // Invert Y for screen coords
        document.getElementById('stick-l-thumb').style.transform = `translate(${lxPx}px, ${lyPx}px)`;
        document.getElementById('stick-l-coords').textContent = `X:${s.stick_lx} Y:${s.stick_ly}`;

        const rxPx = (s.stick_rx / 32767) * 45;
        const ryPx = (-s.stick_ry / 32767) * 45;
        document.getElementById('stick-r-thumb').style.transform = `translate(${rxPx}px, ${ryPx}px)`;
        document.getElementById('stick-r-coords').textContent = `X:${s.stick_rx} Y:${s.stick_ry}`;

        // Triggers (0-255)
        const tlPct = Math.min(100, Math.round((s.trigger_l / 255) * 100));
        const trPct = Math.min(100, Math.round((s.trigger_r / 255) * 100));
        document.getElementById('trigger-l-bar').style.width = tlPct + '%';
        document.getElementById('trigger-l-val').textContent = `${s.trigger_l} / 255`;
        document.getElementById('trigger-r-bar').style.width = trPct + '%';
        document.getElementById('trigger-r-val').textContent = `${s.trigger_r} / 255`;
      } catch (e) {
        console.error(e);
      }
    }
    setInterval(updateStatus, 80);
    updateStatus();
  </script>
</body>
</html>
"""


class WebCompanionServer:
    def __init__(
        self,
        get_status_callback: Callable[[], Dict[str, Any]],
        port: int = 42102
    ):
        self.get_status_callback = get_status_callback
        self.port = port
        self._server = None

    async def start(self) -> None:
        self._server = await asyncio.start_server(
            self._handle_http_client, "0.0.0.0", self.port
        )
        logger.info(f"Web Companion Dashboard running at http://localhost:{self.port}")

    async def _handle_http_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        try:
            request_line = await reader.readline()
            if not request_line:
                return

            req_str = request_line.decode("utf-8").strip()
            parts = req_str.split()
            if len(parts) < 2:
                return

            method, path = parts[0], parts[1]

            # Drain headers
            while True:
                line = await reader.readline()
                if not line or line == b"\r\n" or line == b"\n":
                    break

            if path == "/" or path.startswith("/index"):
                body = HTML_DASHBOARD.encode("utf-8")
                header = (
                    b"HTTP/1.1 200 OK\r\n"
                    b"Content-Type: text/html; charset=utf-8\r\n"
                    b"Content-Length: " + str(len(body)).encode("ascii") + b"\r\n"
                    b"Connection: close\r\n\r\n"
                )
                writer.write(header + body)
                await writer.drain()

            elif path == "/api/status":
                status_data = self.get_status_callback()
                body = json.dumps(status_data).encode("utf-8")
                header = (
                    b"HTTP/1.1 200 OK\r\n"
                    b"Content-Type: application/json\r\n"
                    b"Access-Control-Allow-Origin: *\r\n"
                    b"Content-Length: " + str(len(body)).encode("ascii") + b"\r\n"
                    b"Connection: close\r\n\r\n"
                )
                writer.write(header + body)
                await writer.drain()

            else:
                writer.write(b"HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n")
                await writer.drain()

        except Exception as e:
            logger.debug(f"HTTP handler error: {e}")
        finally:
            writer.close()
            try:
                await writer.wait_closed()
            except Exception:
                pass

    async def stop(self) -> None:
        if self._server:
            self._server.close()
            await self._server.wait_closed()

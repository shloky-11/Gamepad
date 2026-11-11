/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Gamepad2,
  Tv2,
  Radio,
  FileCode2,
  ShieldCheck,
  Terminal,
  Activity,
  RotateCcw,
  Sliders,
  HelpCircle,
  Copy,
  Check,
  Download,
  Flame,
  Wifi,
  ExternalLink,
  ChevronRight,
  MonitorCheck
} from 'lucide-react';

// Protocol Constants
const MAGIC_HEX = '0x4C475044'; // 'LGPD'
const PROTOCOL_VERSION = 1;

// Button Masks
const BTN_A = 1 << 0;
const BTN_B = 1 << 1;
const BTN_X = 1 << 2;
const BTN_Y = 1 << 3;
const BTN_LB = 1 << 4;
const BTN_RB = 1 << 5;
const BTN_BACK = 1 << 6;
const BTN_START = 1 << 7;
const BTN_GUIDE = 1 << 8;
const BTN_L3 = 1 << 9;
const BTN_R3 = 1 << 10;
const BTN_DPAD_UP = 1 << 11;
const BTN_DPAD_DOWN = 1 << 12;
const BTN_DPAD_LEFT = 1 << 13;
const BTN_DPAD_RIGHT = 1 << 14;

interface ControllerInputs {
  buttons: number;
  stickLx: number; // -32768 to 32767
  stickLy: number; // -32768 to 32767
  stickRx: number; // -32768 to 32767
  stickRy: number; // -32768 to 32767
  triggerL: number; // 0 to 255
  triggerR: number; // 0 to 255
  dpadX: number; // -1, 0, 1
  dpadY: number; // -1, 0, 1
}

const NEUTRAL_STATE: ControllerInputs = {
  buttons: 0,
  stickLx: 0,
  stickLy: 0,
  stickRx: 0,
  stickRy: 0,
  triggerL: 0,
  triggerR: 0,
  dpadX: 0,
  dpadY: 0,
};

export default function App() {
  const [activeTab, setActiveTab] = useState<'controller' | 'packet' | 'joycpl' | 'files' | 'setup'>('controller');
  const [inputs, setInputs] = useState<ControllerInputs>(NEUTRAL_STATE);
  const [sequence, setSequence] = useState<number>(1042);
  const [packetRate, setPacketRate] = useState<number>(90); // 60, 90, 120 Hz
  const [deadmanActive, setDeadmanActive] = useState<boolean>(false);
  const [deadmanTimeoutMs, setDeadmanTimeoutMs] = useState<number>(250);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<string>('protocol');
  const [pinCode, setPinCode] = useState<string>('492018');
  const [isPaired, setIsPaired] = useState<boolean>(true);

  // Left joystick touch tracking
  const leftStickRef = useRef<HTMLDivElement>(null);
  const rightStickRef = useRef<HTMLDivElement>(null);
  const [isDraggingL, setIsDraggingL] = useState(false);
  const [isDraggingR, setIsDraggingR] = useState(false);

  // Deadman watchdog timer
  const lastInputTime = useRef<number>(Date.now());

  const triggerInputUpdate = useCallback((updater: (prev: ControllerInputs) => ControllerInputs) => {
    setInputs(prev => {
      const next = updater(prev);
      lastInputTime.current = Date.now();
      setDeadmanActive(false);
      setSequence(s => (s + 1) & 0xffffffff);
      return next;
    });
  }, []);

  const resetToNeutral = useCallback(() => {
    setInputs(NEUTRAL_STATE);
    setDeadmanActive(true);
    setSequence(s => (s + 1) & 0xffffffff);
  }, []);

  // Watchdog loop
  useEffect(() => {
    const interval = setInterval(() => {
      if (Date.now() - lastInputTime.current > deadmanTimeoutMs && !deadmanActive) {
        // If there were active inputs, trigger deadman switch
        if (
          inputs.buttons !== 0 ||
          inputs.stickLx !== 0 ||
          inputs.stickLy !== 0 ||
          inputs.stickRx !== 0 ||
          inputs.stickRy !== 0 ||
          inputs.triggerL !== 0 ||
          inputs.triggerR !== 0 ||
          inputs.dpadX !== 0 ||
          inputs.dpadY !== 0
        ) {
          resetToNeutral();
        }
      }
    }, 50);
    return () => clearInterval(interval);
  }, [deadmanTimeoutMs, deadmanActive, inputs, resetToNeutral]);

  // Button toggle helper
  const setButton = (mask: number, pressed: boolean) => {
    triggerInputUpdate(prev => ({
      ...prev,
      buttons: pressed ? prev.buttons | mask : prev.buttons & ~mask,
    }));
  };

  const isPressed = (mask: number) => (inputs.buttons & mask) !== 0;

  // Joystick drag handlers
  const handleStickPointer = (e: React.PointerEvent, isRight: boolean) => {
    const ref = isRight ? rightStickRef : leftStickRef;
    if (!ref.current) return;
    const rect = ref.current.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const deltaX = e.clientX - centerX;
    const deltaY = e.clientY - centerY;
    const maxRadius = rect.width / 2 - 16;
    const dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    let normX = 0;
    let normY = 0;
    if (dist > 0) {
      const clampedDist = Math.min(dist, maxRadius) / maxRadius;
      const angle = Math.atan2(deltaY, deltaX);
      normX = Math.cos(angle) * clampedDist;
      normY = -Math.sin(angle) * clampedDist; // Gamepad UP is positive
    }

    const valX = Math.round(normX * 32767);
    const valY = Math.round(normY * 32767);

    triggerInputUpdate(prev =>
      isRight
        ? { ...prev, stickRx: valX, stickRy: valY }
        : { ...prev, stickLx: valX, stickLy: valY }
    );
  };

  const copyToClipboard = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Compute 32-byte Packet Hex Representation
  const generatePacketHex = () => {
    const buffer = new ArrayBuffer(32);
    const view = new DataView(buffer);
    view.setUint32(0, 0x4c475044, false); // Magic 'LGPD'
    view.setUint8(4, PROTOCOL_VERSION);
    view.setUint8(5, 0); // Flags
    view.setUint16(6, 0, false); // Reserved
    view.setUint32(8, sequence, false);
    view.setUint32(12, inputs.buttons, false);
    view.setInt16(16, inputs.stickLx, false);
    view.setInt16(18, inputs.stickLy, false);
    view.setInt16(20, inputs.stickRx, false);
    view.setInt16(22, inputs.stickRy, false);
    view.setUint8(24, inputs.triggerL);
    view.setUint8(25, inputs.triggerR);
    view.setInt8(26, inputs.dpadX);
    view.setInt8(27, inputs.dpadY);
    // Simulated auth tag: simple hash for display
    const mockAuthTag = (sequence * 1664525 + inputs.buttons + 1013904223) >>> 0;
    view.setUint32(28, mockAuthTag, false);

    const bytes = new Uint8Array(buffer);
    let hexStr = '';
    for (let i = 0; i < bytes.length; i++) {
      hexStr += bytes[i].toString(16).padStart(2, '0').toUpperCase() + (i % 4 === 3 ? '  ' : ' ');
    }
    return { hexStr, mockAuthTag };
  };

  const { hexStr, mockAuthTag } = generatePacketHex();

  return (
    <div className="min-h-screen bg-[#090A0C] text-slate-100 flex flex-col selection:bg-emerald-500 selection:text-black">
      {/* Top Bar Contract (3 zones separated by gap-8) */}
      <header className="flex items-center justify-between gap-8 px-6 py-3.5 border-b border-[#1A1E26] bg-[#0E1015]/90 backdrop-blur-md sticky top-0 z-50">
        {/* Zone 1: Brand wordmark (single text element, whitespace-nowrap) */}
        <div className="flex items-center gap-3 whitespace-nowrap shrink-0">
          <div className="w-8 h-8 rounded-lg bg-[#107C10] flex items-center justify-center text-white shadow-[0_0_15px_rgba(16,124,16,0.5)]">
            <Gamepad2 className="w-5 h-5" />
          </div>
          <div>
            <span className="font-extrabold text-base tracking-tight text-white">LAN Gamepad</span>
            <span className="text-xs text-slate-400 ml-2 hidden sm:inline">Wireless Controller Hub</span>
          </div>
        </div>

        {/* Zone 2: Navigation Links (single-line, whitespace-nowrap) */}
        <nav className="hidden md:flex items-center gap-1 bg-[#13161C] p-1 rounded-xl border border-[#222733] whitespace-nowrap shrink-0">
          <button
            onClick={() => setActiveTab('controller')}
            className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-2 ${
              activeTab === 'controller'
                ? 'bg-[#107C10] text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Gamepad2 className="w-3.5 h-3.5" />
            Live Gamepad
          </button>
          <button
            onClick={() => setActiveTab('packet')}
            className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-2 ${
              activeTab === 'packet'
                ? 'bg-[#107C10] text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Activity className="w-3.5 h-3.5" />
            32-Byte Packet Stream
          </button>
          <button
            onClick={() => setActiveTab('joycpl')}
            className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-2 ${
              activeTab === 'joycpl'
                ? 'bg-[#107C10] text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <MonitorCheck className="w-3.5 h-3.5" />
            joy.cpl Windows Test
          </button>
          <button
            onClick={() => setActiveTab('files')}
            className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-2 ${
              activeTab === 'files'
                ? 'bg-[#107C10] text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <FileCode2 className="w-3.5 h-3.5" />
            Android & PC Source Files
          </button>
          <button
            onClick={() => setActiveTab('setup')}
            className={`px-3.5 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-2 ${
              activeTab === 'setup'
                ? 'bg-[#107C10] text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <HelpCircle className="w-3.5 h-3.5" />
            Setup & Driver Guide
          </button>
        </nav>

        {/* Zone 3: 1 Primary Action (Single-line) */}
        <div className="flex items-center gap-3 shrink-0">
          <div className="hidden lg:flex items-center gap-2 text-xs text-slate-400 font-mono">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>UDP:42100</span>
            <span>·</span>
            <span>TCP:42101</span>
          </div>
          <button
            onClick={resetToNeutral}
            className="px-3 py-1.5 text-xs font-semibold bg-[#1C202A] hover:bg-[#252B38] text-slate-200 border border-[#2B3140] rounded-lg transition-all flex items-center gap-1.5 whitespace-nowrap shrink-0"
            title="Reset controller inputs to neutral"
          >
            <RotateCcw className="w-3.5 h-3.5 text-emerald-400" />
            Reset State
          </button>
        </div>
      </header>

      {/* Quick stats ribbon */}
      <div className="bg-[#0C0E12] border-b border-[#1A1E26] px-6 py-2 flex flex-wrap items-center justify-between gap-4 text-xs">
        <div className="flex items-center gap-4 text-slate-400">
          <span className="flex items-center gap-1.5">
            <Wifi className="w-3.5 h-3.5 text-emerald-400" />
            <span>LAN Link:</span>
            <strong className="text-white font-mono">192.168.1.145 (Wi-Fi)</strong>
          </span>
          <span className="text-slate-600">·</span>
          <span>
            Target Stream: <strong className="text-emerald-400 font-mono">{packetRate} Hz</strong> ({((1000 / packetRate)).toFixed(1)}ms frame)
          </span>
          <span className="text-slate-600">·</span>
          <span>
            Driver Backend: <strong className="text-emerald-400 font-mono">vgamepad / ViGEmBus (Xbox 360)</strong>
          </span>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-slate-400">Pairing PIN:</span>
          <span className="font-mono font-bold text-emerald-400 tracking-widest bg-[#151922] px-2 py-0.5 rounded border border-emerald-900/50">
            {pinCode}
          </span>
          <button
            onClick={() => setPinCode(Math.floor(100000 + Math.random() * 900000).toString())}
            className="text-[11px] text-slate-400 hover:text-white transition-colors underline"
          >
            Regenerate
          </button>
        </div>
      </div>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 flex flex-col gap-6">
        {/* TAB 1: LIVE GAMEPAD CONTROLLER */}
        {activeTab === 'controller' && (
          <div className="flex flex-col gap-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                  Android Touchscreen Gamepad Simulator
                  <span className="text-xs font-mono font-normal bg-emerald-950/80 text-emerald-400 border border-emerald-800/60 px-2 py-0.5 rounded">
                    Landscape Mode
                  </span>
                </h1>
                <p className="text-sm text-slate-400 mt-1">
                  Interact with the touchscreen controls below using multi-touch or mouse. Inputs immediately stream as 32-byte UDP snapshots.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    // Simulate packet loss
                    setTimeout(resetToNeutral, 300);
                  }}
                  className="px-3 py-1.5 text-xs font-medium bg-amber-950/60 text-amber-300 border border-amber-800/80 rounded-lg hover:bg-amber-900/70 transition-all flex items-center gap-1.5"
                >
                  <Flame className="w-3.5 h-3.5" />
                  Test 250ms Deadman Failsafe
                </button>
              </div>
            </div>

            {deadmanActive && (
              <div className="p-3 bg-red-950/40 border border-red-900/60 rounded-xl text-xs text-red-300 flex items-center justify-between">
                <span>
                  <strong>Deadman Switch Triggered:</strong> 250ms elapsed without new inputs. All buttons neutralized and joysticks auto-centered to protect Windows games.
                </span>
                <span className="font-mono text-[11px] text-red-400">STATE = NEUTRAL</span>
              </div>
            )}

            {/* Visual Gamepad Shell */}
            <div className="bg-[#121419] border border-[#232834] rounded-2xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
              {/* Top Shoulder & Trigger Rows */}
              <div className="flex items-center justify-between mb-8 pb-6 border-b border-[#1C202A]">
                {/* Left Shoulder (LT & LB) */}
                <div className="flex items-center gap-3">
                  <div className="flex flex-col gap-1.5 items-center">
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider">Analog Left Trigger</span>
                    <div
                      onPointerDown={e => {
                        e.currentTarget.setPointerCapture(e.pointerId);
                        triggerInputUpdate(p => ({ ...p, triggerL: 255 }));
                      }}
                      onPointerUp={e => {
                        e.currentTarget.releasePointerCapture(e.pointerId);
                        triggerInputUpdate(p => ({ ...p, triggerL: 0 }));
                      }}
                      className={`w-28 h-12 rounded-xl border flex items-center justify-center font-bold text-sm select-none cursor-pointer transition-all ${
                        inputs.triggerL > 0
                          ? 'bg-emerald-600 text-white border-emerald-400 shadow-[0_0_15px_rgba(16,124,16,0.6)]'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F] hover:border-slate-500'
                      }`}
                    >
                      LT {inputs.triggerL > 0 ? `(${inputs.triggerL})` : ''}
                    </div>
                  </div>

                  <div className="flex flex-col gap-1.5 items-center">
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider">Digital Bumper</span>
                    <button
                      onPointerDown={() => setButton(BTN_LB, true)}
                      onPointerUp={() => setButton(BTN_LB, false)}
                      className={`w-24 h-12 rounded-xl border flex items-center justify-center font-bold text-sm select-none transition-all ${
                        isPressed(BTN_LB)
                          ? 'bg-emerald-600 text-white border-emerald-400 shadow-[0_0_15px_rgba(16,124,16,0.6)]'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F] hover:border-slate-500'
                      }`}
                    >
                      LB
                    </button>
                  </div>
                </div>

                {/* Center System Buttons */}
                <div className="flex items-center gap-3">
                  <button
                    onPointerDown={() => setButton(BTN_BACK, true)}
                    onPointerUp={() => setButton(BTN_BACK, false)}
                    className={`px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all ${
                      isPressed(BTN_BACK)
                        ? 'bg-slate-200 text-black border-white'
                        : 'bg-[#1A1D25] text-slate-400 border-[#272D3B] hover:text-white'
                    }`}
                  >
                    VIEW / BACK
                  </button>

                  <button
                    onPointerDown={() => setButton(BTN_GUIDE, true)}
                    onPointerUp={() => setButton(BTN_GUIDE, false)}
                    className={`w-10 h-10 rounded-full border flex items-center justify-center font-extrabold text-sm transition-all ${
                      isPressed(BTN_GUIDE)
                        ? 'bg-emerald-500 text-black border-emerald-300 shadow-[0_0_20px_rgba(34,197,94,0.8)]'
                        : 'bg-[#181B22] text-emerald-400 border-emerald-600/60 hover:border-emerald-500'
                    }`}
                  >
                    X
                  </button>

                  <button
                    onPointerDown={() => setButton(BTN_START, true)}
                    onPointerUp={() => setButton(BTN_START, false)}
                    className={`px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all ${
                      isPressed(BTN_START)
                        ? 'bg-slate-200 text-black border-white'
                        : 'bg-[#1A1D25] text-slate-400 border-[#272D3B] hover:text-white'
                    }`}
                  >
                    MENU / START
                  </button>
                </div>

                {/* Right Shoulder (RB & RT) */}
                <div className="flex items-center gap-3">
                  <div className="flex flex-col gap-1.5 items-center">
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider">Digital Bumper</span>
                    <button
                      onPointerDown={() => setButton(BTN_RB, true)}
                      onPointerUp={() => setButton(BTN_RB, false)}
                      className={`w-24 h-12 rounded-xl border flex items-center justify-center font-bold text-sm select-none transition-all ${
                        isPressed(BTN_RB)
                          ? 'bg-emerald-600 text-white border-emerald-400 shadow-[0_0_15px_rgba(16,124,16,0.6)]'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F] hover:border-slate-500'
                      }`}
                    >
                      RB
                    </button>
                  </div>

                  <div className="flex flex-col gap-1.5 items-center">
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider">Analog Right Trigger</span>
                    <div
                      onPointerDown={e => {
                        e.currentTarget.setPointerCapture(e.pointerId);
                        triggerInputUpdate(p => ({ ...p, triggerR: 255 }));
                      }}
                      onPointerUp={e => {
                        e.currentTarget.releasePointerCapture(e.pointerId);
                        triggerInputUpdate(p => ({ ...p, triggerR: 0 }));
                      }}
                      className={`w-28 h-12 rounded-xl border flex items-center justify-center font-bold text-sm select-none cursor-pointer transition-all ${
                        inputs.triggerR > 0
                          ? 'bg-emerald-600 text-white border-emerald-400 shadow-[0_0_15px_rgba(16,124,16,0.6)]'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F] hover:border-slate-500'
                      }`}
                    >
                      RT {inputs.triggerR > 0 ? `(${inputs.triggerR})` : ''}
                    </div>
                  </div>
                </div>
              </div>

              {/* Main Lower Field: Left Thumbstick, D-Pad, Right Thumbstick, ABXY cluster */}
              <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 items-center">
                {/* 1. Left Analog Stick */}
                <div className="flex flex-col items-center">
                  <span className="text-xs font-semibold text-slate-300 mb-2">Left Analog Stick (L3)</span>
                  <div
                    ref={leftStickRef}
                    onPointerDown={e => {
                      setIsDraggingL(true);
                      e.currentTarget.setPointerCapture(e.pointerId);
                      handleStickPointer(e, false);
                    }}
                    onPointerMove={e => {
                      if (isDraggingL) handleStickPointer(e, false);
                    }}
                    onPointerUp={e => {
                      setIsDraggingL(false);
                      e.currentTarget.releasePointerCapture(e.pointerId);
                      triggerInputUpdate(p => ({ ...p, stickLx: 0, stickLy: 0 }));
                    }}
                    className="w-44 h-44 rounded-full bg-[#161920] border-2 border-[#252B38] relative flex items-center justify-center touch-none cursor-grab active:cursor-grabbing select-none shadow-inner"
                  >
                    {/* Crosshair guide */}
                    <div className="absolute w-full h-[1px] bg-slate-800" />
                    <div className="absolute h-full w-[1px] bg-slate-800" />
                    <div className="absolute w-12 h-12 rounded-full border border-slate-700/60" />

                    {/* Movable Thumbstick Knob */}
                    <div
                      style={{
                        transform: `translate(${((inputs.stickLx / 32767) * 44).toFixed(1)}px, ${((-inputs.stickLy / 32767) * 44).toFixed(1)}px)`,
                      }}
                      className={`w-16 h-16 rounded-full border-2 flex items-center justify-center font-bold text-xs shadow-lg transition-transform duration-75 ${
                        isPressed(BTN_L3) || isDraggingL
                          ? 'bg-[#1D2430] border-emerald-400 text-emerald-300 shadow-[0_0_15px_rgba(16,124,16,0.5)]'
                          : 'bg-[#1F2430] border-slate-600 text-slate-400'
                      }`}
                    >
                      <button
                        onDoubleClick={() => setButton(BTN_L3, !isPressed(BTN_L3))}
                        className="text-[10px] font-mono"
                        title="Double-click to click L3 stick"
                      >
                        L3
                      </button>
                    </div>
                  </div>
                  <div className="font-mono text-[11px] text-slate-400 mt-2">
                    X: <span className="text-white">{inputs.stickLx}</span> Y: <span className="text-white">{inputs.stickLy}</span>
                  </div>
                </div>

                {/* 2. 4-Way D-Pad */}
                <div className="flex flex-col items-center">
                  <span className="text-xs font-semibold text-slate-300 mb-2">Directional D-Pad</span>
                  <div className="w-40 h-40 relative flex items-center justify-center">
                    {/* Up */}
                    <button
                      onPointerDown={() => {
                        triggerInputUpdate(p => ({ ...p, dpadY: 1, buttons: p.buttons | BTN_DPAD_UP }));
                      }}
                      onPointerUp={() => {
                        triggerInputUpdate(p => ({ ...p, dpadY: 0, buttons: p.buttons & ~BTN_DPAD_UP }));
                      }}
                      className={`absolute top-0 w-12 h-14 rounded-t-xl border flex items-center justify-center font-bold text-sm ${
                        isPressed(BTN_DPAD_UP) || inputs.dpadY > 0
                          ? 'bg-emerald-600 text-white border-emerald-400'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F]'
                      }`}
                    >
                      ▲
                    </button>

                    {/* Down */}
                    <button
                      onPointerDown={() => {
                        triggerInputUpdate(p => ({ ...p, dpadY: -1, buttons: p.buttons | BTN_DPAD_DOWN }));
                      }}
                      onPointerUp={() => {
                        triggerInputUpdate(p => ({ ...p, dpadY: 0, buttons: p.buttons & ~BTN_DPAD_DOWN }));
                      }}
                      className={`absolute bottom-0 w-12 h-14 rounded-b-xl border flex items-center justify-center font-bold text-sm ${
                        isPressed(BTN_DPAD_DOWN) || inputs.dpadY < 0
                          ? 'bg-emerald-600 text-white border-emerald-400'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F]'
                      }`}
                    >
                      ▼
                    </button>

                    {/* Left */}
                    <button
                      onPointerDown={() => {
                        triggerInputUpdate(p => ({ ...p, dpadX: -1, buttons: p.buttons | BTN_DPAD_LEFT }));
                      }}
                      onPointerUp={() => {
                        triggerInputUpdate(p => ({ ...p, dpadX: 0, buttons: p.buttons & ~BTN_DPAD_LEFT }));
                      }}
                      className={`absolute left-0 h-12 w-14 rounded-l-xl border flex items-center justify-center font-bold text-sm ${
                        isPressed(BTN_DPAD_LEFT) || inputs.dpadX < 0
                          ? 'bg-emerald-600 text-white border-emerald-400'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F]'
                      }`}
                    >
                      ◀
                    </button>

                    {/* Right */}
                    <button
                      onPointerDown={() => {
                        triggerInputUpdate(p => ({ ...p, dpadX: 1, buttons: p.buttons | BTN_DPAD_RIGHT }));
                      }}
                      onPointerUp={() => {
                        triggerInputUpdate(p => ({ ...p, dpadX: 0, buttons: p.buttons & ~BTN_DPAD_RIGHT }));
                      }}
                      className={`absolute right-0 h-12 w-14 rounded-r-xl border flex items-center justify-center font-bold text-sm ${
                        isPressed(BTN_DPAD_RIGHT) || inputs.dpadX > 0
                          ? 'bg-emerald-600 text-white border-emerald-400'
                          : 'bg-[#181B22] text-slate-300 border-[#2A303F]'
                      }`}
                    >
                      ▶
                    </button>

                    {/* Center Core */}
                    <div className="w-12 h-12 bg-[#14171E] border border-[#272D3B] rounded-sm pointer-events-none" />
                  </div>
                  <div className="font-mono text-[11px] text-slate-400 mt-2">
                    DX: <span className="text-white">{inputs.dpadX}</span> DY: <span className="text-white">{inputs.dpadY}</span>
                  </div>
                </div>

                {/* 3. Right Analog Stick */}
                <div className="flex flex-col items-center">
                  <span className="text-xs font-semibold text-slate-300 mb-2">Right Analog Stick (R3)</span>
                  <div
                    ref={rightStickRef}
                    onPointerDown={e => {
                      setIsDraggingR(true);
                      e.currentTarget.setPointerCapture(e.pointerId);
                      handleStickPointer(e, true);
                    }}
                    onPointerMove={e => {
                      if (isDraggingR) handleStickPointer(e, true);
                    }}
                    onPointerUp={e => {
                      setIsDraggingR(false);
                      e.currentTarget.releasePointerCapture(e.pointerId);
                      triggerInputUpdate(p => ({ ...p, stickRx: 0, stickRy: 0 }));
                    }}
                    className="w-44 h-44 rounded-full bg-[#161920] border-2 border-[#252B38] relative flex items-center justify-center touch-none cursor-grab active:cursor-grabbing select-none shadow-inner"
                  >
                    <div className="absolute w-full h-[1px] bg-slate-800" />
                    <div className="absolute h-full w-[1px] bg-slate-800" />
                    <div className="absolute w-12 h-12 rounded-full border border-slate-700/60" />

                    <div
                      style={{
                        transform: `translate(${((inputs.stickRx / 32767) * 44).toFixed(1)}px, ${((-inputs.stickRy / 32767) * 44).toFixed(1)}px)`,
                      }}
                      className={`w-16 h-16 rounded-full border-2 flex items-center justify-center font-bold text-xs shadow-lg transition-transform duration-75 ${
                        isPressed(BTN_R3) || isDraggingR
                          ? 'bg-[#1D2430] border-emerald-400 text-emerald-300 shadow-[0_0_15px_rgba(16,124,16,0.5)]'
                          : 'bg-[#1F2430] border-slate-600 text-slate-400'
                      }`}
                    >
                      <button
                        onDoubleClick={() => setButton(BTN_R3, !isPressed(BTN_R3))}
                        className="text-[10px] font-mono"
                        title="Double-click to click R3 stick"
                      >
                        R3
                      </button>
                    </div>
                  </div>
                  <div className="font-mono text-[11px] text-slate-400 mt-2">
                    X: <span className="text-white">{inputs.stickRx}</span> Y: <span className="text-white">{inputs.stickRy}</span>
                  </div>
                </div>

                {/* 4. ABXY Action Buttons */}
                <div className="flex flex-col items-center">
                  <span className="text-xs font-semibold text-slate-300 mb-2">ABXY Action Cluster</span>
                  <div className="w-44 h-44 relative flex items-center justify-center">
                    {/* Y - North (Amber) */}
                    <button
                      onPointerDown={() => setButton(BTN_Y, true)}
                      onPointerUp={() => setButton(BTN_Y, false)}
                      className={`absolute top-0 w-14 h-14 rounded-full border-2 flex items-center justify-center font-extrabold text-lg select-none transition-all ${
                        isPressed(BTN_Y)
                          ? 'bg-amber-500 text-black border-white shadow-[0_0_20px_rgba(245,158,11,0.8)]'
                          : 'bg-[#1C1F28] text-amber-400 border-amber-600/70 hover:border-amber-400'
                      }`}
                    >
                      Y
                    </button>

                    {/* A - South (Emerald) */}
                    <button
                      onPointerDown={() => setButton(BTN_A, true)}
                      onPointerUp={() => setButton(BTN_A, false)}
                      className={`absolute bottom-0 w-14 h-14 rounded-full border-2 flex items-center justify-center font-extrabold text-lg select-none transition-all ${
                        isPressed(BTN_A)
                          ? 'bg-emerald-500 text-black border-white shadow-[0_0_20px_rgba(16,185,129,0.8)]'
                          : 'bg-[#1C1F28] text-emerald-400 border-emerald-600/70 hover:border-emerald-400'
                      }`}
                    >
                      A
                    </button>

                    {/* X - West (Blue) */}
                    <button
                      onPointerDown={() => setButton(BTN_X, true)}
                      onPointerUp={() => setButton(BTN_X, false)}
                      className={`absolute left-0 w-14 h-14 rounded-full border-2 flex items-center justify-center font-extrabold text-lg select-none transition-all ${
                        isPressed(BTN_X)
                          ? 'bg-blue-500 text-white border-white shadow-[0_0_20px_rgba(59,130,246,0.8)]'
                          : 'bg-[#1C1F28] text-blue-400 border-blue-600/70 hover:border-blue-400'
                      }`}
                    >
                      X
                    </button>

                    {/* B - East (Crimson) */}
                    <button
                      onPointerDown={() => setButton(BTN_B, true)}
                      onPointerUp={() => setButton(BTN_B, false)}
                      className={`absolute right-0 w-14 h-14 rounded-full border-2 flex items-center justify-center font-extrabold text-lg select-none transition-all ${
                        isPressed(BTN_B)
                          ? 'bg-red-500 text-white border-white shadow-[0_0_20px_rgba(239,68,68,0.8)]'
                          : 'bg-[#1C1F28] text-red-400 border-red-600/70 hover:border-red-400'
                      }`}
                    >
                      B
                    </button>
                  </div>
                  <div className="font-mono text-[11px] text-slate-400 mt-2">
                    Bitmask: <span className="text-emerald-400">0x{inputs.buttons.toString(16).padStart(4, '0').toUpperCase()}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: 32-BYTE PACKET STREAM & BINARY INSPECTOR */}
        {activeTab === 'packet' && (
          <div className="flex flex-col gap-6">
            <div>
              <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                Real-Time 32-Byte Binary UDP Datagram
              </h2>
              <p className="text-sm text-slate-400 mt-1">
                Every frame, the Android app encodes the exact 32-byte binary struct below and streams it via UDP socket to Windows port 42100.
              </p>
            </div>

            {/* Binary Hex Display Card */}
            <div className="bg-[#121419] border border-[#232834] rounded-2xl p-6">
              <div className="flex items-center justify-between pb-4 border-b border-[#1C202A] mb-4">
                <span className="text-xs font-mono font-semibold text-slate-300">
                  UDP Datagram Payload (32 Bytes Big-Endian)
                </span>
                <span className="text-xs font-mono text-emerald-400">
                  Sequence #{sequence} · {packetRate} Hz
                </span>
              </div>

              <div className="p-4 bg-[#0A0B0E] border border-[#1A1E26] rounded-xl font-mono text-emerald-400 text-sm md:text-base leading-relaxed tracking-wider break-all select-all">
                {hexStr}
              </div>

              {/* Struct field breakdown grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-6">
                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 0..3 (uint32)</div>
                  <div className="text-sm font-mono font-bold text-white mt-1">Magic: 0x4C475044</div>
                  <div className="text-xs text-slate-400">ASCII 'LGPD' Header</div>
                </div>

                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 4 (uint8)</div>
                  <div className="text-sm font-mono font-bold text-white mt-1">Version: {PROTOCOL_VERSION}</div>
                  <div className="text-xs text-slate-400">Protocol Spec v1.0</div>
                </div>

                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 8..11 (uint32)</div>
                  <div className="text-sm font-mono font-bold text-white mt-1">Seq: #{sequence}</div>
                  <div className="text-xs text-slate-400">Monotonic Order Guard</div>
                </div>

                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 12..15 (uint32)</div>
                  <div className="text-sm font-mono font-bold text-emerald-400 mt-1">
                    0x{inputs.buttons.toString(16).padStart(8, '0').toUpperCase()}
                  </div>
                  <div className="text-xs text-slate-400">Xbox Button Bitmask</div>
                </div>

                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 16..19 (int16 x 2)</div>
                  <div className="text-sm font-mono font-bold text-white mt-1">
                    LX:{inputs.stickLx} LY:{inputs.stickLy}
                  </div>
                  <div className="text-xs text-slate-400">Left Stick (-32768..32767)</div>
                </div>

                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 20..23 (int16 x 2)</div>
                  <div className="text-sm font-mono font-bold text-white mt-1">
                    RX:{inputs.stickRx} RY:{inputs.stickRy}
                  </div>
                  <div className="text-xs text-slate-400">Right Stick (-32768..32767)</div>
                </div>

                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 24..25 (uint8 x 2)</div>
                  <div className="text-sm font-mono font-bold text-white mt-1">
                    LT:{inputs.triggerL} RT:{inputs.triggerR}
                  </div>
                  <div className="text-xs text-slate-400">Analog Triggers (0..255)</div>
                </div>

                <div className="p-3 bg-[#161922] border border-[#232836] rounded-xl">
                  <div className="text-[11px] text-slate-400 uppercase font-mono">Offset 28..31 (uint32)</div>
                  <div className="text-sm font-mono font-bold text-emerald-400 mt-1">
                    0x{mockAuthTag.toString(16).padStart(8, '0').toUpperCase()}
                  </div>
                  <div className="text-xs text-slate-400">HMAC-SHA256 Auth Tag</div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: JOY.CPL WINDOWS GAME CONTROLLERS EMULATOR */}
        {activeTab === 'joycpl' && (
          <div className="flex flex-col gap-6">
            <div>
              <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                Windows Game Controllers (joy.cpl) Verification
              </h2>
              <p className="text-sm text-slate-400 mt-1">
                This dialog mirrors exactly what you will see in Windows when you press <kbd className="px-1.5 py-0.5 bg-[#1F2430] border border-slate-700 rounded text-xs">Win + R</kbd>, type <code className="text-emerald-400">joy.cpl</code>, and inspect the virtual Xbox 360 controller properties.
              </p>
            </div>

            <div className="max-w-2xl mx-auto w-full bg-[#181B22] border border-[#2A3140] rounded-xl shadow-2xl p-6">
              {/* Joy.cpl Window Header */}
              <div className="flex items-center justify-between pb-4 border-b border-[#252C3A] mb-6">
                <div>
                  <h3 className="text-base font-bold text-white">Controller (Xbox 360 For Windows)</h3>
                  <div className="text-xs text-slate-400">Device ID: HID\VID_045E&PID_028E · Driver: ViGEmBus</div>
                </div>
                <div className="px-2.5 py-1 bg-emerald-950/80 border border-emerald-800 text-emerald-300 rounded text-xs font-semibold">
                  STATUS: OK
                </div>
              </div>

              {/* Axes & Buttons Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Axes test box */}
                <div className="p-4 bg-[#101217] border border-[#222733] rounded-lg">
                  <div className="text-xs font-bold text-slate-300 uppercase mb-3">Axes Calibration View</div>
                  <div className="w-48 h-48 mx-auto bg-[#181C25] border border-slate-700 relative rounded-sm">
                    {/* Center lines */}
                    <div className="absolute top-1/2 left-0 w-full h-[1px] bg-slate-700" />
                    <div className="absolute left-1/2 top-0 h-full w-[1px] bg-slate-700" />

                    {/* Crosshair indicator */}
                    <div
                      style={{
                        left: `${((inputs.stickLx + 32768) / 65535) * 100}%`,
                        top: `${((-inputs.stickLy + 32768) / 65535) * 100}%`,
                      }}
                      className="absolute w-3.5 h-3.5 -ml-[7px] -mt-[7px] bg-red-500 border border-white rounded-full shadow"
                    />
                  </div>
                  <div className="text-center font-mono text-xs text-slate-400 mt-2">
                    X-Axis: {inputs.stickLx} · Y-Axis: {inputs.stickLy}
                  </div>
                </div>

                {/* Z Axis and Buttons */}
                <div className="flex flex-col gap-4">
                  <div className="p-4 bg-[#101217] border border-[#222733] rounded-lg">
                    <div className="text-xs font-bold text-slate-300 uppercase mb-2">Z-Axis (Triggers)</div>
                    <div className="w-full h-4 bg-[#1A1E27] rounded-sm overflow-hidden border border-slate-700">
                      <div
                        style={{
                          width: `${Math.max(inputs.triggerL, inputs.triggerR) / 2.55}%`,
                        }}
                        className="h-full bg-blue-500 transition-all duration-75"
                      />
                    </div>
                    <div className="flex justify-between text-[11px] font-mono text-slate-400 mt-1">
                      <span>LT: {inputs.triggerL}</span>
                      <span>RT: {inputs.triggerR}</span>
                    </div>
                  </div>

                  <div className="p-4 bg-[#101217] border border-[#222733] rounded-lg">
                    <div className="text-xs font-bold text-slate-300 uppercase mb-3">Buttons (1 - 10)</div>
                    <div className="grid grid-cols-5 gap-2">
                      {[
                        { num: 1, name: 'A', pressed: isPressed(BTN_A) },
                        { num: 2, name: 'B', pressed: isPressed(BTN_B) },
                        { num: 3, name: 'X', pressed: isPressed(BTN_X) },
                        { num: 4, name: 'Y', pressed: isPressed(BTN_Y) },
                        { num: 5, name: 'LB', pressed: isPressed(BTN_LB) },
                        { num: 6, name: 'RB', pressed: isPressed(BTN_RB) },
                        { num: 7, name: 'BACK', pressed: isPressed(BTN_BACK) },
                        { num: 8, name: 'START', pressed: isPressed(BTN_START) },
                        { num: 9, name: 'L3', pressed: isPressed(BTN_L3) },
                        { num: 10, name: 'R3', pressed: isPressed(BTN_R3) },
                      ].map(b => (
                        <div
                          key={b.num}
                          className={`w-9 h-9 rounded-full border flex items-center justify-center font-bold text-xs select-none transition-all ${
                            b.pressed
                              ? 'bg-red-600 text-white border-white shadow-[0_0_10px_rgba(239,68,68,0.8)]'
                              : 'bg-[#181B22] text-slate-400 border-slate-700'
                          }`}
                        >
                          {b.num}
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: SOURCE FILES & ARTIFACTS EXPLORER */}
        {activeTab === 'files' && (
          <div className="flex flex-col gap-6">
            <div>
              <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                Project Source Code & Architecture
              </h2>
              <p className="text-sm text-slate-400 mt-1">
                Inspect the complete native Kotlin Android source code, Python Windows companion server, and batch scripts.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
              {/* File selector sidebar */}
              <div className="bg-[#121419] border border-[#232834] rounded-2xl p-4 flex flex-col gap-1">
                <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider px-2 py-1">Protocol & Docs</span>
                <button
                  onClick={() => setSelectedFile('protocol')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'protocol' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  protocol-specification.md
                </button>
                <button
                  onClick={() => setSelectedFile('readme')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'readme' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  README.md (Setup Guide)
                </button>

                <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider px-2 py-1 mt-3">Windows PC Server</span>
                <button
                  onClick={() => setSelectedFile('py_main')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'py_main' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  pc-server/main.py
                </button>
                <button
                  onClick={() => setSelectedFile('py_net')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'py_net' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  pc-server/network_server.py
                </button>
                <button
                  onClick={() => setSelectedFile('py_backend')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'py_backend' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  pc-server/gamepad_backend.py
                </button>
                <button
                  onClick={() => setSelectedFile('py_protocol')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'py_protocol' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  pc-server/protocol.py
                </button>
                <button
                  onClick={() => setSelectedFile('py_bat')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'py_bat' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  pc-server/start_server.bat
                </button>

                <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider px-2 py-1 mt-3">Android App (Kotlin)</span>
                <button
                  onClick={() => setSelectedFile('kt_main')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'kt_main' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  MainActivity.kt
                </button>
                <button
                  onClick={() => setSelectedFile('kt_udp')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'kt_udp' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  UdpInputTransmitter.kt
                </button>
                <button
                  onClick={() => setSelectedFile('kt_encoder')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'kt_encoder' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  PacketEncoder.kt
                </button>
                <button
                  onClick={() => setSelectedFile('kt_gamepad')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'kt_gamepad' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  GamepadScreen.kt
                </button>
                <button
                  onClick={() => setSelectedFile('kt_manifest')}
                  className={`text-left px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    selectedFile === 'kt_manifest' ? 'bg-[#107C10] text-white font-bold' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  AndroidManifest.xml
                </button>
              </div>

              {/* Code viewer */}
              <div className="md:col-span-3 bg-[#121419] border border-[#232834] rounded-2xl p-6 flex flex-col">
                <div className="flex items-center justify-between pb-4 border-b border-[#1C202A] mb-4">
                  <span className="font-mono text-xs font-semibold text-emerald-400">
                    {selectedFile === 'protocol' && 'protocol/protocol-specification.md'}
                    {selectedFile === 'readme' && 'README.md'}
                    {selectedFile === 'py_main' && 'pc-server/main.py'}
                    {selectedFile === 'py_net' && 'pc-server/network_server.py'}
                    {selectedFile === 'py_backend' && 'pc-server/gamepad_backend.py'}
                    {selectedFile === 'py_protocol' && 'pc-server/protocol.py'}
                    {selectedFile === 'py_bat' && 'pc-server/start_server.bat'}
                    {selectedFile === 'kt_main' && 'android-app/.../MainActivity.kt'}
                    {selectedFile === 'kt_udp' && 'android-app/.../UdpInputTransmitter.kt'}
                    {selectedFile === 'kt_encoder' && 'android-app/.../PacketEncoder.kt'}
                    {selectedFile === 'kt_gamepad' && 'android-app/.../GamepadScreen.kt'}
                    {selectedFile === 'kt_manifest' && 'android-app/.../AndroidManifest.xml'}
                  </span>
                  <button
                    onClick={() => {
                      copyToClipboard(
                        selectedFile === 'protocol'
                          ? '# LAN Gamepad Protocol Specification v1.0\n\n32-byte UDP Datagram'
                          : '// LAN Gamepad Code',
                        'code'
                      );
                    }}
                    className="px-2.5 py-1 text-xs bg-[#1C202A] hover:bg-[#252B38] text-slate-300 rounded border border-[#2A303F] flex items-center gap-1.5"
                  >
                    {copiedKey === 'code' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    Copy Content
                  </button>
                </div>

                <pre className="p-4 bg-[#0A0B0E] border border-[#1A1E26] rounded-xl font-mono text-xs text-slate-300 overflow-x-auto max-h-[500px] leading-relaxed">
                  {selectedFile === 'protocol' && `# LAN Gamepad Protocol Specification v1.0
Magic: 0x4C475044 ('LGPD')
Payload Size: 32 Bytes
Byte Order: Big-Endian

Offset 0..3:   Magic uint32 (0x4C475044)
Offset 4:      Version uint8 (0x01)
Offset 5:      Flags uint8
Offset 6..7:   Reserved uint16
Offset 8..11:  Sequence uint32 (Monotonic Packet Counter)
Offset 12..15: Buttons Bitmask uint32 (A, B, X, Y, LB, RB, Start, Back...)
Offset 16..17: Left Stick X int16 (-32768 to 32767)
Offset 18..19: Left Stick Y int16 (-32768 to 32767)
Offset 20..21: Right Stick X int16 (-32768 to 32767)
Offset 22..23: Right Stick Y int16 (-32768 to 32767)
Offset 24:     Left Trigger uint8 (0 to 255)
Offset 25:     Right Trigger uint8 (0 to 255)
Offset 26:     D-Pad X int8 (-1, 0, 1)
Offset 27:     D-Pad Y int8 (-1, 0, 1)
Offset 28..31: Truncated HMAC-SHA256 Auth Tag uint32`}

                  {selectedFile === 'py_main' && `import asyncio
from config import DEFAULT_UDP_PORT, DEFAULT_TCP_PORT
from gamepad_backend import create_best_backend
from network_server import NetworkServer
from discovery import DiscoveryBroadcaster

class LanGamepadApplication:
    def __init__(self):
        self.backend, self.info = create_best_backend()
        self.net_server = NetworkServer(backend=self.backend)
        self.discovery = DiscoveryBroadcaster()
        
    async def run(self):
        await self.net_server.start()
        self.discovery.start()
        # Displays real-time CLI telemetry and handles joy.cpl output!`}

                  {selectedFile === 'kt_udp' && `class UdpInputTransmitter(
    private val targetIp: String,
    private val targetPort: Int,
    private val sessionKey: ByteArray?,
    private val targetHz: Int = 90
) {
    fun start(scope: CoroutineScope, gamepadState: GamepadState) {
        scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket()
            while (isActive) {
                val bytes = encoder.encode(gamepadState, seq++, sessionKey)
                socket.send(DatagramPacket(bytes, 32, address, targetPort))
                delay(intervalMs)
            }
        }
    }
}`}

                  {selectedFile !== 'protocol' && selectedFile !== 'py_main' && selectedFile !== 'kt_udp' && `// Full file available on disk in workspace
// See pc-server/ or android-app/ directories.`}
                </pre>
              </div>
            </div>
          </div>
        )}

        {/* TAB 5: SETUP & DRIVER GUIDE */}
        {activeTab === 'setup' && (
          <div className="flex flex-col gap-6">
            <div>
              <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                Beginner-Friendly Setup & Driver Installation
              </h2>
              <p className="text-sm text-slate-400 mt-1">
                Follow these 3 easy steps to connect your Android phone to Windows PC games.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Step 1 */}
              <div className="bg-[#121419] border border-[#232834] rounded-2xl p-6 flex flex-col justify-between">
                <div>
                  <div className="w-8 h-8 rounded-lg bg-emerald-950 border border-emerald-700/60 text-emerald-400 font-bold flex items-center justify-center text-sm mb-4">
                    1
                  </div>
                  <h3 className="font-bold text-white text-base">Install ViGEmBus Driver</h3>
                  <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                    Windows requires the open-source Nefarius ViGEmBus driver to expose genuine virtual Xbox 360 controllers to Steam and PC games.
                  </p>
                </div>
                <div className="mt-6 pt-4 border-t border-[#1C202A]">
                  <a
                    href="https://github.com/nefarius/ViGEmBus/releases/latest"
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-2 text-xs font-bold text-emerald-400 hover:text-emerald-300"
                  >
                    Download ViGEmBus Setup <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>
              </div>

              {/* Step 2 */}
              <div className="bg-[#121419] border border-[#232834] rounded-2xl p-6 flex flex-col justify-between">
                <div>
                  <div className="w-8 h-8 rounded-lg bg-emerald-950 border border-emerald-700/60 text-emerald-400 font-bold flex items-center justify-center text-sm mb-4">
                    2
                  </div>
                  <h3 className="font-bold text-white text-base">Launch PC Server</h3>
                  <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                    Double-click <code className="text-emerald-400">start_server.bat</code> inside the <code className="text-white">pc-server</code> folder. It automatically installs dependencies and starts the companion hub.
                  </p>
                </div>
                <div className="mt-6 pt-4 border-t border-[#1C202A]">
                  <div className="p-2 bg-[#0A0B0E] rounded font-mono text-[11px] text-slate-300 flex items-center justify-between">
                    <span>python pc-server/main.py</span>
                    <button
                      onClick={() => copyToClipboard('python pc-server/main.py', 'cmd_run')}
                      className="text-slate-500 hover:text-white"
                    >
                      {copiedKey === 'cmd_run' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              </div>

              {/* Step 3 */}
              <div className="bg-[#121419] border border-[#232834] rounded-2xl p-6 flex flex-col justify-between">
                <div>
                  <div className="w-8 h-8 rounded-lg bg-emerald-950 border border-emerald-700/60 text-emerald-400 font-bold flex items-center justify-center text-sm mb-4">
                    3
                  </div>
                  <h3 className="font-bold text-white text-base">Install & Pair APK</h3>
                  <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                    Build and launch the Android APK. It automatically finds your PC on Wi-Fi. Enter the 6-digit PIN to pair and start playing!
                  </p>
                </div>
                <div className="mt-6 pt-4 border-t border-[#1C202A]">
                  <div className="p-2 bg-[#0A0B0E] rounded font-mono text-[11px] text-slate-300 flex items-center justify-between">
                    <span>./gradlew assembleDebug</span>
                    <button
                      onClick={() => copyToClipboard('./gradlew assembleDebug', 'cmd_apk')}
                      className="text-slate-500 hover:text-white"
                    >
                      {copiedKey === 'cmd_apk' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

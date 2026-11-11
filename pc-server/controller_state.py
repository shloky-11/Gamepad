"""
Controller State dataclass and neutral state helpers.
"""
from dataclasses import dataclass
from typing import Dict, Any
import protocol


@dataclass
class ControllerState:
    buttons: int = 0
    stick_lx: int = 0
    stick_ly: int = 0
    stick_rx: int = 0
    stick_ry: int = 0
    trigger_l: int = 0
    trigger_r: int = 0
    dpad_x: int = 0
    dpad_y: int = 0

    @classmethod
    def neutral(cls) -> "ControllerState":
        """Returns a completely released/centered controller state."""
        return cls(
            buttons=0,
            stick_lx=0,
            stick_ly=0,
            stick_rx=0,
            stick_ry=0,
            trigger_l=0,
            trigger_r=0,
            dpad_x=0,
            dpad_y=0
        )

    def is_button_pressed(self, mask: int) -> bool:
        return bool(self.buttons & mask)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "btn_a": self.is_button_pressed(protocol.BTN_A),
            "btn_b": self.is_button_pressed(protocol.BTN_B),
            "btn_x": self.is_button_pressed(protocol.BTN_X),
            "btn_y": self.is_button_pressed(protocol.BTN_Y),
            "btn_lb": self.is_button_pressed(protocol.BTN_LB),
            "btn_rb": self.is_button_pressed(protocol.BTN_RB),
            "btn_back": self.is_button_pressed(protocol.BTN_BACK),
            "btn_start": self.is_button_pressed(protocol.BTN_START),
            "btn_guide": self.is_button_pressed(protocol.BTN_GUIDE),
            "btn_l3": self.is_button_pressed(protocol.BTN_L3),
            "btn_r3": self.is_button_pressed(protocol.BTN_R3),
            "dpad_up": self.is_button_pressed(protocol.BTN_DPAD_UP) or self.dpad_y > 0,
            "dpad_down": self.is_button_pressed(protocol.BTN_DPAD_DOWN) or self.dpad_y < 0,
            "dpad_left": self.is_button_pressed(protocol.BTN_DPAD_LEFT) or self.dpad_x < 0,
            "dpad_right": self.is_button_pressed(protocol.BTN_DPAD_RIGHT) or self.dpad_x > 0,
            "stick_lx": self.stick_lx,
            "stick_ly": self.stick_ly,
            "stick_rx": self.stick_rx,
            "stick_ry": self.stick_ry,
            "trigger_l": self.trigger_l,
            "trigger_r": self.trigger_r,
        }

    def summary(self) -> str:
        active = []
        if self.is_button_pressed(protocol.BTN_A): active.append("A")
        if self.is_button_pressed(protocol.BTN_B): active.append("B")
        if self.is_button_pressed(protocol.BTN_X): active.append("X")
        if self.is_button_pressed(protocol.BTN_Y): active.append("Y")
        if self.is_button_pressed(protocol.BTN_LB): active.append("LB")
        if self.is_button_pressed(protocol.BTN_RB): active.append("RB")
        if self.is_button_pressed(protocol.BTN_START): active.append("START")
        if self.is_button_pressed(protocol.BTN_BACK): active.append("BACK")
        btn_str = "+".join(active) if active else "None"
        return f"Btns: [{btn_str}] LX:{self.stick_lx:+06d} LY:{self.stick_ly:+06d} RX:{self.stick_rx:+06d} RY:{self.stick_ry:+06d} LT:{self.trigger_l:03d} RT:{self.trigger_r:03d}"

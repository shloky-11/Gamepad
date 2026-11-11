"""
Gamepad Backend abstraction for LAN Gamepad Windows Server.
Provides support for genuine Windows virtual Xbox 360 controller emulation via vgamepad (ViGEmBus)
with seamless auto-fallback to an interactive SimulatorBackend when running without kernel drivers.
"""
from abc import ABC, abstractmethod
import logging
import sys
from typing import Optional, Tuple
from controller_state import ControllerState
import protocol

logger = logging.getLogger("LAN_Gamepad.Backend")


class GamepadBackend(ABC):
    """Abstract interface for virtual controller backends."""

    @abstractmethod
    def connect(self) -> bool:
        """Initializes the virtual gamepad device."""
        pass

    @abstractmethod
    def update_controller(self, state: ControllerState) -> None:
        """Applies a full controller state snapshot."""
        pass

    @abstractmethod
    def release_all(self) -> None:
        """Immediately neutralizes all buttons, sticks, and triggers."""
        pass

    @abstractmethod
    def disconnect(self) -> None:
        """Destroys the virtual device and cleans up resources."""
        pass

    @abstractmethod
    def get_name(self) -> str:
        """Returns the human-readable name of the backend."""
        pass

    @abstractmethod
    def is_real_hardware(self) -> bool:
        """True if producing a real Windows OS controller device visible in joy.cpl."""
        pass


class VgamepadBackend(GamepadBackend):
    """
    Genuine Windows Xbox 360 virtual gamepad implementation using vgamepad and the ViGEmBus driver.
    Generates a real Windows DirectInput / XInput device recognized by Steam, joy.cpl, and PC games.
    """

    def __init__(self):
        self._gamepad = None
        self._connected = False
        self._vg = None

    def connect(self) -> bool:
        try:
            import vgamepad as vg
            self._vg = vg
            self._gamepad = vg.VX360Gamepad()
            self._connected = True
            logger.info("Successfully initialized virtual Xbox 360 controller via vgamepad/ViGEmBus.")
            return True
        except ImportError:
            logger.warning("vgamepad package not installed. Run 'pip install vgamepad'.")
            self._connected = False
            return False
        except Exception as exc:
            logger.warning(
                f"Could not initialize ViGEmBus device: {exc}. "
                "Ensure the Nefarius ViGEmBus driver is installed on Windows: "
                "https://github.com/nefarius/ViGEmBus/releases"
            )
            self._connected = False
            return False

    def update_controller(self, state: ControllerState) -> None:
        if not self._connected or not self._gamepad or not self._vg:
            return

        vg = self._vg
        gp = self._gamepad

        # Reset button register and rebuild from bitmask
        gp.reset()

        # Map digital buttons
        if state.is_button_pressed(protocol.BTN_A):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_A)
        if state.is_button_pressed(protocol.BTN_B):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_B)
        if state.is_button_pressed(protocol.BTN_X):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_X)
        if state.is_button_pressed(protocol.BTN_Y):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_Y)
        if state.is_button_pressed(protocol.BTN_LB):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER)
        if state.is_button_pressed(protocol.BTN_RB):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER)
        if state.is_button_pressed(protocol.BTN_BACK):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK)
        if state.is_button_pressed(protocol.BTN_START):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_START)
        if state.is_button_pressed(protocol.BTN_GUIDE):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE)
        if state.is_button_pressed(protocol.BTN_L3):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB)
        if state.is_button_pressed(protocol.BTN_R3):
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB)

        # D-pad mapping
        if state.is_button_pressed(protocol.BTN_DPAD_UP) or state.dpad_y > 0:
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP)
        if state.is_button_pressed(protocol.BTN_DPAD_DOWN) or state.dpad_y < 0:
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN)
        if state.is_button_pressed(protocol.BTN_DPAD_LEFT) or state.dpad_x < 0:
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT)
        if state.is_button_pressed(protocol.BTN_DPAD_RIGHT) or state.dpad_x > 0:
            gp.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT)

        # Joysticks (-32768 to 32767)
        gp.left_joystick(x_value=state.stick_lx, y_value=state.stick_ly)
        gp.right_joystick(x_value=state.stick_rx, y_value=state.stick_ry)

        # Triggers (0 to 255)
        gp.left_trigger(value=state.trigger_l)
        gp.right_trigger(value=state.trigger_r)

        # Push state to OS driver
        gp.update()

    def release_all(self) -> None:
        if self._connected and self._gamepad:
            self._gamepad.reset()
            self._gamepad.left_joystick(x_value=0, y_value=0)
            self._gamepad.right_joystick(x_value=0, y_value=0)
            self._gamepad.left_trigger(value=0)
            self._gamepad.right_trigger(value=0)
            self._gamepad.update()

    def disconnect(self) -> None:
        if self._connected and self._gamepad:
            self.release_all()
            # Free device
            del self._gamepad
            self._gamepad = None
            self._connected = False
            logger.info("Virtual Xbox 360 controller disconnected.")

    def get_name(self) -> str:
        return "vgamepad (Virtual Xbox 360 Controller via ViGEmBus)"

    def is_real_hardware(self) -> bool:
        return self._connected


class SimulatorBackend(GamepadBackend):
    """
    Simulation backend used for testing and development environments where
    ViGEmBus kernel drivers are not present. Keeps state and broadcasts telemetry to the UI.
    """

    def __init__(self):
        self._current_state = ControllerState.neutral()
        self._connected = False

    def connect(self) -> bool:
        self._connected = True
        logger.info("SimulatorBackend active. Controller inputs will be captured, inspected, and mirrored to Web Dashboard.")
        return True

    def update_controller(self, state: ControllerState) -> None:
        self._current_state = state

    def release_all(self) -> None:
        self._current_state = ControllerState.neutral()
        logger.debug("SimulatorBackend: All inputs released to neutral state.")

    def disconnect(self) -> None:
        self.release_all()
        self._connected = False
        logger.info("SimulatorBackend disconnected.")

    def get_name(self) -> str:
        return "Built-in Controller Simulator (Development / Testing Mode)"

    def is_real_hardware(self) -> bool:
        return False

    @property
    def current_state(self) -> ControllerState:
        return self._current_state


def create_best_backend() -> Tuple[GamepadBackend, str]:
    """
    Attempts to initialize vgamepad Xbox 360 controller backend.
    If unavailable or driver missing, falls back to SimulatorBackend and returns instructions.
    """
    real_backend = VgamepadBackend()
    if real_backend.connect():
        return real_backend, "Real virtual Xbox 360 controller active and ready for Windows games."

    sim_backend = SimulatorBackend()
    sim_backend.connect()
    info = (
        "Using built-in SimulatorBackend. To expose a real Xbox 360 gamepad to Windows games:\n"
        "  1. Run: pip install vgamepad\n"
        "  2. Install ViGEmBus Driver from: https://github.com/nefarius/ViGEmBus/releases/latest\n"
        "  3. Restart LAN Gamepad server"
    )
    return sim_backend, info

"""
Unit tests for GamepadBackend and ControllerState.
"""
import unittest
from controller_state import ControllerState
from gamepad_backend import SimulatorBackend
import protocol


class TestBackend(unittest.TestCase):
    def test_simulator_backend_lifecycle(self):
        backend = SimulatorBackend()
        self.assertTrue(backend.connect())

        # Update controller state
        state = ControllerState(
            buttons=protocol.BTN_A | protocol.BTN_B,
            stick_lx=15000,
            stick_ly=20000,
            trigger_l=128
        )
        backend.update_controller(state)
        self.assertEqual(backend.current_state.stick_lx, 15000)
        self.assertTrue(backend.current_state.is_button_pressed(protocol.BTN_A))
        self.assertTrue(backend.current_state.is_button_pressed(protocol.BTN_B))
        self.assertFalse(backend.current_state.is_button_pressed(protocol.BTN_X))

        # Test release_all (Deadman watchdog behavior)
        backend.release_all()
        self.assertEqual(backend.current_state.buttons, 0)
        self.assertEqual(backend.current_state.stick_lx, 0)
        self.assertEqual(backend.current_state.stick_ly, 0)
        self.assertEqual(backend.current_state.trigger_l, 0)

        backend.disconnect()
        self.assertFalse(backend._connected)


if __name__ == "__main__":
    unittest.main()

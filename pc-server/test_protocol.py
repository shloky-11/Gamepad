"""
Automated unit tests for binary protocol packing, unpacking, and validation.
"""
import unittest
import struct
import protocol
from config import PROTOCOL_MAGIC, PROTOCOL_VERSION


class TestProtocol(unittest.TestCase):
    def test_packet_size(self):
        packet = protocol.pack_input_packet(
            sequence=1,
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
        self.assertEqual(len(packet), 32, "Packet must be exactly 32 bytes")

    def test_pack_and_unpack_roundtrip(self):
        buttons = protocol.BTN_A | protocol.BTN_LB | protocol.BTN_START
        stick_lx = 12345
        stick_ly = -32000
        stick_rx = -15000
        stick_ry = 28000
        trigger_l = 210
        trigger_r = 95
        dpad_x = -1
        dpad_y = 1
        seq = 42091

        packet = protocol.pack_input_packet(
            sequence=seq,
            buttons=buttons,
            stick_lx=stick_lx,
            stick_ly=stick_ly,
            stick_rx=stick_rx,
            stick_ry=stick_ry,
            trigger_l=trigger_l,
            trigger_r=trigger_r,
            dpad_x=dpad_x,
            dpad_y=dpad_y
        )

        success, parsed, err = protocol.unpack_input_packet(packet)
        self.assertTrue(success, f"Unpack failed: {err}")
        self.assertEqual(parsed["sequence"], seq)
        self.assertEqual(parsed["buttons"], buttons)
        self.assertEqual(parsed["stick_lx"], stick_lx)
        self.assertEqual(parsed["stick_ly"], stick_ly)
        self.assertEqual(parsed["stick_rx"], stick_rx)
        self.assertEqual(parsed["stick_ry"], stick_ry)
        self.assertEqual(parsed["trigger_l"], trigger_l)
        self.assertEqual(parsed["trigger_r"], trigger_r)
        self.assertEqual(parsed["dpad_x"], dpad_x)
        self.assertEqual(parsed["dpad_y"], dpad_y)

    def test_hmac_authentication_verification(self):
        session_key = b"super_secret_session_key_32bytes"
        packet = protocol.pack_input_packet(
            sequence=10,
            buttons=protocol.BTN_X,
            stick_lx=0,
            stick_ly=0,
            stick_rx=0,
            stick_ry=0,
            trigger_l=0,
            trigger_r=0,
            dpad_x=0,
            dpad_y=0,
            session_key=session_key
        )

        # Unpack with correct key
        ok, parsed, err = protocol.unpack_input_packet(packet, session_key=session_key)
        self.assertTrue(ok)
        self.assertEqual(parsed["buttons"], protocol.BTN_X)

        # Unpack with wrong key -> must fail HMAC
        wrong_key = b"tampered_invalid_session_key____"
        ok_wrong, _, err_wrong = protocol.unpack_input_packet(packet, session_key=wrong_key)
        self.assertFalse(ok_wrong)
        self.assertIn("HMAC", err_wrong)

    def test_range_clamping(self):
        # Exceed boundaries
        packet = protocol.pack_input_packet(
            sequence=1,
            buttons=0,
            stick_lx=999999,  # clamped to 32767
            stick_ly=-999999, # clamped to -32768
            stick_rx=0,
            stick_ry=0,
            trigger_l=500,    # clamped to 255
            trigger_r=-50,    # clamped to 0
            dpad_x=5,         # clamped to 1
            dpad_y=-10        # clamped to -1
        )
        ok, parsed, _ = protocol.unpack_input_packet(packet)
        self.assertTrue(ok)
        self.assertEqual(parsed["stick_lx"], 32767)
        self.assertEqual(parsed["stick_ly"], -32768)
        self.assertEqual(parsed["trigger_l"], 255)
        self.assertEqual(parsed["trigger_r"], 0)
        self.assertEqual(parsed["dpad_x"], 1)
        self.assertEqual(parsed["dpad_y"], -1)


if __name__ == "__main__":
    unittest.main()

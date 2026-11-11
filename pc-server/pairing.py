"""
Pairing and Session Security Manager for LAN Gamepad.
Generates cryptographically random 6-digit PINs, ephemeral session keys, and manages authorized client sessions.
"""
import secrets
import time
import logging
from typing import Optional, Dict
from config import PIN_EXPIRY_SECONDS, MAX_PIN_ATTEMPTS

logger = logging.getLogger("LAN_Gamepad.Pairing")


class PairingSession:
    def __init__(self, session_id: str, session_token: bytes, client_ip: str, device_name: str):
        self.session_id = session_id
        self.session_token = session_token
        self.client_ip = client_ip
        self.device_name = device_name
        self.created_at = time.time()
        self.last_seen = time.time()


class PairingManager:
    def __init__(self):
        self._current_pin: str = self._generate_secure_pin()
        self._pin_created_at: float = time.time()
        self._failed_attempts: int = 0
        self._active_sessions: Dict[str, PairingSession] = {}  # session_id -> PairingSession
        self._ip_to_session: Dict[str, PairingSession] = {}   # client_ip -> PairingSession

    def _generate_secure_pin(self) -> str:
        # Generates a 6-digit random number string
        return f"{secrets.randbelow(1000000):06d}"

    def get_current_pin(self) -> str:
        # Regenerate if expired
        if time.time() - self._pin_created_at > PIN_EXPIRY_SECONDS:
            self.regenerate_pin()
        return self._current_pin

    def regenerate_pin(self) -> str:
        self._current_pin = self._generate_secure_pin()
        self._pin_created_at = time.time()
        self._failed_attempts = 0
        logger.info(f"New Pairing PIN generated: {self._current_pin}")
        return self._current_pin

    def verify_and_create_session(
        self, client_ip: str, device_name: str, pin_entered: str
    ) -> Optional[PairingSession]:
        """
        Validates entered PIN against active PIN and issues an ephemeral session key.
        """
        if self._failed_attempts >= MAX_PIN_ATTEMPTS:
            logger.warning(f"Excessive failed pairing attempts from {client_ip}. Regenerating PIN.")
            self.regenerate_pin()
            return None

        current_pin = self.get_current_pin()
        # Constant-time comparison
        if not secrets.compare_digest(current_pin, pin_entered.strip()):
            self._failed_attempts += 1
            logger.warning(
                f"Failed PIN attempt from {client_ip} ({self._failed_attempts}/{MAX_PIN_ATTEMPTS})"
            )
            return None

        # Success - create session
        self._failed_attempts = 0
        session_id = f"sess_{secrets.token_hex(8)}"
        # 32-byte secret session key for HMAC
        session_token = secrets.token_bytes(32)

        session = PairingSession(
            session_id=session_id,
            session_token=session_token,
            client_ip=client_ip,
            device_name=device_name
        )

        self._active_sessions[session_id] = session
        self._ip_to_session[client_ip] = session
        logger.info(f"Successfully paired device '{device_name}' from {client_ip} with session {session_id}")
        return session

    def get_session_by_ip(self, client_ip: str) -> Optional[PairingSession]:
        return self._ip_to_session.get(client_ip)

    def revoke_all(self) -> None:
        self._active_sessions.clear()
        self._ip_to_session.clear()
        self.regenerate_pin()
        logger.info("All paired sessions revoked.")

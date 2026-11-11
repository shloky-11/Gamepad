package com.langamepad.network

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val HMAC_ALGORITHM = "HmacSHA256"

    fun computeAuthTag(sessionKey: ByteArray?, payloadPrefix28Bytes: ByteArray): Int {
        if (sessionKey == null || sessionKey.isEmpty()) {
            return 0
        }
        return try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            val keySpec = SecretKeySpec(sessionKey, HMAC_ALGORITHM)
            mac.init(keySpec)
            val hmacBytes = mac.doFinal(payloadPrefix28Bytes)
            // Extract first 4 bytes as Big-Endian Int
            ByteBuffer.wrap(hmacBytes, 0, 4).int
        } catch (e: Exception) {
            0
        }
    }
}

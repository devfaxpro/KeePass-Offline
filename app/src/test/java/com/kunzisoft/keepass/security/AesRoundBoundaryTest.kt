package com.kunzisoft.keepass.security

import com.kunzisoft.encrypt.aes.AESTransformer
import com.kunzisoft.keepass.database.crypto.kdf.AesKdf
import java.io.IOException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Test

class AesRoundBoundaryTest {
    @Test fun overflowIsRejectedBeforeEitherBackendCanRun() {
        for (rounds in listOf(0x100000000uL, 0x100000001uL, Long.MAX_VALUE.toULong(), ULong.MAX_VALUE)) {
            assertThrows(IOException::class.java) { AESTransformer.transformKey(ByteArray(32), ByteArray(32), rounds) }
            assertThrows(IOException::class.java) { AESTransformer.transformKeyInJVM(ByteArray(32), ByteArray(32), rounds) }
        }
        AESTransformer.validateRounds(0xffffffffuL)
        assertEquals(0xffffffffuL, AesKdf().maxKeyRounds)
        val kdf = AesKdf()
        kdf.parameters.setUInt64(AesKdf.PARAM_ROUNDS, 0x100000000uL)
        assertThrows(SecurityException::class.java) { kdf.transform(ByteArray(32)) }
    }

    @Test fun supportedRoundsKeepTheExistingAesTransformation() {
        val seed = ByteArray(32) { it.toByte() }
        val key = ByteArray(32) { (it + 32).toByte() }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(seed, "AES"))
        var expected = key.copyOf()
        repeat(3) { expected = cipher.doFinal(expected) }
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(expected),
            AESTransformer.transformKeyInJVM(seed, key, 3uL))
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(key),
            AESTransformer.transformKeyInJVM(seed, key, 0uL))
    }
}

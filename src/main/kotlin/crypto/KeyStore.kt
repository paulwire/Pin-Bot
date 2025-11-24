package crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object KeyStore {

    private val keyFile = File("pinbot.key")
    private const val ALGO = "AES"
    private const val AES_GCM = "AES/GCM/NoPadding"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    val masterKey: SecretKey by lazy { loadOrCreateKey() }

    private fun loadOrCreateKey(): SecretKey {
        val password = System.getProperty("PINBOT_MASTER_PASSWORD")
            ?: System.getenv("PINBOT_MASTER_PASSWORD")
            ?: error("Set PINBOT_MASTER_PASSWORD env var or system property!")

        if (!keyFile.exists()) {
            val keyBytes = ByteArray(32)
            SecureRandom().nextBytes(keyBytes)
            val encrypted = encryptWithPassword(password, keyBytes)
            keyFile.writeBytes(encrypted)
        }

        val encrypted = keyFile.readBytes()
        val decrypted = decryptWithPassword(password, encrypted)
        return SecretKeySpec(decrypted, ALGO)
    }

    private fun encryptWithPassword(password: String, data: ByteArray): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        return salt + iv + cipher.doFinal(data)
    }

    private fun decryptWithPassword(password: String, combined: ByteArray): ByteArray {
        val salt = combined.copyOfRange(0, 16)
        val iv = combined.copyOfRange(16, 28)
        val ciphertext = combined.copyOfRange(28, combined.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 60000, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, ALGO)
    }
}

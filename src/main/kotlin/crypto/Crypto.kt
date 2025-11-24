package crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object Crypto {

    init { Security.addProvider(BouncyCastleProvider()) }

    private const val AES_GCM = "AES/GCM/NoPadding"

    fun encrypt(plain: ByteArray): ByteArray {
        val key = KeyStore.masterKey
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))

        return iv + cipher.doFinal(plain)
    }

    fun decrypt(combined: ByteArray): ByteArray {
        val key = KeyStore.masterKey

        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)

        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

        return cipher.doFinal(ciphertext)
    }
}

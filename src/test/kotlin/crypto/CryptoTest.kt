package crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class CryptoTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            // 1) Set password for KeyStore in tests
            System.setProperty("PINBOT_MASTER_PASSWORD", "test-password-123")

            // 2) Ensure we don't reuse a real keyfile from your repo
            // (delete test keyfile if it exists)
            File("pinbot.key").delete()
        }
    }

    @Test
    fun `encrypt then decrypt returns original plaintext`() {
        val plaintext = "hello wire pin bot"
        val encrypted = Crypto.encrypt(plaintext.toByteArray(Charsets.UTF_8))
        val decrypted = Crypto.decrypt(encrypted).toString(Charsets.UTF_8)

        assertEquals(plaintext, decrypted)
    }
}

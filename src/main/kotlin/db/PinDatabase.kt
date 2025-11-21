package db
import java.sql.Connection
import java.sql.DriverManager

object PinDatabase {

    private val connection: Connection by lazy {
        val url = "jdbc:sqlite:pins.db"
        val conn = DriverManager.getConnection(url)

        conn.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS pinned_messages (
                    conversation_id TEXT PRIMARY KEY,
                    encrypted_value BLOB NOT NULL,
                    updated_at INTEGER NOT NULL
                );
                """
            )
        }
        conn
    }

    fun setEncryptedPin(conversationId: String, encrypted: ByteArray) {
        val sql = """
            INSERT INTO pinned_messages (conversation_id, encrypted_value, updated_at)
            VALUES (?, ?, strftime('%s','now'))
            ON CONFLICT(conversation_id)
            DO UPDATE SET encrypted_value = excluded.encrypted_value,
                          updated_at = excluded.updated_at;
        """

        connection.prepareStatement(sql).use {
            it.setString(1, conversationId)
            it.setBytes(2, encrypted)
            it.executeUpdate()
        }
    }

    fun getEncryptedPin(conversationId: String): ByteArray? {
        val sql = "SELECT encrypted_value FROM pinned_messages WHERE conversation_id = ?;"
        connection.prepareStatement(sql).use {
            it.setString(1, conversationId)
            val rs = it.executeQuery()
            return if (rs.next()) rs.getBytes("encrypted_value") else null
        }
    }

    fun deletePin(conversationId: String) {
        val sql = "DELETE FROM pinned_messages WHERE conversation_id = ?;"
        connection.prepareStatement(sql).use {
            it.setString(1, conversationId)
            it.executeUpdate()
        }
    }
}

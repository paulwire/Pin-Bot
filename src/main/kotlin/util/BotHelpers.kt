package util

import com.wire.sdk.model.QualifiedId
import com.wire.sdk.model.WireMessage
import handler.SampleEventsHandler
import java.util.UUID

class BotHelpers {

    suspend fun getBotMention(handler: SampleEventsHandler): String {
        val userIdString = System.getenv("WIRE_SDK_USER_ID")
            ?: throw IllegalStateException("WIRE_SDK_USER_ID not set")

        val environment = System.getenv("WIRE_SDK_ENVIRONMENT")
            ?: throw IllegalStateException("WIRE_SDK_ENVIRONMENT not set")

        val userId = UUID.fromString(userIdString)
        val qualifiedId = QualifiedId(userId, environment)

        val botName = handler.manager.getUserSuspending(qualifiedId).name
        return "@$botName"
    }

    fun sendHelp(
        conversationId: QualifiedId,
        wireMessage: WireMessage.Text,
        botMention: String,
        handler: SampleEventsHandler
    ) {
        val text =
            if (botMention.isBlank()) {
                "Hey, you have to mention me first if you want my help"
            } else """
                🤖 How to use $botMention

                ⚒️ **Usage**:
                $botMention pin "your message"

                🧩 **Example**:
                $botMention pin "Welcome to the group!"

                ✍️ **Update an existing pinned message**:
                $botMention update "your new pinned message"

                👀 **Read back current pin**:
                $botMention check

                🛟 **Help**:
                $botMention help

                ℹ️ **Note**: Only admins can set a pinned message.
            """.trimIndent()

        val msg = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = text,
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )
        handler.manager.sendMessage(msg)
    }


    fun sendHelpOnJoin(
        conversationId: QualifiedId,
        botMention: String,
        handler: SampleEventsHandler
    ) {
        val text = """
            🤖 How to use $botMention

            ⚒️ **Add a new pinned message**:
            $botMention pin "your message"

            🔄 **Update a pinned message**
            $botMention update "your message"

            👀 **Read back current pin**:
            $botMention check

            ℹ️ **Note**: Only admins can set a pinned message.
        """.trimIndent()

        val msg = WireMessage.Text.create(
            conversationId = conversationId,
            text = text
        )
        handler.manager.sendMessage(msg)
    }

    fun sendAdminOnly(conversationId: QualifiedId, handler: SampleEventsHandler) {
        val msg = WireMessage.Text.create(
            conversationId = conversationId,
            text = "Sorry, only group admins can pin messages"
        )
        handler.manager.sendMessage(msg)
    }

    fun sendPinConfirmation(
        conversationId: QualifiedId,
        wireMessage: WireMessage.Text,
        pinnedText: String,
        handler: SampleEventsHandler
    ) {
        val msg = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = "📌 I pinned this message: $pinnedText",
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )
        handler.manager.sendMessage(msg)
    }
}

import com.wire.sdk.WireAppSdk
import com.wire.sdk.WireEventsHandlerSuspending
import com.wire.sdk.model.ConversationData
import com.wire.sdk.model.ConversationMember
import com.wire.sdk.model.QualifiedId
import com.wire.sdk.model.WireMessage
import java.util.UUID

fun main() {
    val wireAppSdk = WireAppSdk(
        applicationId = UUID.randomUUID(),
        apiToken = "myApiToken",
        apiHost = "https://staging-nginz-https.zinfra.io",
        cryptographyStoragePassword = "myDummyPasswordOfRandom32BytesCH",
        wireEventsHandler = SampleEventsHandler()
    )
    wireAppSdk.startListening()
}

class SampleEventsHandler : WireEventsHandlerSuspending() {

    private val pinnedMessagesByConversation = mutableMapOf<UUID, String>()

    // ------------------------------------------------------------
    // MESSAGE HANDLER
    // ------------------------------------------------------------

    override suspend fun onMessage(wireMessage: WireMessage.Text) {
        val conversationId = wireMessage.conversationId
        val botId = manager.getApplicationDataSuspending().appClientId

        // Build message to inspect mentions
        val msg = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = wireMessage.text,
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )

        val text = msg.text.trim()

        // ------------------------------------------------------------
        // BLOCK 1 — CHECKS
        // ------------------------------------------------------------

        // A) No mentions at all → ignore
        if (msg.mentions.isEmpty()) return

        val firstMention = msg.mentions[0].userId.toString().take(7)
        val botShortId = botId.take(7)

        // B) First mention is NOT the bot → check if bot mentioned later
        if (firstMention != botShortId) {
            for (m in msg.mentions) {
                if (m.userId.toString().take(7) == botShortId) {
                    sendHelp(conversationId, wireMessage, "")
                    return
                }
            }
            return
        }

        // C) Extract "@BotName"
        val firstSpace = text.indexOf(" ")
        val mentionText = if (firstSpace == -1) text else text.substring(0, firstSpace)
// --- UPDATE COMMAND HANDLING ---
        val updateRegex = Regex("""update\s+"([^"]*)"""", RegexOption.IGNORE_CASE)
        val updateMatch = updateRegex.find(text)

        if (updateMatch != null) {
            // Only admins can update
            if (!isUserAdmin(wireMessage)) {
                sendAdminOnly(conversationId)
                return
            }

            val newPinnedText = updateMatch.groupValues[1].trim()
            if (newPinnedText.isBlank()) {
                val msgEmptyUpdate = WireMessage.Text.createReply(
                    conversationId = conversationId,
                    text = "Update failed: the message cannot be empty.",
                    originalMessage = wireMessage,
                    mentions = wireMessage.mentions
                )
                manager.sendMessage(msgEmptyUpdate)
                return
            }

            pinnedMessagesByConversation[conversationId.id] = newPinnedText

            val updateConfirmation = WireMessage.Text.createReply(
                conversationId = conversationId,
                text = "🔄 Updated pinned message: \"$newPinnedText\"",
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )
            manager.sendMessage(updateConfirmation)
            return
        }

        // If only "@BotName" was sent
        if (firstSpace == -1) {
            sendHelp(conversationId, wireMessage, mentionText)
            return
        }

        // ------------------------------------------------------------
        // BLOCK 2 — COMMAND LOGIC
        // ------------------------------------------------------------
        // CHECK command: "@Bot check"
        if (text.contains("check", ignoreCase = true)) {
            val pinned = pinnedMessagesByConversation[conversationId.id]

            val response = if (pinned.isNullOrEmpty()) {
                "There is no pinned message yet."
            } else {
                "📌 Currently pinned message:\n\"$pinned\""
            }

            val reply = WireMessage.Text.createReply(
                conversationId = conversationId,
                text = response,
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )
            manager.sendMessage(reply)
            return
        }

        // 1) Help request
        if (text.contains("help", ignoreCase = true)) {
            sendHelp(conversationId, wireMessage, mentionText)
            return
        }

        // 2) Pin syntax rules
        val regex = Regex("""pin\s+"([^"]*)"""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)

        if (!text.contains("pin", ignoreCase = true)) {
            sendHelp(conversationId, wireMessage, mentionText)
            return
        }

        if (!text.contains("\"")) {
            sendHelp(conversationId, wireMessage, mentionText)
            return
        }

        if (match != null && match.groupValues[1].isBlank()) {
            sendHelp(conversationId, wireMessage, mentionText)
            return
        }

        // 3) Only admins may pin
        if (!isUserAdmin(wireMessage)) {
            sendAdminOnly(conversationId)
            return
        }
        // If a pinned message already exists, don't allow overwriting
        val existingPinned = pinnedMessagesByConversation[conversationId.id]
        if (!existingPinned.isNullOrEmpty()) {
            val warning = WireMessage.Text.createReply(
                conversationId = conversationId,
                text = "❗ A pinned message already exists.\n" +
                        "Use ${mentionText} update \"new message\" instead.\n\n" +
                        "Currently pinned message:\n\"$existingPinned\"",
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )
            manager.sendMessage(warning)
            return
        }

        // 4) Valid PIN
        if (match != null) {
            val pinnedText = match.groupValues[1].trim()
            pinnedMessagesByConversation[conversationId.id] = pinnedText
            sendPinConfirmation(conversationId, wireMessage, pinnedText)
            return
        }

        // Fallback
        sendHelp(conversationId, wireMessage, mentionText)
    }

    // ------------------------------------------------------------
    // ADMIN CHECK
    // ------------------------------------------------------------
    override suspend fun onMemberLeave(conversationId: QualifiedId, members: List<QualifiedId>) {
        super.onMemberLeave(conversationId, members)

        val botUserIdString = System.getenv("WIRE_SDK_USER_ID")
            ?: throw IllegalStateException("WIRE_SDK_USER_ID not set")

        val environment = System.getenv("WIRE_SDK_ENVIRONMENT")
            ?: throw IllegalStateException("WIRE_SDK_ENVIRONMENT not set")

        val botQualifiedId = QualifiedId(UUID.fromString(botUserIdString), environment)

        // Was the bot removed?
        val botRemoved = members.any { it == botQualifiedId }

        if (botRemoved) {
            pinnedMessagesByConversation.remove(conversationId.id)
            println("Pin removed because bot left conversation $conversationId")
        }
    }


    private fun isUserAdmin(wireMessage: WireMessage.Text): Boolean {
        //somehow this check does not work, if the bot is added during group creation
        val userIdString = System.getenv("WIRE_SDK_USER_ID")
            ?: throw IllegalStateException("WIRE_SDK_USER_ID not set")
        val environment = System.getenv("WIRE_SDK_ENVIRONMENT")
            ?: throw IllegalStateException("WIRE_SDK_ENVIRONMENT not set")

        val userId = UUID.fromString(userIdString)
        val qualifiedId = QualifiedId(userId, environment)

        val members = manager.getStoredConversationMembers(wireMessage.conversationId)
        return members.any { it.userId == qualifiedId && it.role.toString() == "ADMIN" }
    }

    // ------------------------------------------------------------
    // EVENTS
    // ------------------------------------------------------------

    override suspend fun onConversationJoin(conversation: ConversationData, members: List<ConversationMember>) {
        super.onConversationJoin(conversation, members)
        val botMention = getBotMention()
        sendHelpOnJoin(conversation.id, botMention)
    }

    override suspend fun onMemberJoin(conversationId: QualifiedId, members: List<ConversationMember>) {
        super.onMemberJoin(conversationId, members)

        val pinned = pinnedMessagesByConversation[conversationId.id]
        if (!pinned.isNullOrEmpty()) {
            val msg = WireMessage.Text.create(
                conversationId = conversationId,
                text = "📌 $pinned"
            )
            manager.sendMessage(msg)
        }
    }

    // ------------------------------------------------------------
    // HELPERS — BOT NAME / MESSAGES
    // ------------------------------------------------------------

    private suspend fun getBotMention(): String {
        val userIdString = System.getenv("WIRE_SDK_USER_ID")
            ?: throw IllegalStateException("WIRE_SDK_USER_ID not set")
        val environment = System.getenv("WIRE_SDK_ENVIRONMENT")
            ?: throw IllegalStateException("WIRE_SDK_ENVIRONMENT not set")

        val userId = UUID.fromString(userIdString)
        val qualifiedId = QualifiedId(userId, environment)

        val botName = manager.getUserSuspending(qualifiedId).name
        return "@$botName"
    }

    // ------------------------------------------------------------
    // BLOCK 3 — MESSAGE SENDING
    // ------------------------------------------------------------

    private fun sendHelp(
        conversationId: QualifiedId,
        wireMessage: WireMessage.Text,
        botMention: String
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
        manager.sendMessage(msg)
    }

    private fun sendHelpOnJoin(conversationId: QualifiedId, botMention: String) {
        val text =
            if (botMention.isBlank()) {
                "Hey, you have to mention me first if you want my help"
            } else """
                🤖 How to use $botMention

                ⚒️ **Add a new pinned message**:
                $botMention pin "your message"

                🔄 **Update a pinned message**
                $botMention update "your message"
                
                🧩 **Example**:
                $botMention pin "Welcome to the group!"

                👀 **Read back current pin**: 
                $botMention check
                
                🛟 **Help**:
                $botMention help

                ℹ️ **Note**: Only admins can set a pinned message.
            """.trimIndent()

        val msg = WireMessage.Text.create(
            conversationId = conversationId,
            text = text
        )
        manager.sendMessage(msg)
    }

    private fun sendAdminOnly(conversationId: QualifiedId) {
        val msg = WireMessage.Text.create(
            conversationId = conversationId,
            text = "Sorry, only group admins can pin messages"
        )
        manager.sendMessage(msg)
    }

    private fun sendPinConfirmation(
        conversationId: QualifiedId,
        wireMessage: WireMessage.Text,
        pinnedText: String
    ) {
        val msg = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = "📌 I pinned this message: $pinnedText",
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )
        manager.sendMessage(msg)
    }
}

package logic

import com.wire.sdk.model.WireMessage
import db.PinDatabase
import handler.SampleEventsHandler
import util.BotHelpers

class PinLogic {

    suspend fun handleTextMessage(
        wireMessage: WireMessage.Text,
        adminCheck: AdminCheck,
        helpers: BotHelpers,
        handler: SampleEventsHandler
    ) {
        val manager = handler.manager
        val conversationId = wireMessage.conversationId
        val botId = manager.getApplicationDataSuspending().appClientId

        val msg = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = wireMessage.text,
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )

        val text = msg.text.trim()

        // ------------------------------------------------------------
        // CHECKS
        // ------------------------------------------------------------

        if (msg.mentions.isEmpty()) return

        val firstMention = msg.mentions[0].userId.toString().take(7)
        val botShortId = botId.take(7)

        if (firstMention != botShortId) {
            if (msg.mentions.any { it.userId.toString().take(7) == botShortId }) {
                helpers.sendHelp(conversationId, wireMessage, "", handler)
            }
            return
        }

        val firstSpace = text.indexOf(" ")
        val mentionText = if (firstSpace == -1) text else text.substring(0, firstSpace)

        // ------------------------------------------------------------
        // UPDATE
        // ------------------------------------------------------------

        val updateRegex = Regex("""update\s+"([^"]*)"""", RegexOption.IGNORE_CASE)
        val updateMatch = updateRegex.find(text)

        if (updateMatch != null) {

            if (!adminCheck.isUserAdmin(wireMessage, handler)) {
                helpers.sendAdminOnly(conversationId, handler)
                return
            }

            val newPinnedText = updateMatch.groupValues[1].trim()
            if (newPinnedText.isBlank()) {
                val emptyReply = WireMessage.Text.createReply(
                    conversationId = conversationId,
                    text = "Update failed: the message cannot be empty.",
                    originalMessage = wireMessage,
                    mentions = wireMessage.mentions
                )
                manager.sendMessage(emptyReply)
                return
            }

            PinDatabase.setEncryptedPin(
                conversationId.id.toString(),
                newPinnedText.toByteArray()
            )

            val confirm = WireMessage.Text.createReply(
                conversationId = conversationId,
                text = "🔄 Updated pinned message: \"$newPinnedText\"",
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )
            manager.sendMessage(confirm)
            return
        }

        if (firstSpace == -1) {
            helpers.sendHelp(conversationId, wireMessage, mentionText, handler)
            return
        }

        // ------------------------------------------------------------
        // CHECK command
        // ------------------------------------------------------------

        if (text.contains("check", ignoreCase = true)) {
            val pinnedBytes = PinDatabase.getEncryptedPin(conversationId.id.toString())
            val pinned = pinnedBytes?.toString(Charsets.UTF_8)

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

        if (text.contains("help", ignoreCase = true)) {
            helpers.sendHelp(conversationId, wireMessage, mentionText, handler)
            return
        }

        // ------------------------------------------------------------
        // PIN command
        // ------------------------------------------------------------

        val regex = Regex("""pin\s+"([^"]*)"""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)

        if (!text.contains("pin", ignoreCase = true)
            || !text.contains("\"")
            || (match != null && match.groupValues[1].isBlank())
        ) {
            helpers.sendHelp(conversationId, wireMessage, mentionText, handler)
            return
        }

        if (!adminCheck.isUserAdmin(wireMessage, handler)) {
            helpers.sendAdminOnly(conversationId, handler)
            return
        }

        val existingPinnedBytes = PinDatabase.getEncryptedPin(conversationId.id.toString())
        val existingPinned = existingPinnedBytes?.toString(Charsets.UTF_8)

        if (!existingPinned.isNullOrEmpty()) {
            val warn = WireMessage.Text.createReply(
                conversationId = conversationId,
                text = "❗ A pinned message already exists.\n" +
                        "Use ${mentionText} update \"new message\" instead.\n\n" +
                        "Currently pinned message:\n\"$existingPinned\"",
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )
            manager.sendMessage(warn)
            return
        }

        if (match != null) {
            val pinnedText = match.groupValues[1].trim()

            PinDatabase.setEncryptedPin(
                conversationId.id.toString(),
                pinnedText.toByteArray()
            )

            helpers.sendPinConfirmation(conversationId, wireMessage, pinnedText, handler)
            return
        }

        helpers.sendHelp(conversationId, wireMessage, mentionText, handler)
    }
}


package handler

import com.wire.sdk.WireEventsHandlerSuspending
import com.wire.sdk.model.ConversationData
import com.wire.sdk.model.ConversationMember
import com.wire.sdk.model.QualifiedId
import com.wire.sdk.model.WireMessage
import db.PinDatabase
import logic.PinLogic
import logic.AdminCheck
import util.BotHelpers
import java.util.UUID
import crypto.Crypto

class SampleEventsHandler : WireEventsHandlerSuspending() {

    private val pinLogic = PinLogic()
    private val adminCheck = AdminCheck()
    private val helpers = BotHelpers()

    override suspend fun onTextMessageReceived(wireMessage: WireMessage.Text) {
        pinLogic.handleTextMessage(wireMessage, adminCheck, helpers, this)
    }

    override suspend fun onAppAddedToConversation(
        conversation: ConversationData,
        members: List<ConversationMember>
    ) {
        super.onAppAddedToConversation(conversation, members)
        val botMention = helpers.getBotMention(this)
        helpers.sendHelpOnJoin(conversation.id, botMention, this)
    }

    override suspend fun onUserJoinedConversation(
        conversationId: QualifiedId,
        members: List<ConversationMember>
    ) {
        super.onUserJoinedConversation(conversationId, members)

//        val pinnedBytes = PinDatabase.getEncryptedPin(conversationId.id.toString())
//        val pinned = pinnedBytes?.toString(Charsets.UTF_8)
        val pinnedBytes = PinDatabase.getEncryptedPin(conversationId.id.toString())
        val pinned = pinnedBytes?.let { Crypto.decrypt(it).toString(Charsets.UTF_8) }

        if (!pinned.isNullOrEmpty()) {
            val msg = WireMessage.Text.create(
                conversationId = conversationId,
                text = "📌 $pinned"
            )
            manager.sendMessage(msg)
        }
    }

    override suspend fun onUserLeftConversation(
        conversationId: QualifiedId,
        members: List<QualifiedId>
    ) {
        super.onUserLeftConversation(conversationId, members)

        val botUserIdString = System.getenv("WIRE_SDK_USER_ID")
            ?: throw IllegalStateException("WIRE_SDK_USER_ID not set")

        val environment = System.getenv("WIRE_SDK_ENVIRONMENT")
            ?: throw IllegalStateException("WIRE_SDK_ENVIRONMENT not set")

        val botQualifiedId = QualifiedId(UUID.fromString(botUserIdString), environment)

        val botRemoved = members.any { it == botQualifiedId }

        if (botRemoved) {
            PinDatabase.deletePin(conversationId.id.toString())
            println("Pin removed because bot left conversation $conversationId")
        }
    }
}

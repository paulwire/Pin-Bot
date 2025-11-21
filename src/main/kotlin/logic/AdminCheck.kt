package logic

import com.wire.sdk.model.WireMessage
import handler.SampleEventsHandler
import com.wire.sdk.model.QualifiedId
import java.util.UUID

class AdminCheck {

    fun isUserAdmin(
        wireMessage: WireMessage.Text,
        handler: SampleEventsHandler
    ): Boolean {
        val environment = System.getenv("WIRE_SDK_ENVIRONMENT")
            ?: throw IllegalStateException("WIRE_SDK_ENVIRONMENT not set")

        val qualifiedId = QualifiedId(wireMessage.sender.id, environment)
        val members = handler.manager.getStoredConversationMembers(wireMessage.conversationId)

        return members.any { it.userId == qualifiedId && it.role.toString() == "ADMIN" }
    }
}

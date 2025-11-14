import com.wire.sdk.WireAppSdk
import com.wire.sdk.WireEventsHandlerSuspending
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
    override suspend fun onMessage(wireMessage: WireMessage.Text) {
        val conversationId = wireMessage.conversationId
        val message = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = wireMessage.text,
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )

        val botId = manager.getApplicationDataSuspending().appClientId
        if (message.mentions.isNotEmpty() &&
            message.mentions[0].userId.toString().take(7) == botId.take(7)) {

            // Remove the mention from the text
            val mentionLength = message.mentions[0].length
            val textWithoutMention = message.text.drop(mentionLength + 1).trim()

            // Store pinned message for this conversation
            pinnedMessagesByConversation[conversationId.id] = textWithoutMention

            val confirmationMessage = WireMessage.Text.createReply(
                conversationId = conversationId,
                text = "I pinned this message: $textWithoutMention",
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )
            manager.sendMessage(confirmationMessage)
        }
    }


    override suspend fun onMemberJoin(conversationId: QualifiedId, members: List<ConversationMember>) {
        super.onMemberJoin(conversationId, members)

        val pinned = pinnedMessagesByConversation[conversationId.id]  // null if nothing pinned
        if (!pinned.isNullOrEmpty()) {
            val message = WireMessage.Text.create(
                conversationId = conversationId,
                text = "📌 $pinned"
            )
            manager.sendMessage(message)
        }
    }


}
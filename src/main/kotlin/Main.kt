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
    private var pinnedMessage: String = ""
    override suspend fun onMessage(wireMessage: WireMessage.Text) {
        val message = WireMessage.Text.createReply(
            conversationId = wireMessage.conversationId,
            text = wireMessage.text,
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )

        val botId = manager.getApplicationDataSuspending().appClientId
        if(message.mentions [0].userId.toString().take(7) == botId.take(7)){
            pinnedMessage = message.text.substring(message.mentions [0].length+1)
            val confirmationMessage = WireMessage.Text.createReply(
                conversationId = wireMessage.conversationId,
                text = "I pinned this message: $pinnedMessage",
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )
            manager.sendMessage(confirmationMessage)
        }
    }

    override suspend fun onMemberJoin(conversationId: QualifiedId, members: List<ConversationMember>) {
        super.onMemberJoin(conversationId, members)
        if (pinnedMessage.isNotEmpty()){
            val message = WireMessage.Text.create(
                conversationId = conversationId,
                text = "📌 $pinnedMessage"
            )
            manager.sendMessage(message)
        }
    }

}
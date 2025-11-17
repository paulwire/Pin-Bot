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
        val botId = manager.getApplicationDataSuspending().appClientId
        // Must build message to inspect mentions
        val msg = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = wireMessage.text,
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )
        // check if there are any mentions or if bot is mentioned in
        if (msg.mentions.isEmpty() ||
            msg.mentions[0].userId.toString().take(7) != botId.take(7)) {
            //find if bot is mentioned not in first place
            for(i in 1..msg.mentions.size){
                if(msg.mentions[i-1].userId.toString().take(7) == botId.take(7)){
                    sendHelp(conversationId,wireMessage,"")
                    return
                }
            }
            return
        }

        val text = msg.text.trim()
        val firstSpaceIndex = text.indexOf(" ")
        val mentionText =
            if (firstSpaceIndex == -1){
                text
            } // no space → whole text is mention
            else text.substring(0, firstSpaceIndex)
        if (firstSpaceIndex == -1){
            sendHelp(conversationId,wireMessage,mentionText)
            return
        }
        // 1) Check for explicit help: "@Bot help"
        if (text.contains("help", ignoreCase = true)) {
            sendHelp(conversationId, wireMessage,mentionText)
            return
        }

        // 2) Check for pin command syntax
        val regex = Regex("""pin\s+"([^"]*)"""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)

        // CASE A — no "pin" keyword → show help
        if (!text.contains("pin", ignoreCase = true)) {
            sendHelp(conversationId, wireMessage,mentionText)
            return
        }

        // CASE B — pin exists but no quotes → show help
        if (!text.contains("\"")) {
            sendHelp(conversationId, wireMessage,mentionText)
            return
        }

        // CASE C — quotes present but empty text → show help
        if (match != null && match.groupValues[1].isBlank()) {
            sendHelp(conversationId, wireMessage,mentionText)
            return
        }

        // CASE D — valid pin
        if (match != null) {
            val pinnedText = match.groupValues[1].trim()

            pinnedMessagesByConversation[conversationId.id] = pinnedText

            val confirmationMessage = WireMessage.Text.createReply(
                conversationId = conversationId,
                text = "📌 I pinned this message: $pinnedText",
                originalMessage = wireMessage,
                mentions = wireMessage.mentions
            )

            manager.sendMessage(confirmationMessage)
            return
        }

        // Fallback: if nothing matches, show help
        sendHelp(conversationId, wireMessage,mentionText)
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
    private fun sendHelp(
        conversationId: QualifiedId,
        wireMessage: WireMessage.Text,
        botMention: String
    ) {
        val helpText: String
        if(botMention==""){
            helpText= "Hey, you have to mention me first if you want my help"

        }else {
            helpText = """
        🤖 How to use $botMention

        Usage:
        $botMention pin "your message"
        
        Example:
        $botMention pin "Welcome to the group!"""".trimIndent()
        }
        val helpMessage = WireMessage.Text.createReply(
            conversationId = conversationId,
            text = helpText,
            originalMessage = wireMessage,
            mentions = wireMessage.mentions
        )

        manager.sendMessage(helpMessage)
    }
}
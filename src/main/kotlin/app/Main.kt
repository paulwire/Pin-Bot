package app

import com.wire.sdk.WireAppSdk
import handler.SampleEventsHandler
import java.util.UUID
// this runs, however @bot pin "this" does not seem to work, needs to be debugged
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

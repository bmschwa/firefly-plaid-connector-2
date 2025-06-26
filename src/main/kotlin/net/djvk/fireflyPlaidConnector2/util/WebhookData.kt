package net.djvk.fireflyPlaidConnector2.util

import kotlinx.serialization.Serializable
import net.djvk.fireflyPlaidConnector2.constants.IntervalMilliSecs

@Serializable
data class WebhookData(

    val loopDurationMs: IntervalMilliSecs

){

}

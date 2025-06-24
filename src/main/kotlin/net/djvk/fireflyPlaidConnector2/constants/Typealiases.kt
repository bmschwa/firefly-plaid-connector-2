package net.djvk.fireflyPlaidConnector2.constants
import kotlinx.serialization.Serializable


typealias IntervalSeconds = Long
typealias IntervalMinutes = Long

@JvmInline
@Serializable
value class IntervalMilliSecs(val value: Long)
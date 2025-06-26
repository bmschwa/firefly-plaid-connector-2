package net.djvk.fireflyPlaidConnector2.constants
import kotlinx.serialization.Serializable


typealias IntervalSeconds = Long
typealias IntervalMinutes = Long

typealias FireflyTransactionId = String
typealias FireflyExternalId = String
typealias FireflyTransactionSplitId = String
typealias PlaidAccountId = String
typealias FireflyAccountId = Int


@JvmInline
@Serializable
value class IntervalMilliSecs(val value: Long)
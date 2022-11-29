/**
 * The Plaid API
 *
 * The Plaid REST API. Please see https://plaid.com/docs/api for more details.
 *
 * The version of the OpenAPI document: 2020-09-14_1.164.8
 *
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package net.djvk.fireflyPlaidConnector2.api.plaid.models


import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A repayment is created automatically after one or more guaranteed transactions receive a return. If there are multiple eligible returns in a day, they are batched together into a single repayment.  Repayments are sent over ACH, with funds typically available on the next banking day.
 *
 * @param repaymentId Identifier of the repayment.
 * @param created The datetime when the repayment occurred, in RFC 3339 format.
 * @param amount Decimal amount of the repayment as it appears on your account ledger.
 * @param isoCurrencyCode The currency of the repayment, e.g. \"USD\".
 */

data class TransferRepayment(

    /* Identifier of the repayment. */
    @field:JsonProperty("repayment_id")
    val repaymentId: kotlin.String,

    /* The datetime when the repayment occurred, in RFC 3339 format. */
    @field:JsonProperty("created")
    val created: java.time.OffsetDateTime,

    /* Decimal amount of the repayment as it appears on your account ledger. */
    @field:JsonProperty("amount")
    val amount: kotlin.String,

    /* The currency of the repayment, e.g. \"USD\". */
    @field:JsonProperty("iso_currency_code")
    val isoCurrencyCode: kotlin.String

) : kotlin.collections.HashMap<String, kotlin.Any>()

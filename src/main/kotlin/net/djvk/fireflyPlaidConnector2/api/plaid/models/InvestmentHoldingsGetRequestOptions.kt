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
 * An optional object to filter `/investments/holdings/get` results. If provided, must not be `null`.
 *
 * @param accountIds An array of `account_id`s to retrieve for the Item. An error will be returned if a provided `account_id` is not associated with the Item.
 */

data class InvestmentHoldingsGetRequestOptions(

    /* An array of `account_id`s to retrieve for the Item. An error will be returned if a provided `account_id` is not associated with the Item. */
    @field:JsonProperty("account_ids")
    val accountIds: kotlin.collections.List<kotlin.String>? = null

)

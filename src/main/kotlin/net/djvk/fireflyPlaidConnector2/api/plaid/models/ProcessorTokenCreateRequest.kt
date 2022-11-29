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
 * ProcessorTokenCreateRequest defines the request schema for `/processor/token/create`
 *
 * @param accessToken The access token associated with the Item data is being requested for.
 * @param accountId The `account_id` value obtained from the `onSuccess` callback in Link
 * @param processor The processor you are integrating with.
 * @param clientId Your Plaid API `client_id`. The `client_id` is required and may be provided either in the `PLAID-CLIENT-ID` header or as part of a request body.
 * @param secret Your Plaid API `secret`. The `secret` is required and may be provided either in the `PLAID-SECRET` header or as part of a request body.
 */

data class ProcessorTokenCreateRequest(

    /* The access token associated with the Item data is being requested for. */
    @field:JsonProperty("access_token")
    val accessToken: kotlin.String,

    /* The `account_id` value obtained from the `onSuccess` callback in Link */
    @field:JsonProperty("account_id")
    val accountId: kotlin.String,

    /* The processor you are integrating with. */
    @field:JsonProperty("processor")
    val processor: Processor,

    /* Your Plaid API `client_id`. The `client_id` is required and may be provided either in the `PLAID-CLIENT-ID` header or as part of a request body. */
    @field:JsonProperty("client_id")
    val clientId: kotlin.String? = null,

    /* Your Plaid API `secret`. The `secret` is required and may be provided either in the `PLAID-SECRET` header or as part of a request body. */
    @field:JsonProperty("secret")
    val secret: kotlin.String? = null

) {

    /**
     * The processor you are integrating with.
     *
     * Values: dwolla,galileo,modernTreasury,ocrolus,primeTrust,vesta,drivewealth,vopay,achq,check,checkbook,circle,silaMoney,rize,svbApi,unit,wyre,lithic,alpaca,astra,moov,treasuryPrime,marqeta,checkout,solid,highnote,apexClearing,gusto
     */
    enum class Processor(val value: kotlin.String) {
        @JsonProperty(value = "dwolla")
        dwolla("dwolla"),
        @JsonProperty(value = "galileo")
        galileo("galileo"),
        @JsonProperty(value = "modern_treasury")
        modernTreasury("modern_treasury"),
        @JsonProperty(value = "ocrolus")
        ocrolus("ocrolus"),
        @JsonProperty(value = "prime_trust")
        primeTrust("prime_trust"),
        @JsonProperty(value = "vesta")
        vesta("vesta"),
        @JsonProperty(value = "drivewealth")
        drivewealth("drivewealth"),
        @JsonProperty(value = "vopay")
        vopay("vopay"),
        @JsonProperty(value = "achq")
        achq("achq"),
        @JsonProperty(value = "check")
        check("check"),
        @JsonProperty(value = "checkbook")
        checkbook("checkbook"),
        @JsonProperty(value = "circle")
        circle("circle"),
        @JsonProperty(value = "sila_money")
        silaMoney("sila_money"),
        @JsonProperty(value = "rize")
        rize("rize"),
        @JsonProperty(value = "svb_api")
        svbApi("svb_api"),
        @JsonProperty(value = "unit")
        unit("unit"),
        @JsonProperty(value = "wyre")
        wyre("wyre"),
        @JsonProperty(value = "lithic")
        lithic("lithic"),
        @JsonProperty(value = "alpaca")
        alpaca("alpaca"),
        @JsonProperty(value = "astra")
        astra("astra"),
        @JsonProperty(value = "moov")
        moov("moov"),
        @JsonProperty(value = "treasury_prime")
        treasuryPrime("treasury_prime"),
        @JsonProperty(value = "marqeta")
        marqeta("marqeta"),
        @JsonProperty(value = "checkout")
        checkout("checkout"),
        @JsonProperty(value = "solid")
        solid("solid"),
        @JsonProperty(value = "highnote")
        highnote("highnote"),
        @JsonProperty(value = "apex_clearing")
        apexClearing("apex_clearing"),
        @JsonProperty(value = "gusto")
        gusto("gusto");
    }
}

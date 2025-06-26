package net.djvk.fireflyPlaidConnector2.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory


import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import net.djvk.fireflyPlaidConnector2.api.firefly.models.TransactionRead

import net.djvk.fireflyPlaidConnector2.constants.IntervalMilliSecs
import net.djvk.fireflyPlaidConnector2.constants.FireflyTransactionId
import net.djvk.fireflyPlaidConnector2.transactions.FireflyTransactionDto


import net.djvk.fireflyPlaidConnector2.util.WebhookData


import java.time.Duration

class WebhookService (

    private val resultCallbackUrl: ResultCallbackUrl,
    private val resultCallbackBearerToken: ResultCallbackBearerToken,

    )  {
    private val logger = LoggerFactory.getLogger(this::class.java)


    private var webhookClient :HttpClient? = null
    init{
        logger.trace("Init Webhook Client")

        if  (!resultCallbackBearerToken.isNullOrBlank()){
            webhookClient = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        prettyPrint = false
                        isLenient = true
                    })
                }
            }
            logger.debug("Created Webhook Client for $resultCallbackUrl")
        }
    }

    fun addDataForHook(ffTx: List<TransactionRead>,
                       ptr: PlaidTransactionResult,
                       creates: List<FireflyTransactionDto>,
                       updates: List<FireflyTransactionDto>,
                       deletes: List<FireflyTransactionId>){
        logger.debug("Adding some data for callback")

        val t = buildJsonObject {
            put("fireFlyTransactionsRead", JsonPrimitive(ffTx.size))
        }
    }

    fun enabled() : Boolean{
        webhookClient?.let{
            return true
        }
        return false
    }

    suspend fun post(
        loopDuration: Duration){

        logger.info("Sending results to $resultCallbackUrl")
        val webhookData = WebhookData(
            IntervalMilliSecs(loopDuration.toMillis())
        )

        webhookClient?.use { webhookClient ->
            val response: HttpResponse = webhookClient.post("$resultCallbackUrl") {

                if (!resultCallbackBearerToken.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $resultCallbackBearerToken")

                }
                contentType(ContentType.Application.Json)
                setBody(webhookData)
            }

            logger.info("Webhook Response (${response.status}): ${response.bodyAsText()}")
        } // HttpClient is closed here automatically
    }
}
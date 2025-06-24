package net.djvk.fireflyPlaidConnector2.sync

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import net.djvk.fireflyPlaidConnector2.transactions.FireflyAccountId
import net.djvk.fireflyPlaidConnector2.transactions.TransactionConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

// For the webhook
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.djvk.fireflyPlaidConnector2.api.firefly.models.Webhook
import java.time.Clock
import java.time.Duration

typealias IntervalMinutes = Int
typealias PlaidSyncCursor = String
typealias ResultCallbackUrl = String?


@Serializable
data class WebhookData(

    val loopDurationMillis: Int
)

/**
 * Orchestrates the polled sync process.
 *
 * Handles the "polled" sync mode, which periodically polls for new transactions and processes them.
 * This class coordinates the different components involved in the sync process.
 */
@ConditionalOnProperty(name = ["fireflyPlaidConnector2.syncMode"], havingValue = "polled")
@Component
class PolledSyncOrchestrator(
    @Value("\${fireflyPlaidConnector2.polled.syncFrequencyMinutes}")
    private val syncFrequencyMinutes: IntervalMinutes,

    @Value("\${fireflyPlaidConnector2.polled.resultCallbackUrl:}")
    private val resultCallbackUrl: ResultCallbackUrl,


    private val syncHelper: SyncHelper,
    private val cursorManager: CursorManager,
    private val plaidSyncService: PlaidSyncService,
    private val fireflyTransactionService: FireflyTransactionService,
    private val converter: TransactionConverter,
) : Runner, DisposableBean {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val terminated = AtomicBoolean(false)
    private lateinit var mainJob: Job

    /**
     * Initializes cursors for access tokens that don't have one yet.
     */
    suspend fun initializeCursors() {
        // Read cursor map from storage
        val cursorMap = cursorManager.readCursorMap()

        // Get account mappings
        val (accountMap, accountAccessTokenSequence) = syncHelper.getAllPlaidAccessTokenAccountIdSets()

        // Initialize cursors for access tokens that don't have one yet
        plaidSyncService.initializeCursors(accountAccessTokenSequence, cursorMap)
        cursorManager.writeCursorMap(cursorMap)
    }

    /**
     * Processes transactions by fetching from Plaid, converting, and updating Firefly.
     */
    suspend fun processTransactions(
        accountMap: Map<PlaidAccountId, FireflyAccountId>,
        accountAccessTokenSequence: Sequence<Pair<PlaidAccessToken, List<PlaidAccountId>>>,
        cursorMap: MutableMap<PlaidAccessToken, PlaidSyncCursor>
    ) {
        // Fetch existing Firefly transactions
        val existingFireflyTxs = fireflyTransactionService.fetchExistingFireflyTransactions()

        // Process Plaid transactions
        val plaidTransactions = plaidSyncService.processPlaidTransactions(
            accountAccessTokenSequence,
            cursorMap
        )

        // Convert Plaid transactions to Firefly format
        logger.debug("Converting plaidTransactions transactions to Firefly ${existingFireflyTxs.size} transactions")
        val convertResult = converter.convertPollSync(
            accountMap,
            plaidTransactions.created,
            plaidTransactions.updated,
            plaidTransactions.deleted,
            existingFireflyTxs,
        )
        logger.debug(
            "Conversion result: ${convertResult.creates.size} creates; " +
                    "${convertResult.updates.size} updates; " +
                    "${convertResult.deletes.size} deletes;"
        )

        // Process transaction updates in Firefly
        fireflyTransactionService.processFireflyTransactionUpdates(
            convertResult.creates,
            convertResult.updates,
            convertResult.deletes
        )

        // Update cursor map after successful processing
        cursorManager.writeCursorMap(cursorMap)

    }

    suspend fun hookResults(
        loopDuration: Duration
    ) {
        logger.info("Sending results to $resultCallbackUrl")
        val webhookData = WebhookData(
            loopDuration.toMillis())
        HttpClient(CIO).use { client ->
            val response: HttpResponse = client.post("$resultCallbackUrl") {
                header(HttpHeaders.Authorization, "Bearer GdxY7vELMlfJyBSu6oTJoUASJ6JQf3s0")
                contentType(ContentType.Application.Json)
                setBody(webhookData)
            }

            println("Response status: ${response.status}")
            println("Response body: ${response.bodyAsText()}")
        } // HttpClient is closed here automatically
    }

    override fun run() {
        runBlocking {
            syncHelper.setApiCreds()

            mainJob = launch {
                // Initialize cursors
                initializeCursors()

                // Get account mappings for the polling loop
                val (accountMap, accountAccessTokenSequence) = syncHelper.getAllPlaidAccessTokenAccountIdSets()
                val cursorMap = cursorManager.readCursorMap()
                val clock = Clock.systemUTC()
                /**
                 * Periodic polling loop
                 */
                do {
                    logger.debug("Polling loop start")
                    val loop_start = clock.instant()

                    // Process transactions
                    processTransactions(accountMap, accountAccessTokenSequence, cursorMap)

                    // Trigger GC to try to reduce heap size
                    logger.trace("Calling System.gc()")
                    System.gc()

                    // If configured, report to callback
                    if (! resultCallbackUrl.isNullOrBlank() ){
                        try {
                            hookResults(Duration.between(loop_start, clock.instant()))
                        } catch (e: Exception){
                            logger.error("Failed to post results to $resultCallbackUrl: $e")
                        }

                    }

                    // Sleep until next poll
                    logger.info("Sleeping $syncFrequencyMinutes")
                    delay(syncFrequencyMinutes.minutes)
                } while (!terminated.get())
            }
        }
    }

    override fun destroy() {
        logger.info("Shutting down ${this::class}")
        terminated.set(true)
        mainJob.cancel()
    }
}

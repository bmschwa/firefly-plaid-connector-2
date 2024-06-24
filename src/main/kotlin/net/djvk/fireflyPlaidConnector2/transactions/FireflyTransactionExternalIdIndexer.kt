package net.djvk.fireflyPlaidConnector2.transactions

import net.djvk.fireflyPlaidConnector2.api.firefly.apis.FireflyExternalId
import net.djvk.fireflyPlaidConnector2.api.firefly.apis.FireflyTransactionId
import net.djvk.fireflyPlaidConnector2.api.firefly.models.TransactionRead

class FireflyTransactionExternalIdIndexer(
    existingFireflyTxs: List<TransactionRead>,
) {
    private val fireflyTxsByExternalId: Map<FireflyExternalId, TransactionRead>

    init {
        val out = mutableMapOf<FireflyExternalId, TransactionRead>()
        for (existingFireflyTx in existingFireflyTxs) {
            for (tx in existingFireflyTx.attributes.transactions) {
                if (tx.externalId == null) continue

                out[tx.externalId] = existingFireflyTx
            }
        }

        fireflyTxsByExternalId = out
    }

    fun findExistingFireflyTx(
        plaidTransactionId: String,
    ): TransactionRead? {
        return fireflyTxsByExternalId[getExternalId(plaidTransactionId)]
    }

    companion object {
        fun getExternalId(txId: String): String {
            return "plaid-${txId}"
        }
    }
}

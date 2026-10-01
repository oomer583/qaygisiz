package com.omer.qaygisiz

import android.content.BroadcastReceiver
import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

object Alerter {

    private const val TAG = "Qaygisiz"

    // A BroadcastReceiver's PendingResult is only guaranteed for about ten
    // seconds. Everything below has to finish inside this budget, or the system
    // kills the process and the family member never gets the warning.
    private const val TOTAL_BUDGET_MS = 8500L
    private const val EXPLAIN_BUDGET_MS = 3000L

    fun handle(
        context: Context,
        pending: BroadcastReceiver.PendingResult?,
        sender: String,
        body: String
    ) {
        val result = LinkScanner.scan(sender, body)

        Log.d(TAG, "-----------------------------")
        Log.d(TAG, "from  : $sender")
        Log.d(TAG, "body  : $body")
        Log.d(TAG, "score : ${result.score}  ->  ${result.level}")
        Log.d(TAG, "lang  : ${Settings.language.code}")
        result.urls.forEach { Log.d(TAG, "url   : $it") }
        result.reasons.forEach { Log.d(TAG, "sebeb : ${it.code}") }

        if (result.level == ScanResult.Level.SAFE) {
            Log.d(TAG, "telegram : gonderilmedi (temiz)")
            pending?.finish()
            return
        }

        // The phone's owner is told nothing about the fraud and asked nothing. This
        // only holds them still until their family member calls, and it stays off
        // unless a family deliberately turned it on.
        if (Prefs.showOwnerNotice) UserNotice.show(context)

        Thread {
            val deadline = SystemClock.elapsedRealtime() + TOTAL_BUDGET_MS
            try {
                val explanation = explainWithin(sender, body, result)
                val message = TelegramNotifier.buildAlert(sender, body, result, explanation)

                var sent = TelegramNotifier.send(message)
                if (!sent && SystemClock.elapsedRealtime() < deadline) {
                    Log.w(TAG, "telegram : birinci cehd ugursuz, tekrar")
                    try { Thread.sleep(500) } catch (e: InterruptedException) { }
                    sent = TelegramNotifier.send(message)
                }
                if (!sent) Log.e(TAG, "telegram : XEBERDARLIQ GONDERILMEDI")
            } finally {
                pending?.finish()
            }
        }.start()
    }

    // The explanation is a nice-to-have. The warning is not. If the models are
    // slow or down we stop waiting and send the alert without them.
    private fun explainWithin(
        sender: String,
        body: String,
        result: ScanResult
    ): String? {
        val holder = AtomicReference<String?>(null)
        val worker = Thread {
            try {
                holder.set(
                    OpenRouterExplainer.explain(sender, body, result)
                        ?: GeminiExplainer.explain(sender, body, result)
                )
            } catch (e: Exception) {
                Log.w(TAG, "izah xetasi: " + e.message)
            }
        }
        worker.isDaemon = true
        worker.start()
        worker.join(EXPLAIN_BUDGET_MS)

        if (worker.isAlive) {
            Log.w(TAG, "izah : vaxt bitdi, izahsiz gonderilir")
        } else if (holder.get() == null) {
            Log.w(TAG, "izah : her iki model de cavab vermedi")
        }
        return holder.get()
    }
}

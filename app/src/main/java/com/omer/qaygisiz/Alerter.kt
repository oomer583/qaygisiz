package com.omer.qaygisiz

import android.content.BroadcastReceiver
import android.util.Log

object Alerter {

    private const val TAG = "Qaygisiz"

    fun handle(pending: BroadcastReceiver.PendingResult?, sender: String, body: String) {
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

        Thread {
            try {
                val explanation =
                    OpenRouterExplainer.explain(sender, body, result)
                        ?: GeminiExplainer.explain(sender, body, result)

                if (explanation == null) {
                    Log.w(TAG, "izah : her iki model de cavab vermedi")
                }

                TelegramNotifier.send(
                    TelegramNotifier.buildAlert(sender, body, result, explanation)
                )
            } finally {
                pending?.finish()
            }
        }.start()
    }
}

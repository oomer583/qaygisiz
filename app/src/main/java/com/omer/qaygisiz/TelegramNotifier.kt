package com.omer.qaygisiz

import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramNotifier {

    private const val TAG = "Qaygisiz"

    fun send(text: String): Boolean {
        val token = Prefs.telegramToken
        val chatId = Prefs.telegramChatId

        if (token.isBlank() || token.startsWith("BURAYA") || chatId.isBlank()) {
            Log.w(TAG, "telegram : qurulmayib")
            return false
        }

        return try {
            val conn = URL("https://api.telegram.org/bot$token/sendMessage")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            )

            val payload = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                "&text=" + URLEncoder.encode(text, "UTF-8") +
                "&disable_web_page_preview=true"

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload) }

            val code = conn.responseCode
            val ok = code in 200..299
            if (ok) {
                conn.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "telegram : HTTP $code OK")
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "telegram : HTTP $code $err")
            }
            conn.disconnect()
            ok
        } catch (e: Exception) {
            Log.e(TAG, "telegram xetasi: " + e.message)
            false
        }
    }

    fun buildAlert(
        sender: String,
        body: String,
        result: ScanResult,
        explanation: String?
    ): String {
        val lang = Settings.language

        val head = when (result.level) {
            ScanResult.Level.DANGEROUS -> Texts.headDangerous(lang)
            ScanResult.Level.SUSPICIOUS -> Texts.headSuspicious(lang)
            ScanResult.Level.SAFE -> Texts.headInfo(lang)
        }
        val risk = when (result.level) {
            ScanResult.Level.DANGEROUS -> Texts.riskHigh(lang)
            ScanResult.Level.SUSPICIOUS -> Texts.riskMedium(lang)
            ScanResult.Level.SAFE -> Texts.riskLow(lang)
        }

        val sb = StringBuilder()
        sb.append(head).append("\n")
        sb.append(Texts.riskLabel(lang)).append(": ").append(risk).append("\n")

        val person = Prefs.protectedPerson
        if (person.isNotBlank()) {
            sb.append(Texts.phoneOwner(lang)).append(": ").append(person).append("\n")
        }

        if (!explanation.isNullOrBlank()) {
            sb.append("\n").append(explanation).append("\n")
        }

        sb.append("\n").append(Texts.from(lang)).append(": ").append(sender).append("\n")
        sb.append(Texts.messageLabel(lang)).append(": ").append(body).append("\n")

        if (result.reasons.isNotEmpty()) {
            sb.append("\n").append(Texts.reasonsLabel(lang)).append(":\n")
            result.reasons.forEach {
                sb.append("• ").append(ReasonText.of(it, lang)).append("\n")
            }
        }
        if (result.urls.isNotEmpty()) {
            sb.append("\n").append(Texts.linksLabel(lang)).append(":\n")
            result.urls.forEach { sb.append("• ").append(it).append("\n") }
        }

        sb.append("\n").append(Texts.footer(lang))
        return sb.toString()
    }
}

package com.omer.qaygisiz

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object OpenRouterExplainer {

    private const val TAG = "Qaygisiz"
    private const val PRIMARY = "google/gemma-4-31b-it:free"

    private val FALLBACKS = listOf(
        "qwen/qwen3.8-27b:free",
        "google/gemma-4-26b-a4b-it:free",
        "nvidia/nemotron-3-ultra-550b-a55b:free"
    )

    fun explain(sender: String, body: String, result: ScanResult): String? {
        val key = Secrets.OPENROUTER_API_KEY
        if (key.isBlank()) {
            Log.w(TAG, "openrouter : acar qurulmayib")
            return null
        }

        try {
            val conn = URL("https://openrouter.ai/api/v1/chat/completions")
                .openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 15000
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("X-Title", "Qaygisiz")

            val models = JSONArray()
            FALLBACKS.forEach { models.put(it) }

            val payload = JSONObject()
                .put("model", PRIMARY)
                .put("models", models)
                .put("temperature", 0.2)
                .put("max_tokens", 700)
                .put("reasoning", JSONObject().put("enabled", false))
                .put(
                    "messages",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", Prompts.explainScam(sender, body, result))
                    )
                )
                .toString()

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload) }

            val code = conn.responseCode
            val raw = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()

            val flat = raw.replace("\n", " ").replace("\r", " ")

            if (code !in 200..299) {
                Log.e(TAG, "openrouter : HTTP $code " + flat.take(500))
                return null
            }

            Log.d(TAG, "openrouter raw: " + flat.take(900))

            val obj = JSONObject(raw)
            val used = obj.optString("model", "?")
            val choice = obj.optJSONArray("choices")?.optJSONObject(0)
            val message = choice?.optJSONObject("message")

            var text = ""
            if (message != null && !message.isNull("content")) {
                text = message.optString("content", "").trim()
            }
            if (text.isBlank() || text.equals("null", ignoreCase = true)) {
                val finish = choice?.optString("finish_reason", "?") ?: "?"
                Log.w(TAG, "openrouter ($used) : bos cavab, finish_reason=$finish")
                return null
            }

            Log.d(TAG, "openrouter ($used) : $text")
            return text
        } catch (e: Exception) {
            Log.e(TAG, "openrouter xetasi: " + e.message)
            return null
        }
    }
}
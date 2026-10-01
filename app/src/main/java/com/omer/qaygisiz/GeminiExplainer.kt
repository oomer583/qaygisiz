package com.omer.qaygisiz

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiExplainer {

    private const val TAG = "Qaygisiz"
    private const val MODEL = "gemini-3.8-flash"

    fun explain(sender: String, body: String, result: ScanResult): String? {
        val key = Secrets.GEMINI_API_KEY
        if (key.isBlank() || key.startsWith("BURAYA")) {
            Log.w(TAG, "gemini : acar qurulmayib")
            return null
        }

        try {
            val conn = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    MODEL + ":generateContent?key=" + key
            ).openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 9000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject()
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "text",
                                    Prompts.explainScam(sender, body, result)
                                )
                            )
                        )
                    )
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", 0.2)
                        .put("maxOutputTokens", 1024)
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

            if (code !in 200..299) {
                Log.e(TAG, "gemini : HTTP $code " + raw.take(300))
                return null
            }

            Log.d(TAG, "gemini raw: " + raw.take(700))

            val text = JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            Log.d(TAG, "gemini : $text")
            return if (text.isBlank()) null else text
        } catch (e: Exception) {
            Log.e(TAG, "gemini xetasi: " + e.message)
            return null
        }
    }
}
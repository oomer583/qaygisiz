package com.omer.qaygisiz

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val FILE = "qaygisiz_prefs"

    private var sp: SharedPreferences? = null

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    private fun get(key: String, fallback: String): String {
        val value = sp?.getString(key, null)
        return if (value.isNullOrBlank()) fallback else value
    }

    private fun put(key: String, value: String) {
        sp?.edit()?.putString(key, value.trim())?.apply()
    }

    var language: String
        get() = get("language", Secrets.LANGUAGE)
        set(value) = put("language", value)

    var protectedPerson: String
        get() = get("person", Secrets.PROTECTED_PERSON)
        set(value) = put("person", value)

    var telegramToken: String
        get() = get("token", Secrets.TELEGRAM_BOT_TOKEN)
        set(value) = put("token", value)

    var telegramChatId: String
        get() = get("chat", Secrets.TELEGRAM_CHAT_ID)
        set(value) = put("chat", value)

    val isConfigured: Boolean
        get() {
            val t = telegramToken
            return t.isNotBlank() && !t.startsWith("BURAYA") && telegramChatId.isNotBlank()
        }
}

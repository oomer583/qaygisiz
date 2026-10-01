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

    private fun getBool(key: String, fallback: Boolean): Boolean =
        sp?.getBoolean(key, fallback) ?: fallback

    private fun putBool(key: String, value: Boolean) {
        sp?.edit()?.putBoolean(key, value)?.apply()
    }

    /**
     * Whether the protected phone also shows its owner a calm notice.
     *
     * Off by default, on purpose. The point of this app is that the person who
     * received the scam is never asked to judge it. I tested the idea against the
     * most phone-fearful person in my family, who is frightened even by a routine
     * balance message: for her, an extra notice is itself the harm. Families who
     * want the extra hold can turn it on.
     */
    var showOwnerNotice: Boolean
        get() = getBool("owner_notice", false)
        set(value) = putBool("owner_notice", value)
    val isConfigured: Boolean
        get() {
            val t = telegramToken
            return t.isNotBlank() && !t.startsWith("BURAYA") && telegramChatId.isNotBlank()
        }
}

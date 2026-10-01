package com.omer.qaygisiz

enum class Lang(val code: String, val englishName: String) {
    AZ("az", "Azerbaijani"),
    EN("en", "English"),
    RU("ru", "Russian")
}

object Settings {

    val language: Lang
        get() = when (Prefs.language.lowercase()) {
            "en" -> Lang.EN
            "ru" -> Lang.RU
            else -> Lang.AZ
        }
}

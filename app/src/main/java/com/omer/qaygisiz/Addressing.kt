package com.omer.qaygisiz

/**
 * The setup screen asks "who is being protected?", and the natural answer is a word in
 * the first person: "atam" - my father. But the alert is written by the app and read by
 * somebody else, so repeating that word makes the app talk about its own father. The
 * reader has to be addressed instead: "atanız" - your father.
 *
 * This converts the common kinship words into the form that addresses the reader, with
 * the right vowel harmony, and leaves everything else alone. An ordinary name passes
 * through untouched, which is exactly what should happen.
 *
 * It is a lookup rather than a morphology engine on purpose: a table is exact, costs
 * nothing, needs no network, and anyone reading it can see what it will do. The AI that
 * writes the explanation is given the converted form too, so it never has to guess.
 */
object Addressing {

    private val TABLE = mapOf(
        // Azerbaijani
        "atam" to "atanız",
        "anam" to "ananız",
        "nənəm" to "nənəniz",
        "babam" to "babanız",
        "bacım" to "bacınız",
        "qardaşım" to "qardaşınız",
        "əmim" to "əminiz",
        "dayım" to "dayınız",
        "xalam" to "xalanız",
        "bibim" to "bibiniz",
        "ərim" to "əriniz",
        "arvadım" to "arvadınız",
        "həyat yoldaşım" to "həyat yoldaşınız",
        "qayınanam" to "qayınananız",
        "qayınatam" to "qayınatanız",
        "oğlum" to "oğlunuz",
        "qızım" to "qızınız",
        // Turkish spellings, since the two are close enough that people mix them
        "annem" to "anneniz",
        "anneannem" to "anneanneniz",
        "babaannem" to "babaanneniz",
        "dedem" to "dedeniz",
        "ninem" to "nineniz",
        "ablam" to "ablanız",
        "abim" to "abiniz",
        "kardeşim" to "kardeşiniz",
        "eşim" to "eşiniz",
        "amcam" to "amcanız",
        "teyzem" to "teyzeniz",
        "halam" to "halanız",
        "kızım" to "kızınız",
        // Russian
        "мой отец" to "ваш отец",
        "моя мать" to "ваша мать",
        "моя мама" to "ваша мама",
        "мой папа" to "ваш папа",
        "моя бабушка" to "ваша бабушка",
        "мой дедушка" to "ваш дедушка",
        "мой брат" to "ваш брат",
        "моя сестра" to "ваша сестра",
        "мой муж" to "ваш муж",
        "моя жена" to "ваша жена",
        "мой сын" to "ваш сын",
        "моя дочь" to "ваша дочь"
    )

    /**
     * Returns the stored word in the form that addresses whoever reads the alert.
     * Unknown input, which includes every ordinary name, comes back exactly as typed.
     */
    fun forReader(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val hit = TABLE[trimmed.lowercase()] ?: return trimmed
        return if (trimmed.first().isUpperCase()) {
            hit.replaceFirstChar { it.uppercase() }
        } else {
            hit
        }
    }
}

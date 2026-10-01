package com.omer.qaygisiz

object Prompts {

    fun explainScam(sender: String, body: String, result: ScanResult): String {
        val lang = Settings.language
        val checks = result.reasons.joinToString("; ") { ReasonText.of(it, Lang.EN) }
        val who = if (Prefs.protectedPerson.isBlank()) {
            Texts.defaultPerson(lang)
        } else {
            Prefs.protectedPerson
        }

        return """
You are a fraud-detection assistant inside an app that protects elderly people from scam SMS.
A suspicious message arrived on an elderly person's phone. Write a short warning that will be sent to their adult child.

LANGUAGE RULES
- Write the answer in ${lang.englishName}. Nothing else.
- If the target language is Azerbaijani, do NOT drift into Turkish. Turkish words or Turkish grammar count as a failure.
- Use the everyday word for a web link, not a formal synonym.

AUDIENCE RULES
- The reader is the FAMILY MEMBER, not the person who received the SMS.
- The person who received the SMS is referred to as: $who
- Refer to that person exactly that way. Never write as if the reader received the message. Do not say "your account" or "your money" about the reader.

FORMAT RULES
- Exactly two short sentences. One sentence if the message is harmless.
- First sentence: who the sender pretends to be and what they are trying to obtain.
- Second sentence: what happens if $who does what the message says.
- Plain everyday words. No markdown, no bullet points, no greeting, no sign-off.
- Output only the warning text and nothing else.
- Capitalize the first letter of every sentence, including when a sentence starts with that name.

EXAMPLE OF A GOOD ANSWER (in ${lang.englishName})
${example(lang, who)}

NOW ANALYSE THIS MESSAGE

Sender: $sender
Message: $body
Automatic checks that flagged it: $checks
        """.trimIndent()
    }

    private fun example(lang: Lang, who: String): String = when (lang) {
        Lang.AZ ->
            "Göndərən özünü Kapital Bank kimi təqdim edir və hesabın bloklanması " +
                "bəhanəsi ilə şəxsi məlumatları ələ keçirməyə çalışır. " +
                "$who linkə keçib məlumatlarını yazsa, kartındakı pul oğurlana bilər."
        Lang.EN ->
            "The sender pretends to be Kapital Bank and uses a fake account block " +
                "to get personal details. If $who opens the link and types anything in, " +
                "the money on the card can be stolen."
        Lang.RU ->
            "Отправитель выдаёт себя за Kapital Bank и под предлогом блокировки счёта " +
                "пытается получить личные данные. Если $who перейдёт по ссылке и введёт данные, " +
                "деньги с карты могут украсть."
    }
}

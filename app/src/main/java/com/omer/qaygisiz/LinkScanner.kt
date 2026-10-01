package com.omer.qaygisiz

data class ScanResult(
    val score: Int,
    val level: Level,
    val reasons: List<Reason>,
    val urls: List<String>
) {
    enum class Level { SAFE, SUSPICIOUS, DANGEROUS }
}

/**
 * On-device scam scoring. No network, no model file, no Android dependency:
 * this object is plain Kotlin so it can be tested on the JVM.
 *
 * Measured on the public Azerbaijani SMS corpus of Shahbazov (2026),
 * doi 10.25045/jpit.v17.i1.04. On the held-out half of that corpus:
 * 90.3% of smishing caught, 1.9% of legitimate messages falsely flagged.
 * See MEASUREMENT.md for how to reproduce.
 */
object LinkScanner {

    private const val THRESHOLD_SUSPICIOUS = 3
    private const val THRESHOLD_DANGEROUS = 6

    // ---- weights, tuned on the development half of the corpus only ----
    private const val W_IP = 5
    private const val W_PUNYCODE = 4
    private const val W_SHORTENER = 1      // measured: shorteners are NOT a smishing signal here
    private const val W_SUSPICIOUS_TLD = 2
    private const val W_HOST_BRAND = 5
    private const val W_BODY_BRAND = 4
    private const val W_INSECURE_HTTP = 1
    private const val W_URGENCY = 2        // per hit, capped at 3 hits
    private const val W_PRIZE_CLAIM = 2    // per hit, capped at 2 hits
    private const val W_PRIZE_LURE = 1     // per hit, capped at 2 hits
    private const val W_PREMIUM_RATE = 3
    private const val W_CALLBACK = 3
    private const val W_SHORTCODE = 3
    private const val W_MONEY_PRIZE = 1
    private const val W_LINK_ACTION = 3
    private const val W_SENDER = 2

    private val KNOWN_TLDS = setOf(
        "com", "net", "org", "info", "biz", "co", "io", "me", "tv", "cc", "mobi", "pro",
        "az", "tr", "ru", "ge", "ir", "ua", "kz", "uz", "by", "su",
        "uk", "de", "fr", "nl", "pl", "es", "it", "us", "in", "cn",
        "ly", "gl", "gd", "gy", "at", "id", "to", "ae", "st", "sh", "ink",
        "xyz", "top", "click", "icu", "cfd", "rest", "live", "online", "site", "shop",
        "store", "website", "space", "fun", "link", "app", "dev", "page", "today",
        "life", "world", "vip", "win", "bid", "loan", "work", "email", "help", "support",
        "td", "bz", "cyou", "do"
    )

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "click", "icu", "cfd", "rest", "live", "online", "site",
        "space", "fun", "vip", "win", "bid", "loan", "work", "link", "cyou", "td"
    )

    private val SHORTENERS = setOf(
        "bit.ly", "bit.do", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "is.gd", "rb.gy",
        "shorturl.at", "rebrand.ly", "ow.ly", "buff.ly", "clck.ru", "vk.cc",
        "tiny.cc", "s.id", "bl.ink", "lnk.to", "surl.li", "qr.ae", "u.to",
        "linkr.it", "smsg.io"
    )

    /** Domains these institutions actually use. A link anywhere else is impersonation. */
    private val OFFICIAL_DOMAINS = setOf(
        "kapitalbank.az", "pashabank.az", "abb-bank.az", "birbank.az", "unibank.az",
        "accessbank.az", "rabitabank.az", "bankofbaku.com", "expressbank.az", "leobank.az",
        "azercell.com", "bakcell.com", "nar.az", "azerpost.az", "azerpocht.az",
        "my.gov.az", "e-gov.az", "edu.gov.az", "taxes.gov.az", "dim.gov.az",
        "m10.az", "asanimza.az", "asanpay.az",
        "whatsapp.com", "instagram.com", "facebook.com", "telegram.org",
        "google.com", "apple.com", "paypal.com"
    )

    // Keyword lists are written in ordinary Azerbaijani and folded at startup, so the
    // spelling here never has to be transliterated by hand. Attackers write "təsdiq" as
    // "tasdiq", "tesdiq" or "təsdiq"; folding collapses all three to the same form.
    private val INSTITUTIONS = folded(
        "kapitalbank", "paşabank", "pasha bank", "abb bank", "beynəlxalq bankı",
        "birbank", "unibank", "accessbank", "rabitabank", "bank of baku", "expressbank",
        "leobank", "azercell", "bakcell", "azərpoçt", "azerpoct", "azərpost",
        "vergi xidməti", "vergilər", "dövlət vergi", "dvx",
        "prezident administrasiyası", "sosial müdafiə", "dyp", "bdypi",
        "asan imza", "mygov",
        "whatsapp", "instagram", "facebook", "telegram", "paypal",
        "vodafone", "westpac", "apple id", "apple dəstək"
    )

    private val URGENCY_WORDS = folded(
        "blok", "təcili", "dərhal", "acil", "son xəbərdarlıq", "son gün", "son tarix",
        "son şans", "hesabınız", "hesabın", "kartınız", "kartın", "şifrə", "parol",
        "pin kod", "təsdiq", "dondurul", "bağlanacaq", "bağland", "dayandır",
        "cərimə", "borc", "məhkəmə"
    ) + listOf("срочно", "заблокирован", "подтвердите", "блокирован")

    /** Claims that a prize has already been won. Rare in legitimate traffic. */
    private val PRIZE_CLAIM_WORDS = folded(
        "mükafat", "qazanmış", "qazandın", "qazandınız", "qazanıb",
        "qazanma şansı", "qazanmaq şansı", "zəmanətli", "təbrik",
        "seçilmiş", "seçilib", "uddunuz", "uduş", "iddia et"
    )

    /** Softer bait words. These do occur in real marketing, so they score less. */
    private val PRIZE_LURE_WORDS = folded(
        "nağd pul", "hədiyyə", "fırlatma", "pulsuz", "endirim kupon",
        "hüququna malik", "hüququnuz var", "haqqına sahib"
    )

    private val CALL_VERBS = folded(
        "zəng ed", "zəng et", "əlaqə saxla", "göndərin", "göndər",
        "yazıb", "yığıb", "daxil edin", "cavab ver", "müraciət ed", "tələb et"
    )

    private val LINK_ACTIONS = folded(
        "daxil ol", "keçid", "linkdə", "linkdəki", "buraya", "saytına",
        "təsdiqlə", "yeniləyin", "aktivləşdir", "müraciət", "ziyarət et"
    )

    private val URL_REGEX = Regex(
        "(?:https?://)?(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,24}(?::\\d{1,5})?(?:/[^\\s]*)?",
        RegexOption.IGNORE_CASE
    )

    private val IP_URL_REGEX = Regex(
        "(?:https?://)?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?::\\d{1,5})?(?:/[^\\s]*)?"
    )

    /** A phone number to call back. X is kept because published corpora redact digits that way. */
    private val PHONE_REGEX = Regex("(?<![\\dxX])\\+?\\d[\\dxX]{6,14}(?![\\dxX])")

    /** A 4-6 digit short code to text, as in: send YES to 85023. */
    private val SHORTCODE_REGEX = Regex("(?<![\\w.])\\d{4,6}(?![\\w.])")

    private val MONEY_REGEX = Regex("\\b\\d[\\d.,]{0,8}\\s?azn\\b")

    /**
     * A disclosed per-minute or per-message price. In the measured corpus this appeared
     * in 38 smishing and 37 spam messages and in not one legitimate message: a premium-rate
     * line has to print its price, and an honest sender has no price to print.
     */
    private val PREMIUM_REGEX = Regex(
        "(q[əe]p|qap|qepik)\\s*/\\s*(d[əe]q|dq|sms|mesaj)" +
            "|/\\s*d[əe]q" +
            "|d[əe]qiq[əe]si\\s*\\d" +
            "|\\d[\\d.,]*\\s*azn\\s*/?\\s*(h[əe]r\\s*)?(mesaj|sms|d[əe]q)" +
            "|abun[əe](lik)?\\s*:?\\s*\\d"
    )

    fun scan(sender: String, body: String): ScanResult {
        val normalized = fold(body)
        val lower = body.lowercase()
        val reasons = mutableListOf<Reason>()
        var score = 0

        val ipUrls = IP_URL_REGEX.findAll(body).map { it.value }.toList()
        val namedUrls = URL_REGEX.findAll(body).map { it.value }
            .filter { isLikelyUrl(it) }
            .toList()
        val urls = (ipUrls + namedUrls).distinct()
        val hasLink = urls.isNotEmpty()
        var offsite = false

        if (ipUrls.isNotEmpty()) {
            score += W_IP
            reasons += Reason(ReasonCode.IP_URL)
        }

        for (url in namedUrls) {
            val host = hostOf(url)
            val domain = registrable(host)
            val tld = host.substringAfterLast('.', "")

            if (host.startsWith("xn--") || host.contains(".xn--")) {
                score += W_PUNYCODE
                reasons += Reason(ReasonCode.PUNYCODE)
            }
            if (SHORTENERS.contains(domain)) {
                score += W_SHORTENER
                reasons += Reason(ReasonCode.SHORTENER)
            }
            if (SUSPICIOUS_TLDS.contains(tld)) {
                score += W_SUSPICIOUS_TLD
                reasons += Reason(ReasonCode.SUSPICIOUS_TLD, tld)
            }
            val hostBrand = INSTITUTIONS.firstOrNull {
                fold(host).replace("-", "").contains(it.replace(" ", ""))
            }
            if (hostBrand != null && !OFFICIAL_DOMAINS.contains(domain)) {
                score += W_HOST_BRAND
                reasons += Reason(ReasonCode.BRAND_MISMATCH, hostBrand, domain)
            }
            if (url.lowercase().startsWith("http://")) {
                score += W_INSECURE_HTTP
                reasons += Reason(ReasonCode.INSECURE_HTTP)
            }
            if (!OFFICIAL_DOMAINS.contains(domain) && !SHORTENERS.contains(domain)) {
                offsite = true
            }
        }

        // The message speaks for an institution, but its link does not go to that
        // institution's own domain. This is the single most productive rule for the
        // fake-parcel and fake-tax campaigns.
        val claimed = INSTITUTIONS.firstOrNull { normalized.contains(it) }
        if (claimed != null && hasLink && offsite) {
            score += W_BODY_BRAND
            reasons += Reason(ReasonCode.BODY_BRAND_MISMATCH, claimed)
        }

        val urgencyHits = URGENCY_WORDS.count { normalized.contains(it) }
        if (urgencyHits > 0) {
            score += minOf(urgencyHits, 3) * W_URGENCY
            reasons += Reason(ReasonCode.URGENCY)
        }

        val claimHits = PRIZE_CLAIM_WORDS.count { normalized.contains(it) }
        if (claimHits > 0) {
            score += minOf(claimHits, 2) * W_PRIZE_CLAIM
            reasons += Reason(ReasonCode.PRIZE_CLAIM)
        }

        val lureHits = PRIZE_LURE_WORDS.count { normalized.contains(it) }
        if (lureHits > 0) {
            score += minOf(lureHits, 2) * W_PRIZE_LURE
            reasons += Reason(ReasonCode.PRIZE_LURE)
        }

        if (PREMIUM_REGEX.containsMatchIn(lower)) {
            score += W_PREMIUM_RATE
            reasons += Reason(ReasonCode.PREMIUM_RATE)
        }

        val hasPhone = PHONE_REGEX.containsMatchIn(body)
        val asksToContact = CALL_VERBS.any { normalized.contains(it) }

        // No link at all, but a number to ring and an instruction to ring it.
        if (!hasLink && hasPhone && asksToContact) {
            score += W_CALLBACK
            reasons += Reason(ReasonCode.CALLBACK)
        }
        // Send a word to a short code: the premium-SMS subscription trap.
        if (!hasLink && !hasPhone && asksToContact && SHORTCODE_REGEX.containsMatchIn(body)) {
            score += W_SHORTCODE
            reasons += Reason(ReasonCode.PREMIUM_SHORTCODE)
        }
        if (MONEY_REGEX.containsMatchIn(lower) && claimHits > 0) {
            score += W_MONEY_PRIZE
            reasons += Reason(ReasonCode.MONEY_PRIZE)
        }
        if (hasLink && offsite && LINK_ACTIONS.any { normalized.contains(it) }) {
            score += W_LINK_ACTION
            reasons += Reason(ReasonCode.UNKNOWN_LINK_ACTION)
        }

        val senderDigits = sender.trimStart('+')
        val senderIsPlainNumber = senderDigits.length >= 7 &&
            senderDigits.all { it.isDigit() || it == 'x' || it == 'X' } &&
            senderDigits.any { it.isDigit() }
        if (senderIsPlainNumber && claimed != null) {
            score += W_SENDER
            reasons += Reason(ReasonCode.SENDER_MISMATCH)
        }

        val level = when {
            score >= THRESHOLD_DANGEROUS -> ScanResult.Level.DANGEROUS
            score >= THRESHOLD_SUSPICIOUS -> ScanResult.Level.SUSPICIOUS
            else -> ScanResult.Level.SAFE
        }
        return ScanResult(score, level, reasons, urls)
    }

    private fun folded(vararg words: String): List<String> = words.map { fold(it) }

    private fun isLikelyUrl(match: String): Boolean {
        val lower = match.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return true
        if (lower.startsWith("www.")) return true
        val head = lower.substringBefore('/')
        return head.contains('.') && KNOWN_TLDS.contains(head.substringAfterLast('.', ""))
    }

    private fun hostOf(url: String): String {
        var s = url
        val scheme = s.indexOf("://")
        if (scheme >= 0) s = s.substring(scheme + 3)
        s = s.substringBefore('/')
        s = s.substringBefore(':')
        return s.lowercase()
    }

    private fun registrable(host: String): String {
        val parts = host.split('.')
        if (parts.size < 2) return host
        val twoLevel = setOf(
            "co.uk", "com.tr", "com.az", "gov.az", "org.az", "net.az", "edu.az",
            "co.il", "co.az"
        )
        if (parts.size >= 3) {
            val last2 = parts[parts.size - 2] + "." + parts[parts.size - 1]
            if (twoLevel.contains(last2)) {
                return parts[parts.size - 3] + "." + last2
            }
        }
        return parts[parts.size - 2] + "." + parts[parts.size - 1]
    }

    /**
     * Collapses the spellings of the same Azerbaijani word into one form: diacritics are
     * stripped and a/e are merged, because scam senders transliterate "ə" as either letter.
     */
    private fun fold(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text.lowercase()) {
            if (ch in '̀'..'ͯ') continue
            sb.append(
                when (ch) {
                    'ə' -> 'a'   // ə
                    'e' -> 'a'
                    'ı' -> 'i'   // ı
                    'í' -> 'i'   // í
                    'î' -> 'i'   // î
                    'ğ' -> 'g'   // ğ
                    'ş' -> 's'   // ş
                    'ç' -> 'c'   // ç
                    'ö' -> 'o'   // ö
                    'ô' -> 'o'   // ô
                    'ü' -> 'u'   // ü
                    'û' -> 'u'   // û
                    'â' -> 'a'   // â
                    else -> ch
                }
            )
        }
        return sb.toString()
    }
}

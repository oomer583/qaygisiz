package com.omer.qaygisiz

data class ScanResult(
    val score: Int,
    val level: Level,
    val reasons: List<Reason>,
    val urls: List<String>
) {
    enum class Level { SAFE, SUSPICIOUS, DANGEROUS }
}

object LinkScanner {

    private const val THRESHOLD_SUSPICIOUS = 3
    private const val THRESHOLD_DANGEROUS = 6

    private val KNOWN_TLDS = setOf(
        "com", "net", "org", "info", "biz", "co", "io", "me", "tv", "cc", "mobi", "pro",
        "az", "tr", "ru", "ge", "ir", "ua", "kz", "uz", "by", "su",
        "uk", "de", "fr", "nl", "pl", "es", "it", "us", "in", "cn",
        "ly", "gl", "gd", "gy", "at", "id", "to", "ae", "st", "sh", "ink",
        "xyz", "top", "click", "icu", "cfd", "rest", "live", "online", "site", "shop",
        "store", "website", "space", "fun", "link", "app", "dev", "page", "today",
        "life", "world", "vip", "win", "bid", "loan", "work", "email", "help", "support"
    )

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "click", "icu", "cfd", "rest", "live", "online", "site",
        "space", "fun", "vip", "win", "bid", "loan", "work", "shop", "store", "link"
    )

    private val SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "is.gd", "rb.gy",
        "shorturl.at", "rebrand.ly", "ow.ly", "buff.ly", "clck.ru", "vk.cc",
        "tiny.cc", "s.id", "bl.ink", "lnk.to", "surl.li", "qr.ae", "u.to"
    )

    private val OFFICIAL_DOMAINS = setOf(
        "kapitalbank.az", "pashabank.az", "abb-bank.az", "birbank.az", "unibank.az",
        "accessbank.az", "rabitabank.az", "bankofbaku.com", "expressbank.az",
        "azercell.com", "bakcell.com", "nar.az", "azerpost.az", "my.gov.az",
        "e-gov.az", "m10.az", "asanimza.az",
        "whatsapp.com", "instagram.com", "facebook.com", "telegram.org",
        "google.com", "apple.com", "paypal.com"
    )

    private val BRAND_WORDS = listOf(
        "kapital", "pasha", "abbbank", "abb-bank", "birbank", "unibank", "accessbank",
        "rabita", "bankofbaku", "expressbank", "azercell", "bakcell", "azerpost",
        "asan", "egov", "e-gov", "m10",
        "whatsapp", "instagram", "facebook", "telegram", "google", "apple", "paypal"
    )

    private val URGENCY_WORDS = listOf(
        "blok", "tecili", "derhal", "acil", "son xeberdarliq", "son gun",
        "hesabiniz", "hesabin", "kartiniz", "kartin", "sifre", "parol", "pin",
        "tesdiq", "dondurul", "baglanacaq", "bagland",
        "uddunuz", "udus", "mukafat", "hediyye", "bonus",
        "kocurme", "odenis",
        "srochno", "zablokirovan", "podtverdite"
    )

    private val URL_REGEX = Regex(
        "(?:https?://)?(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,24}(?::\\d{1,5})?(?:/[^\\s]*)?",
        RegexOption.IGNORE_CASE
    )

    private val IP_URL_REGEX = Regex(
        "(?:https?://)?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?::\\d{1,5})?(?:/[^\\s]*)?"
    )

    fun scan(sender: String, body: String): ScanResult {
        val normalized = fold(body)
        val reasons = mutableListOf<Reason>()
        var score = 0

        val ipUrls = IP_URL_REGEX.findAll(body).map { it.value }.toList()
        val namedUrls = URL_REGEX.findAll(body).map { it.value }
            .filter { isLikelyUrl(it) }
            .toList()
        val urls = (ipUrls + namedUrls).distinct()

        if (ipUrls.isNotEmpty()) {
            score += 5
            reasons += Reason(ReasonCode.IP_URL)
        }

        for (url in namedUrls) {
            val host = hostOf(url)
            val domain = registrable(host)
            val tld = host.substringAfterLast('.', "")

            if (host.startsWith("xn--") || host.contains(".xn--")) {
                score += 4
                reasons += Reason(ReasonCode.PUNYCODE)
            }

            if (SHORTENERS.contains(domain)) {
                score += 3
                reasons += Reason(ReasonCode.SHORTENER)
            }

            if (SUSPICIOUS_TLDS.contains(tld)) {
                score += 2
                reasons += Reason(ReasonCode.SUSPICIOUS_TLD, tld)
            }

            val brand = BRAND_WORDS.firstOrNull { fold(host).contains(it) }
            if (brand != null && !OFFICIAL_DOMAINS.contains(domain)) {
                score += 5
                reasons += Reason(ReasonCode.BRAND_MISMATCH, brand, domain)
            }

            if (url.lowercase().startsWith("http://")) {
                score += 1
                reasons += Reason(ReasonCode.INSECURE_HTTP)
            }
        }

        var urgencyHits = 0
        for (word in URGENCY_WORDS) {
            if (normalized.contains(word)) urgencyHits++
        }
        if (urgencyHits > 0) {
            score += minOf(urgencyHits, 3)
            reasons += Reason(ReasonCode.URGENCY)
        }

        val senderDigits = sender.trimStart('+')
        val senderIsPlainNumber = senderDigits.length >= 7 && senderDigits.all { it.isDigit() }
        if (senderIsPlainNumber && BRAND_WORDS.any { normalized.contains(it) }) {
            score += 2
            reasons += Reason(ReasonCode.SENDER_MISMATCH)
        }

        val level = when {
            score >= THRESHOLD_DANGEROUS -> ScanResult.Level.DANGEROUS
            score >= THRESHOLD_SUSPICIOUS -> ScanResult.Level.SUSPICIOUS
            else -> ScanResult.Level.SAFE
        }

        return ScanResult(score, level, reasons, urls)
    }

    private fun isLikelyUrl(match: String): Boolean {
        val lower = match.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return true
        if (lower.startsWith("www.")) return true
        return KNOWN_TLDS.contains(lower.substringBefore('/').substringAfterLast('.', ""))
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
            "co.uk", "com.tr", "com.az", "gov.az", "org.az", "net.az", "edu.az", "co.il"
        )
        if (parts.size >= 3) {
            val last2 = parts[parts.size - 2] + "." + parts[parts.size - 1]
            if (twoLevel.contains(last2)) {
                return parts[parts.size - 3] + "." + last2
            }
        }
        return parts[parts.size - 2] + "." + parts[parts.size - 1]
    }

    private fun fold(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text.lowercase()) {
            if (ch in '̀'..'ͯ') continue
            sb.append(
                when (ch) {
                    'ə' -> 'e'
                    'ı' -> 'i'
                    'ğ' -> 'g'
                    'ş' -> 's'
                    'ç' -> 'c'
                    'ö' -> 'o'
                    'ü' -> 'u'
                    else -> ch
                }
            )
        }
        return sb.toString()
    }
}

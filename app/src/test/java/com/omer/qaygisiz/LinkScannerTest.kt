package com.omer.qaygisiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the on-device scoring engine.
 *
 * These run on the JVM: no emulator, no SIM card, no network, no API key.
 *     ./gradlew test
 *
 * Every expected value here was produced by running the scanner itself over the
 * message and recorded, so these tests pin down real behaviour rather than a guess.
 * The wider accuracy figures come from a separate corpus run; see MEASUREMENT.md.
 */
class LinkScannerTest {

    private fun level(sender: String, body: String) = LinkScanner.scan(sender, body).level
    private fun codes(sender: String, body: String) =
        LinkScanner.scan(sender, body).reasons.map { it.code }

    // ---------- must stay quiet ----------
    // An alert that cries wolf is worse than no alert: the family member stops reading it.

    @Test
    fun plainChat_isSafe() {
        assertEquals(ScanResult.Level.SAFE, level("4040", "Salam, gorusek saat 5de."))
    }

    @Test
    fun realBankBalanceNotification_isSafe() {
        assertEquals(
            ScanResult.Level.SAFE,
            level("Birbank", "Balansiniz: 45.20 AZN. Mehsul: nagd pul cixarisi.")
        )
    }

    @Test
    fun officialBankDomain_isSafe() {
        assertEquals(
            ScanResult.Level.SAFE,
            level("Kapital", "Hormetli musteri, kapitalbank.az saytinda yeni xidmet.")
        )
    }

    @Test
    fun officialBankSubdomain_isSafe() {
        // A real payment confirmation from a deep subdomain of a genuine bank domain.
        assertEquals(
            ScanResult.Level.SAFE,
            level("Kapital", "www.edvgerial.kapitalbank.az\nTesdigleme kodu: 950807\nMebleg: 5.00 AZN")
        )
    }

    @Test
    fun legitimateTelecomUsingAShortener_isSafe() {
        // Measured on a real Azerbaijani SMS corpus: link shorteners appear slightly more
        // often in legitimate telecom marketing than in smishing, so they barely score.
        assertEquals(
            ScanResult.Level.SAFE,
            level("Azercell", "Hormetli Abunechi, Avtomatik balans melumati xidmeti yenilendi. Etrafli: https://bit.ly/balans-az")
        )
    }

    @Test
    fun suspiciousTldAlone_isNotEnoughToAlarm() {
        assertEquals(ScanResult.Level.SAFE, level("1010", "Yeni endirim: promo.shop/kampaniya"))
    }

    // ---------- links that impersonate ----------

    @Test
    fun lookalikeBankDomain_isDangerous() {
        val r = LinkScanner.scan(
            "+994501234567",
            "Hesabiniz bloklandi. Tecili olaraq http://kapitalbank-az.xyz/giris linkine daxil olun."
        )
        assertEquals(ScanResult.Level.DANGEROUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.BRAND_MISMATCH })
    }

    @Test
    fun institutionNamedInBodyButLinkGoesElsewhere_isDangerous() {
        // The most productive rule against the fake-parcel campaigns: the message speaks
        // for Azerpoct, the link does not go to Azerpoct.
        val r = LinkScanner.scan(
            "Azerpoct",
            "Bağlamanız göndərilib. Çatdırılma üçün ünvanı təsdiqləyin: www.azerpoct-srz.td/dazcc"
        )
        assertEquals(ScanResult.Level.DANGEROUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.BODY_BRAND_MISMATCH })
    }

    @Test
    fun punycodeLookalike_isSuspicious() {
        val r = LinkScanner.scan("+994702223344", "https://xn--pashabnk-x1a.com/login")
        assertEquals(ScanResult.Level.SUSPICIOUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.PUNYCODE })
    }

    @Test
    fun bareIpLinkWithUrgency_isDangerous() {
        val r = LinkScanner.scan("5050", "Kartiniz bloklandi. http://185.62.190.14/giris")
        assertEquals(ScanResult.Level.DANGEROUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.IP_URL })
    }

    @Test
    fun bareIpLinkAlone_isSuspicious() {
        assertEquals(
            ScanResult.Level.SUSPICIOUS,
            level("5050", "Yeni mesaj var: http://185.62.190.14/login")
        )
    }

    @Test
    fun shortenerWithUrgency_isSuspicious() {
        val r = LinkScanner.scan("ABB", "Odenis tesdiqi ucun bit.ly/3xK9pQa")
        assertEquals(ScanResult.Level.SUSPICIOUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.SHORTENER })
    }

    // ---------- scams with no link at all ----------
    // These were the detector's blind spot: four out of five missed messages had no link.

    @Test
    fun prizeWithACallbackNumber_isDangerous() {
        val r = LinkScanner.scan(
            "",
            "Siz 750AZN mükafat qazanmısınız. Mükafatı almaq üçün 0501234567 nömrəsinə zəng edin!"
        )
        assertEquals(ScanResult.Level.DANGEROUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.CALLBACK })
        assertTrue(r.reasons.any { it.code == ReasonCode.PRIZE_CLAIM })
    }

    @Test
    fun premiumRatePriceDisclosure_isDangerous() {
        // A disclosed per-minute price never appeared in a legitimate message in the
        // measured corpus: a premium-rate line has to print its price.
        val r = LinkScanner.scan(
            "",
            "Gizli bir heyranınız var. Kim olduğunu öyrənmək üçün zəng edin: 0509998877. Qiymət: 20qəp/dəq"
        )
        assertEquals(ScanResult.Level.DANGEROUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.PREMIUM_RATE })
    }

    @Test
    fun textAWordToAShortCode_isDangerous() {
        val r = LinkScanner.scan(
            "",
            "Son şans! 150AZN dəyərində endirim kuponlarını əldə etmək üçün HƎ yazıb 85023-ə göndərin!"
        )
        assertEquals(ScanResult.Level.DANGEROUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.PREMIUM_SHORTCODE })
    }

    // ---------- language handling ----------

    @Test
    fun azerbaijaniDiacritics_areFolded() {
        val r = LinkScanner.scan("4040", "Hesabınız bloklanıb, təcili təsdiq edin")
        assertEquals(ScanResult.Level.DANGEROUS, r.level)
        assertTrue(r.reasons.any { it.code == ReasonCode.URGENCY })
    }

    @Test
    fun asciiTransliterationOfAzerbaijani_isAlsoFolded() {
        // Scam senders write "təsdiq" as "tasdiq", not "tesdiq". Folding merges a and e so
        // both spellings hit the same keyword. Missing this cost most of one whole campaign.
        assertTrue(codes("4040", "Hesabiniz bloklanib, tacili tasdiqlayin").contains(ReasonCode.URGENCY))
        assertTrue(codes("4040", "Hesabiniz bloklanib, tecili tesdiqleyin").contains(ReasonCode.URGENCY))
    }

    @Test
    fun detectedUrlIsReported() {
        val r = LinkScanner.scan("1010", "Daxil olun: http://kapitalbank-az.xyz/giris")
        assertTrue(r.urls.any { it.contains("kapitalbank-az.xyz") })
    }
}

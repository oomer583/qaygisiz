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
 * That is deliberate. The part of this app that decides whether a message is
 * dangerous is pure Kotlin with no Android dependency, so it can be tested
 * cheaply and repeatedly.
 */
class LinkScannerTest {

    private fun level(sender: String, body: String) = LinkScanner.scan(sender, body).level

    // ---------- must stay quiet ----------

    @Test
    fun plainChat_isSafe() {
        assertEquals(
            ScanResult.Level.SAFE,
            level("4040", "Salam, gorusek saat 5de.")
        )
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
    fun suspiciousTldAlone_isNotEnoughToAlarm() {
        // A cheap TLD on its own is not evidence of fraud. This test exists to
        // hold the false-positive rate down: an alert that cries wolf is worse
        // than no alert, because the family member stops reading it.
        assertEquals(
            ScanResult.Level.SAFE,
            level("1010", "Yeni endirim: promo.shop/kampaniya")
        )
    }

    // ---------- must raise the alarm ----------

    @Test
    fun lookalikeBankDomain_isDangerous() {
        val result = LinkScanner.scan(
            "+994501234567",
            "Hesabiniz bloklandi. Tecili olaraq http://kapitalbank-az.xyz/giris linkine daxil olun."
        )
        assertEquals(ScanResult.Level.DANGEROUS, result.level)
        assertTrue(result.reasons.any { it.code == ReasonCode.BRAND_MISMATCH })
    }

    @Test
    fun punycodeLookalike_isDangerous() {
        val result = LinkScanner.scan("+994702223344", "https://xn--pashabnk-x1a.com/login")
        assertEquals(ScanResult.Level.DANGEROUS, result.level)
        assertTrue(result.reasons.any { it.code == ReasonCode.PUNYCODE })
    }

    @Test
    fun bareIpLinkWithUrgency_isDangerous() {
        val result = LinkScanner.scan("5050", "Kartiniz bloklandi. http://185.62.190.14/giris")
        assertEquals(ScanResult.Level.DANGEROUS, result.level)
        assertTrue(result.reasons.any { it.code == ReasonCode.IP_URL })
    }

    @Test
    fun bareIpLinkAlone_isSuspicious() {
        // Documents current behaviour: a raw IP link with no urgency wording
        // scores 5, which is below the DANGEROUS threshold of 6.
        assertEquals(
            ScanResult.Level.SUSPICIOUS,
            level("5050", "Yeni mesaj var: http://185.62.190.14/login")
        )
    }

    @Test
    fun urlShortenerWithUrgency_isSuspicious() {
        val result = LinkScanner.scan("ABB", "Odenis tesdiqi ucun bit.ly/3xK9pQa")
        assertEquals(ScanResult.Level.SUSPICIOUS, result.level)
        assertTrue(result.reasons.any { it.code == ReasonCode.SHORTENER })
    }

    // ---------- language handling ----------

    @Test
    fun azerbaijaniDiacritics_areFolded() {
        // The urgency word list is stored in ASCII. A message written with real
        // Azerbaijani letters must still match it, otherwise the scanner would
        // be blind to the way these messages are actually written.
        val body = "Hesabınız bloklanıb, təcili təsdiq edin"
        val result = LinkScanner.scan("4040", body)
        assertEquals(ScanResult.Level.SUSPICIOUS, result.level)
        assertTrue(result.reasons.any { it.code == ReasonCode.URGENCY })
    }

    @Test
    fun detectedUrlIsReported() {
        val result = LinkScanner.scan("1010", "Daxil olun: http://kapitalbank-az.xyz/giris")
        assertTrue(result.urls.any { it.contains("kapitalbank-az.xyz") })
    }
}

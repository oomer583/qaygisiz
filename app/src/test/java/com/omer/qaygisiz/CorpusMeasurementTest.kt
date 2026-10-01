package com.omer.qaygisiz

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Measures the detector against a public, peer-reviewed Azerbaijani SMS corpus.
 *
 *   Shahbazov, V. (2026). SMS dataset for multi-class classification of ham, spam,
 *   and smishing in Azerbaijani language. Problems of Information Technology,
 *   17(1), 32-39. doi:10.25045/jpit.v17.i1.04
 *
 * The corpus is not redistributed here (it carries no licence file). Fetch it with
 * tools/fetch-corpus.ps1 or tools/fetch-corpus.sh, then:
 *
 *     ./gradlew test --tests '*CorpusMeasurementTest*'
 *
 * Without the corpus present this test is skipped, so a clean clone still builds.
 *
 * corpus/split_dev.json and corpus/split_test.json are checked in on purpose. The
 * weights in LinkScanner were tuned while looking only at the dev half; the test
 * half was scored once, at the end. Shipping the two index lists is what makes that
 * claim checkable by someone else instead of being merely asserted.
 */
class CorpusMeasurementTest {

    private class Counts {
        var tp = 0; var fn = 0; var fp = 0; var tn = 0
        var spamN = 0; var spamAlarm = 0
        val recall get() = 100.0 * tp / maxOf(1, tp + fn)
        val precision get() = 100.0 * tp / maxOf(1, tp + fp)
        val falseAlarm get() = 100.0 * fp / maxOf(1, fp + tn)
        val f1 get() = 2 * precision * recall / maxOf(1e-9, precision + recall)
    }

    @Test
    fun measureAgainstPublicCorpus() {
        val corpus = findUp("corpus/dataset.csv")
        assumeTrue(
            "corpus/dataset.csv not found - run tools/fetch-corpus.ps1 (or .sh) to measure",
            corpus != null
        )
        val devIds = readIds(findUp("corpus/split_dev.json")!!)
        val testIds = readIds(findUp("corpus/split_test.json")!!)
        val rows = readCorpus(corpus!!)

        val report = StringBuilder()
        report.appendLine("Qaygisiz detection measurement")
        report.appendLine("corpus: ${rows.size} messages, Shahbazov (2026) doi:10.25045/jpit.v17.i1.04")
        report.appendLine()

        val dev = score(rows, devIds)
        val test = score(rows, testIds)
        val all = score(rows, rows.indices.toList())
        for ((name, c) in listOf("DEV (weights were tuned here)" to dev,
                                 "TEST (held out)" to test,
                                 "WHOLE CORPUS" to all)) {
            report.appendLine(name)
            report.appendLine("  smishing  n=%4d  caught=%4d  missed=%3d   recall=%.1f%%"
                .format(Locale.ROOT, c.tp + c.fn, c.tp, c.fn, c.recall))
            report.appendLine("  legitimate n=%4d false alarms=%3d (%.1f%%)  precision=%.1f%%  F1=%.1f"
                .format(Locale.ROOT, c.fp + c.tn, c.fp, c.falseAlarm, c.precision, c.f1))
            report.appendLine("  spam      n=%4d  alarms=%4d"
                .format(Locale.ROOT, c.spamN, c.spamAlarm))
            report.appendLine()
        }
        val text = report.toString()
        println(text)
        findUp("README.md")?.parentFile?.let { File(it, "MEASUREMENT.txt").writeText(text) }

        // Regression guards, set below the measured figures so ordinary tuning does not
        // trip them but a real regression does.
        assertTrue("recall on the held-out half fell below 85%", test.recall >= 85.0)
        assertTrue("false alarms on legitimate messages rose above 3%", test.falseAlarm <= 3.0)
    }

    private fun score(rows: List<Triple<String, String, String>>, ids: List<Int>): Counts {
        val c = Counts()
        for (i in ids) {
            val (sender, body, label) = rows[i]
            val alarm = LinkScanner.scan(sender, body).level != ScanResult.Level.SAFE
            when (label) {
                "smishing" -> if (alarm) c.tp++ else c.fn++
                "ham" -> if (alarm) c.fp++ else c.tn++
                "spam" -> { c.spamN++; if (alarm) c.spamAlarm++ }
            }
        }
        return c
    }

    private fun findUp(relative: String): File? {
        var dir: File? = File("").absoluteFile
        repeat(5) {
            val candidate = File(dir, relative)
            if (candidate.exists()) return candidate
            dir = dir?.parentFile
        }
        return null
    }

    private fun readIds(file: File): List<Int> =
        file.readText().trim().removePrefix("[").removeSuffix("]")
            .split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }

    /** sender, message, label - CSV with quoted fields that may contain newlines. */
    private fun readCorpus(file: File): List<Triple<String, String, String>> {
        val records = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (ch in file.readText()) {
            if (ch == '"') inQuotes = !inQuotes
            if (ch == '\n' && !inQuotes) {
                records.add(current.toString().trimEnd('\r'))
                current = StringBuilder()
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) records.add(current.toString().trimEnd('\r'))

        val out = mutableListOf<Triple<String, String, String>>()
        for ((index, record) in records.withIndex()) {
            if (index == 0 || record.isBlank()) continue
            val fields = splitCsv(record)
            if (fields.size < 3) continue
            out.add(Triple(fields[0], fields[1], fields[2].trim()))
        }
        return out
    }

    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { out.add(field.toString()); field.setLength(0) }
                else -> field.append(c)
            }
            i++
        }
        out.add(field.toString())
        return out
    }
}

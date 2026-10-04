package com.omer.qaygisiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Paste a message, see what the detector decides and which rules fired.
 *
 * This screen calls LinkScanner.scan exactly as SmsReceiver does when a real
 * SMS arrives. Nothing here re-implements the detection: it is the same object,
 * the same thresholds and the same rule codes that MEASUREMENT.md reports on.
 * That is the point of the screen - anyone can check the claim without waiting
 * for a scam to reach their phone.
 */
@Composable
fun CheckScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val lang = checkLang()

    var sender by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ScanResult?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = checkBack(lang)
                )
            }
            Text(
                checkTitle(lang),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Text(
            checkIntro(lang),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = sender,
            onValueChange = { sender = it },
            label = { Text(checkSenderLabel(lang)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text(checkBodyLabel(lang)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        Button(
            onClick = { result = LinkScanner.scan(sender.trim(), body) },
            enabled = body.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(checkButton(lang))
        }

        val r = result
        if (r != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (r.level) {
                        ScanResult.Level.SAFE -> MaterialTheme.colorScheme.surfaceVariant
                        ScanResult.Level.SUSPICIOUS -> MaterialTheme.colorScheme.tertiaryContainer
                        ScanResult.Level.DANGEROUS -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        checkLevelText(r.level, lang),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        checkScoreLabel(lang) + ": " + r.score,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        checkWhyLabel(lang),
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (r.reasons.isEmpty()) {
                        Text(
                            checkNoReason(lang),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        r.reasons.forEach { reason ->
                            Text(
                                "• " + ReasonText.of(reason, lang) +
                                    "   [" + reason.code.name + "]",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (r.urls.isNotEmpty()) {
                        Text(
                            checkLinksLabel(lang),
                            style = MaterialTheme.typography.titleSmall
                        )
                        r.urls.forEach {
                            Text(
                                "• " + it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text(
                checkSameRules(lang),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun checkLang(): Lang = when (Prefs.language.lowercase()) {
    "en" -> Lang.EN
    "ru" -> Lang.RU
    else -> Lang.AZ
}

private fun checkTitle(lang: Lang) = when (lang) {
    Lang.AZ -> "Mesajı yoxla"
    Lang.EN -> "Check a message"
    Lang.RU -> "Проверить сообщение"
}

private fun checkBack(lang: Lang) = when (lang) {
    Lang.AZ -> "Geri"
    Lang.EN -> "Back"
    Lang.RU -> "Назад"
}

private fun checkIntro(lang: Lang) = when (lang) {
    Lang.AZ -> "Şübhəli bir mesajı bura yapışdırın. Bu, SMS gələndə işə düşən " +
        "qaydaların eynisidir - heç nə göndərilmir, heç nə yadda saxlanılmır."
    Lang.EN -> "Paste a suspicious message here. These are the same rules that run " +
        "when an SMS arrives - nothing is sent anywhere and nothing is stored."
    Lang.RU -> "Вставьте сюда подозрительное сообщение. Это те же правила, которые " +
        "срабатывают при получении SMS - ничего не отправляется и не сохраняется."
}

private fun checkSenderLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Göndərən (istəyə bağlı)"
    Lang.EN -> "Sender (optional)"
    Lang.RU -> "Отправитель (необязательно)"
}

private fun checkBodyLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Mesaj mətni"
    Lang.EN -> "Message text"
    Lang.RU -> "Текст сообщения"
}

private fun checkButton(lang: Lang) = when (lang) {
    Lang.AZ -> "Yoxla"
    Lang.EN -> "Check"
    Lang.RU -> "Проверить"
}

private fun checkLevelText(level: ScanResult.Level, lang: Lang) = when (level) {
    ScanResult.Level.SAFE -> when (lang) {
        Lang.AZ -> "Təmiz"
        Lang.EN -> "Clean"
        Lang.RU -> "Чисто"
    }
    ScanResult.Level.SUSPICIOUS -> when (lang) {
        Lang.AZ -> "Şübhəli"
        Lang.EN -> "Suspicious"
        Lang.RU -> "Подозрительно"
    }
    ScanResult.Level.DANGEROUS -> when (lang) {
        Lang.AZ -> "Təhlükəli"
        Lang.EN -> "Dangerous"
        Lang.RU -> "Опасно"
    }
}

private fun checkScoreLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Xal"
    Lang.EN -> "Score"
    Lang.RU -> "Баллы"
}

private fun checkWhyLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "İşə düşən qaydalar"
    Lang.EN -> "Rules that fired"
    Lang.RU -> "Сработавшие правила"
}

private fun checkNoReason(lang: Lang) = when (lang) {
    Lang.AZ -> "Heç bir qayda işə düşmədi."
    Lang.EN -> "No rule fired."
    Lang.RU -> "Ни одно правило не сработало."
}

private fun checkLinksLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Tapılan linklər"
    Lang.EN -> "Links found"
    Lang.RU -> "Найденные ссылки"
}

private fun checkSameRules(lang: Lang) = when (lang) {
    Lang.AZ -> "Kvadrat mötərizədəki adlar MEASUREMENT.md-də ölçülən qayda kodlarıdır."
    Lang.EN -> "The names in square brackets are the rule codes measured in MEASUREMENT.md."
    Lang.RU -> "Названия в квадратных скобках - коды правил, измеренных в MEASUREMENT.md."
}

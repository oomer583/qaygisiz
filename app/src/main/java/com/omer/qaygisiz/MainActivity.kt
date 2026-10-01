package com.omer.qaygisiz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.omer.qaygisiz.ui.theme.QaygisizTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QaygisizTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    SetupScreen(Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var langCode by remember { mutableStateOf(Prefs.language) }
    val lang = when (langCode.lowercase()) {
        "en" -> Lang.EN
        "ru" -> Lang.RU
        else -> Lang.AZ
    }

    var person by remember { mutableStateOf(Prefs.protectedPerson) }
    var token by remember { mutableStateOf(Prefs.telegramToken) }
    var chatId by remember { mutableStateOf(Prefs.telegramChatId) }
    var message by remember { mutableStateOf("") }
    var ownerNotice by remember { mutableStateOf(Prefs.showOwnerNotice) }

    var hasSms by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasSms = granted }

    val tokenLooksSet = token.isNotBlank() && !token.startsWith("BURAYA")
    val ready = tokenLooksSet && chatId.isNotBlank() && hasSms

    fun persist() {
        Prefs.language = langCode
        Prefs.protectedPerson = person
        Prefs.telegramToken = token
        Prefs.telegramChatId = chatId
        Prefs.showOwnerNotice = ownerNotice
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(UiTexts.appTitle(lang), style = MaterialTheme.typography.headlineMedium)
        Text(UiTexts.tagline(lang), style = MaterialTheme.typography.bodyMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (ready) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Text(
                text = if (ready) UiTexts.statusActive(lang) else UiTexts.statusIncomplete(lang),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Text(UiTexts.languageLabel(lang), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LangButton("AZ", langCode == "az") { langCode = "az" }
            LangButton("EN", langCode == "en") { langCode = "en" }
            LangButton("RU", langCode == "ru") { langCode = "ru" }
        }

        OutlinedTextField(
            value = person,
            onValueChange = { person = it },
            label = { Text(UiTexts.protectedLabel(lang)) },
            placeholder = { Text(UiTexts.protectedHint(lang)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(UiTexts.tokenLabel(lang)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            label = { Text(UiTexts.chatIdLabel(lang)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            UiTexts.telegramHelp(lang),
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                UiTexts.permissionLabel(lang) + ": " +
                    if (hasSms) UiTexts.permissionGranted(lang) else UiTexts.permissionMissing(lang)
            )
            if (!hasSms) {
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                }) {
                    Text(UiTexts.grantButton(lang))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ownerNoticeLabel(lang))
                Text(
                    ownerNoticeHelp(lang),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = ownerNotice, onCheckedChange = { ownerNotice = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                persist()
                message = UiTexts.saved(lang)
            }) {
                Text(UiTexts.saveButton(lang))
            }

            OutlinedButton(
                enabled = token.isNotBlank() && chatId.isNotBlank(),
                onClick = {
                    persist()
                    message = UiTexts.testSending(lang)
                    val current = lang
                    Thread {
                        val ok = TelegramNotifier.send(UiTexts.testMessage(current))
                        Handler(Looper.getMainLooper()).post {
                            message = if (ok) UiTexts.testOk(current) else UiTexts.testFailed(current)
                        }
                    }.start()
                }
            ) {
                Text(UiTexts.testButton(lang))
            }
        }

        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Off by default. The whole point of the app is that the person who received the
 * scam is never asked to judge it, and for a very anxious user an extra notice is
 * itself the harm. A family that wants the extra hold can turn it on here.
 */
private fun ownerNoticeLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Telefon sahibinə də sakit bildiriş göstər"
    Lang.EN -> "Also show the phone's owner a calm notice"
    Lang.RU -> "Показывать владельцу телефона спокойное уведомление"
}

private fun ownerNoticeHelp(lang: Lang) = when (lang) {
    Lang.AZ -> "Varsayılan olaraq bağlıdır. Çox narahat olan istifadəçi üçün əlavə bildirişin özü ziyandır."
    Lang.EN -> "Off by default. For a very anxious user, an extra notice is itself the harm."
    Lang.RU -> "По умолчанию выключено. Для очень тревожного человека лишнее уведомление само по себе вредно."
}
@Composable
private fun LangButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QaygisizTheme {
                // No separate "setup done" flag. If the two fields a warning cannot be
                // sent without are filled in, setup happened. One less thing to go stale.
                var showSettings by remember { mutableStateOf(!isConfigured()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    if (showSettings) {
                        SetupScreen(
                            modifier = Modifier.padding(inner),
                            onBack = { showSettings = false }
                        )
                    } else {
                        HomeScreen(
                            modifier = Modifier.padding(inner),
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

private fun isConfigured(): Boolean {
    val token = Prefs.telegramToken
    return token.isNotBlank() &&
        !token.startsWith("BURAYA") &&
        Prefs.telegramChatId.isNotBlank()
}

private fun currentLang(): Lang = when (Prefs.language.lowercase()) {
    "en" -> Lang.EN
    "ru" -> Lang.RU
    else -> Lang.AZ
}

/**
 * The home screen says one thing: the app is watching, and the person does not
 * have to do anything. The only other fact on it is who is being protected,
 * because that is the one setting that can be silently wrong.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val lang = currentLang()

    val hasSms = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED
    val ready = isConfigured() && hasSms

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                UiTexts.appTitle(lang),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = settingsTitle(lang))
            }
        }

        Spacer(Modifier.height(8.dp))

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
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (ready) UiTexts.statusActive(lang) else UiTexts.statusIncomplete(lang),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    if (ready) homeListening(lang) else homeIncompleteHelp(lang),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!ready) {
            Button(onClick = onOpenSettings) { Text(homeFinishSetup(lang)) }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SetupScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
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

    // Confirmations are news for a moment and clutter after that.
    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            delay(2500)
            message = ""
        }
    }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { persist(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel(lang))
            }
            Text(
                settingsTitle(lang),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

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

        // Nothing is reported while nothing is wrong.
        if (!hasSms) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    UiTexts.permissionLabel(lang) + ": " + UiTexts.permissionMissing(lang),
                    modifier = Modifier.weight(1f)
                )
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
            // A switch that needs a second button to take effect is a switch that lies.
            Switch(
                checked = ownerNotice,
                onCheckedChange = {
                    ownerNotice = it
                    Prefs.showOwnerNotice = it
                }
            )
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
                    // The family member keeps this message. Naming the person here
                    // means they can look up who this bot is watching over, months
                    // later, without the app having to answer questions.
                    val addressed = Prefs.protectedPersonAddressed
                    val setupText = if (addressed.isNotBlank()) {
                        UiTexts.testMessage(current) + "\n\n" +
                            protectedShortLabel(current) + ": " + addressed
                    } else {
                        UiTexts.testMessage(current)
                    }
                    Thread {
                        val ok = TelegramNotifier.send(setupText)
                        Handler(Looper.getMainLooper()).post {
                            message = if (ok) UiTexts.testOk(current) else UiTexts.testFailed(current)
                        }
                    }.start()
                }
            ) {
                Text(UiTexts.testButton(lang))
            }
        }

        Box(modifier = Modifier.height(24.dp)) {
            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun settingsTitle(lang: Lang) = when (lang) {
    Lang.AZ -> "Ayarlar"
    Lang.EN -> "Settings"
    Lang.RU -> "Настройки"
}

private fun backLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Geri"
    Lang.EN -> "Back"
    Lang.RU -> "Назад"
}

private fun homeListening(lang: Lang) = when (lang) {
    Lang.AZ -> "Gələn SMS-lər yoxlanılır. Təhlükəli mesaj gələndə xəbərdarlıq ailə üzvünə göndəriləcək. Sizin heç nə etmənizə ehtiyac yoxdur."
    Lang.EN -> "Incoming SMS are being checked. If a dangerous message arrives, the warning goes to the family member. You do not have to do anything."
    Lang.RU -> "Входящие SMS проверяются. Если придёт опасное сообщение, предупреждение получит член семьи. Вам ничего делать не нужно."
}

private fun homeIncompleteHelp(lang: Lang) = when (lang) {
    Lang.AZ -> "Xəbərdarlıq göndərilə bilməz. Qurulumu tamamlayın."
    Lang.EN -> "Warnings cannot be sent yet. Finish the setup."
    Lang.RU -> "Предупреждения пока не могут отправляться. Завершите настройку."
}

private fun protectedShortLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Qorunan"
    Lang.EN -> "Protecting"
    Lang.RU -> "Защищается"
}

private fun homeFinishSetup(lang: Lang) = when (lang) {
    Lang.AZ -> "Qurulumu tamamla"
    Lang.EN -> "Finish setup"
    Lang.RU -> "Завершить настройку"
}

/**
 * Off by default. The whole point of the app is that the person who received the
 * scam is never asked to judge it, and for a very anxious user an extra notice is
 * itself the harm. A family that wants the extra hold can turn it on here. The
 * help text below describes what the switch does; the reasoning stays in this
 * comment, where it belongs.
 */
private fun ownerNoticeLabel(lang: Lang) = when (lang) {
    Lang.AZ -> "Telefon sahibinə də sakit bildiriş göstər"
    Lang.EN -> "Also show the phone's owner a calm notice"
    Lang.RU -> "Показывать владельцу телефона спокойное уведомление"
}

private fun ownerNoticeHelp(lang: Lang) = when (lang) {
    Lang.AZ -> "Mesajın yoxlandığını görür. Ondan heç nə soruşulmur."
    Lang.EN -> "They see that the message is being checked. They are never asked to decide."
    Lang.RU -> "Он видит, что сообщение проверяется. Его ни о чём не спрашивают."
}

@Composable
private fun LangButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
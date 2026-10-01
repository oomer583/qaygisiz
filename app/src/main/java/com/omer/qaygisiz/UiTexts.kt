package com.omer.qaygisiz

object UiTexts {

    fun appTitle(lang: Lang) = when (lang) {
        Lang.AZ -> "Qayğısız"
        Lang.EN -> "Qaygisiz"
        Lang.RU -> "Qaygisiz"
    }

    fun tagline(lang: Lang) = when (lang) {
        Lang.AZ -> "Dələduz SMS-lərdən qoruyur, xəbərdarlığı sizə göndərir."
        Lang.EN -> "Catches scam SMS and sends the warning to you, not to them."
        Lang.RU -> "Ловит мошеннические SMS и предупреждает вас, а не их."
    }

    fun statusActive(lang: Lang) = when (lang) {
        Lang.AZ -> "Qoruma aktivdir"
        Lang.EN -> "Protection is active"
        Lang.RU -> "Защита включена"
    }

    fun statusIncomplete(lang: Lang) = when (lang) {
        Lang.AZ -> "Qurulum tamamlanmayıb"
        Lang.EN -> "Setup is not complete"
        Lang.RU -> "Настройка не завершена"
    }

    fun languageLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Dil"
        Lang.EN -> "Language"
        Lang.RU -> "Язык"
    }

    fun protectedLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Kim qorunur?"
        Lang.EN -> "Who is protected?"
        Lang.RU -> "Кого защищаем?"
    }

    fun protectedHint(lang: Lang) = when (lang) {
        Lang.AZ -> "Atam, anam, nənəm..."
        Lang.EN -> "My father, my mother, my grandmother..."
        Lang.RU -> "Мой отец, моя мама, моя бабушка..."
    }

    fun tokenLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Telegram bot tokeni"
        Lang.EN -> "Telegram bot token"
        Lang.RU -> "Токен Telegram-бота"
    }

    fun chatIdLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Telegram chat ID"
        Lang.EN -> "Telegram chat ID"
        Lang.RU -> "Telegram chat ID"
    }

    fun telegramHelp(lang: Lang) = when (lang) {
        Lang.AZ -> "Telegram-da @BotFather ilə bot yaradın. Xəbərdarlıq alacaq şəxs bota Start desin."
        Lang.EN -> "Create a bot with @BotFather in Telegram. The person who will receive the warnings must press Start."
        Lang.RU -> "Создайте бота через @BotFather в Telegram. Получатель предупреждений должен нажать Start."
    }

    fun permissionLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "SMS icazəsi"
        Lang.EN -> "SMS permission"
        Lang.RU -> "Разрешение на SMS"
    }

    fun permissionGranted(lang: Lang) = when (lang) {
        Lang.AZ -> "verilib"
        Lang.EN -> "granted"
        Lang.RU -> "выдано"
    }

    fun permissionMissing(lang: Lang) = when (lang) {
        Lang.AZ -> "verilməyib"
        Lang.EN -> "not granted"
        Lang.RU -> "не выдано"
    }

    fun grantButton(lang: Lang) = when (lang) {
        Lang.AZ -> "İcazə ver"
        Lang.EN -> "Grant"
        Lang.RU -> "Выдать"
    }

    fun saveButton(lang: Lang) = when (lang) {
        Lang.AZ -> "Yadda saxla"
        Lang.EN -> "Save"
        Lang.RU -> "Сохранить"
    }

    fun testButton(lang: Lang) = when (lang) {
        Lang.AZ -> "Test mesajı göndər"
        Lang.EN -> "Send a test message"
        Lang.RU -> "Отправить тест"
    }

    fun saved(lang: Lang) = when (lang) {
        Lang.AZ -> "Yadda saxlanıldı"
        Lang.EN -> "Saved"
        Lang.RU -> "Сохранено"
    }

    fun testSending(lang: Lang) = when (lang) {
        Lang.AZ -> "Göndərilir..."
        Lang.EN -> "Sending..."
        Lang.RU -> "Отправка..."
    }

    fun testOk(lang: Lang) = when (lang) {
        Lang.AZ -> "Test mesajı göndərildi"
        Lang.EN -> "Test message sent"
        Lang.RU -> "Тестовое сообщение отправлено"
    }

    fun testFailed(lang: Lang) = when (lang) {
        Lang.AZ -> "Göndərilmədi. Token və chat ID-ni yoxlayın."
        Lang.EN -> "Not sent. Check the token and the chat ID."
        Lang.RU -> "Не отправлено. Проверьте токен и chat ID."
    }

    fun testMessage(lang: Lang) = when (lang) {
        Lang.AZ -> "Qayğısız qurulub. Təhlükəli SMS gələndə xəbərdarlıq bu çata gələcək."
        Lang.EN -> "Qaygisiz is set up. Warnings about dangerous SMS will arrive in this chat."
        Lang.RU -> "Qaygisiz настроен. Предупреждения об опасных SMS будут приходить в этот чат."
    }
}

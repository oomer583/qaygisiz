package com.omer.qaygisiz

enum class ReasonCode {
    IP_URL,
    PUNYCODE,
    SHORTENER,
    SUSPICIOUS_TLD,
    BRAND_MISMATCH,
    INSECURE_HTTP,
    URGENCY,
    SENDER_MISMATCH
}

data class Reason(val code: ReasonCode, val a: String = "", val b: String = "")

object ReasonText {

    fun of(reason: Reason, lang: Lang): String = when (reason.code) {

        ReasonCode.IP_URL -> when (lang) {
            Lang.AZ -> "Link ad əvəzinə çılpaq IP ünvanına aparır"
            Lang.EN -> "The link points to a bare IP address instead of a name"
            Lang.RU -> "Ссылка ведёт на голый IP-адрес вместо имени"
        }

        ReasonCode.PUNYCODE -> when (lang) {
            Lang.AZ -> "Domen adında gizlədilmiş hərflər var"
            Lang.EN -> "The domain name contains disguised characters"
            Lang.RU -> "В имени домена спрятаны подменные символы"
        }

        ReasonCode.SHORTENER -> when (lang) {
            Lang.AZ -> "Qısaldılmış link: hara apardığı görünmür"
            Lang.EN -> "Shortened link: its real destination is hidden"
            Lang.RU -> "Сокращённая ссылка: куда она ведёт, не видно"
        }

        ReasonCode.SUSPICIOUS_TLD -> when (lang) {
            Lang.AZ -> "Şübhəli domen sonluğu: ." + reason.a
            Lang.EN -> "Suspicious domain ending: ." + reason.a
            Lang.RU -> "Подозрительное окончание домена: ." + reason.a
        }

        ReasonCode.BRAND_MISMATCH -> when (lang) {
            Lang.AZ -> "Tanınmış ad (" + reason.a + ") var, amma domen rəsmi deyil: " + reason.b
            Lang.EN -> "Uses a known name (" + reason.a + ") but the domain is not official: " + reason.b
            Lang.RU -> "Использует известное имя (" + reason.a + "), но домен не официальный: " + reason.b
        }

        ReasonCode.INSECURE_HTTP -> when (lang) {
            Lang.AZ -> "Bağlantı şifrələnməyib (http)"
            Lang.EN -> "The connection is not encrypted (http)"
            Lang.RU -> "Соединение не зашифровано (http)"
        }

        ReasonCode.URGENCY -> when (lang) {
            Lang.AZ -> "Mesaj təcili hərəkət tələb edir"
            Lang.EN -> "The message demands urgent action"
            Lang.RU -> "Сообщение требует срочных действий"
        }

        ReasonCode.SENDER_MISMATCH -> when (lang) {
            Lang.AZ -> "Şirkət adından danışır, amma adi telefon nömrəsindən gəlib"
            Lang.EN -> "Speaks for a company but came from an ordinary phone number"
            Lang.RU -> "Говорит от имени компании, но пришло с обычного номера"
        }
    }
}

object Texts {

    fun headDangerous(lang: Lang) = when (lang) {
        Lang.AZ -> "TƏHLÜKƏLİ SMS"
        Lang.EN -> "DANGEROUS SMS"
        Lang.RU -> "ОПАСНОЕ SMS"
    }

    fun headSuspicious(lang: Lang) = when (lang) {
        Lang.AZ -> "ŞÜBHƏLİ SMS"
        Lang.EN -> "SUSPICIOUS SMS"
        Lang.RU -> "ПОДОЗРИТЕЛЬНОЕ SMS"
    }

    fun headInfo(lang: Lang) = when (lang) {
        Lang.AZ -> "Məlumat"
        Lang.EN -> "Notice"
        Lang.RU -> "Информация"
    }

    fun riskLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Risk"
        Lang.EN -> "Risk"
        Lang.RU -> "Риск"
    }

    fun riskHigh(lang: Lang) = when (lang) {
        Lang.AZ -> "yüksək"
        Lang.EN -> "high"
        Lang.RU -> "высокий"
    }

    fun riskMedium(lang: Lang) = when (lang) {
        Lang.AZ -> "orta"
        Lang.EN -> "medium"
        Lang.RU -> "средний"
    }

    fun riskLow(lang: Lang) = when (lang) {
        Lang.AZ -> "aşağı"
        Lang.EN -> "low"
        Lang.RU -> "низкий"
    }

    fun phoneOwner(lang: Lang) = when (lang) {
        Lang.AZ -> "Telefon sahibi"
        Lang.EN -> "Phone owner"
        Lang.RU -> "Владелец телефона"
    }

    fun from(lang: Lang) = when (lang) {
        Lang.AZ -> "Kimdən"
        Lang.EN -> "From"
        Lang.RU -> "От"
    }

    fun messageLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Mətn"
        Lang.EN -> "Message"
        Lang.RU -> "Сообщение"
    }

    fun reasonsLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Texniki səbəblər"
        Lang.EN -> "Technical reasons"
        Lang.RU -> "Технические причины"
    }

    fun linksLabel(lang: Lang) = when (lang) {
        Lang.AZ -> "Linklər"
        Lang.EN -> "Links"
        Lang.RU -> "Ссылки"
    }

    fun footer(lang: Lang) = when (lang) {
        Lang.AZ -> "Linkə toxunmayın, cavab yazmayın."
        Lang.EN -> "Do not tap the link and do not reply."
        Lang.RU -> "Не открывайте ссылку и не отвечайте."
    }

    fun defaultPerson(lang: Lang) = when (lang) {
        Lang.AZ -> "yaxınınız"
        Lang.EN -> "your relative"
        Lang.RU -> "ваш близкий"
    }
}

package com.omer.qaygisiz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * A calm notice on the protected phone. Off by default: see Prefs.showOwnerNotice.
 *
 * Every other app I looked at shows the recipient a warning and then asks them to
 * decide: "this may be dangerous, open anyway?". That hands the decision back to the
 * one person the scam has already fooled.
 *
 * This notice asks nothing. It has one button, "Got it", which only dismisses it. It
 * does not describe the fraud, does not use the word dangerous, and offers no choice.
 * Its only job is to hold the person still for the few minutes before their family
 * calls. It stays put rather than fading, because it has to still be there when they
 * look back at the phone.
 *
 * It is off unless a family turns it on. I tested the idea against the most
 * phone-fearful person I know, who is frightened even by a routine balance message:
 * for someone like her, an extra notice is itself the harm.
 */
object UserNotice {

    const val ACTION_DISMISS = "com.omer.qaygisiz.NOTICE_OK"
    const val NOTIFICATION_ID = 4801
    private const val CHANNEL_ID = "qaygisiz_hold"

    fun show(context: Context) {
        val lang = Settings.language
        val manager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Qayğısız",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = channelText(lang)
            channel.enableVibration(false)
            manager.createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dismiss = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, NoticeDismissReceiver::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val body = bodyText(lang)

        @Suppress("DEPRECATION")
        val notification = builder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titleText(lang))
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_launcher_foreground, okText(lang), dismiss)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted. The family alert still goes out.
        }
    }

    private fun titleText(lang: Lang) = when (lang) {
        Lang.AZ -> "Bu mesaj yoxlanılır"
        Lang.EN -> "This message is being checked"
        Lang.RU -> "Это сообщение проверяется"
    }

    private fun bodyText(lang: Lang) = when (lang) {
        Lang.AZ -> "Narahat olmayın. Ailəniz xəbərdar edildi. Siz heç nə etməlisiniz - sadəcə linkə toxunmayın."
        Lang.EN -> "Nothing to worry about. Your family has been told. You do not need to do anything - just do not tap the link."
        Lang.RU -> "Не волнуйтесь. Вашей семье уже сообщили. Вам ничего делать не нужно - просто не открывайте ссылку."
    }

    private fun okText(lang: Lang) = when (lang) {
        Lang.AZ -> "Anladım"
        Lang.EN -> "Got it"
        Lang.RU -> "Понятно"
    }

    private fun channelText(lang: Lang) = when (lang) {
        Lang.AZ -> "Sakit xəbərdarlıq - heç bir sual vermir"
        Lang.EN -> "A calm notice - it never asks you anything"
        Lang.RU -> "Спокойное уведомление - оно ничего не спрашивает"
    }
}

/** Clears the notice when the person presses "Got it". It does nothing else. */
class NoticeDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(UserNotice.NOTIFICATION_ID)
    }
}

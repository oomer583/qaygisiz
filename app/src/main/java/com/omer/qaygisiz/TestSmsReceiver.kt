package com.omer.qaygisiz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TestSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sender = intent.getStringExtra("sender") ?: "+994500000000"
        val body = intent.getStringExtra("body") ?: ""

        Alerter.handle(goAsync(), sender, body)
    }
}
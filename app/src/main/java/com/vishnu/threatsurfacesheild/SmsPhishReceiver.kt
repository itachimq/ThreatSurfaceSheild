package com.vishnu.threatsurfaceshield

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.net.Uri

class SmsPhishReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle: Bundle? = intent.extras
        val format = bundle?.getString("format")
        val pdus = bundle?.get("pdus") as? Array<*>

        if (pdus != null) {
            val builder = StringBuilder()
            for (pdu in pdus) {
                val msg = SmsMessage.createFromPdu(pdu as ByteArray, format)
                builder.append(msg.messageBody)
                builder.append('\n')
            }
            val text = builder.toString()
            val url = Regex("""https?://\S+|www\.\S+""").find(text)?.value

            if (url != null) {
                // Launch ThreatSurface to scan this SMS link
                val i = Intent(context, SecureBrowserActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
            }
        }
    }
}

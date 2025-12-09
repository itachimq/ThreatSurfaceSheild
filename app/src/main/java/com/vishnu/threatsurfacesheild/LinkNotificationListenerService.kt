package com.vishnu.threatsurfaceshield

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.net.Uri

class LinkNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        val url = extractFirstUrl(text) ?: return

        val i = Intent(this, SecureBrowserActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(i)
    }

    private fun extractFirstUrl(text: String): String? {
        val regex = Regex("""https?://\S+|www\.\S+""")
        return regex.find(text)?.value
    }
}

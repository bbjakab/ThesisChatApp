package hu.bme.vik.biborjakab.thesischatapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hu.bme.vik.biborjakab.thesischatapp.service.NotificationService

/**
 * Broadcast receiver a rendszer felállásához, jöhessenek értesítések a bejövő üzenetkről
 */
class StartServiceOnBoot: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, NotificationService::class.java)
            context.startService(serviceIntent)
        }
    }
}
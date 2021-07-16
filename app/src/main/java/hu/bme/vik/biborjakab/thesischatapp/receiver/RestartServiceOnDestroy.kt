package hu.bme.vik.biborjakab.thesischatapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hu.bme.vik.biborjakab.thesischatapp.service.NotificationService

/**
 * Broadcastreceiver arra az esetre, hogyha az android rendszer megölné a NotificationService-t
 * Ilyenkor újraindul a service
 */
class RestartServiceOnDestroy: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "hu.bme.vik.biborjakab.thesischatapp.NotificationServiceRestart") {
            val serviceIntent = Intent(context, NotificationService::class.java)
            context.startService(serviceIntent)
        }
    }
}
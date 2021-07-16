package hu.bme.vik.biborjakab.thesischatapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.AnyNewMessagesListener
import hu.bme.vik.biborjakab.thesischatapp.ui.MainActivity
import hu.bme.vik.biborjakab.thesischatapp.util.AnyNewMessageEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Service, ami figyeli a bejövő üzeneteket,
 * hogy értesülhessünk róluk.
 * Az alkalmazással együtt indul és még utána is fut
 */
class NotificationService: Service() {
    private val TAG = javaClass.name
    private val CHANNEL_ID = "AnyNewMessage"
    private var notificationIDCounter = 0
    private lateinit var anyNewMessagesListener: AnyNewMessagesListener

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        anyNewMessagesListener = AnyNewMessagesListener()

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        createNotificationChannel()

        anyNewMessagesListener.attach()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        anyNewMessagesListener.detach()
        EventBus.getDefault().unregister(this)

        sendBroadcast(Intent("hu.bme.vik.biborjakab.thesischatapp.NotificationServiceRestart"))
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.text_notification_channel_description)
            val descriptionText = getString(R.string.text_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Subscribe()
    fun onNewMessage(event: AnyNewMessageEvent) {
        val sender = event.messagePartner.userInfo
        val message = event.messagePartner.lastMessage

        val activityIntent = Intent(this, MainActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_message)
            .setContentTitle(sender.displayName)
            .setContentText(message.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(++notificationIDCounter, notification)
        }
    }





}
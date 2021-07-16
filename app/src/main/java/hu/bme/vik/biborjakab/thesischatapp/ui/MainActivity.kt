package hu.bme.vik.biborjakab.thesischatapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.data.Database
import hu.bme.vik.biborjakab.thesischatapp.service.NotificationService
import hu.bme.vik.biborjakab.thesischatapp.util.NetworkAvailableEvent
import hu.bme.vik.biborjakab.thesischatapp.util.NetworkUtils
import hu.bme.vik.biborjakab.thesischatapp.util.UserLogOutEvent
import hu.bme.vik.biborjakab.thesischatapp.util.UserLoginSuccessEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.tbMainToolbar))
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        NetworkUtils.registerNetworkCallback(applicationContext)
        if (Auth.currentUser != null) {
            EventBus.getDefault().post(UserLoginSuccessEvent())
        }
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        NetworkUtils.unregisterNetworkCallback(applicationContext)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.findFragmentById(R.id.nav_graph_host)!!.childFragmentManager.backStackEntryCount == 0) {
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.text_confirm_exit))
                    .setTitle(getString(R.string.action_exit))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.action_yes)
                    ) { _, _ ->
                        super.onBackPressed()
                    }
                    .setNegativeButton(getString(R.string.action_no), null)
                    .show()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Amikor egy felhasználó sikeresen bejelentkezik,
     * iratkozzunk fel a bejövő üzenetek figyelésére
     */
    @Subscribe
    fun onUserLoggedInEvent(event: UserLoginSuccessEvent) {
        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)
    }

    /**
     * Amikor a felhasználó kijelentkezik, iratkozzunk le és
     * "takarítsuk fel" az objektumokat
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserLoggedOutEvent(event: UserLogOutEvent) {
        Database.cleanUp()
        val serviceIntent = Intent(this, NotificationService::class.java)
        stopService(serviceIntent)
    }

    /**
     * Hálózat-változás által kiváltott esemény eseménye
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNetworkAvailableEvent(event: NetworkAvailableEvent) {
        if (event.isNetworkAvailable) {
            findViewById<TextView>(R.id.tvConnectionProblem).visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.tvConnectionProblem).visibility = View.VISIBLE
        }
    }

}
package hu.bme.vik.biborjakab.thesischatapp.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.Timestamp
import hu.bme.vik.biborjakab.thesischatapp.R
import org.greenrobot.eventbus.EventBus
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.*


//forrás: https://emailregex.com/
val REGEX_EMAIL = Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")

const val PAGINATING_AMOUNT_CHAT_MESSAGES: Long = 20
const val PAGINATING_AMOUNT_USER_RESULTS: Long = 10

enum class OrderBy {
    DISPLAY_NAME_ASC, DISPLAY_NAME_DESC, EMAIL_ADDRESS_ASC, EMAIL_ADDRESS_DESC
}

fun Fragment.showLoading(isLoading: Boolean) {
    val progressBar = requireActivity().findViewById(R.id.pbMainProgressBar) as LinearProgressIndicator
    if (isLoading) {
        progressBar.visibility = View.VISIBLE
    } else {
        progressBar.visibility = View.GONE
    }
}

fun Fragment.isLoading(): Boolean {
    val progressBar = requireActivity().findViewById(R.id.pbMainProgressBar) as LinearProgressIndicator
    return progressBar.visibility == View.VISIBLE
}

object DateTimeUtils {
    private val localeHU = Locale.forLanguageTag("hu-HU")

    fun formatDate(timestamp: Timestamp, context: Context): String {
        val date = LocalDate(timestamp.toDate())
        val now = LocalDate.now()

        val resultString =
            when {
                now.equals(date) -> {
                    context.getString(R.string.text_today)
                }
                now.minusDays(1) == date -> {
                    context.getString(R.string.text_yesterday)
                }
                date > now.minusWeeks(1) -> {
                    date.dayOfWeek().getAsText(localeHU)
                }
                date > now.minusYears(1) -> {
                    "${date.monthOfYear().getAsText(localeHU)} ${date.dayOfMonth().asText}."
                }
                else -> {
                    "${date.year().asText}. ${date.monthOfYear().getAsText(localeHU)} ${date.dayOfMonth().asText}."
                }
            }
        return resultString.capitalize(localeHU)
    }

    fun formatTime(timestamp: Timestamp): String {
        val format: DateTimeFormatter = DateTimeFormat.shortTime().withLocale(localeHU)

        val date = LocalDateTime(timestamp.toDate())
        return date.toString(format)
    }

    fun getSingleDateTime(timestamp: Timestamp, context: Context): String {
        return "${formatDate(timestamp, context)}, ${formatTime(timestamp)}"
    }
}

object Utils {
    fun showKeyboard(showKeyboard: Boolean, context: Context, view: View) {
        val imm: InputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (!showKeyboard) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } else {
            imm.showSoftInput(view, 0)
        }
    }

/*
//   fun toggleViewVisibility(view: View, useGoneInsteadOfInvisible: Boolean) {
//        if (view.visibility == View.VISIBLE) {
//            if (useGoneInsteadOfInvisible) {
//                view.visibility = View.GONE
//            }
//            else {
//                view.visibility = View.INVISIBLE
//            }
//        } else {
//            view.visibility = View.VISIBLE
//        }
//    }
*/

}

abstract class RecyclerViewScrollListener(private val isScrollReversed: Boolean) : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = (recyclerView.layoutManager as LinearLayoutManager).childCount
        val totalItemCount = (recyclerView.layoutManager as LinearLayoutManager).itemCount

        val direction = if (isScrollReversed) -1 else 1
        if (!recyclerView.canScrollVertically(direction) && visibleItemCount <= totalItemCount) {
            if (!isLoading()) {
                loadMoreItems()
            }
        }
    }

    protected abstract fun loadMoreItems()
    abstract fun isLoading(): Boolean

}

object NetworkUtils {
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            EventBus.getDefault().post(NetworkAvailableEvent(true))
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            EventBus.getDefault().post(NetworkAvailableEvent(false))
        }
    }

    fun registerNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder().build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        if (connectivityManager.activeNetworkInfo?.isAvailable == false || connectivityManager.activeNetworkInfo == null) {
            EventBus.getDefault().post(NetworkAvailableEvent(false))
        }
    }

    fun unregisterNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

}





package hu.bme.vik.biborjakab.thesischatapp.ui.chat

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.data.model.TextMessageModel
import hu.bme.vik.biborjakab.thesischatapp.util.DateTimeUtils
import org.joda.time.LocalDate

private const val RECEIVED_MESSAGE = 0
private const val SENT_MESSAGE = 1

/**
 * Recyclerviewadapter a chatablak üzenetinek listában megjelenítésére
 */
class ChatMessagesAdapter(private val messages: MutableList<TextMessageModel>): RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater =  LayoutInflater.from(parent.context)

        return if (viewType == SENT_MESSAGE) {
            ViewHolder(inflater.inflate(R.layout.row_item_message_me, parent, false))
        } else {
            ViewHolder(inflater.inflate(R.layout.row_item_message_them, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]

        holder.tvMessageText.text = message.message
        holder.tvMessageSentTime.text = message.timestamp?.let { DateTimeUtils.formatTime(it) }

        if (message.timestamp == null)
            return

        var previousTimestamp = Timestamp(0, 0)
        if (position >= 1) {
            val previousMessage = messages[position - 1]
            previousTimestamp = previousMessage.timestamp!!
        }
        displayMessageDate(previousTimestamp, message.timestamp!!, holder.tvMessageSentDate)
    }


    private fun displayMessageDate(previousTimestamp: Timestamp, currentTimestamp: Timestamp, tvDate: TextView) {
        val sameDay = LocalDate(previousTimestamp.toDate()) == LocalDate(currentTimestamp.toDate())
        if (!sameDay) {
            tvDate.text = DateTimeUtils.formatDate(currentTimestamp, tvDate.context)
            tvDate.visibility = View.VISIBLE
        } else {
            tvDate.visibility = View.GONE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderUID == Auth.currentUser!!.uid ) {
            SENT_MESSAGE
        }
        else {
            RECEIVED_MESSAGE
        }
    }

    override fun getItemCount() = messages.size

    fun clear() {
        notifyItemRangeRemoved(0, messages.size)
        messages.clear()
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvMessageText: TextView
        val tvMessageSentDate: TextView
        val tvMessageSentTime: TextView

        init {
            tvMessageText = view.findViewById(R.id.tvMessage)
            tvMessageSentDate = view.findViewById(R.id.tvMessageSentDate)
            tvMessageSentTime = view.findViewById(R.id.tvMessageSentTime)
            tvMessageSentDate.visibility = View.GONE
        }
    }

    /**
     * Margóbeállítás a recyclerview elemeknek
     */
    class MarginItemDecorator(private val marginSizeVertical: Int, private val marginSizeHorizontal: Int): RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            with(outRect) {
                top = marginSizeVertical
                bottom = marginSizeVertical
                left = marginSizeHorizontal
                right = marginSizeHorizontal
            }
        }
    }
}


package hu.bme.vik.biborjakab.thesischatapp.ui.messagePartners

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.data.model.MessagePartnerModel
import hu.bme.vik.biborjakab.thesischatapp.util.DateTimeUtils

/**
 * Üzenetváltás-partnerek listája
 */
class MessagePartnersAdapter(private val messagePartners: List<MessagePartnerModel>) : RecyclerView.Adapter<MessagePartnersAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item_message_partner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvParnerName.text = messagePartners[position].userInfo.displayName
        holder.tvTimeDate.text = messagePartners[position].lastMessage.timestamp?.let { DateTimeUtils.getSingleDateTime(it, holder.itemView.context) }

        val text = if (messagePartners[position].lastMessage.senderUID == Auth.currentUser!!.uid) {
                        holder.itemView.context.getString(R.string.text_you) + ": " + messagePartners[position].lastMessage.message
                    }
                    else {
                        messagePartners[position].lastMessage.message
                    }
        holder.tvLastMessage.text = text
    }

    override fun getItemCount() = messagePartners.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvParnerName: TextView
        val tvLastMessage: TextView
        val tvTimeDate: TextView

        init {
            tvParnerName = view.findViewById(R.id.tvPartnerName)
            tvLastMessage = view.findViewById(R.id.tvLastMessage)
            tvTimeDate = view.findViewById(R.id.tvTimeDate)

            view.setOnClickListener {
                view.findNavController().navigate(
                        MessagesFragmentDirections.actionMessagesFragmentToChatFragment(messagePartners[adapterPosition].userInfo)
                )
            }
        }
    }

}

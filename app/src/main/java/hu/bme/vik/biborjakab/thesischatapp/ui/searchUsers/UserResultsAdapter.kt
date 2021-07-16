package hu.bme.vik.biborjakab.thesischatapp.ui.searchUsers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.data.model.UserModel

/**
 * A keresés képernyőn található felhasználók listája
 */
class UserResultsAdapter(private val userResults: MutableList<UserModel>): RecyclerView.Adapter<UserResultsAdapter.ViewHolder>() {
    private val TAG = javaClass.name

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item_partner_userinfo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvDisplayName.text = userResults[position].displayName
        holder.tvEmailAddress.text = userResults[position].emailAddress
        if (userResults[position].userID == Auth.currentUser!!.uid) {
            holder.tvDisplayName.text = holder.tvDisplayName.context.getString(R.string.text_userinfo_me_brackets, holder.tvDisplayName.text.toString())
            holder.itemView.setOnClickListener {  }
        }

    }

    override fun getItemCount() = userResults.size

    fun clear() {
        notifyItemRangeRemoved(0, userResults.size)
        userResults.clear()
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvDisplayName: TextView
        val tvEmailAddress: TextView

        init {
            tvDisplayName = view.findViewById(R.id.tvDisplayName)
            tvEmailAddress = view.findViewById(R.id.tvEmailAddress)
            view.setOnClickListener {
                view.findNavController().navigate(
                        SearchUsersFragmentDirections.actionPartnerResultsToChatFragment(userResults[adapterPosition])
                )
            }
        }
    }
}
package hu.bme.vik.biborjakab.thesischatapp.ui.messagePartners

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.data.Database
import hu.bme.vik.biborjakab.thesischatapp.databinding.FragmentMessagesBinding
import hu.bme.vik.biborjakab.thesischatapp.data.model.MessagePartnerModel
import hu.bme.vik.biborjakab.thesischatapp.util.GetMessagePartnersEvent
import hu.bme.vik.biborjakab.thesischatapp.util.MessagePartnerUpdateEvent
import hu.bme.vik.biborjakab.thesischatapp.util.showLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * A legutóbbi üzenetek listáját megjelenítő oldal
 */
class MessagesFragment : Fragment(R.layout.fragment_messages) {
    private val TAG = this.javaClass.name

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val messagePartners = mutableListOf<MessagePartnerModel>()
    private lateinit var adapterRecyclerView: MessagePartnersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        val view = binding.root

        (requireActivity() as AppCompatActivity).supportActionBar?.title = getString(R.string.title_messages)
        setHasOptionsMenu(true)

        return view
    }

    override fun onStart() {
        super.onStart()
        showLoading(false)

        if (Auth.currentUser == null) {
            binding.tvWelcomeTop.visibility = View.INVISIBLE
            showNotSignedInDialog()
        } else {
            binding.tvWelcomeTop.visibility = View.VISIBLE
            binding.tvWelcomeTop.text = getString(R.string.text_messages_welcome_top, Auth.currentUser?.displayName, Auth.currentUser?.email)
            initRecyclerView(requireView())

        }
        EventBus.getDefault().register(this)

        if (messagePartners.size <= 1) {
            showLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                Database.getMessagePartnersNow()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_messages_toolbar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_logout -> {
                AlertDialog.Builder(requireContext())
                        .setMessage(getString(R.string.text_confirm_sign_out))
                        .setTitle(getString(R.string.action_sign_out))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.action_yes)
                        ) { _, _ ->
                            Auth.signOut()
                            findNavController().navigate(R.id.action_messagesFlow_to_loginFlow)
                        }
                        .setNegativeButton(getString(R.string.action_no), null)
                        .show()
                true
            }
            R.id.action_add_partner -> {
                findNavController().navigate(R.id.action_messages_fragment_to_addPartnerFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNotSignedInDialog(){
        AlertDialog.Builder(requireContext())
                .setTitle("Nincs bejelentkezve")
                .setMessage("Az alkalmazás használatához bejelentkezés szükséges.")
                .setPositiveButton(R.string.action_login) { _, _ ->
                    view?.post {
                        findNavController().navigate(R.id.action_messagesFlow_to_loginFlow)
                    }
                }
                .setNegativeButton(R.string.action_exit) { _, _ ->
                    requireActivity().finish()
                }
                .setCancelable(false)
                .create()
                .show()
    }

    private fun initRecyclerView(view: View) {
        adapterRecyclerView = MessagePartnersAdapter(messagePartners)
        binding.recyclerViewMessagePartners.adapter = adapterRecyclerView
        binding.recyclerViewMessagePartners.layoutManager = LinearLayoutManager(view.context)
        if (binding.recyclerViewMessagePartners.itemDecorationCount == 0) {
            binding.recyclerViewMessagePartners.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    /**
     * Amikor először jövünk erre a képernyőre, lekérjük a partnereket,
     * a válaszra itt iratkozunk fel
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onGetMessagePartnersEvent(event: GetMessagePartnersEvent){
        val stickyEvent = EventBus.getDefault().getStickyEvent(GetMessagePartnersEvent::class.java)
        if (stickyEvent != null) {
            if (!EventBus.getDefault().removeStickyEvent(stickyEvent)) {
                Log.d(TAG, "error removing sticky event getmessagepartnersonce")
            }
        }

        messagePartners.clear()
        messagePartners.addAll(event.messagePartners)
        adapterRecyclerView.notifyDataSetChanged()
        showLoading(false)
    }

    /**
     * Üzenet-partner változás esemény: ha a partnerrel
     * az utolsó üzenet megváltozik (azaz új üzenet jön/megy)
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessagePartnerUpdated(event: MessagePartnerUpdateEvent) {
        val result = messagePartners.filter {
            it.userInfo.userID == event.messagePartner.userInfo.userID
        }
        when (result.size) {
            0 -> {
                messagePartners.add(event.messagePartner)
            }
            1 -> {
                val index = messagePartners.indexOf(result[0])
                messagePartners[index].lastMessage = event.messagePartner.lastMessage
            }
            else -> {
                Log.d(TAG, "ERROR messagepartner duplicates!")
            }
        }

        messagePartners.sortByDescending{ it.lastMessage.timestamp }
        adapterRecyclerView.notifyDataSetChanged()
    }
}

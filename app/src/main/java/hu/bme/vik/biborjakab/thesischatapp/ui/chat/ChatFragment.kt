package hu.bme.vik.biborjakab.thesischatapp.ui.chat

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.databinding.FragmentChatBinding
import hu.bme.vik.biborjakab.thesischatapp.data.Database
import hu.bme.vik.biborjakab.thesischatapp.data.model.TextMessageModel
import hu.bme.vik.biborjakab.thesischatapp.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Chat ablak fragment, ahol a 2 fél üzenetei jelennek meg
 */
class ChatFragment : Fragment(R.layout.fragment_chat) {
    private val TAG = javaClass.name

    private var _binding:  FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val args: ChatFragmentArgs by navArgs()
    
    private val messages = mutableListOf<TextMessageModel>()
    private lateinit var recyclerViewAdapter: ChatMessagesAdapter
    private lateinit var recyclerViewLayoutManager: LinearLayoutManager
    private var noMessagesYet: Boolean = false
    private var startedOnceMore = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val view = binding.root

        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.messagePartner.displayName

        setHasOptionsMenu(true)
        initRecyclerView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noMessagesYetView(false)

        //ha a softkeyboard megjelenik, menjen az üzenetek aljára
        binding.recyclerViewChatMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                binding.recyclerViewChatMessages.post {
                    binding.recyclerViewChatMessages.scrollToPosition(messages.lastIndex)
                }
            }
        }

        binding.etMessage.doOnTextChanged { _, _, _, _ ->
            binding.textFieldMessage.isErrorEnabled = false
        }

        binding.btnSendMessage.setOnClickListener {
            if (!binding.etMessage.text.isNullOrEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    Database.sendTextMessage(args.messagePartner.userID, binding.etMessage.text.toString())
                    if (noMessagesYet) {
                        Database.attachNewMessagesListener(args.messagePartner.userID)
                    }
                }
                isSendingEnabled(false)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        showLoading(false)
        EventBus.getDefault().register(this)

        if (startedOnceMore) {
            recyclerViewAdapter.clear()
        }
        startedOnceMore = true

        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            isSendingEnabled(false)
            Database.getMessagesBatch(args.messagePartner.userID)
            Database.attachNewMessagesListener(args.messagePartner.userID)
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        Database.detachNewMessagesListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_chat_window_toolbar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_view_partnerInfo -> {
                AlertDialog.Builder(requireContext())
                        .setTitle(args.messagePartner.displayName)
                        .setMessage(args.messagePartner.emailAddress)
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.action_ok)
                        ) { dialogInterface: DialogInterface, _: Int ->
                            dialogInterface.dismiss()
                        }
                        .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun initRecyclerView() {
        val recyclerView = binding.recyclerViewChatMessages

        recyclerViewLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerViewLayoutManager.stackFromEnd = true
        recyclerView.layoutManager = recyclerViewLayoutManager

        recyclerViewAdapter = ChatMessagesAdapter(messages)
        recyclerView.adapter = recyclerViewAdapter

        recyclerView.addItemDecoration(ChatMessagesAdapter.MarginItemDecorator(0, 16))

        //paging megvalósítása
        recyclerView.addOnScrollListener(object : RecyclerViewScrollListener(true) {
            override fun loadMoreItems() {
                showLoading(true)
                lifecycleScope.launch(Dispatchers.IO) {
                    Database.getMessagesBatch(args.messagePartner.userID, messages.first())
                }
            }

            override fun isLoading(): Boolean {
                return this@ChatFragment.isLoading()
            }
        })
    }

    /**
     * Megjelenés és működés változtatása az alapján,
     * hogy van e már üzenetváltás a 2 partner között
     */
    private fun noMessagesYetView(noMessagesYet: Boolean) {
        this.noMessagesYet = noMessagesYet
        if (noMessagesYet) {
            binding.recyclerViewChatMessages.visibility = View.GONE
            binding.tvNoMessagesYet.visibility = View.VISIBLE
        } else {
            binding.recyclerViewChatMessages.visibility = View.VISIBLE
            binding.tvNoMessagesYet.visibility = View.GONE
        }
    }

    /**
     * Amíg el nem ment az üzenet, addig le van tiltva a küldés
     */
    private fun isSendingEnabled(isSendingEnabled: Boolean) {
        binding.btnSendMessage.isEnabled = isSendingEnabled
    }

    /**
     * Múltbeli üzenetek megjelenítése
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPastMessagesEvent(event: PastMessagesEvent) {
        if (event.messageList == null) {
            Log.d(TAG, "CHAT WINDOW load messages error ${event.error}")
            return
        }

        for (msg in event.messageList) {
            messages.add(0, msg)
        }

        recyclerViewAdapter.notifyItemRangeInserted(0, event.messageList.size)
        if (event.isFirst) {
            recyclerViewLayoutManager.scrollToPosition(messages.lastIndex)
        }

        showLoading(false)
        isSendingEnabled(true)
    }

    /**
     * Ha még nincs közös chatszobájuk, ami azt jelenti,
     * hogy nincs még  üzenetváltás a 2 partner között
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNoChatRoomEvent(event: NoChatRoomExceptionEvent) {
        noMessagesYetView(true)
        showLoading(false)
        isSendingEnabled(true)
    }

    /**
     * Ide futnak be az új, ebben chatszobában lévő üzenetek, tehát
     * ami vagy "tőlem" vagy "tőle" jött.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewMessageInChatRoomEvent(event: NewMessageInChatRoomEvent) {
        if (event.message.senderUID == Auth.currentUser!!.uid) {
            isSendingEnabled(true)
            binding.etMessage.text = null
        }
        noMessagesYetView(false)

        messages.add(event.message)
        recyclerViewAdapter.notifyItemInserted(messages.lastIndex)
        recyclerViewLayoutManager.scrollToPosition(messages.lastIndex)
    }

    /**
     * Ha hálózati hiba miatt hibára fut a küldés
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNetworkErrorEvent(event: NetworkErrorEvent) {
        binding.textFieldMessage.error = getString(R.string.error_network_error)
        isSendingEnabled(true)
    }

}
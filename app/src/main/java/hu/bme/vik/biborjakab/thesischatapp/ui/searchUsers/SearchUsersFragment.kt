package hu.bme.vik.biborjakab.thesischatapp.ui.searchUsers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.DocumentSnapshot
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Database
import hu.bme.vik.biborjakab.thesischatapp.data.model.UserModel
import hu.bme.vik.biborjakab.thesischatapp.databinding.FragmentAddPartnerBinding
import hu.bme.vik.biborjakab.thesischatapp.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Képernyő a rendszerben található felhasználók keresésére,
 * egyfajta telefonkönyv
 */
class SearchUsersFragment: Fragment(R.layout.fragment_add_partner) {
    private val TAG = javaClass.name
    private var _binding: FragmentAddPartnerBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerViewAdapter: UserResultsAdapter
    private val usersList = mutableListOf<UserModel>()

    private var lastQueriedUserDoc: DocumentSnapshot? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddPartnerBinding.inflate(inflater, container, false)
        val view = binding.root

        (requireActivity() as AppCompatActivity).supportActionBar?.title = getString(R.string.title_fragment_add_partner)

        initDropDownList()
        initRecyclerView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSearch.setOnClickListener {
            recyclerViewAdapter.clear()
            showLoading(true)

            lifecycleScope.launch(Dispatchers.IO) {
                Database.searchUsers(getOrderBy())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        showLoading(false)
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun getOrderBy(): OrderBy {
        return when (binding.textFieldOrderBy.editText?.text.toString()){
            getString(R.string.text_sort_email_asc) -> {
                OrderBy.EMAIL_ADDRESS_ASC
            }
            getString(R.string.text_sort_email_desc) -> {
                OrderBy.EMAIL_ADDRESS_DESC
            }
            getString(R.string.text_sort_name_asc) -> {
                OrderBy.DISPLAY_NAME_ASC
            }
            getString(R.string.text_sort_name_desc) -> {
                OrderBy.DISPLAY_NAME_DESC
            } else -> {
                throw Exception("Wrong text in dropdown menu")
            }
        }
    }

    private fun initDropDownList() {
        val items = listOf(getString(R.string.text_sort_name_asc), getString(R.string.text_sort_name_desc), getString(R.string.text_sort_email_asc), getString(R.string.text_sort_email_desc))
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.row_item_menu, items)
        val dropDown = (binding.textFieldOrderBy.editText as AutoCompleteTextView)
        dropDown.setAdapter(arrayAdapter)
        dropDown.setText(getString(R.string.text_sort_name_asc), false)
    }

    private fun initRecyclerView() {
        recyclerViewAdapter = UserResultsAdapter(usersList)
        binding.recyclerViewPartnerResults.adapter = recyclerViewAdapter
        binding.recyclerViewPartnerResults.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        binding.recyclerViewPartnerResults.addOnScrollListener(object : RecyclerViewScrollListener(false) {
            override fun loadMoreItems() {
                showLoading(true)

                lifecycleScope.launch(Dispatchers.IO) {
                    Database.searchUsers(getOrderBy(), lastQueriedUserDoc)
                }
            }

            override fun isLoading(): Boolean {
                return this@SearchUsersFragment.isLoading()
            }
        })
    }

    /**
     * Keresési találatok ereménye
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserSearchResultsEvent(event: SearchUsersResultEvent) {
        showLoading(false)
        if (event.usersList == null) {
            if (event.error !is NoSuchElementException)
                Log.d(TAG, "ERROR ${event.error}")
            return
        }

        lastQueriedUserDoc = event.lastQueriedUserDoc

        usersList.addAll(event.usersList)
        recyclerViewAdapter.notifyItemRangeInserted(usersList.lastIndex, event.usersList.size)
    }

}
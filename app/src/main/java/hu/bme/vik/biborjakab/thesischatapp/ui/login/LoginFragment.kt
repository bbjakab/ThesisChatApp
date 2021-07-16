package hu.bme.vik.biborjakab.thesischatapp.ui.login


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.util.REGEX_EMAIL
import hu.bme.vik.biborjakab.thesischatapp.util.showLoading
import hu.bme.vik.biborjakab.thesischatapp.databinding.FragmentLoginBinding
import hu.bme.vik.biborjakab.thesischatapp.util.UserLogInEvent
import hu.bme.vik.biborjakab.thesischatapp.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * A bejelentkezés képernyő.
 */

class LoginFragment : Fragment(R.layout.fragment_login) {
    private val TAG = this.javaClass.name

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = getString(R.string.action_login)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emailPassword.textFieldPassword.errorIconDrawable = null

        binding.emailPassword.etEmail.doOnTextChanged { _, _, _, _ ->
            binding.emailPassword.textFieldEmail.error = null
        }

        binding.emailPassword.etPassword.doOnTextChanged { _, _, _, _ ->
            binding.emailPassword.textFieldPassword.error = null
        }

        binding.btnLogin.setOnClickListener {
            //email ellenőrzés
            if (binding.emailPassword.etEmail.text.isNullOrBlank()) {
                binding.emailPassword.textFieldEmail.error = getString(R.string.error_email_empty)
            } else if (!binding.emailPassword.etEmail.text.toString().matches(REGEX_EMAIL)) {
                binding.emailPassword.textFieldEmail.error = getString(R.string.error_email_invalid_format)
            } else {
                binding.emailPassword.textFieldEmail.error = null
            }
            //jelszó ellenőrzés
            if (binding.emailPassword.etPassword.text.isNullOrEmpty()) {
                binding.emailPassword.textFieldPassword.error = getString(R.string.error_password_empty)
            } else {
                binding.emailPassword.textFieldPassword.error = null
            }

            if (binding.emailPassword.textFieldEmail.error.isNullOrEmpty() && binding.emailPassword.textFieldPassword.error.isNullOrEmpty()) {
                showLoading(true)
                lifecycleScope.launch(Dispatchers.IO) {
                    Auth.signInWithEmailAndPassword(
                            binding.emailPassword.etEmail.text.toString(),
                            binding.emailPassword.etPassword.text.toString()
                    )
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            requireView().findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        showLoading(false)
        EventBus.getDefault().register(this)
        if (Auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFlow_to_messagesFlow)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.emailPassword.etPassword.text = null
        binding.emailPassword.textFieldEmail.error = null
        binding.emailPassword.textFieldPassword.error = null
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * A bejelentkezés ereménye
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserLoginEvent(event: UserLogInEvent) {
        showLoading(false)
        if (event.exception != null) {
            Log.d(TAG, "ERROR ${event.exception}")
        }

        val snackbar = Snackbar.make(requireView(), getString(R.string.error_login_failed), Snackbar.LENGTH_LONG)

        when (event.exception) {
            null -> {
                Utils.showKeyboard(false, requireContext(), requireView())
                findNavController().navigate(R.id.action_loginFlow_to_messagesFlow)
            }
            is FirebaseAuthInvalidCredentialsException -> {
                snackbar.setText(getString(R.string.error_login_invalid_password))
                snackbar.show()
            }
            is FirebaseAuthInvalidUserException -> {
                snackbar.setText(R.string.error_login_no_user)
                snackbar.show()
            }
            is FirebaseTooManyRequestsException -> {
                snackbar.setText(R.string.error_too_many_requests)
                snackbar.show()
            }
            else -> {
                snackbar.show()
            }
        }
    }
}


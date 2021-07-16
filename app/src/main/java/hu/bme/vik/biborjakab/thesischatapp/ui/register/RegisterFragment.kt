package hu.bme.vik.biborjakab.thesischatapp.ui.register

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.*
import hu.bme.vik.biborjakab.thesischatapp.R
import hu.bme.vik.biborjakab.thesischatapp.data.Auth
import hu.bme.vik.biborjakab.thesischatapp.util.REGEX_EMAIL
import hu.bme.vik.biborjakab.thesischatapp.util.UserRegisterEvent
import hu.bme.vik.biborjakab.thesischatapp.util.showLoading
import hu.bme.vik.biborjakab.thesischatapp.databinding.FragmentRegisterBinding
import hu.bme.vik.biborjakab.thesischatapp.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Regisztráció képernyő
 */
class RegisterFragment : Fragment(R.layout.fragment_register) {
    private val TAG = this.javaClass.name

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = getString(R.string.action_register)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textFieldPasswordSecond.errorIconDrawable = null
        binding.emailPassword.textFieldPassword.errorIconDrawable = null

        binding.etDisplayName.doOnTextChanged { _, _, _, _ ->
            binding.textFieldDisplayName.error = null
        }
        binding.emailPassword.etEmail.doOnTextChanged { _, _, _, _ ->
            binding.emailPassword.textFieldEmail.error = null
        }
        binding.emailPassword.etPassword.doOnTextChanged { _, _, _, _ ->
            binding.emailPassword.textFieldPassword.error = null
            binding.textFieldPasswordSecond.error = null
        }
        binding.etPasswordSecond.doOnTextChanged { _, _, _, _ ->
            binding.textFieldPasswordSecond.error = null
        }

        binding.btnRegister.setOnClickListener {
            //felhasználónév ellenőrzés
            if (binding.etDisplayName.text.isNullOrBlank()) {
                binding.textFieldDisplayName.error = getString(R.string.error_displayname_empty)
            }
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
            //jelszó ismét egyezés ellenőrzés
            if (binding.emailPassword.etPassword.text.toString() != binding.etPasswordSecond.text.toString()) {
                binding.textFieldPasswordSecond.error = getString(R.string.error_passwords_dont_match)

            }

            if (binding.textFieldDisplayName.error.isNullOrEmpty() &&
                binding.emailPassword.textFieldEmail.error.isNullOrEmpty() &&
                binding.emailPassword.textFieldPassword.error.isNullOrEmpty() &&
                binding.textFieldPasswordSecond.error.isNullOrEmpty()) {

                showLoading(true)
                lifecycleScope.launch(Dispatchers.IO) {
                    Auth.registerWithEmailAndPassword(
                            binding.emailPassword.etEmail.text.toString(),
                            binding.emailPassword.etPassword.text.toString(),
                            binding.etDisplayName.text.toString()
                    )
                }
            }
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
        binding.textFieldDisplayName.error = null
        binding.emailPassword.etPassword.text = null
        binding.etPasswordSecond.text = null
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
     * Felhasználó regisztráció eredménye
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserRegisterEvent(event: UserRegisterEvent){
        showLoading(false)

        if (event.exception != null) {
            Log.d(TAG, "REGISTRATION ERROR ${event.exception}")
        }

        val snackbar = Snackbar.make(requireView(), getString(R.string.error_register_failed), Snackbar.LENGTH_LONG)

        when (event.exception) {
            null -> {
                Utils.showKeyboard(false, requireContext(), requireView())
                findNavController().navigate(R.id.action_loginFlow_to_messagesFlow)
            }
            is FirebaseAuthUserCollisionException -> {
                if (event.exception.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                    snackbar.setText(R.string.error_register_email_already_in_use)
                }
                snackbar.show()
            }
            is FirebaseAuthWeakPasswordException -> {
                snackbar.setText(R.string.error_password_weak)
                snackbar.show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                if (event.exception.errorCode == "ERROR_INVALID_EMAIL") {
                    snackbar.setText(R.string.error_register_email_invalid)
                }
                snackbar.show()
            }
            else -> {
                snackbar.show()
            }
        }
    }
}
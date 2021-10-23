package com.guerrero.mytrusteddevice.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import com.guerrero.mytrusteddevice.R
import com.guerrero.mytrusteddevice.databinding.FragmentRegisterBinding
import com.guerrero.mytrusteddevice.di.ViewModelFactory
import com.guerrero.mytrusteddevice.shared.User
import com.guerrero.mytrusteddevice.viewmodel.MyViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: MyViewModel by activityViewModels { factory }

    private var pushToken: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBtnLoginListener()
        getPushToken()
        observeViewState()
    }

    private fun setupBtnLoginListener() {
        with(binding) {
            btnLogin.setOnClickListener {
                val name = fieldName.editText?.text.toString()
                val password = fieldPassword.editText?.text.toString()
                if (name.isNotEmpty() && password.isNotEmpty()) {
                    val user = User(name, password)
                    viewModel.registerUser(user, pushToken)
                }
            }
        }
    }

    private fun getPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Snackbar.make(
                    binding.root,
                    task.exception?.message.toString(),
                    Snackbar.LENGTH_LONG
                ).show()

            } else {
                pushToken = task.result
            }
        }
    }

    private fun observeViewState() {
        viewModel.getViewStateObservable().observe(viewLifecycleOwner, { viewState ->
            when (viewState) {
                is ViewState.Loading -> setStateLoading()
                is ViewState.Error -> setStateError(viewState.message)
                is ViewState.Normal -> enableViews()
                is ViewState.Success -> setStateSuccess()
            }
        })
    }

    private fun setStateLoading() {
        disableViews()
        binding.run {
            btnLogin.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun setStateError(message: String) {
        enableViews()
        binding.run {
            btnLogin.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun setStateSuccess() {
        enableViews()
        binding.root.findNavController().navigate(R.id.action_registerFragment_to_challengesFragment)
    }

    private fun disableViews() {
        with(binding) {
            fieldName.isEnabled = false
            fieldPassword.isEnabled = false
            btnLogin.isEnabled = false
        }
    }

    private fun enableViews() {
        with(binding) {
            fieldName.isEnabled = true
            fieldPassword.isEnabled = true
            btnLogin.isEnabled = true
        }
    }
}

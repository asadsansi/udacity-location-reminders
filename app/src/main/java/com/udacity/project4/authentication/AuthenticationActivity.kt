package com.udacity.project4.authentication


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding

import com.udacity.project4.locationreminders.RemindersActivity


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val TAG = "AuthenticationActivity"
        const val SIGN_IN_RESULT_CODE = 1001
    }


    private val viewModel by viewModels<AuthenticationViewModel>()
    private lateinit var binding: ActivityAuthenticationBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)


        observeAuthenticationState()


        binding.loginButton.setOnClickListener {
            launchSignInFlow()
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SIGN_IN_RESULT_CODE) {

            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // User successfully signed in
                Log.i(
                    TAG,
                    "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!"
                )

               // navigateToRemindersActivity()

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    /**
     * Observes the authentication state and changes the Activity
     */

    private fun observeAuthenticationState() {

        viewModel.authenticationState.observe(this, Observer { authenticationState ->

            when (authenticationState) {

                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    Log.i("StartedActivity", "it's started")
                    navigateToRemindersActivity()
                }
                else -> {
                   // not doing anything here
//                    binding.loginButton.setOnClickListener {
//                        launchSignInFlow()
//                    }
                }
            }
        })
    }

    private fun navigateToRemindersActivity() {
        startActivity(Intent(this, RemindersActivity::class.java))
        finish()
    }

    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                //.setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .build(),
            SIGN_IN_RESULT_CODE
        )
    }

}




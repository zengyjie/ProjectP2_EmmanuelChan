package com.example.projectp2_emmanuelchan.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectp2_emmanuelchan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentSettingsBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var signInClient: SignInClient

    companion object {
        private const val TAG = "SettingsFragment"
        private const val WEB_CLIENT_ID = "1000486878358-3s8eb23gbcldg8556k5ctnj379dk2tva.apps.googleusercontent.com" // Replace with your Web Client ID
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = Identity.getSignInClient(requireActivity())
                .getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken

            if (idToken != null) {
                // Got an ID token from Google. Use it to authenticate with Firebase.
                firebaseAuthWithGoogle(idToken)
            } else {
                Log.d(TAG, "No ID token!")
                Toast.makeText(requireContext(), "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            Toast.makeText(requireContext(), "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Google Sign-In client
        signInClient = Identity.getSignInClient(requireActivity())

        // Initialize Credential Manager
        credentialManager = CredentialManager.create(requireContext())

        // Set up theme spinner
        setupThemeSpinner()

        // Update UI based on current sign-in state
        updateUI(auth.currentUser)

        binding.accountButton.setOnClickListener {
            if (auth.currentUser != null) {
                // User is signed in, show sign out dialog
                showSignOutDialog()
            } else {
                // User is not signed in, start sign-in flow
                signIn()
            }
        }

        binding.clearAllButton.setOnClickListener {
            showClearDataDialog()
        }

        return root
    }

    private fun setupThemeSpinner() {
        val themeSpinner: Spinner = binding.themeSpinner
        val themes = arrayOf("Default", "Dark")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, themes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = adapter
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences.getInt("theme", 0)
        themeSpinner.setSelection(currentTheme)

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != currentTheme) {
                    sharedPreferences.edit { putInt("theme", position) }
                    requireActivity().recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun signIn() {
        lifecycleScope.launch {
            try {
                val request = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(WEB_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()

                val result = signInClient.beginSignIn(request).await()
                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                signInLauncher.launch(intentSenderRequest)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting sign-in flow", e)
                Toast.makeText(requireContext(), "Could not start sign-in flow: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                    Toast.makeText(requireContext(), "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun showSignOutDialog() {
        val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
        val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(confirmDeleteView)
        val deleteDialog = confirmDialogBuilder.create()

        deleteDialog.findViewById<TextView>(R.id.nameTextView)?.text = "Are you sure you want to sign out?"
        deleteDialog.findViewById<Button>(R.id.noButton)?.setOnClickListener { deleteDialog.dismiss() }
        deleteDialog.findViewById<Button>(R.id.yesButton)?.setOnClickListener {
            auth.signOut()
            updateUI(null)
            Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
            deleteDialog.dismiss()
        }
        deleteDialog.show()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            binding.accountButton.text = "Sign Out"
            binding.accountTextView.text = user.displayName
        } else {
            binding.accountButton.text = "Sign In"
            binding.accountTextView.text = "Not logged in"
        }
    }

    private fun showClearDataDialog() {
        val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
        val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(confirmDeleteView)
        val deleteDialog = confirmDialogBuilder.create()

        confirmDeleteView.findViewById<TextView>(R.id.nameTextView).text = "Clear data? This action cannot be undone!"

        confirmDeleteView.findViewById<Button>(R.id.yesButton).setOnClickListener {
            val wineWiseDir = File(requireContext().filesDir, "WineWise")
            if (wineWiseDir.exists() && wineWiseDir.isDirectory) {
                wineWiseDir.listFiles()?.forEach { file ->
                    if (!file.delete()) { file.deleteOnExit() }
                }
            }
            fridges.clear()
            val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            // Keep the user signed in even after clearing app data
            val signedInUser = auth.currentUser != null
            sharedPreferences.edit().clear().apply()
            if (signedInUser) {
                // Preserve that the user is signed in
                sharedPreferences.edit { putBoolean("user_signed_in", true) }
            }
            FridgesFragment.saveFridges(requireContext())
            deleteDialog.dismiss()
            Toast.makeText(requireContext(), "All data cleared successfully", Toast.LENGTH_SHORT).show()
        }

        confirmDeleteView.findViewById<Button>(R.id.noButton).setOnClickListener {
            deleteDialog.dismiss()
        }

        deleteDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
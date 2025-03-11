package com.example.projectp2_emmanuelchan.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectp2_emmanuelchan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentSettingsBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var credentialManager: CredentialManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

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

        setupSignIn()

        binding.clearAllButton.setOnClickListener {
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
                sharedPreferences.edit().clear().apply()
                FridgesFragment.saveFridges(requireContext())
                deleteDialog.dismiss()
                Toast.makeText(requireContext(), "All data cleared successfully", Toast.LENGTH_SHORT).show()
            }


            confirmDeleteView.findViewById<Button>(R.id.noButton).setOnClickListener {
                deleteDialog.dismiss()
            }

            deleteDialog.show()
        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSignIn() {
        credentialManager = CredentialManager.create(requireContext())
        updateUI()

        binding.accountButton.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                signOut()
            } else {
                signIn()
            }
        }
    }

    private fun signIn() {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .setServerClientId(requireContext().getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse =
                    credentialManager.getCredential(requireContext(), request)

                val credential = result.credential
                if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken.toString(), null)
                    val result = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                    println(result.user)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        lifecycleScope.launch {
            try {
                val result = FirebaseAuth.getInstance().signInWithCredential(credential).await()
                println(result.user)
                if (result.user != null) {
                    updateUI()
                    Toast.makeText(context, "Sign-in successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                FirebaseAuth.getInstance().signOut()
                updateUI()
                Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Sign-out failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.accountTextView.text = user.displayName ?: "User"
            binding.accountButton.text = "Logout"
        } else {
            binding.accountTextView.text = "Not Signed In"
            binding.accountButton.text = "Sign In"
        }
    }
}
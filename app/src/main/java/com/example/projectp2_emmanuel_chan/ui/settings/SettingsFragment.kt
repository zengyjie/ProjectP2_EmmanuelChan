package com.example.projectp2_emmanuel_chan.ui.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuel_chan.R
import com.example.projectp2_emmanuel_chan.databinding.FragmentSettingsBinding
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Calendar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var signInClient: SignInClient
    private val WEB_CLIENT_ID get() = context?.getString(R.string.default_web_client_id) ?: ""
    private lateinit var db: FirebaseFirestore
    private val gson = Gson()


    companion object {
        private const val TAG = "SettingsFragment"
        fun getExpiredWines(): List<FridgesFragment.Wine> {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return fridges.flatMap { fridge ->
                fridge.wines.flatten().flatten().flatten()
            }.filter { wine ->
                wine.drinkBy != null &&
                        wine.drinkBy < currentYear &&
                        wine.parentFridge != "drunk"
            }
        }

        fun sendWineNotification(context: Context) {
            val wines = getExpiredWines()
            if (wines.isEmpty()) { return }
            val channelId = "wines_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Wines to drink",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }

            val notificationManager = NotificationManagerCompat.from(context)
            val intent = Intent(context, this::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("You have ${wines.size} wines past the drink-by date")
                .setContentText(wines.take(wines.size).joinToString("\n") { "${it.name} (${it.year})" })
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) { return }

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = Identity.getSignInClient(requireActivity())
                .getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) { firebaseAuthWithGoogle(idToken) }
            else { Toast.makeText(requireContext(), "Sign in failed", Toast.LENGTH_SHORT).show() }
        } catch (_: ApiException) {
            Toast.makeText(requireContext(), "Sign in failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requestNotificationPermission()
        auth = FirebaseAuth.getInstance()
        signInClient = Identity.getSignInClient(requireActivity())
        credentialManager = CredentialManager.create(requireContext())
        db = FirebaseFirestore.getInstance()

        setupThemeSpinner()
        updateUI(auth.currentUser)
        binding.loginButton.setOnClickListener {
            if (auth.currentUser != null) { showSignOutDialog() }
            else { signIn() }
        }
        binding.accountSettingsButton.setOnClickListener { showAccountSettingsDialog() }

        binding.clearAllButton.setOnClickListener { showClearDataDialog() }

        binding.contactUsButton.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "message/rfc822"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("winewise@gmail.com"))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact Us")
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }

        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        binding.notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications_enabled", false)

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()

            if (isChecked) {
                sendWineNotification(requireContext())
                Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
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
                            .setServerClientId(WEB_CLIENT_ID.toString())
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
                    val user = auth.currentUser
                    updateUI(user)
                    Toast.makeText(requireContext(), "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                } else {
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

        confirmDeleteView.findViewById<TextView>(R.id.nameTextView)?.text = "Are you sure you want to sign out?"
        confirmDeleteView.findViewById<Button>(R.id.noButton)?.setOnClickListener { deleteDialog.dismiss() }
        confirmDeleteView.findViewById<Button>(R.id.yesButton)?.setOnClickListener {
            auth.signOut()
            updateUI(null)
            Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
            deleteDialog.dismiss()
        }
        deleteDialog.show()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            binding.loginButton.text = "Sign Out"
            binding.accountTextView.text = user.displayName
            binding.accountSettingsButton.visibility = View.VISIBLE
        } else {
            binding.loginButton.text = "Sign In"
            binding.accountTextView.text = "Not logged in"
            binding.accountSettingsButton.visibility = View.GONE
        }
    }

    private fun showAccountSettingsDialog() {
        val accountSettingsView = LayoutInflater.from(context).inflate(R.layout.account_settings, null)
        val accountSettingsDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(accountSettingsView)
        val accountSettingsDialog = accountSettingsDialogBuilder.create()
        accountSettingsDialog.show()

        accountSettingsView.findViewById<TextView>(R.id.accountDialogTextView)?.text = "${auth.currentUser?.displayName}"

        accountSettingsDialog.findViewById<Button>(R.id.saveDataButton)?.setOnClickListener {
            val confirmView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
            val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(confirmView)
            val confirmDialog = confirmDialogBuilder.create()

            confirmView.findViewById<TextView>(R.id.nameTextView).text = "Save data? This will overwrite data on the cloud."

            confirmView.findViewById<Button>(R.id.yesButton).setOnClickListener  {
                uploadDataToCloud()
                confirmDialog.dismiss()
                accountSettingsDialog.dismiss()
            }

            confirmView.findViewById<Button>(R.id.noButton).setOnClickListener {
                confirmDialog.dismiss()
            }

            confirmDialog.show()
        }

        accountSettingsDialog.findViewById<Button>(R.id.loadDataButton)?.setOnClickListener {
            val confirmView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
            val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(confirmView)
            val confirmDialog = confirmDialogBuilder.create()

            confirmView.findViewById<TextView>(R.id.nameTextView).text = "Load data? This will overwrite current data."
            confirmView.findViewById<Button>(R.id.yesButton).setOnClickListener {
                downloadDataFromCloud()
                confirmDialog.dismiss()
                accountSettingsDialog.dismiss()
            }
            confirmView.findViewById<Button>(R.id.noButton).setOnClickListener {
                confirmDialog.dismiss()
            }

            confirmDialog.show()
        }

        accountSettingsDialog.findViewById<Button>(R.id.clearDataButton)?.setOnClickListener {
            val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
            val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(confirmDeleteView)
            val confirmDeleteDialog = confirmDialogBuilder.create()

            confirmDeleteView.findViewById<TextView>(R.id.nameTextView).text = "Clear cloud data? This action cannot be undone!"

            confirmDeleteView.findViewById<Button>(R.id.yesButton).setOnClickListener  {
                clearCloudData()
                Toast.makeText(requireContext(), "Data cleared successfully", Toast.LENGTH_SHORT).show()
                confirmDeleteDialog.dismiss()
                accountSettingsDialog.dismiss()
            }

            confirmDeleteView.findViewById<Button>(R.id.noButton).setOnClickListener {
                confirmDeleteDialog.dismiss()
            }

            confirmDeleteDialog.show()
        }
    }

    private fun showClearDataDialog() {
        val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
        val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(confirmDeleteView)
        val deleteDialog = confirmDialogBuilder.create()

        confirmDeleteView.findViewById<TextView>(R.id.nameTextView).text = "Clear local data? This action cannot be undone!"

        confirmDeleteView.findViewById<Button>(R.id.yesButton).setOnClickListener {
            val wineWiseDir = File(requireContext().filesDir, "WineWise")
            if (wineWiseDir.exists() && wineWiseDir.isDirectory) {
                wineWiseDir.listFiles()?.forEach { file ->
                    if (!file.delete()) { file.deleteOnExit() }
                }
            }
            fridges.clear()
            val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val signedInUser = auth.currentUser != null
            sharedPreferences.edit().clear().apply()
            if (signedInUser) {
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

    private fun uploadDataToCloud() {
        val user = auth.currentUser ?: return
        Toast.makeText(requireContext(), "Uploading data...", Toast.LENGTH_SHORT).show()
        val fridgesJson = gson.toJson(fridges)

        val userData = hashMapOf(
            "fridges" to fridgesJson,
            "theme" to requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getInt("theme", 0),
            "lastSync" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data uploaded successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun downloadDataFromCloud() {
        val user = auth.currentUser ?: return
        Toast.makeText(requireContext(), "Downloading data...", Toast.LENGTH_SHORT).show()

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val fridgesJson = document.getString("fridges")
                        if (!fridgesJson.isNullOrEmpty()) {
                            val type = object : com.google.gson.reflect.TypeToken<ArrayList<Any>>() {}.type
                            val downloadedFridges = gson.fromJson<ArrayList<FridgesFragment.Fridge>>(fridgesJson, type)

                            fridges.clear()
                            fridges.addAll(downloadedFridges)
                            FridgesFragment.saveFridges(requireContext())

                            document.getLong("theme")?.toInt()?.let { theme ->
                                requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit {
                                    putInt("theme", theme)
                                }
                            }

                            Toast.makeText(requireContext(), "Data downloaded successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "No cloud data found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error parsing data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No cloud data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Download failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun clearCloudData() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .delete()
            .addOnSuccessListener { Toast.makeText(requireContext(), "Cloud data cleared", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to clear cloud data", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
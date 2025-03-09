package com.example.projectp2_emmanuelchan.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectp2_emmanuelchan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentSettingsBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import java.io.File

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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
                    sharedPreferences.edit().putInt("theme", position).apply()
                    requireActivity().recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
}
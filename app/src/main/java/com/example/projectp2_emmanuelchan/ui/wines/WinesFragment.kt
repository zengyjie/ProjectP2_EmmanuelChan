package com.example.projectp2_emmanuelchan.ui.wines

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.getIndicesFromPosition
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allWinesAdapter: AllWinesAdapter
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImage: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWinesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val allWines = mutableListOf<FridgesFragment.Wine>()
        val allWinesRecyclerView = binding.allWinesRecyclerView
        allWinesRecyclerView.layoutManager = GridLayoutManager(context, 1)
        allWinesAdapter = AllWinesAdapter(allWines) { wine -> viewWine(wine) }
        allWinesRecyclerView.adapter = allWinesAdapter

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                capturedImage = imageBitmap
                Toast.makeText(context, "Image captured successfully", Toast.LENGTH_SHORT).show()
            } else { Toast.makeText(context, "Image capture failed", Toast.LENGTH_SHORT).show() }
        }

        //dummy data
        allWines.add(FridgesFragment.Wine())
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun viewWine(wine: FridgesFragment.Wine) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.wine_info, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<Button>(R.id.editWineButton).setOnClickListener {
            editWine(requireContext(), wine, cameraLauncher, capturedImage)
        }
        dialog.show()
    }

    companion object {
        fun editWine(context: Context, wine: FridgesFragment.Wine, cameraLauncher: ActivityResultLauncher<Intent>, capturedImage: Bitmap?) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.wine_edit, null)
            val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(dialogView)
            val dialog = dialogBuilder.create()

            dialogView.findViewById<MaterialTextView>(R.id.editTitleTextView)?.text = "Edit Wine"

            val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.editWineType)
            val wineTypes = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified")
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, wineTypes)
            wineTypeSpinner.adapter = spinnerAdapter
            wineTypeSpinner.setSelection(wineTypes.indexOf(wine.type)) // Select correct type

            val wineImageView = dialogView.findViewById<ImageView>(R.id.wineImage)
            if (wine.imagePath.isNotEmpty()) {
                val imageBitmap = BitmapFactory.decodeFile(wine.imagePath)
                wineImageView.setImageBitmap(imageBitmap)
            }

            dialogView.findViewById<ImageButton>(R.id.takeLabelButton).setOnClickListener {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraLauncher.launch(cameraIntent)
            }

            capturedImage?.let {
                wineImageView.setImageBitmap(it)
            }

            dialogView.findViewById<TextInputEditText>(R.id.editWineName)?.setText(wine.name)
            dialogView.findViewById<TextInputEditText>(R.id.editWineYear)?.setText(wine.year.toString())
            dialogView.findViewById<TextInputEditText>(R.id.editVineyard)?.setText(wine.vineyard)
            dialogView.findViewById<TextInputEditText>(R.id.editRegion)?.setText(wine.region)
            dialogView.findViewById<TextInputEditText>(R.id.editVariety)?.setText(wine.grapeVariety)
            dialogView.findViewById<TextInputEditText>(R.id.editRating)?.setText(wine.rating.toString())
            dialogView.findViewById<TextInputEditText>(R.id.editPrice)?.setText(wine.price.toString())
            dialogView.findViewById<TextInputEditText>(R.id.editDrinkBy)?.setText(wine.drinkBy.toString())
            dialogView.findViewById<TextInputEditText>(R.id.editDescription)?.setText(wine.description)

            dialogView.findViewById<Button>(R.id.saveButton)?.setOnClickListener {
                wine.name = dialogView.findViewById<TextInputEditText>(R.id.editWineName)?.text.toString()
                wine.type = wineTypeSpinner.selectedItem.toString()
                wine.year = dialogView.findViewById<TextInputEditText>(R.id.editWineYear)?.text.toString().toIntOrNull() ?: 0
                wine.vineyard = dialogView.findViewById<TextInputEditText>(R.id.editVineyard)?.text.toString()
                wine.region = dialogView.findViewById<TextInputEditText>(R.id.editRegion)?.text.toString()
                wine.grapeVariety = dialogView.findViewById<TextInputEditText>(R.id.editVariety)?.text.toString()
                wine.rating = dialogView.findViewById<TextInputEditText>(R.id.editRating)?.text.toString().toDoubleOrNull() ?: 0.0
                wine.price = dialogView.findViewById<TextInputEditText>(R.id.editPrice)?.text.toString().toIntOrNull() ?: 0
                wine.drinkBy = dialogView.findViewById<TextInputEditText>(R.id.editDrinkBy)?.text.toString().toIntOrNull() ?: 0
                wine.description = dialogView.findViewById<TextInputEditText>(R.id.editDescription)?.text.toString()

                capturedImage?.let {
                    //todo
                }

                dialog.dismiss()
            }

            dialog.show()
        }
    }

    //adapter
    class AllWinesAdapter(
        private val wines: List<FridgesFragment.Wine>,
        private val onWineClick: (FridgesFragment.Wine) -> Unit,
    ) : RecyclerView.Adapter<AllWinesAdapter.AllWinesViewHolder>() {

        override fun getItemCount(): Int = wines.size

        class AllWinesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val wineImageView: ImageView = view.findViewById(R.id.wineImageView)
            val wineNameTextView: TextView = view.findViewById(R.id.wineNameTextView)
            val wineDescTextView: TextView = view.findViewById(R.id.wineDescTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllWinesViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.all_wines_card, parent, false)
            return AllWinesViewHolder(view)
        }

        override fun onBindViewHolder(holder: AllWinesViewHolder, i: Int) {
            val tempWine = wines[i]
            if (tempWine.imagePath != "null") { holder.wineImageView.setImageBitmap(loadImage(wines[i].imagePath)) }
            holder.wineNameTextView.text = tempWine.name
            holder.wineDescTextView.text = "${tempWine.year}, ${tempWine.vineyard}"
            holder.itemView.setOnClickListener { onWineClick(tempWine) }
        }

        private fun loadImage(path: String): Bitmap? {
            try { return BitmapFactory.decodeFile(path) } catch (e: Exception) { e.printStackTrace() }
            return null
        }
    }
}
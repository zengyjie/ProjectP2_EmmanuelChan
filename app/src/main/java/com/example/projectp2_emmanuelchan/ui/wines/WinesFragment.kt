package com.example.projectp2_emmanuelchan.ui.wines

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedWine
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.fridges
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.getFridge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allWinesAdapter: AllWinesAdapter
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImage: Bitmap? = null
    private val allWines = mutableListOf<FridgesFragment.Wine>()
    private var filter = Filter()
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWinesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    val fileName = "wine_${System.currentTimeMillis()}.jpg"
                    val savedImagePath = saveImage(requireContext(), imageBitmap, fileName)

                    if (savedImagePath != null) {
                        selectedWine.imagePath = savedImagePath
                        binding.allWinesRecyclerView.adapter?.notifyDataSetChanged()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        }

        for (fridge in fridges) {
            for (l in fridge.wines.indices) {
                for (s in fridge.wines[l].indices) {
                    for (r in fridge.wines[l][s].indices) {
                        for (c in fridge.wines[l][s][r].indices) {
                            val wine = fridge.wines[l][s][r][c]
                            if (wine.name != "null") { allWines.add(wine)}
                        } } } }
        }

        val allWinesFiltered = allWines
        val allWinesRecyclerView = binding.allWinesRecyclerView
        allWinesRecyclerView.layoutManager = GridLayoutManager(context, 1)
        allWinesAdapter = AllWinesAdapter(allWinesFiltered) { wine -> viewWine(wine) }
        allWinesRecyclerView.adapter = allWinesAdapter

        binding.filterButton.setOnClickListener { showFilterDialog() }
        binding.searchBar.text = null
        binding.searchBar.addTextChangedListener{ text ->
            filter.name = binding.searchBar.text.toString()
            filterWines(filter)
        }

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
        selectedWine = wine
        dialogView.findViewById<TextView>(R.id.wineInfoNameTextView)?.text = wine.name
        dialogView.findViewById<TextView>(R.id.wineInfoDescTextView)?.text =
            "${wine.year}\n${wine.vineyard}, ${wine.region}\nVariety: ${wine.grapeVariety}\nRating: " +
                    "${wine.rating}\nBought at: $${wine.price}\nDrink by: ${wine.drinkBy}\nNotes:\n${wine.description}"

        dialogView.findViewById<Button>(R.id.editWineButton).setOnClickListener {
            editWine(requireContext(), wine, cameraLauncher, capturedImage, permissionLauncher)
        }

        val moveButton = dialogView.findViewById<Button>(R.id.moveWineButton)
        moveButton.text = "locate"
        moveButton.setOnClickListener {
            val parentFridge = wine.parentFridge
            FridgesFragment.highlightedWine = wine
            FridgesFragment.openFridge(getFridge(parentFridge), requireContext(), findWine(getFridge(parentFridge), wine)?.get(0) ?: 3)
        }
        dialog.show()
    }

    companion object {
        fun findWine(fridge: FridgesFragment.Fridge, targetWine: FridgesFragment.Wine) : List<Int>? {
            for (l in fridge.wines.indices) {
                for (s in fridge.wines[l].indices) {
                    for (r in fridge.wines[l][s].indices) {
                        for (c in fridge.wines[l][s][r].indices) {
                            if (fridge.wines[l][s][r][c] == targetWine) {
                                return listOf(l, s, r, c) } } } } }

            return mutableListOf(0,0,0,0)
        }

        fun saveImage(context: Context, bitmap: Bitmap, fileName: String): String? {
            val directory = File(context.filesDir, "WineWise")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val imageFile = File(directory, fileName)
            return try {
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                imageFile.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        private fun loadImage(imagePath: String, imageView: ImageView) {
            if (imagePath.isNotEmpty()) {
                val imgFile = File(imagePath)
                if (imgFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    imageView.setImageBitmap(bitmap)
                }
            }
        }

        fun editWine(
            context: Context,
            wine: FridgesFragment.Wine,
            cameraLauncher: ActivityResultLauncher<Intent>,
            capturedImage: Bitmap?,
            permissionLauncher: ActivityResultLauncher<String>
        ) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.wine_edit, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            dialogView.findViewById<MaterialTextView>(R.id.editTitleTextView)?.text = "Edit Wine"
            val wineImageView = dialogView.findViewById<ImageView>(R.id.wineImage)

            // Load existing image if available
            if (wine.imagePath.isNotEmpty()) {
                loadImage(wine.imagePath, wineImageView)
            }

            val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.editWineType)
            val wineTypes = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified")
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, wineTypes)
            wineTypeSpinner.adapter = spinnerAdapter
            wineTypeSpinner.setSelection(wineTypes.indexOf(wine.type))

            dialogView.findViewById<ImageButton>(R.id.takeLabelButton).setOnClickListener {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                }
            }

            capturedImage?.let {
                wineImageView.setImageBitmap(it)
            }

            val nameInput = dialogView.findViewById<TextInputEditText>(R.id.editWineName)
            val yearInput = dialogView.findViewById<TextInputEditText>(R.id.editWineYear)
            val vineyardInput = dialogView.findViewById<TextInputEditText>(R.id.editVineyard)
            val regionInput = dialogView.findViewById<TextInputEditText>(R.id.editRegion)
            val varietyInput = dialogView.findViewById<TextInputEditText>(R.id.editVariety)
            val ratingInput = dialogView.findViewById<TextInputEditText>(R.id.editRating)
            val priceInput = dialogView.findViewById<TextInputEditText>(R.id.editPrice)
            val drinkByInput = dialogView.findViewById<TextInputEditText>(R.id.editDrinkBy)
            val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.editDescription)

            nameInput.setText(wine.name)
            yearInput.setText(wine.year.toString())
            vineyardInput.setText(wine.vineyard)
            regionInput.setText(wine.region)
            varietyInput.setText(wine.grapeVariety)
            ratingInput.setText(wine.rating.toString())
            priceInput.setText(wine.price.toString())
            drinkByInput.setText(wine.drinkBy.toString())
            descriptionInput.setText(wine.description)

            val saveBtn = dialogView.findViewById<Button>(R.id.saveWineButton)
            saveBtn.text = "Save"
            saveBtn.setOnClickListener {
                var isValid = true

                fun validateField(input: TextInputEditText, errorMessage: String): Boolean {
                    if (input.text.isNullOrBlank()) {
                        input.error = errorMessage
                        return false
                    }
                    return true
                }

                fun validateNumberField(input: TextInputEditText, errorMessage: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int? {
                    val value = input.text.toString().toIntOrNull()
                    return if (value == null || value < min || value > max) {
                        input.error = errorMessage
                        null
                    } else value
                }

                fun validateDoubleField(input: TextInputEditText, errorMessage: String, min: Double = 0.0, max: Double = 100.0): Double? {
                    val value = input.text.toString().toDoubleOrNull()
                    return if (value == null || value < min || value > max) {
                        input.error = errorMessage
                        null
                    } else value
                }

                isValid = validateField(nameInput, "Name is required") && isValid
                isValid = validateField(vineyardInput, "Vineyard is required") && isValid
                isValid = validateField(regionInput, "Region is required") && isValid
                isValid = validateField(varietyInput, "Variety is required") && isValid

                val year = validateNumberField(yearInput, "Invalid year", 1000, 2100) ?: run { isValid = false; 0 }
                val rating = validateDoubleField(ratingInput, "Rating must be between 0 and 100") ?: run { isValid = false; 0.0 }
                val price = validateNumberField(priceInput, "Invalid price", 0) ?: run { isValid = false; 0 }
                val drinkBy = validateNumberField(drinkByInput, "Invalid drink-by year", 1000, 2100) ?: run { isValid = false; 0 }

                if (!isValid) return@setOnClickListener

                wine.name = nameInput.text.toString()
                wine.type = wineTypeSpinner.selectedItem.toString()
                wine.year = year
                wine.vineyard = vineyardInput.text.toString()
                wine.region = regionInput.text.toString()
                wine.grapeVariety = varietyInput.text.toString()
                wine.rating = rating
                wine.price = price
                wine.drinkBy = drinkBy
                wine.description = descriptionInput.text.toString()

                capturedImage?.let {
                    val fileName = "wine_${System.currentTimeMillis()}.jpg"
                    val savedImagePath = saveImage(context, it, fileName)
                    if (savedImagePath != null) {
                        wine.imagePath = savedImagePath
                    }
                }

                dialog.dismiss()
            }

            dialog.show()
        }

    }

    //filtering
    data class Filter(
        val year: Int? = null,
        val maxYear: Int? = null,
        val minPrice: Int? = null,
        val maxPrice: Int? = null,
        val type: String? = null,
        val vineyard: String? = null,
        val minRating: Double? = null,
        val maxRating: Double? = null,
        var name: String? = null
    )

    private fun filterWines(filter: Filter) {
        val filteredList = allWines.filter { wine ->
            (filter.year == null || wine.year == filter.year) &&
                    (filter.minPrice == null || wine.price >= filter.minPrice) &&
                    (filter.maxPrice == null || wine.price <= filter.maxPrice) &&
                    (filter.type.isNullOrEmpty() || wine.type == filter.type) &&
                    (filter.vineyard.isNullOrEmpty() || wine.vineyard.contains(filter.vineyard, ignoreCase = true)) &&
                    (filter.minRating == null || wine.rating >= filter.minRating) &&
                    (filter.maxRating == null || wine.rating <= filter.maxRating) &&
                    (filter.name == null || wine.name.contains(filter.name!!))
        }

        allWinesAdapter.updateList(filteredList)
    }

    private fun showFilterDialog() {

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_dialog, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val dialog = dialogBuilder.create()

        val minPriceButton = dialogView.findViewById<Button>(R.id.minPriceButton)
        val maxPriceButton = dialogView.findViewById<Button>(R.id.maxPriceButton)
        val priceCheckBox = dialogView.findViewById<CheckBox>(R.id.priceCheckBox)

        val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.wineTypeSpinner)
        val wineTypes = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, wineTypes)
        wineTypeSpinner.adapter = spinnerAdapter
        val typeCheckBox = dialogView.findViewById<CheckBox>(R.id.typeCheckBox)

        val yearPickerButton = dialogView.findViewById<ImageButton>(R.id.yearPickerButton)
        val yearTextView = dialogView.findViewById<TextView>(R.id.yearTextView)
        val yearCheckBox = dialogView.findViewById<CheckBox>(R.id.yearCheckBox)

        val vineyardEditText = dialogView.findViewById<EditText>(R.id.vineyardEditText)
        val vineyardCheckBox = dialogView.findViewById<CheckBox>(R.id.vineyardCheckBox)

        val minRatingButton = dialogView.findViewById<Button>(R.id.minRatingButton)
        val maxRatingButton = dialogView.findViewById<Button>(R.id.maxRatingButton)
        val ratingCheckBox = dialogView.findViewById<CheckBox>(R.id.ratingCheckBox)

        minPriceButton.text = filter.minPrice?.toString() ?: "Min"
        maxPriceButton.text = filter.maxPrice?.toString() ?: "Max"
        priceCheckBox.isChecked = filter.minPrice != null || filter.maxPrice != null

        yearTextView.text = filter.year?.toString() ?: "Select Year"
        yearCheckBox.isChecked = filter.year != null

        vineyardEditText.setText(filter.vineyard ?: "")
        vineyardCheckBox.isChecked = filter.vineyard != null

        minRatingButton.text = filter.minRating?.toString() ?: "Min"
        maxRatingButton.text = filter.maxRating?.toString() ?: "Max"
        ratingCheckBox.isChecked = filter.minRating != null || filter.maxRating != null

        fun showNumberPicker(context: Context, min: Int, max: Int, current: Int?, onNumberSelected: (Int) -> Unit) {
            val numberPicker = NumberPicker(context).apply {
                minValue = min
                maxValue = max
                value = current?.coerceIn(min, max) ?: min
                wrapSelectorWheel = false
                setBackgroundColor(context.getThemeColor(android.R.attr.colorBackground))
            }

            AlertDialog.Builder(context)
                .setView(numberPicker)
                .setPositiveButton("OK") { _, _ -> onNumberSelected(numberPicker.value) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        fun showYearPicker(context: Context, onYearSelected: (Int) -> Unit) {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val numberPicker = NumberPicker(context).apply {
                minValue = 1900
                maxValue = currentYear
                value = currentYear
                wrapSelectorWheel = false
            }

            AlertDialog.Builder(context)
                .setTitle("Select Year")
                .setView(numberPicker)
                .setPositiveButton("OK") { _, _ -> onYearSelected(numberPicker.value) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        minPriceButton.setOnClickListener {
            val currentMin = minPriceButton.text.toString().toIntOrNull()
            val currentMax = maxPriceButton.text.toString().toIntOrNull() ?: 1000000
            showNumberPicker(requireContext(), 0, currentMax, currentMin) { selectedValue ->
                minPriceButton.text = selectedValue.toString()
            }
        }

        maxPriceButton.setOnClickListener {
            val currentMax = maxPriceButton.text.toString().toIntOrNull()
            val currentMin = minPriceButton.text.toString().toIntOrNull() ?: 0
            showNumberPicker(requireContext(), currentMin, 1000000, currentMax) { selectedValue ->
                maxPriceButton.text = selectedValue.toString()
            }
        }

        minRatingButton.setOnClickListener {
            val currentMin = minRatingButton.text.toString().toIntOrNull()
            val currentMax = maxRatingButton.text.toString().toIntOrNull() ?: 100
            showNumberPicker(requireContext(), 0, currentMax, currentMin) { selectedValue ->
                minRatingButton.text = selectedValue.toString()
            }
        }

        maxRatingButton.setOnClickListener {
            val currentMax = maxRatingButton.text.toString().toIntOrNull()
            val currentMin = minRatingButton.text.toString().toIntOrNull() ?: 0
            showNumberPicker(requireContext(), currentMin, 100, currentMax) { selectedValue ->
                maxRatingButton.text = selectedValue.toString()
            }
        }

        yearPickerButton.setOnClickListener {
            showYearPicker(requireContext()) { selectedYear ->
                yearTextView.text = selectedYear.toString()
            }
        }

        dialogView.findViewById<Button>(R.id.applyFilterButton).setOnClickListener {
            val minPrice = minPriceButton.text.toString().toIntOrNull()
            val maxPrice = maxPriceButton.text.toString().toIntOrNull()
            val priceChecked = priceCheckBox.isChecked

            val type = wineTypeSpinner.selectedItem.toString()
            val typeChecked = typeCheckBox.isChecked

            val yearText = yearTextView.text.toString().toIntOrNull()
            val yearChecked = yearCheckBox.isChecked

            val vineyard = vineyardEditText.text.toString().takeIf { it.isNotEmpty() }
            val vineyardChecked = vineyardCheckBox.isChecked

            val minRating = minRatingButton.text.toString().toDoubleOrNull()
            val maxRating = maxRatingButton.text.toString().toDoubleOrNull()
            val ratingChecked = ratingCheckBox.isChecked

            filter = Filter(
                year = if (yearChecked) yearText else null,
                minPrice = if (priceChecked) minPrice else null,
                maxPrice = if (priceChecked) maxPrice else null,
                type = if (typeChecked) type else null,
                vineyard = if (vineyardChecked) vineyard else null,
                minRating = if (ratingChecked) minRating else null,
                maxRating = if (ratingChecked) maxRating else null
            )

            filterWines(filter)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.clearFiltersButton).setOnClickListener{
            filter = Filter()
            filterWines(filter)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun Context.getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    //adapter
    class AllWinesAdapter(
        private var wines: List<FridgesFragment.Wine>,
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
            if (tempWine.imagePath != "null") {
                loadImage(tempWine.imagePath, holder.wineImageView)
            }
            holder.wineNameTextView.text = tempWine.name
            holder.wineDescTextView.text = "${tempWine.year}, ${tempWine.vineyard}"
            holder.itemView.setOnClickListener { onWineClick(tempWine) }
        }

        fun updateList(newWines: List<FridgesFragment.Wine>) {
            wines = newWines
            notifyDataSetChanged()
        }
    }
}
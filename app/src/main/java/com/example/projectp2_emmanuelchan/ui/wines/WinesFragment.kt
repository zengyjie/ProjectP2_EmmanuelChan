package com.example.projectp2_emmanuelchan.ui.wines

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.fridges
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.getFridge
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allWinesAdapter: AllWinesAdapter
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImage: Bitmap? = null
    private val allWines = mutableListOf<FridgesFragment.Wine>()
    private var filter = Filter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWinesBinding.inflate(inflater, container, false)
        val root: View = binding.root

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

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                capturedImage = imageBitmap
                Toast.makeText(context, "Image captured successfully", Toast.LENGTH_SHORT).show()
            } else { Toast.makeText(context, "Image capture failed", Toast.LENGTH_SHORT).show() }
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
        dialogView.findViewById<TextView>(R.id.wineInfoNameTextView)?.text = wine.name
        dialogView.findViewById<TextView>(R.id.wineInfoDescTextView)?.text =
            "${wine.year}\n${wine.vineyard}, ${wine.region}\nVariety: ${wine.grapeVariety}\nRating: " +
                    "${wine.rating}\nBought at: $${wine.price}\nDrink by: ${wine.drinkBy}\nNotes:\n${wine.description}"

        dialogView.findViewById<Button>(R.id.editWineButton).setOnClickListener {
            editWine(requireContext(), wine, cameraLauncher, capturedImage)
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
            wineTypeSpinner.setSelection(wineTypes.indexOf(wine.type))

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

            val deleteBtn = dialogView.findViewById<Button>(R.id.deleteWineButton)
            deleteBtn.visibility = View.VISIBLE
            deleteBtn.setOnClickListener {
                val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
                val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(context)
                    .setView(confirmDeleteView)
                val deleteDialog = confirmDialogBuilder.create()

                val wineName = wine.name
                confirmDeleteView.findViewById<TextView>(R.id.nameTextView).text = "Delete ${wineName}?"

                confirmDeleteView.findViewById<Button>(R.id.yesButton).setOnClickListener {
                    wine.name = "null"
                    deleteDialog.dismiss()
                    dialog.dismiss()
                    Toast.makeText(context, "${wineName} deleted", Toast.LENGTH_SHORT).show()
                }

                confirmDeleteView.findViewById<Button>(R.id.noButton).setOnClickListener {
                    deleteDialog.dismiss()
                }

                deleteDialog.show()
            }

            val saveBtn = dialogView.findViewById<Button>(R.id.saveWineButton)
            saveBtn.text = "Save"
            saveBtn.setOnClickListener {
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
            if (tempWine.imagePath != "null") { holder.wineImageView.setImageBitmap(loadImage(wines[i].imagePath)) }
            holder.wineNameTextView.text = tempWine.name
            holder.wineDescTextView.text = "${tempWine.year}, ${tempWine.vineyard}"
            holder.itemView.setOnClickListener { onWineClick(tempWine) }
        }

        fun updateList(newWines: List<FridgesFragment.Wine>) {
            wines = newWines
            notifyDataSetChanged()
        }

        private fun loadImage(path: String): Bitmap? {
            try { return BitmapFactory.decodeFile(path) } catch (e: Exception) { e.printStackTrace() }
            return null
        }
    }
}
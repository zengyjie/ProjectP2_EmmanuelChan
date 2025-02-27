package com.example.projectp2_emmanuelchan.ui.wines

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.editWine
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.saveImage
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.selectedImageView
import com.example.projectp2_emmanuelchan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuelchan.MainActivity.Companion.getFridge
import com.example.projectp2_emmanuelchan.MainActivity.Companion.highlightedWineName
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedFridge
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedWine
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.fridgeToOpen
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.itemLayer
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.saveFridges
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allWinesAdapter: AllWinesAdapter
    private val allWines = mutableListOf<FridgesFragment.Wine>()
    private var filter = Filter()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImage: Bitmap? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(requireContext() , "Camera permission denied", Toast.LENGTH_SHORT).show()
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
        allWinesAdapter = AllWinesAdapter(allWinesFiltered) { wine ->
            val fridge = fridges[getFridge(wine.parentFridge)]
            val indices = findWine(fridge, wine)
            viewWine(fridge.wines[indices?.get(0)!!][indices[1]][indices[2]][indices[3]])
        }
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
                if (imageBitmap != null) {
                    val fileName = "wine_${System.currentTimeMillis()}.jpg"
                    val savedImagePath = saveImage(requireContext(), imageBitmap, fileName)

                    if (savedImagePath != null) {
                        selectedWine.imagePath = savedImagePath
                        selectedImageView?.setImageBitmap(imageBitmap)
                    } else {
                        Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Image capture failed", Toast.LENGTH_SHORT).show()
            }
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

        val imageView = dialogView.findViewById<ImageView>(R.id.imageView)
        if (wine.imagePath.isNotEmpty()) {
            loadImage(wine.imagePath, imageView)
        }

        dialogView.findViewById<TextView>(R.id.wineInfoNameTextView)?.text = wine.name
        dialogView.findViewById<TextView>(R.id.wineInfoDescTextView)?.text =
            "${wine.year}\n${wine.vineyard}, ${wine.region}\nVariety: ${wine.grapeVariety}\nRating: " +
                    "${wine.rating}\nBought at: $${wine.price}\nDrink by: ${wine.drinkBy}\nNotes:\n${wine.description}"

        //deepcopyTODO
        dialogView.findViewById<ImageButton>(R.id.showPairingsButton).setOnClickListener {
            val dialogView1 = LayoutInflater.from(requireContext()).inflate(R.layout.wine_pairings, null)
            val dialogBuilder1 = AlertDialog.Builder(requireContext()).setView(dialogView1)
            val dialog1 = dialogBuilder1.create()

            dialogView1.findViewById<TextView>(R.id.winePairingsNameTextView).text = "Pairings for ${wine.name}"

            val pairingsEditText = dialogView1.findViewById<AppCompatEditText>(R.id.winePairingsEditText)
            val revertPairingsButton = dialogView1.findViewById<Button>(R.id.revertPairingsButton)
            val originalPairings = getPairingSuggestion(requireContext(), wine.grapeVariety)

            pairingsEditText.setText(wine.pairings)

            pairingsEditText.setOnClickListener {
                pairingsEditText.isFocusable = true
                pairingsEditText.isFocusableInTouchMode = true
            }

            pairingsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    wine.pairings = pairingsEditText.text.toString()
                    val indices = findWine(selectedFridge, wine)
                    if (indices != null) {
                        selectedFridge.wines[indices[0]][indices[1]][indices[2]][indices[3]].pairings = wine.pairings
                    }
                }
            }

            dialog1.setOnDismissListener {
                wine.pairings = pairingsEditText.text.toString()
                val fridgeIndex = getFridge(wine.parentFridge)

                if (fridgeIndex != -1) {
                    val fridge = fridges[fridgeIndex]
                    val indices = findWine(fridge, wine)

                    if (indices != null) {
                        fridge.wines[indices[0]][indices[1]][indices[2]][indices[3]].pairings = wine.pairings
                    }
                }
            }


            revertPairingsButton.setOnClickListener {
                pairingsEditText.setText(originalPairings)
                wine.pairings = originalPairings
            }

            dialog1.show()
        }

        dialogView.findViewById<Button>(R.id.editWineButton)?.setOnClickListener {
            dialog.dismiss()
            editWine(requireContext(), wine, cameraLauncher, capturedImage, permissionLauncher, allWinesAdapter)
        }

        dialogView.findViewById<Button>(R.id.duplicateWineButton).visibility = View.GONE

        val moveButton = dialogView.findViewById<Button>(R.id.moveWineButton)
        moveButton.text = "locate"
        moveButton.setOnClickListener {
            dialog.dismiss()
            fridgeToOpen = fridges[getFridge(wine.parentFridge)]
            highlightedWineName = wine.name
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            navView.selectedItemId = R.id.navigation_fridges
            itemLayer = findWine(fridgeToOpen!!, wine)?.get(0) ?: 3
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

        fun loadImage(imagePath: String, imageView: ImageView) {
            if (imagePath.isNotEmpty()) {
                val imgFile = File(imagePath)
                if (imgFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    imageView.setImageBitmap(bitmap)
                }
            }
        }

        fun getPairingSuggestion(context: Context, variety: String): String {
            return try {
                val jsonString = context.assets.open("pairings.json").bufferedReader().use { it.readText() }

                val jsonMap: Map<String, String> = Gson().fromJson(jsonString, object : TypeToken<Map<String, String>>() {}.type)

                jsonMap[variety] ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
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

    override fun onResume() {
        super.onResume()
        allWinesAdapter.notifyDataSetChanged()
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

    //save/load
    override fun onPause() {
        super.onPause()
        saveFridges(requireContext())
    }

    override fun onStop() {
        super.onStop()
        saveFridges(requireContext())
    }
}
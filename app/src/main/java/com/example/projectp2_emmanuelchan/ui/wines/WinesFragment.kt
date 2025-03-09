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
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
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
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.getIndicesFromPosition
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.loadDB
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.saveImageBm
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.saveImageUri
import com.example.projectp2_emmanuelchan.FridgeActivity.Companion.selectedImageView
import com.example.projectp2_emmanuelchan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuelchan.MainActivity.Companion.getFridge
import com.example.projectp2_emmanuelchan.MainActivity.Companion.highlightedWineName
import com.example.projectp2_emmanuelchan.MainActivity.Companion.moveMode
import com.example.projectp2_emmanuelchan.MainActivity.Companion.moving
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedFridge
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedIndices
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedWine
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.fridgeToOpen
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.itemLayer
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.resizeWinesArray
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.saveFridges
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allWinesAdapter: AllWinesAdapter
    private val allMyWines = mutableListOf<FridgesFragment.Wine>()
    private var currentWineSet = allMyWines
    private var currentWineSetName = "my"
    private var filter = Filter()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
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
                            if (wine.name != "null") { allMyWines.add(wine)}
                        } } } } }

        val databaseWines = loadDB(requireContext()).toMutableList()
        val allWinesFiltered = allMyWines
        val allWinesRecyclerView = binding.allWinesRecyclerView
        allWinesRecyclerView.layoutManager = GridLayoutManager(context, 1)
        allWinesAdapter = AllWinesAdapter(allWinesFiltered) { wine ->
            if (wine.parentFridge == "database") { viewWine(wine) }
            else {
                val fridge = fridges[getFridge(wine.parentFridge)]
                val indices = findWine(fridge, wine)
                viewWine(fridge.wines[indices[0]][indices[1]][indices[2]][indices[3]])
            }
        }
        allWinesRecyclerView.adapter = allWinesAdapter
        filterWines(Filter(), allMyWines)

        binding.filterButton.setOnClickListener { showFilterDialog() }
        binding.searchBar.text = null
        binding.searchBar.addTextChangedListener{
            filter.name = binding.searchBar.text.toString()
            filterWines(filter, currentWineSet)
        }

        binding.myWinesToggleButton.isChecked = true

        var myWinesListener = CompoundButton.OnCheckedChangeListener { _, _ -> (return@OnCheckedChangeListener)}
        val allWinesListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.allWinesToggleButton.isClickable = false
                binding.myWinesToggleButton.isClickable = true
                binding.myWinesToggleButton.setOnCheckedChangeListener(null)
                binding.myWinesToggleButton.isChecked = false
                binding.myWinesToggleButton.setOnCheckedChangeListener(myWinesListener)
                allWinesAdapter.updateList(databaseWines)
                currentWineSet = databaseWines
                currentWineSetName = "all"
                filterWines(filter, databaseWines)
                binding.allWinesRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
        myWinesListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.myWinesToggleButton.isClickable = false
                binding.allWinesToggleButton.isClickable = true
                binding.allWinesToggleButton.setOnCheckedChangeListener(null)
                binding.allWinesToggleButton.isChecked = false
                binding.allWinesToggleButton.setOnCheckedChangeListener(allWinesListener)
                allWinesAdapter.updateList(allMyWines)
                currentWineSet = allMyWines
                currentWineSetName = "my"
                filterWines(filter, allMyWines)
                binding.allWinesRecyclerView.adapter?.notifyDataSetChanged()
            }
        }

        binding.myWinesToggleButton.setOnCheckedChangeListener(myWinesListener)
        binding.allWinesToggleButton.setOnCheckedChangeListener(allWinesListener)

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    val fileName = "wine_${System.currentTimeMillis()}.jpg"
                    val savedImagePath = saveImageBm(requireContext(), imageBitmap, fileName)

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

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    val fileName = "wine_${System.currentTimeMillis()}.jpg"
                    val savedImagePath = saveImageUri(requireContext(), imageUri, fileName)

                    if (savedImagePath != null) {
                        selectedWine.imagePath = savedImagePath
                        selectedImageView?.setImageURI(imageUri)
                    } else {
                        Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        allWinesRecyclerView.adapter?.notifyDataSetChanged()
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

        dialogView.findViewById<TextView>(R.id.wineInfoNameTextView)?.text = "${wine.name}\n(${wine.year})"
        dialogView.findViewById<TextView>(R.id.wineInfoDescTextView)?.text =
            "${wine.vineyard}\n${wine.type} Wine from ${wine.region}, ${wine.country}\nVariety: ${wine.grapeVariety}\nRating: " +
            "${wine.rating} / 100\nNotes:\n${wine.description}\nDrink by: ${wine.drinkBy}\nBought at: $${wine.price}"

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
                    selectedFridge.wines[indices[0]][indices[1]][indices[2]][indices[3]].pairings = wine.pairings
                }
            }

            dialog1.setOnDismissListener {
                wine.pairings = pairingsEditText.text.toString()
                val fridgeIndex = getFridge(wine.parentFridge)

                if (fridgeIndex != -1) {
                    val fridge = fridges[fridgeIndex]
                    val indices = findWine(fridge, wine)
                    fridge.wines[indices[0]][indices[1]][indices[2]][indices[3]].pairings = wine.pairings
                }
            }

            if (wine.parentFridge == "database") {
                revertPairingsButton.visibility = View.GONE
                pairingsEditText.isFocusable = false
                pairingsEditText.isClickable = false
                pairingsEditText.isLongClickable = false
                pairingsEditText.setText(originalPairings)
                wine.pairings = originalPairings
            }


            revertPairingsButton.setOnClickListener {
                pairingsEditText.setText(originalPairings)
                wine.pairings = originalPairings
            }

            dialog1.show()
        }

        val markDrunkButton = dialogView.findViewById<Button>(R.id.markDrunkButton)
        if (wine.parentFridge == "drunk") { markDrunkButton.text = "put back" }
        else { markDrunkButton.visibility = View.GONE }
        markDrunkButton.setOnClickListener {
            if (wine.parentFridge == "drunk") {
                moving = true
                selectedWine = wine
                val dialogView1 =
                    LayoutInflater.from(requireContext()).inflate(R.layout.select_fridge, null)
                val dialogBuilder1 = AlertDialog.Builder(requireContext()).setView(dialogView1)
                val dialog1 = dialogBuilder1.create()

                val fridgeSpinner = dialogView1.findViewById<Spinner>(R.id.selectFridgeSpinner)
                val openChangedFridgeButton =
                    dialogView1.findViewById<Button>(R.id.openChangedFridgeButton)

                val availableFridges = fridges.filter { it.name != selectedFridge.name }

                val fridgeNames = availableFridges.map { it.name }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    fridgeNames.filter { it != "drunk" }
                )
                fridgeSpinner.adapter = adapter

                openChangedFridgeButton.setOnClickListener {
                    val selectedFridgeName = fridgeSpinner.selectedItem?.toString()
                    val selectedFridge = fridges[getFridge(selectedFridgeName ?: "")]
                    selectedIndices = findWine(fridges[getFridge("drunk")], wine).toMutableList()
                    if (selectedFridge.name != "null") {
                        moveMode = "putBack"
                        FridgesFragment.openFridge(selectedFridge, requireContext())
                        dialog1.dismiss()
                    } else {
                        Toast.makeText(context, "Invalid selection", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog1.show()

                dialog.dismiss()
            }
        }

        dialogView.findViewById<Button>(R.id.editWineButton)?.visibility = View.GONE
        dialogView.findViewById<Button>(R.id.duplicateWineButton).visibility = View.GONE

        val moveButton = dialogView.findViewById<Button>(R.id.moveWineButton)
        if (wine.parentFridge == "drunk" || wine.parentFridge == "database") { moveButton.visibility = View.GONE }
        moveButton.text = "locate"
        moveButton.setOnClickListener {
            dialog.dismiss()
            fridgeToOpen = fridges[getFridge(wine.parentFridge)]
            highlightedWineName = wine.name
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            navView.selectedItemId = R.id.navigation_fridges
            itemLayer = findWine(fridgeToOpen!!, wine)[0]
        }
        dialog.show()
    }

    companion object {
        fun findWine(fridge: FridgesFragment.Fridge, targetWine: FridgesFragment.Wine) : List<Int> {
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

        fun getPairingSuggestion(context: Context, grapeVariety: String): String {
            return try {
                val inputStream = context.assets.open("pairings.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val pairingsMap: Map<String, String> = Gson().fromJson(jsonString, object : TypeToken<Map<String, String>>() {}.type)
                val varieties = grapeVariety.split(",").map { it.trim() }

                val matchedPairings = varieties.mapNotNull { pairingsMap[it] }
                if (matchedPairings.isNotEmpty()) { matchedPairings.joinToString("\n") } else { "" }
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
        val region: String? = null,
        val country: String? = null,
        val minRating: Double? = null,
        val maxRating: Double? = null,
        var name: String? = null,
        val drunk: Boolean = false,
    )

    private fun filterWines(filter: Filter, wines: MutableList<FridgesFragment.Wine>) {
        var filteredList = wines.filter { wine ->
            (filter.year == null || wine.year == filter.year) &&
                    (filter.minPrice == null || wine.price >= filter.minPrice) &&
                    (filter.maxPrice == null || wine.price <= filter.maxPrice) &&
                    (filter.type.isNullOrEmpty() || wine.type.equals(filter.type, ignoreCase = true)) &&
                    (filter.vineyard.isNullOrEmpty() || wine.vineyard.contains(filter.vineyard, ignoreCase = true)) &&
                    (filter.region.isNullOrEmpty() || wine.region.contains(filter.region, ignoreCase = true)) &&
                    (filter.country.isNullOrEmpty() || wine.parentFridge.contains(filter.country, ignoreCase = true)) &&
                    (filter.minRating == null || wine.rating >= filter.minRating) &&
                    (filter.maxRating == null || wine.rating <= filter.maxRating) &&
                    (filter.name.isNullOrEmpty() || wine.name.contains(filter.name!!, ignoreCase = true))
        }
        filteredList = if (filter.drunk) { filteredList.filter { wine -> wine.parentFridge == "drunk"} }
        else { filteredList.filter { wine -> wine.parentFridge != "drunk"} }

        allWinesAdapter.updateList(filteredList)
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_dialog, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val dialog = dialogBuilder.create()

        val allVineyards = currentWineSet.map { it.vineyard }.filter { it.isNotEmpty() }.distinct().sorted()
        val allRegions = currentWineSet.map { it.region }.filter { it.isNotEmpty() }.distinct().sorted()
        val allCountries = currentWineSet.map { it.parentFridge }.filter { it.isNotEmpty() }.distinct().sorted()

        fun setupSpinner(spinner: Spinner, options: List<String>, selectedValue: String?) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
            spinner.adapter = adapter
            selectedValue?.let { spinner.setSelection(options.indexOf(it).takeIf { it >= 0 } ?: 0) }
        }

        val yearPickerButton = dialogView.findViewById<ImageButton>(R.id.yearPickerButton)
        val yearTextView = dialogView.findViewById<TextView>(R.id.yearTextView)
        val yearCheckBox = dialogView.findViewById<CheckBox>(R.id.yearCheckBox)

        val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.wineTypeSpinner)
        val wineTypes = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified")
        setupSpinner(wineTypeSpinner, wineTypes, filter.type)
        val typeCheckBox = dialogView.findViewById<CheckBox>(R.id.typeCheckBox)

        val vineyardSpinner = dialogView.findViewById<Spinner>(R.id.vineyardSpinner)
        val vineyardCheckBox = dialogView.findViewById<CheckBox>(R.id.vineyardCheckBox)
        setupSpinner(vineyardSpinner, allVineyards, filter.vineyard)

        val regionSpinner = dialogView.findViewById<Spinner>(R.id.regionSpinner)
        val regionCheckBox = dialogView.findViewById<CheckBox>(R.id.regionCheckBox)
        setupSpinner(regionSpinner, allRegions, filter.region)

        val countrySpinner = dialogView.findViewById<Spinner>(R.id.countrySpinner)
        val countryCheckBox = dialogView.findViewById<CheckBox>(R.id.countryCheckBox)
        setupSpinner(countrySpinner, allCountries, filter.country)

        val minRatingButton = dialogView.findViewById<Button>(R.id.minRatingButton)
        val maxRatingButton = dialogView.findViewById<Button>(R.id.maxRatingButton)
        val ratingCheckBox = dialogView.findViewById<CheckBox>(R.id.ratingCheckBox)

        val minPriceButton = dialogView.findViewById<Button>(R.id.minPriceButton)
        val maxPriceButton = dialogView.findViewById<Button>(R.id.maxPriceButton)
        val priceCheckBox = dialogView.findViewById<CheckBox>(R.id.priceCheckBox)

        val drunkCheckBox = dialogView.findViewById<CheckBox>(R.id.drunkCheckBox)

        minPriceButton.text = filter.minPrice?.toString() ?: "Min"
        maxPriceButton.text = filter.maxPrice?.toString() ?: "Max"
        priceCheckBox.isChecked = filter.minPrice != null || filter.maxPrice != null

        yearTextView.text = filter.year?.toString() ?: "Select Year"
        yearCheckBox.isChecked = filter.year != null

        vineyardCheckBox.isChecked = filter.vineyard != null
        regionCheckBox.isChecked = filter.region != null
        countryCheckBox.isChecked = filter.country != null

        minRatingButton.text = filter.minRating?.toString() ?: "Min"
        maxRatingButton.text = filter.maxRating?.toString() ?: "Max"
        ratingCheckBox.isChecked = filter.minRating != null || filter.maxRating != null

        fun showNumberPicker(context: Context, min: Int, max: Int, current: Int?, onNumberSelected: (Int) -> Unit) {
            val numberPicker = NumberPicker(context).apply {
                minValue = min
                maxValue = max
                value = current?.coerceIn(min, max) ?: min
                wrapSelectorWheel = false
            }

            AlertDialog.Builder(context)
                .setView(numberPicker)
                .setPositiveButton("OK") { _, _ -> onNumberSelected(numberPicker.value) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        fun showYearPicker(context: Context) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            showNumberPicker(context, 1900, currentYear, filter.year) { selectedYear ->
                yearTextView.text = selectedYear.toString()
            }
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
            showYearPicker(requireContext())
        }

        dialogView.findViewById<Button>(R.id.applyFilterButton).setOnClickListener {
            val minPrice = minPriceButton.text.toString().toIntOrNull()
            val maxPrice = maxPriceButton.text.toString().toIntOrNull()
            val priceChecked = priceCheckBox.isChecked

            val type = wineTypeSpinner.selectedItem.toString()
            val typeChecked = typeCheckBox.isChecked

            val yearText = yearTextView.text.toString().toIntOrNull()
            val yearChecked = yearCheckBox.isChecked

            val vineyard = vineyardSpinner.selectedItem?.toString()
            val vineyardChecked = vineyardCheckBox.isChecked

            val region = regionSpinner.selectedItem?.toString()
            val regionChecked = regionCheckBox.isChecked

            val country = countrySpinner.selectedItem?.toString()
            val countryChecked = countryCheckBox.isChecked

            val minRating = minRatingButton.text.toString().toDoubleOrNull()
            val maxRating = maxRatingButton.text.toString().toDoubleOrNull()
            val ratingChecked = ratingCheckBox.isChecked

            filter = Filter(
                year = if (yearChecked) yearText else null,
                minPrice = if (priceChecked) minPrice else null,
                maxPrice = if (priceChecked) maxPrice else null,
                type = if (typeChecked) type else null,
                vineyard = if (vineyardChecked) vineyard else null,
                region = if (regionChecked) region else null,
                country = if (countryChecked) country else null,
                minRating = if (ratingChecked) minRating else null,
                maxRating = if (ratingChecked) maxRating else null,
                drunk = drunkCheckBox.isChecked
            )

            filterWines(filter, currentWineSet)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.clearFiltersButton).setOnClickListener {
            filter = Filter()
            filterWines(filter, currentWineSet)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        binding.allWinesRecyclerView.adapter?.notifyDataSetChanged()
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
            holder.wineDescTextView.text = tempWine.year.toString()
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
package com.example.projectp2_emmanuel_chan.ui.wines

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
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
import com.example.projectp2_emmanuel_chan.FridgeActivity.Companion.loadDB
import com.example.projectp2_emmanuel_chan.FridgeActivity.Companion.saveImageBm
import com.example.projectp2_emmanuel_chan.FridgeActivity.Companion.saveImageUri
import com.example.projectp2_emmanuel_chan.FridgeActivity.Companion.selectedImageView
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.filter
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.getFridge
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.highlightedWineName
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.moveMode
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.moving
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.selectedFridge
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.selectedIndices
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.selectedWine
import com.example.projectp2_emmanuel_chan.R
import com.example.projectp2_emmanuel_chan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Companion.fridgeToOpen
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Companion.itemLayer
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Companion.loadFridges
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Companion.saveFridges
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.Normalizer

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allWinesAdapter: AllWinesAdapter
    private val allMyWines = mutableListOf<FridgesFragment.Wine>()
    private var currentWineSet = allMyWines
    private var currentWineSetName = "my"
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

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
        if (binding.myWinesToggleButton.isChecked) { filterWines(filter, allMyWines) }
        else { filterWines(filter, databaseWines) }

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
        val descText = SpannableStringBuilder()
        descText.append("${wine.vineyard}\n")
        descText.append("${wine.type} Wine from ${wine.region}, ${wine.country}\n")
        descText.append("Variety: ").apply {
            descText.setSpan(StyleSpan(Typeface.BOLD), descText.length - "Variety: ".length, descText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        descText.append("${wine.grapeVariety}\n")
        descText.append("Rating: ").apply {
            descText.setSpan(StyleSpan(Typeface.BOLD), descText.length - "Rating: ".length, descText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        descText.append("${wine.rating} / 100\n")
        descText.append("Notes:\n").apply {
            descText.setSpan(StyleSpan(Typeface.BOLD), descText.length - "Notes:\n".length, descText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        descText.append("${wine.description}\n")
        descText.append("Drink by: ").apply {
            descText.setSpan(StyleSpan(Typeface.BOLD), descText.length - "Drink by: ".length, descText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        descText.append("${wine.drinkBy}\n")
        descText.append("Bought at: ").apply {
            descText.setSpan(StyleSpan(Typeface.BOLD), descText.length - "Bought at: ".length, descText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        descText.append("$${wine.price}")
        dialogView.findViewById<TextView>(R.id.wineInfoDescTextView)?.text = descText

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
                        this.onPause()
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
        val sort: String = "A-Z"
    )

    private fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
    }

    private fun filterWines(filter: Filter, wines: MutableList<FridgesFragment.Wine>) {
        var filteredList = wines.filter { wine ->
            (filter.year == null || wine.year == filter.year) &&
                    (filter.minPrice == null || wine.price >= filter.minPrice) &&
                    (filter.maxPrice == null || wine.price <= filter.maxPrice) &&
                    (filter.type.isNullOrEmpty() || normalize(wine.type) == normalize(filter.type)) &&
                    (filter.vineyard.isNullOrEmpty() || normalize(wine.vineyard).contains(normalize(filter.vineyard))) &&
                    (filter.region.isNullOrEmpty() || normalize(wine.region).contains(normalize(filter.region))) &&
                    (filter.country.isNullOrEmpty() || normalize(wine.parentFridge).contains(normalize(filter.country))) &&
                    (filter.minRating == null || wine.rating >= filter.minRating) &&
                    (filter.maxRating == null || wine.rating <= filter.maxRating) &&
                    (filter.name.isNullOrEmpty() || normalize(wine.name).contains(normalize(filter.name!!)))
        }

        filteredList = if (filter.drunk) {
            filteredList.filter { wine -> wine.parentFridge == "drunk" }
        } else {
            filteredList.filter { wine -> wine.parentFridge != "drunk" }
        }

        when (filter.sort) {
            "A-Z" -> filteredList = filteredList.sortedBy { normalize(it.name) }
            "Z-A" -> filteredList = filteredList.sortedByDescending { normalize(it.name) }
            "Price: increasing" -> filteredList = filteredList.sortedBy { it.price }
            "Price: decreasing" -> filteredList = filteredList.sortedByDescending { it.price }
            "Year: increasing" -> filteredList = filteredList.sortedBy { it.year }
            "Year: decreasing" -> filteredList = filteredList.sortedByDescending { it.year }
        }

        allWinesAdapter.updateList(filteredList)
        if (filteredList.isEmpty()) {
            binding.noWinesTextView.visibility = View.VISIBLE
        } else {
            binding.noWinesTextView.visibility = View.GONE
        }
        binding.allWinesRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun showFilterDialog() {
        fun setupSpinner(spinner: Spinner, options: List<String>, selectedValue: String?) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
            spinner.adapter = adapter
            if (selectedValue != null && options.contains(selectedValue)) {
                spinner.setSelection(options.indexOf(selectedValue))
            }
        }

        fun showSeekBar(
            context: Context,
            min: Int,
            max: Int,
            current: Int?,
            onValueSelected: (Int) -> Unit
        ) {
            val seekBarDialogView = LayoutInflater.from(context).inflate(R.layout.seek_bar_dialog, null)
            val seekBar = seekBarDialogView.findViewById<SeekBar>(R.id.seekBar)
            val valueText = seekBarDialogView.findViewById<TextView>(R.id.valueTextView)
            val cancelButton = seekBarDialogView.findViewById<Button>(R.id.cancelButton)
            val okButton = seekBarDialogView.findViewById<Button>(R.id.okButton)

            seekBar.max = max - min
            seekBar.progress = (current ?: min) - min
            valueText.text = "${seekBar.progress + min}"

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    valueText.text = "${progress + min}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            val seekBarDialog = Dialog(context)
            seekBarDialog.setContentView(seekBarDialogView)
            seekBarDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            cancelButton.setOnClickListener {
                seekBarDialog.dismiss()
            }

            okButton.setOnClickListener {
                onValueSelected(seekBar.progress + min)
                seekBarDialog.dismiss()
            }

            seekBarDialog.show()
        }

        fun showNumberInputDialog(
            context: Context,
            min: Int,
            max: Int,
            current: Int?,
            onNumberSelected: (Int) -> Unit
        ) {
            val numberDialogView = LayoutInflater.from(context).inflate(R.layout.edit_text_dialog, null)
            val inputField = numberDialogView.findViewById<TextInputEditText>(R.id.numberInputField)
            val inputLayout = numberDialogView.findViewById<TextInputLayout>(R.id.numberInputLayout)
            val cancelButton = numberDialogView.findViewById<Button>(R.id.cancelButton)
            val okButton = numberDialogView.findViewById<Button>(R.id.okButton)

            inputField.setText(current?.toString() ?: "")

            val numberInputDialog = Dialog(context)
            numberInputDialog.setContentView(numberDialogView)
            numberInputDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            cancelButton.setOnClickListener {
                numberInputDialog.dismiss()
            }

            okButton.setOnClickListener {
                val value = inputField.text.toString().toIntOrNull()
                if (value != null && value in min..max) {
                    onNumberSelected(value)
                    numberInputDialog.dismiss()
                } else {
                    inputLayout.error = "Value must be between $min and $max"
                }
            }

            inputField.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val value = s?.toString()?.toIntOrNull()
                    if (value == null || value !in min..max) {
                        inputLayout.error = "Value must be between $min and $max"
                    } else {
                        inputLayout.error = null
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            numberInputDialog.show()
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_dialog, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val dialog = dialogBuilder.create()

        val allTypes = currentWineSet.map { it.type }.filter { it.isNotEmpty() }.distinct().sorted()
        val allVineyards = currentWineSet.map { it.vineyard }.filter { it.isNotEmpty() }.distinct().sorted()
        val allRegions = currentWineSet.map { it.region }.filter { it.isNotEmpty() }.distinct().sorted()
        val allCountries = currentWineSet.map { it.country }.filter { it.isNotEmpty() }.distinct().sorted()

        val yearPickerButton = dialogView.findViewById<ImageButton>(R.id.yearPickerButton)
        val yearTextView = dialogView.findViewById<TextView>(R.id.yearTextView)
        val yearCheckBox = dialogView.findViewById<CheckBox>(R.id.yearCheckBox)
        val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.wineTypeSpinner)
        val typeCheckBox = dialogView.findViewById<CheckBox>(R.id.typeCheckBox)

        val vineyardSpinner = dialogView.findViewById<Spinner>(R.id.vineyardSpinner)
        val vineyardCheckBox = dialogView.findViewById<CheckBox>(R.id.vineyardCheckBox)
        val regionSpinner = dialogView.findViewById<Spinner>(R.id.regionSpinner)
        val regionCheckBox = dialogView.findViewById<CheckBox>(R.id.regionCheckBox)
        val countrySpinner = dialogView.findViewById<Spinner>(R.id.countrySpinner)
        val countryCheckBox = dialogView.findViewById<CheckBox>(R.id.countryCheckBox)

        val minRatingButton = dialogView.findViewById<Button>(R.id.minRatingButton)
        val maxRatingButton = dialogView.findViewById<Button>(R.id.maxRatingButton)
        val ratingCheckBox = dialogView.findViewById<CheckBox>(R.id.ratingCheckBox)

        val minPriceButton = dialogView.findViewById<Button>(R.id.minPriceButton)
        val maxPriceButton = dialogView.findViewById<Button>(R.id.maxPriceButton)
        val priceCheckBox = dialogView.findViewById<CheckBox>(R.id.priceCheckBox)

        val drunkCheckBox = dialogView.findViewById<CheckBox>(R.id.drunkCheckBox)

        val sortSpinner = dialogView.findViewById<Spinner>(R.id.sortSpinner)

        yearTextView.text = filter.year?.toString() ?: "Select Year"
        yearCheckBox.isChecked = filter.year != null

        setupSpinner(wineTypeSpinner, allTypes, filter.type)
        typeCheckBox.isChecked = filter.type != null

        setupSpinner(vineyardSpinner, allVineyards, filter.vineyard)
        vineyardCheckBox.isChecked = filter.vineyard != null

        setupSpinner(regionSpinner, allRegions, filter.region)
        regionCheckBox.isChecked = filter.region != null

        setupSpinner(countrySpinner, allCountries, filter.country)
        countryCheckBox.isChecked = filter.country != null

        minPriceButton.text = filter.minPrice?.toString() ?: "Min"
        maxPriceButton.text = filter.maxPrice?.toString() ?: "Max"
        priceCheckBox.isChecked = filter.minPrice != null || filter.maxPrice != null

        minRatingButton.text = filter.minRating?.toString() ?: "Min"
        maxRatingButton.text = filter.maxRating?.toString() ?: "Max"
        ratingCheckBox.isChecked = filter.minRating != null || filter.maxRating != null

        drunkCheckBox.isChecked = filter.drunk

        setupSpinner(sortSpinner, listOf("A-Z", "Z-A", "Price: Increasing", "Price: Decreasing", "Year: Increasing", "Year: Decreasing"), filter.sort)

        yearPickerButton.setOnClickListener {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            showSeekBar(requireContext(), 1900, currentYear, filter.year) { selectedYear ->
                yearTextView.text = selectedYear.toString()
            }
        }

        minPriceButton.setOnClickListener {
            val currentMin = minPriceButton.text.toString().toIntOrNull()
            val currentMax = maxPriceButton.text.toString().toIntOrNull() ?: 1_000_000
            showNumberInputDialog(minPriceButton.context, 0, currentMax, currentMin) { selectedValue ->
                minPriceButton.text = selectedValue.toString()
            }
        }

        maxPriceButton.setOnClickListener {
            val currentMax = maxPriceButton.text.toString().toIntOrNull()
            val currentMin = minPriceButton.text.toString().toIntOrNull() ?: 0
            showNumberInputDialog(maxPriceButton.context, currentMin, 1_000_000, currentMax) { selectedValue ->
                maxPriceButton.text = selectedValue.toString()
            }
        }


        minRatingButton.setOnClickListener {
            val currentMin = minRatingButton.text.toString().toIntOrNull()
            val currentMax = maxRatingButton.text.toString().toIntOrNull() ?: 100
            showSeekBar(requireContext(), 0, currentMax, currentMin) { selectedValue ->
                minRatingButton.text = selectedValue.toString()
            }
        }

        maxRatingButton.setOnClickListener {
            val currentMax = maxRatingButton.text.toString().toIntOrNull()
            val currentMin = minRatingButton.text.toString().toIntOrNull() ?: 0
            showSeekBar(requireContext(), currentMin, 100, currentMax) { selectedValue ->
                maxRatingButton.text = selectedValue.toString()
            }
        }

        dialogView.findViewById<Button>(R.id.applyFilterButton).setOnClickListener {
            filter = Filter(
                year = if (yearCheckBox.isChecked) yearTextView.text.toString().toIntOrNull() else null,
                minPrice = if (priceCheckBox.isChecked) minPriceButton.text.toString().toIntOrNull() else null,
                maxPrice = if (priceCheckBox.isChecked) maxPriceButton.text.toString().toIntOrNull() else null,
                type = if (typeCheckBox.isChecked) wineTypeSpinner.selectedItem.toString() else null,
                vineyard = if (vineyardCheckBox.isChecked) vineyardSpinner.selectedItem?.toString() else null,
                region = if (regionCheckBox.isChecked) regionSpinner.selectedItem?.toString() else null,
                country = if (countryCheckBox.isChecked) countrySpinner.selectedItem?.toString() else null,
                minRating = if (ratingCheckBox.isChecked) minRatingButton.text.toString().toDoubleOrNull() else null,
                maxRating = if (ratingCheckBox.isChecked) maxRatingButton.text.toString().toDoubleOrNull() else null,
                drunk = drunkCheckBox.isChecked,
                sort = sortSpinner.selectedItem.toString()
            )

            filterWines(filter, currentWineSet)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.clearFilterButton).setOnClickListener {
            filter = Filter()
            filterWines(filter, currentWineSet)
            dialog.dismiss()
            Toast.makeText(context, "Filter cleared", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onResume() {
        loadFridges(requireContext())
        print(fridges[getFridge("drunk")])
        allMyWines.clear()
        for (fridge in fridges) {
            for (l in fridge.wines.indices) {
                for (s in fridge.wines[l].indices) {
                    for (r in fridge.wines[l][s].indices) {
                        for (c in fridge.wines[l][s][r].indices) {
                            val wine = fridge.wines[l][s][r][c]
                            if (wine.name != "null") { allMyWines.add(wine)}
                        } } } } }
        if (binding.myWinesToggleButton.isChecked) { filterWines(filter, allMyWines) }
        else { filterWines(filter, loadDB(requireContext()).toMutableList()) }
        super.onResume()
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
            if (tempWine.imagePath != "null") { loadImage(tempWine.imagePath, holder.wineImageView) }
            else { holder.wineImageView.setImageResource(R.drawable.bottle_front) }
            holder.wineNameTextView.text = tempWine.name
            holder.wineDescTextView.text = tempWine.year.toString()
            holder.itemView.setOnClickListener { onWineClick(tempWine) }
            holder.itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(150).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    }
                }
                false
            }
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
package com.example.projectp2_emmanuel_chan

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.fridges
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.getFridge
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.highlightedWineName
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.moveMode
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.moving
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.origSelectedFridge
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.selectedFridge
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.selectedIndices
import com.example.projectp2_emmanuel_chan.MainActivity.Companion.selectedWine
import com.example.projectp2_emmanuel_chan.databinding.ActivityFridgeBinding
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Companion.itemLayer
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Companion.saveFridges
import com.example.projectp2_emmanuel_chan.ui.fridges.FridgesFragment.Wine
import com.example.projectp2_emmanuel_chan.ui.wines.WinesFragment
import com.example.projectp2_emmanuel_chan.ui.wines.WinesFragment.Companion.findWine
import com.example.projectp2_emmanuel_chan.ui.wines.WinesFragment.Companion.getPairingSuggestion
import com.example.projectp2_emmanuel_chan.ui.wines.WinesFragment.Companion.loadImage
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.toColorInt

class FridgeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFridgeBinding
    private lateinit var wineRecyclerView: RecyclerView
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var capturedImage: Bitmap? = null
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
        }
    }
    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(galleryIntent)
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        var selectedImageView: ImageView? = null

        fun getIndicesFromPosition(position: Int, fridge: FridgesFragment.Fridge): MutableList<Int> {
            val perLayer = fridge.sections * fridge.rps * fridge.columns
            val layer = position / perLayer
            val positionInLayer = position % perLayer

            val perSection = fridge.rps * fridge.columns
            val section = positionInLayer / perSection
            val positionInSection = positionInLayer % perSection

            val perRow = fridge.columns
            val row = positionInSection / perRow
            val column = positionInSection % perRow

            return mutableListOf(layer, section, row, column)
        }

        fun saveImageBm(context: Context, bitmap: Bitmap, fileName: String): String? {
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

        fun saveImageUri(context: Context, imageUri: Uri, fileName: String): String? {
            val directory = File(context.filesDir, "WineWise")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val imageFile = File(directory, fileName)
            return try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    FileOutputStream(imageFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                imageFile.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        fun editWine(
            context: Context,
            wine: Wine,
            cameraLauncher: ActivityResultLauncher<Intent>,
            galleryLauncher: ActivityResultLauncher<Intent>,
            capturedImage: Bitmap?,
            cameraPermissionLauncher: ActivityResultLauncher<String>,
            galleryPermissionLauncher: ActivityResultLauncher<String>,
            adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null,
            adapter2: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null,

            ) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.wine_edit, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            dialogView.findViewById<MaterialTextView>(R.id.editTitleTextView)?.text = "Edit Wine"
            val editImageView = dialogView.findViewById<ImageView>(R.id.wineImage)

            if (wine.imagePath.isNotEmpty()) { loadImage(wine.imagePath, editImageView) }
            else { editImageView.setImageResource(R.drawable.bottle_front) }
            selectedImageView = editImageView

            val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.editWineType)
            val wineTypes = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified")
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, wineTypes)
            wineTypeSpinner.adapter = spinnerAdapter
            wineTypeSpinner.setSelection(wineTypes.indexOf(wine.type))

            dialogView.findViewById<ImageButton>(R.id.takeLabelButton).setOnClickListener {
                val modeSelectDialogView = LayoutInflater.from(context).inflate(R.layout.mode_select_dialog, null)
                val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(context)
                    .setView(modeSelectDialogView)
                val modeSelectDialog = dialogBuilder.create()

                modeSelectDialogView.findViewById<Button>(R.id.chooseCameraButton)?.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        cameraLauncher.launch(cameraIntent)
                    }
                    modeSelectDialog.dismiss()
                }

                modeSelectDialogView.findViewById<Button>(R.id.chooseGalleryButton)?.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                        println("ok")
                        galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        val galleryIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        galleryLauncher.launch(galleryIntent)
                    }
                    modeSelectDialog.dismiss()
                }

                modeSelectDialog.show()
            }

            dialogView.findViewById<ImageButton>(R.id.revertImageButton).setOnClickListener {
                var found = false
                for (wine in loadDB(context)) {
                    if (wine.name == selectedWine.name) {
                        selectedWine.imagePath = wine.imagePath
                        loadImage(selectedWine.imagePath, editImageView)
                        found = true
                        break
                    }
                }
                if (!found) { Toast.makeText(context, "No such wine in database.", Toast.LENGTH_SHORT).show() }
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
                    val savedImagePath = saveImageBm(context, it, fileName)
                    if (savedImagePath != null) {
                        wine.imagePath = savedImagePath
                    }
                }

                adapter?.notifyDataSetChanged()
                adapter2?.notifyDataSetChanged()
                dialog.dismiss()
            }

            val deleteButton = dialogView.findViewById<Button>(R.id.deleteWineButton)
            deleteButton.visibility = View.VISIBLE
            deleteButton.setOnClickListener {
                val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
                val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(context)
                    .setView(confirmDeleteView)
                val deleteDialog = confirmDialogBuilder.create()

                confirmDeleteView.findViewById<TextView>(R.id.nameTextView).text = "Delete ${wine.name}?"

                confirmDeleteView.findViewById<Button>(R.id.yesButton).setOnClickListener {
                    val fridge = fridges[(getFridge(wine.parentFridge))]
                    val indices = findWine(fridge, wine)
                    fridge.wines[indices[0]][indices[1]][indices[2]][indices[3]] = Wine()
                    adapter?.notifyDataSetChanged()
                    adapter2?.notifyDataSetChanged()
                    deleteDialog.dismiss()
                    dialog.dismiss()
                    Toast.makeText(context, "${wine.name} saved", Toast.LENGTH_SHORT).show()
                }

                confirmDeleteView.findViewById<Button>(R.id.noButton).setOnClickListener {
                    deleteDialog.dismiss()
                    Toast.makeText(context, "Edit cancelled", Toast.LENGTH_SHORT).show()
                }

                deleteDialog.show()
            }

            dialogView.setOnTouchListener { _, _ ->
                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
                false
            }

            dialog.show()
        }

        fun loadDB(context: Context): List<Wine> {
            return try {
                val inputStream = context.assets.open("database.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                Gson().fromJson(jsonString, object : TypeToken<List<Wine>>() {}.type)
            } catch (e: IOException) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (moving) {
                    moving = false
                    Toast.makeText(this@FridgeActivity, "Cancelled move.", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        })
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        when (sharedPreferences.getInt("theme", 0)) {
            0 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Default)
            1 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Dark)
        }
        binding = ActivityFridgeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val movingTextView = binding.movingTextView
        val cancelMoveButton = binding.cancelMoveButton
        val changeFridgeButton = binding.changeFridgeButton
        val movingButtonsLayout = binding.movingButtonsLayout
        if (moving) {
            var text = ""
            when (moveMode) {
                "move" -> { text = "Moving \"${selectedWine.name}\"" }
                "duplicate" -> { text = "Duplicate \"${selectedWine.name}\"" }
                "putBack" -> { text = "Putting back \"${selectedWine.name}\"" }
            }
            movingTextView.text = text
            movingTextView.visibility = View.VISIBLE
            movingButtonsLayout.visibility = View.VISIBLE
            cancelMoveButton.visibility = View.VISIBLE
        }

        val fridge = selectedFridge
        title = fridge.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        wineRecyclerView = findViewById(R.id.wineRecyclerView)
        val layoutManager = GridLayoutManager(this, fridge.columns)
        wineRecyclerView.layoutManager = layoutManager
        val wineAdapter = WineAdapter(fridge, 0)
        wineRecyclerView.adapter = wineAdapter
        wineRecyclerView.addItemDecoration(RowSeparatorDecoration(this, fridge.rps))

        val depthToggleButton = binding.depthToggleButton
        if (fridge.depth == 1) { depthToggleButton.isClickable = false }
        depthToggleButton.setOnCheckedChangeListener { _, isChecked ->
            val newAdapter = WineAdapter(fridge, if (isChecked) 1 else 0)
            wineRecyclerView.adapter = newAdapter
        }
        if (itemLayer == 1) {
            depthToggleButton.isChecked = true
            itemLayer = 3
        }

        cancelMoveButton.setOnClickListener{
            moving = false
            movingTextView.visibility = View.GONE
            cancelMoveButton.visibility = View.GONE
            changeFridgeButton.visibility = View.GONE
            movingButtonsLayout.visibility = View.GONE
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    val fileName = "wine_${System.currentTimeMillis()}.jpg"
                    val savedImagePath = saveImageBm(this, imageBitmap, fileName)

                    if (savedImagePath != null) {
                        selectedWine.imagePath = savedImagePath
                        wineRecyclerView.adapter?.notifyDataSetChanged()
                        selectedImageView?.setImageBitmap(imageBitmap)
                    } else {
                        Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    val fileName = "wine_${System.currentTimeMillis()}.jpg"
                    val savedImagePath = saveImageUri(this, imageUri, fileName)

                    if (savedImagePath != null) {
                        selectedWine.imagePath = savedImagePath
                        wineRecyclerView.adapter?.notifyDataSetChanged()
                        selectedImageView?.setImageURI(imageUri)
                    } else {
                        Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showAddWineDialog(index: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.wine_add, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val wineAddImage = dialogView.findViewById<ImageView>(R.id.wineAddImage)
        val wineNameInput = dialogView.findViewById<TextInputEditText>(R.id.editWineName)
        val wineNotesInput = dialogView.findViewById<TextInputEditText>(R.id.editDescription)
        val winePriceInput = dialogView.findViewById<TextInputEditText>(R.id.editPrice)
        val searchResultsLayout = dialogView.findViewById<LinearLayout>(R.id.addSearchResultsLayout)
        var presetSelected = false

        val wineList: List<Wine> = loadDB(this)

        fun updateSearchResults(query: String) {
            presetSelected = false
            searchResultsLayout.removeAllViews()
            if (query.isEmpty()) {
                searchResultsLayout.visibility = View.GONE
                return
            }
            searchResultsLayout.visibility = View.VISIBLE
            val filteredWines = wineList.filter { it.name.contains(query, ignoreCase = true) }
            if (filteredWines.isEmpty()) {
                return
            }

            filteredWines.forEach { wine ->
                val wineCardView = LayoutInflater.from(this).inflate(R.layout.all_wines_card, searchResultsLayout, false)

                wineCardView.findViewById<TextView>(R.id.wineNameTextView).text = wine.name
                wineCardView.findViewById<TextView>(R.id.wineDescTextView).text = wine.year.toString()
                val wineImageView = wineCardView.findViewById<ImageView>(R.id.wineImageView)
                if (wine.imagePath != "null" && wine.imagePath.isNotEmpty()) {
                    loadImage(wine.imagePath, wineImageView)
                }

                wineCardView.setOnClickListener {
                    selectedWine = wine
                    wineNameInput.setText(wine.name)
                    wineNotesInput.setText(wine.description)
                    winePriceInput.setText(wine.price.toString())
                    loadImage(wine.imagePath, wineAddImage)
                    searchResultsLayout.visibility = View.GONE
                    val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
                    wineNameInput.clearFocus()
                    wineNameInput.isClickable = false
                    wineNameInput.isFocusable = false
                    wineNotesInput.clearFocus()
                    winePriceInput.clearFocus()
                    presetSelected = true
                }

                searchResultsLayout.addView(wineCardView)
            }
        }

        wineNameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSearchResults(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSearchResults(s.toString())
            }
        })

        dialogView.findViewById<ImageButton>(R.id.clearNameButton).setOnClickListener {
            presetSelected = false
            wineAddImage.setImageResource(R.drawable.bottle_front)
            wineNameInput.text?.clear()
            wineNameInput.isClickable = true
            wineNameInput.isFocusable = true
            wineNameInput.isFocusableInTouchMode = true
            wineNotesInput.text?.clear()
            winePriceInput.text?.clear()
            wineNameInput.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(wineNameInput, InputMethodManager.SHOW_IMPLICIT)
        }

        dialogView.findViewById<Button>(R.id.addWineButton).setOnClickListener {
            if (!presetSelected) {
                Toast.makeText(this, "no preset selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = wineNameInput.text.toString().trim()
            val priceStr = winePriceInput.text.toString().trim()
            val description = wineNotesInput.text.toString().trim()

            if (name.isEmpty()) {
                wineNameInput.error = "Name is required"
                return@setOnClickListener
            }
            if (priceStr.isEmpty() || priceStr.toIntOrNull() == null) {
                winePriceInput.error = "Invalid price"
                return@setOnClickListener
            }
            val price = priceStr.toInt()

            selectedWine.apply {
                this.name = name
                this.description = description
                this.price = price
                this.pairings = getPairingSuggestion(applicationContext, selectedWine.grapeVariety)
                this.parentFridge = selectedFridge.name
            }

            val fridge = selectedFridge
            val indices = getIndicesFromPosition(index, fridge)
            val selectedLayer = if (binding.depthToggleButton.isChecked) 1 else 0
            fridge.wines[selectedLayer][indices[1]][indices[2]][indices[3]] = selectedWine

            dialog.dismiss()
            wineRecyclerView.adapter?.notifyDataSetChanged()
        }

        dialogView.findViewById<Button>(R.id.enterManualWineButton).setOnClickListener {
            dialog.dismiss()
            showManualAddWineDialog(index, cameraLauncher, cameraPermissionLauncher, galleryPermissionLauncher)
        }

        dialogView.setOnTouchListener { _, _ ->
            val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
            false
        }

        dialog.show()
    }

    private fun showManualAddWineDialog(
        index: Int,
        cameraLauncher: ActivityResultLauncher<Intent>,
        cameraPermissionLauncher: ActivityResultLauncher<String>,
        galleryPermissionLauncher: ActivityResultLauncher<String>
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.wine_edit, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialTextView>(R.id.editTitleTextView)?.text = "Add Wine"

        val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.editWineType)
        val wineTypes = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, wineTypes)
        wineTypeSpinner.adapter = spinnerAdapter

        selectedWine = Wine()
        val wineNameInput = dialogView.findViewById<TextInputEditText>(R.id.editWineName)
        val wineYearInput = dialogView.findViewById<TextInputEditText>(R.id.editWineYear)
        val vineyardInput = dialogView.findViewById<TextInputEditText>(R.id.editVineyard)
        val regionInput = dialogView.findViewById<TextInputEditText>(R.id.editRegion)
        val countryInput = dialogView.findViewById<TextInputEditText>(R.id.editCountry)
        val varietyInput = dialogView.findViewById<TextInputEditText>(R.id.editVariety)
        val ratingInput = dialogView.findViewById<TextInputEditText>(R.id.editRating)
        val priceInput = dialogView.findViewById<TextInputEditText>(R.id.editPrice)
        val drinkByInput = dialogView.findViewById<TextInputEditText>(R.id.editDrinkBy)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.editDescription)
        selectedImageView = dialogView.findViewById(R.id.wineImage)

        dialogView.findViewById<ImageButton>(R.id.takeLabelButton).setOnClickListener {
            val modeSelectDialogView = LayoutInflater.from(this).inflate(R.layout.mode_select_dialog, null)
            val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(modeSelectDialogView)
            val modeSelectDialog = dialogBuilder.create()

            modeSelectDialog.findViewById<Button>(R.id.chooseCameraButton)?.setOnClickListener {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                }
            }

            modeSelectDialog.findViewById<Button>(R.id.chooseGalleryButton)?.setOnClickListener {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryLauncher.launch(galleryIntent)
                }
            }
        }

        val saveButton = dialogView.findViewById<Button>(R.id.saveWineButton)
        saveButton.setOnClickListener {
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

            isValid = validateField(wineNameInput, "Name is required") && isValid
            isValid = validateField(vineyardInput, "Vineyard is required") && isValid
            isValid = validateField(regionInput, "Region is required") && isValid
            isValid = validateField(countryInput, "Country is required") && isValid
            isValid = validateField(varietyInput, "Variety is required") && isValid

            val yearIn = validateNumberField(wineYearInput, "Invalid year", 1000, 2100) ?: run { isValid = false; 0 }
            val ratingIn = validateDoubleField(ratingInput, "Rating must be between 0 and 100") ?: run { isValid = false; 0.0 }
            val priceIn = validateNumberField(priceInput, "Invalid price", 0) ?: run { isValid = false; 0 }
            val drinkByIn = validateNumberField(drinkByInput, "Invalid drink-by year", 1000, 2100) ?: run { isValid = false; 0 }

            if (!isValid) return@setOnClickListener

            selectedWine.apply {
                name = wineNameInput.text.toString()
                type = wineTypeSpinner.selectedItem.toString()
                year = yearIn
                vineyard = vineyardInput.text.toString()
                region = regionInput.text.toString()
                country = countryInput.text.toString()
                grapeVariety = varietyInput.text.toString()
                rating = ratingIn
                price = priceIn
                drinkBy = drinkByIn
                description = descriptionInput.text.toString()
                parentFridge = selectedFridge.name
                pairings = getPairingSuggestion(applicationContext, grapeVariety)
            }

            val fridge = selectedFridge
            val indices = getIndicesFromPosition(index, fridge)
            val selectedLayer = if (binding.depthToggleButton.isChecked) 1 else 0
            fridge.wines[selectedLayer][indices[1]][indices[2]][indices[3]] = selectedWine

            dialog.dismiss()
            wineRecyclerView.adapter?.notifyDataSetChanged()
        }

        dialogView.setOnTouchListener { _, _ ->
            val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
            false
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun openWine(wine: Wine, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.wine_info, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
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
            val dialogView1 = LayoutInflater.from(this).inflate(R.layout.wine_pairings, null)
            val dialogBuilder1 = AlertDialog.Builder(this).setView(dialogView1)
            val dialog1 = dialogBuilder1.create()

            dialogView1.findViewById<TextView>(R.id.winePairingsNameTextView).text = "Pairings for ${wine.name}"

            val pairingsEditText = dialogView1.findViewById<AppCompatEditText>(R.id.winePairingsEditText)
            val revertPairingsButton = dialogView1.findViewById<Button>(R.id.revertPairingsButton)
            val originalPairings = getPairingSuggestion(this, wine.grapeVariety)

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
                val indices = findWine(selectedFridge, wine)
                selectedFridge.wines[indices[0]][indices[1]][indices[2]][indices[3]].pairings = wine.pairings
            }

            revertPairingsButton.setOnClickListener {
                pairingsEditText.setText(originalPairings)
                wine.pairings = originalPairings
            }

            dialog1.show()
        }

        dialogView.findViewById<Button>(R.id.markDrunkButton)?.setOnClickListener {
            selectedWine = wine
            selectedFridge = fridges[getFridge("drunk")]
            val indices = getIndicesFromPosition(selectedFridge.counter, selectedFridge)
            selectedFridge.wines[indices[0]][indices[1]][indices[2]][indices[3]] = Wine(
                selectedWine.name,
                selectedWine.price,
                selectedWine.year,
                selectedWine.type,
                selectedWine.vineyard,
                selectedWine.region,
                selectedWine.country,
                selectedWine.grapeVariety,
                selectedWine.rating,
                selectedWine.pairings,
                selectedWine.drinkBy,
                selectedWine.description,
                selectedWine.imagePath,
                "drunk"
            )
            selectedWine = Wine()
            val indices1 = getIndicesFromPosition(position, selectedFridge)
            fridges[getFridge(wine.parentFridge)].wines[indices1[0]][indices1[1]][indices1[2]][indices1[3]] = Wine()
            Toast.makeText(applicationContext, "Marked drunk", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            fridges[getFridge("drunk")].counter++
            wineRecyclerView.adapter?.notifyDataSetChanged()
        }

        dialogView.findViewById<Button>(R.id.editWineButton)?.setOnClickListener {
            dialog.dismiss()
            editWine(this, wine, cameraLauncher, galleryLauncher, capturedImage, cameraPermissionLauncher, galleryPermissionLauncher, wineRecyclerView.adapter)
            wineRecyclerView.adapter?.notifyDataSetChanged()
        }

        dialogView.findViewById<Button>(R.id.moveWineButton).setOnClickListener {
            moving = true
            selectedWine = wine
            selectedIndices = getIndicesFromPosition(position, selectedFridge)
            binding.movingTextView.text = "Moving \"${wine.name}\""
            binding.movingTextView.visibility = View.VISIBLE
            binding.cancelMoveButton.visibility = View.VISIBLE
            binding.changeFridgeButton.visibility = View.VISIBLE
            binding.movingButtonsLayout.visibility = View.VISIBLE

            binding.changeFridgeButton.setOnClickListener {
                val dialogView1 = LayoutInflater.from(this).inflate(R.layout.select_fridge, null)
                val dialogBuilder1 = AlertDialog.Builder(this).setView(dialogView1)
                val dialog1 = dialogBuilder1.create()

                val fridgeSpinner = dialogView1.findViewById<Spinner>(R.id.selectFridgeSpinner)
                val openChangedFridgeButton = dialogView1.findViewById<Button>(R.id.openChangedFridgeButton)

                val availableFridges = fridges.filter { it.name != selectedFridge.name }

                val fridgeNames = availableFridges.map { it.name }
                val adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
                    fridgeNames.filter { it != "drunk" })
                fridgeSpinner.adapter = adapter

                openChangedFridgeButton.setOnClickListener {
                    val selectedFridgeName = fridgeSpinner.selectedItem?.toString()
                    val selectedFridge = fridges[getFridge(selectedFridgeName ?: "")]

                    if (selectedFridge.name != "null") {
                        FridgesFragment.openFridge(selectedFridge, this)
                        dialog1.dismiss()
                    } else {
                        Toast.makeText(this, "Invalid selection", Toast.LENGTH_SHORT).show()
                    }
                }

                dialog1.show()
            }

            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.duplicateWineButton).setOnClickListener {
            moving = true
            moveMode = "duplicate"
            selectedWine = wine
            selectedIndices = getIndicesFromPosition(position, selectedFridge)
            binding.movingTextView.text = "Duplicate \"${wine.name}\""
            binding.movingTextView.visibility = View.VISIBLE
            binding.cancelMoveButton.visibility = View.VISIBLE
            binding.changeFridgeButton.visibility = View.VISIBLE
            binding.movingButtonsLayout.visibility = View.VISIBLE

            binding.changeFridgeButton.setOnClickListener {
                val dialogView1 = LayoutInflater.from(this).inflate(R.layout.select_fridge, null)
                val dialogBuilder1 = AlertDialog.Builder(this).setView(dialogView1)
                val dialog1 = dialogBuilder1.create()

                val fridgeSpinner = dialogView1.findViewById<Spinner>(R.id.selectFridgeSpinner)
                val openChangedFridgeButton = dialogView1.findViewById<Button>(R.id.openChangedFridgeButton)

                val availableFridges = fridges.filter { it.name != selectedFridge.name }

                val fridgeNames = availableFridges.map { it.name }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fridgeNames)
                fridgeSpinner.adapter = adapter

                openChangedFridgeButton.setOnClickListener {
                    val selectedFridgeName = fridgeSpinner.selectedItem?.toString()
                    val selectedFridge = fridges[getFridge(selectedFridgeName ?: "")]

                    if (selectedFridge.name != "null") {
                        FridgesFragment.openFridge(selectedFridge, this)
                        dialog1.dismiss()
                    } else {
                        Toast.makeText(this, "Invalid selection", Toast.LENGTH_SHORT).show()
                    }
                }

                dialog1.show()
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun moveWine(position: Int) {
        val indices = getIndicesFromPosition(position, selectedFridge)
        val selectedLayer = if (binding.depthToggleButton.isChecked) 1 else 0
        selectedFridge.wines[selectedLayer][indices[1]][indices[2]][indices[3]] = Wine(
            selectedWine.name,
            selectedWine.price,
            selectedWine.year,
            selectedWine.type,
            selectedWine.vineyard,
            selectedWine.region,
            selectedWine.country,
            selectedWine.grapeVariety,
            selectedWine.rating,
            selectedWine.pairings,
            selectedWine.drinkBy,
            selectedWine.description,
            selectedWine.imagePath,
            selectedFridge.name,
        )
        selectedWine = Wine()
        when (moveMode) {
            "move" -> origSelectedFridge.wines[selectedIndices[0]][selectedIndices[1]][selectedIndices[2]][selectedIndices[3]] = Wine()
            "putBack" -> {
                fridges[getFridge("drunk")].wines[selectedIndices[0]][selectedIndices[1]][selectedIndices[2]][selectedIndices[3]] =
                    Wine()
                fridges[getFridge("drunk")].counter--
                println(fridges[getFridge("drunk")].toString())
                moveMode = "move"
            }
            "duplicate" ->  moveMode = "move"
        }
        binding.movingTextView.visibility = View.GONE
        binding.cancelMoveButton.visibility = View.GONE
        binding.changeFridgeButton.visibility = View.GONE
        binding.movingButtonsLayout.visibility = View.GONE
        saveFridges(this)
        moving = false
        wineRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        wineRecyclerView.adapter?.notifyDataSetChanged()
        if (!moving) {
            binding.movingTextView.visibility = View.GONE
            binding.movingButtonsLayout.visibility = View.GONE
            binding.cancelMoveButton.visibility = View.GONE
            binding.changeFridgeButton.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                if (moving) {
                    moving = false
                    Toast.makeText(this, "Cancelled move.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //adapter
    class WineAdapter(
        private val fridge: FridgesFragment.Fridge,
        private val selectedLayer: Int
    ) : RecyclerView.Adapter<WineAdapter.WineViewHolder>() {
        override fun getItemCount(): Int {
            return fridge.sections * fridge.rps * fridge.columns
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WineViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.wine_card, parent, false)
            return WineViewHolder(view)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onBindViewHolder(holder: WineViewHolder, position: Int) {
            val indices = getIndicesFromPosition(position, fridge)
            var wine = fridge.wines[selectedLayer][indices[1]][indices[2]][indices[3]]
            holder.bind(wine)
            holder.itemView.setOnClickListener {
                if (moving && wine.name == "null") {(holder.itemView.context as? FridgeActivity)?.moveWine(position) }
                else if (wine.name == "null") {
                    (holder.itemView.context as? FridgeActivity)?.showAddWineDialog(position)
                }
                else { (holder.itemView.context as? FridgeActivity)?.openWine(wine, position) }
            }
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

        class WineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val wineImageView: ImageView = view.findViewById(R.id.wineImageView)

            fun bind(wine: Wine?) {
                if (wine == null || wine.name == "null") { wineImageView.setImageResource(R.drawable.ic_add) }
                else if (highlightedWineName == wine.name) {
                    highlightedWineName = "null"
                    wineImageView.setColorFilter("#EE5555".toColorInt(), PorterDuff.Mode.SRC_IN)
                }
                else { wineImageView.setImageResource(R.drawable.bottle_front) }
            }
        }
    }

    //separator
    class RowSeparatorDecoration(
        context: Context,
        private val rps: Int
    ) : RecyclerView.ItemDecoration() {

        private val divider: Drawable?

        init {
            val attrs = intArrayOf(android.R.attr.listDivider)
            val typedArray: TypedArray = context.obtainStyledAttributes(attrs)
            divider = typedArray.getDrawable(0)
            typedArray.recycle()
        }

        override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (divider == null) return

            val childCount = parent.childCount
            val layoutManager = parent.layoutManager as GridLayoutManager
            val columns = layoutManager.spanCount

            for (i in 0 until childCount - 1) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                if ((position + 1) % (rps * columns) == 0) {
                    val params = child.layoutParams as RecyclerView.LayoutParams
                    val left = parent.paddingLeft
                    val right = parent.width - parent.paddingRight
                    val top = child.bottom + params.bottomMargin
                    val bottom = top + (divider.intrinsicHeight)

                    divider.setBounds(left, top, right, bottom)
                    divider.draw(canvas)
                }
            }
        }
    }
}
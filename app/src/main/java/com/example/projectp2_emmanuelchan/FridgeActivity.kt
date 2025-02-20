package com.example.projectp2_emmanuelchan

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.MainActivity.Companion.moving
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedIndices
import com.example.projectp2_emmanuelchan.MainActivity.Companion.selectedWine
import com.example.projectp2_emmanuelchan.databinding.ActivityFridgeBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.highlightedWine
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.origSelectedFridge
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment.Companion.selectedFridge
import com.example.projectp2_emmanuelchan.ui.wines.WinesFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class FridgeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFridgeBinding
    private lateinit var wineRecyclerView: RecyclerView
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImage: Bitmap? = null

    companion object {
        private lateinit var movingTextView: TextView
        private lateinit var cancelMoveButton: Button
        private lateinit var changeFridgeButton: Button
        private lateinit var movingButtonsLayout: LinearLayout

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
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        when (sharedPreferences.getInt("theme", 0)) {
            0 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Default)
            1 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Dark)
        }
        binding = ActivityFridgeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        movingTextView = binding.movingTextView
        cancelMoveButton = binding.cancelMoveButton
        changeFridgeButton = binding.changeFridgeButton
        movingButtonsLayout = binding.movingButtonsLayout
        if (moving) {
            movingTextView.text = "Moving \"${selectedWine.name}\""
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
        if (intent.getIntExtra("itemLayer", 3) == 1) { depthToggleButton.isChecked = true }

        cancelMoveButton.setOnClickListener{
            moving = false
            movingTextView.visibility = View.GONE
            cancelMoveButton.visibility = View.GONE
            changeFridgeButton.visibility = View.GONE
            movingButtonsLayout.visibility = View.GONE
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                capturedImage = imageBitmap
                Toast.makeText(this, "Image captured successfully", Toast.LENGTH_SHORT).show()
            } else { Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showAddWinePopup(index: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.wine_edit, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this).setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<MaterialTextView>(R.id.editTitleTextView)?.text = "Add Wine"

        val wineTypeSpinner = dialogView.findViewById<Spinner>(R.id.editWineType)
        val wineTypes = listOf("Red", "White", "Rosé", "Sparkling", "Dessert", "Fortified")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, wineTypes)
        wineTypeSpinner.adapter = spinnerAdapter

        val wineNameInput = dialogView.findViewById<TextInputEditText>(R.id.editWineName)
        val wineYearInput = dialogView.findViewById<TextInputEditText>(R.id.editWineYear)
        val vineyardInput = dialogView.findViewById<TextInputEditText>(R.id.editVineyard)
        val regionInput = dialogView.findViewById<TextInputEditText>(R.id.editRegion)
        val varietyInput = dialogView.findViewById<TextInputEditText>(R.id.editVariety)
        val ratingInput = dialogView.findViewById<TextInputEditText>(R.id.editRating)
        val priceInput = dialogView.findViewById<TextInputEditText>(R.id.editPrice)
        val drinkByInput = dialogView.findViewById<TextInputEditText>(R.id.editDrinkBy)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.editDescription)

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

            fun validateDoubleField(input: TextInputEditText, errorMessage: String, min: Double = 0.0, max: Double = 5.0): Double? {
                val value = input.text.toString().toDoubleOrNull()
                return if (value == null || value < min || value > max) {
                    input.error = errorMessage
                    null
                } else value
            }

            isValid = validateField(wineNameInput, "Name is required") && isValid
            isValid = validateField(vineyardInput, "Vineyard is required") && isValid
            isValid = validateField(regionInput, "Region is required") && isValid
            isValid = validateField(varietyInput, "Variety is required") && isValid

            var year = validateNumberField(wineYearInput, "Invalid year", 1000, 2100) ?: run { isValid = false; 0 }
            var rating = validateDoubleField(ratingInput, "Rating must be between 0 and 100", 0.0, 100.0) ?: run { isValid = false; 0.0 }
            var price = validateNumberField(priceInput, "Invalid price", 0) ?: run { isValid = false; 0 }
            var drinkBy = validateNumberField(drinkByInput, "Invalid drink-by year", 1000, 2100) ?: run { isValid = false; 0 }

            if (!isValid) return@setOnClickListener

            val tempWine = FridgesFragment.Wine().apply {
                name = wineNameInput.text.toString()
                type = wineTypeSpinner.selectedItem.toString()
                year = year
                vineyard = vineyardInput.text.toString()
                region = regionInput.text.toString()
                grapeVariety = varietyInput.text.toString()
                rating = rating
                price = price
                drinkBy = drinkBy
                description = descriptionInput.text.toString()
                parentFridge = selectedFridge.name
            }

            val fridge = selectedFridge
            val indices = getIndicesFromPosition(index, fridge)
            val selectedLayer = if (binding.depthToggleButton.isChecked) 1 else 0

            while (fridge.wines[selectedLayer].size <= indices[0]) fridge.wines[selectedLayer].add(mutableListOf())
            while (fridge.wines[selectedLayer][indices[0]].size <= indices[1]) fridge.wines[selectedLayer][indices[0]].add(mutableListOf())
            while (fridge.wines[selectedLayer][indices[0]][indices[1]].size <= indices[2]) fridge.wines[selectedLayer][indices[0]][indices[1]].add(FridgesFragment.Wine())

            fridge.wines[selectedLayer][indices[0]][indices[1]][indices[2]] = tempWine

            dialog.dismiss()
            wineRecyclerView.adapter?.notifyItemChanged(index)
        }

        dialog.show()
    }

    private fun openWine(wine: FridgesFragment.Wine, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.wine_info, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<TextView>(R.id.wineInfoNameTextView)?.text = wine.name
        dialogView.findViewById<TextView>(R.id.wineInfoDescTextView)?.text =
            "${wine.year}\n${wine.vineyard}, ${wine.region}\nVariety: ${wine.grapeVariety}\nRating: " +
            "${wine.rating}\nBought at: $${wine.price}\nDrink by: ${wine.drinkBy}\nNotes:\n${wine.description}"

        dialogView.findViewById<Button>(R.id.editWineButton)?.setOnClickListener {
            dialog.dismiss()
            WinesFragment.editWine(this, wine, cameraLauncher, capturedImage)
            wineRecyclerView.adapter?.notifyDataSetChanged()
        }

        dialogView.findViewById<Button>(R.id.moveWineButton).setOnClickListener {
            moving = true
            selectedWine = wine
            selectedIndices = getIndicesFromPosition(position, selectedFridge)
            movingTextView.text = "Moving \"${wine.name}\""
            movingTextView.visibility = View.VISIBLE
            cancelMoveButton.visibility = View.VISIBLE
            changeFridgeButton.visibility = View.VISIBLE
            movingButtonsLayout.visibility = View.VISIBLE

            changeFridgeButton.setOnClickListener {
                val dialogView1 = LayoutInflater.from(this).inflate(R.layout.select_fridge, null)
                val dialogBuilder1 = AlertDialog.Builder(this).setView(dialogView1)
                val dialog1 = dialogBuilder1.create()

                val fridgeSpinner = dialogView1.findViewById<Spinner>(R.id.selectFridgeSpinner)
                val openChangedFridgeButton = dialogView1.findViewById<Button>(R.id.openChangedFridgeButton)

                val availableFridges = FridgesFragment.fridges.filter { it.name != selectedFridge.name }

                val fridgeNames = availableFridges.map { it.name }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fridgeNames)
                fridgeSpinner.adapter = adapter

                openChangedFridgeButton.setOnClickListener {
                    val selectedFridgeName = fridgeSpinner.selectedItem?.toString()
                    val selectedFridge = FridgesFragment.getFridge(selectedFridgeName ?: "")

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
        System.out.println(selectedFridge)
        val indices = getIndicesFromPosition(position, selectedFridge)
        val selectedLayer = if (binding.depthToggleButton.isChecked) 1 else 0
        selectedFridge.wines[selectedLayer][indices[1]][indices[2]][indices[3]] = FridgesFragment.Wine(
            selectedWine.name,
            selectedWine.price,
            selectedWine.year,
            selectedWine.type,
            selectedWine.vineyard,
            selectedWine.region,
            selectedWine.grapeVariety,
            selectedWine.rating,
            selectedWine.tastingNotes,
            selectedWine.drinkBy,
            selectedWine.description,
            selectedWine.imagePath,
            selectedWine.parentFridge,
            selectedWine.drunk
        )
        selectedWine = FridgesFragment.Wine()
        origSelectedFridge.wines [selectedIndices[0]][selectedIndices[1]][selectedIndices[2]][selectedIndices[3]] = FridgesFragment.Wine()
        movingTextView.visibility = View.GONE
        cancelMoveButton.visibility = View.GONE
        changeFridgeButton.visibility = View.GONE
        movingButtonsLayout.visibility = View.GONE
        moving = false
        wineRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        wineRecyclerView.adapter?.notifyDataSetChanged()
        movingTextView = binding.movingTextView
        cancelMoveButton = binding.cancelMoveButton
        changeFridgeButton = binding.changeFridgeButton
        movingButtonsLayout = binding.movingButtonsLayout
        if (!moving) {
            movingTextView.visibility = View.GONE
            movingButtonsLayout.visibility = View.GONE
            cancelMoveButton.visibility = View.GONE
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

        override fun onBindViewHolder(holder: WineViewHolder, position: Int) {
            val indices = getIndicesFromPosition(position, fridge)
            System.out.println(fridge.name)
            var wine = fridge.wines[selectedLayer][indices[1]][indices[2]][indices[3]]
            holder.bind(wine)
            holder.itemView.setOnClickListener {
                if (moving && wine.name == "null") {(holder.itemView.context as? FridgeActivity)?.moveWine(position) }
                else if (wine.name == "null") { (holder.itemView.context as? FridgeActivity)?.showAddWinePopup(position) }
                else { (holder.itemView.context as? FridgeActivity)?.openWine(wine, position) }
            }
        }

        class WineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val wineImageView: ImageView = view.findViewById(R.id.wineImageView)

            fun bind(wine: FridgesFragment.Wine?) {
                if (wine == null || wine.name == "null") { wineImageView.setImageResource(R.drawable.ic_add) }
                else if (highlightedWine == wine) { wineImageView.setImageResource(R.drawable.bottle_front_highlight) }
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
                    val bottom = top + (divider?.intrinsicHeight ?: 4)

                    divider.setBounds(left, top, right, bottom)
                    divider.draw(canvas)
                }
            }
        }
    }

}
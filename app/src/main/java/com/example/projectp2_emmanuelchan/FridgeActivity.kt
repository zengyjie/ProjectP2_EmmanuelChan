package com.example.projectp2_emmanuelchan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.databinding.ActivityFridgeBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment
import com.example.projectp2_emmanuelchan.ui.wines.WinesFragment
import com.google.android.material.textview.MaterialTextView

class FridgeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFridgeBinding
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImage: Bitmap? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FridgeActivity::class.java)
            context.startActivity(intent)
        }

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
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        when (sharedPreferences.getInt("theme", 0)) {
            0 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Default)
            1 -> setTheme(R.style.Theme_ProjectP2_EmmanuelChan_Dark)
        }
        binding = ActivityFridgeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fridge = FridgesFragment.selectedFridge
        title = fridge.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val wineRecyclerView = findViewById<RecyclerView>(R.id.wineRecyclerView)
        val layoutManager = GridLayoutManager(this, fridge.columns)
        wineRecyclerView.layoutManager = layoutManager
        val wineAdapter = WineAdapter(fridge, 0)
        wineRecyclerView.adapter = wineAdapter
        wineRecyclerView.addItemDecoration(RowSeparatorDecoration(this, fridge.rps))

        if (fridge.depth == 1) {
            binding.depthToggleButton.isClickable = false
        }
        binding.depthToggleButton.setOnCheckedChangeListener { _, isChecked ->
            val selectedLayer = if (isChecked) 1 else 0
            val newAdapter = WineAdapter(fridge, selectedLayer)
            wineRecyclerView.adapter = newAdapter
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                imageBitmap?.let {
                    capturedImage = it
                    Toast.makeText(this, "Image captured successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddWinePopup(index: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.wine_edit, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<MaterialTextView>(R.id.editTitleTextView)?.text = "Add Wine"

        val wineImageView = dialogView.findViewById<ImageView>(R.id.wineImage)
        dialogView.findViewById<ImageButton>(R.id.takeLabelButton).setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        }
        capturedImage?.let {
            wineImageView.setImageBitmap(it)
        }

        dialog.findViewById<Button>(R.id.saveButton)?.setOnClickListener {
            val tempWine = FridgesFragment.Wine()//populate
            val fridge = FridgesFragment.selectedFridge
            val indices = getIndicesFromPosition(index, fridge)
            fridge.wines[indices[0]][indices[1]][indices[2]][indices[3]] = tempWine
        }


        dialog.show()
    }

    private fun openWine(wine: FridgesFragment.Wine) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.wine_info, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialog.findViewById<TextView>(R.id.wineInfoNameTextView)?.text = wine.name
        dialog.findViewById<TextView>(R.id.wineInfoDescTextView)?.text =
            "${wine.year}\n${wine.vineyard}, ${wine.region}\nVariety: ${wine.grapeVariety}\nRating: ${wine.rating}\nBought at: $${wine.price}\nDrink by: ${wine.drinkBy}\n${wine.description}"
        dialog.findViewById<Button>(R.id.editWineButton)?.setOnClickListener { WinesFragment.editWine(this, wine) }
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //adapter
    class WineAdapter(
        private val fridge: FridgesFragment.Fridge,
        private val selectedLayer: Int  // New parameter to filter depth
    ) : RecyclerView.Adapter<WineAdapter.WineViewHolder>() {

        override fun getItemCount(): Int {
            return fridge.sections * fridge.rps * fridge.columns // Only count items in selected layer
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WineViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.wine_card, parent, false)
            return WineViewHolder(view)
        }

        override fun onBindViewHolder(holder: WineViewHolder, position: Int) {
            val indices = getIndicesFromPosition(position, fridge)
            val wine = fridge.wines[selectedLayer][indices[0]][indices[1]][indices[2]]
            holder.bind(wine)
            holder.itemView.setOnClickListener {
                if (wine.name == "null") {
                    (holder.itemView.context as? FridgeActivity)?.showAddWinePopup(position)
                } else {
                    (holder.itemView.context as? FridgeActivity)?.openWine(wine)
                }
            }
        }

        class WineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val wineImageView: ImageView = view.findViewById(R.id.wineImageView)

            fun bind(wine: FridgesFragment.Wine?) {
                if (wine == null || wine.name == "null") {
                    wineImageView.setImageResource(R.drawable.ic_add)
                } else {
                    wineImageView.setImageResource(R.drawable.bottle_front)
                }
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

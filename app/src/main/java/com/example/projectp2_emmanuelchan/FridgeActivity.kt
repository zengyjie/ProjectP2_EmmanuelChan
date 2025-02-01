package com.example.projectp2_emmanuelchan

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.databinding.ActivityFridgeBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment

class FridgeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFridgeBinding

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FridgeActivity::class.java)
            context.startActivity(intent)
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
        val wineAdapter = WineAdapter(fridge) { wine ->
            if (wine != null) {
                openWine(wine)
            }
        }
        wineRecyclerView.adapter = wineAdapter
        wineRecyclerView.addItemDecoration(RowSeparatorDecoration(this, fridge.rps))

        if (fridge.depth == 1) { binding.depthToggleButton.isClickable = false }
    }

    private fun openWine(wine: FridgesFragment.Wine) {
        //TODO
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
        private val onWineClick: (FridgesFragment.Wine?) -> Unit
    ) : RecyclerView.Adapter<WineAdapter.WineViewHolder>() {

        private val totalSlots = fridge.sections * fridge.columns * fridge.rps * fridge.depth
        private val winesWithPlaceholders: List<FridgesFragment.Wine?> = List(totalSlots) { index ->
            fridge.wines.getOrNull(index) // Fill empty slots with null
        }

        override fun getItemCount(): Int = totalSlots

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WineViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.wine_card, parent, false)
            return WineViewHolder(view)
        }

        override fun onBindViewHolder(holder: WineViewHolder, position: Int) {
            holder.bind(winesWithPlaceholders[position])
        }

        class WineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val wineImageView: ImageView = view.findViewById(R.id.wineImageView)

            fun bind(wine: FridgesFragment.Wine?) {
                if (wine == null) {
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

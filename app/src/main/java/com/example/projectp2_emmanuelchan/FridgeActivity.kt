package com.example.projectp2_emmanuelchan

import android.content.Context
import android.content.Intent
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

        binding = ActivityFridgeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var fridge = FridgesFragment.selectedFridge
        title = fridge.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var wineRecyclerView = findViewById<RecyclerView>(R.id.wineRecyclerView)
        wineRecyclerView.layoutManager = GridLayoutManager(this, fridge.columns)
        var wineAdapter = WineAdapter(fridge.wines, fridge.rps, { wine -> openWine(wine) })
        wineRecyclerView.adapter = wineAdapter
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
        private val wines: List<FridgesFragment.Wine>,
        private val rps: Int,  // Rows per section
        private val onWineClick: (FridgesFragment.Wine) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val VIEW_TYPE_WINE = 0
            private const val VIEW_TYPE_SEPARATOR = 1
        }

        override fun getItemViewType(position: Int): Int {
            return if ((position + 1) % (rps + 1) == 0) VIEW_TYPE_SEPARATOR else VIEW_TYPE_WINE
        }

        override fun getItemCount() = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_WINE) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.wine_card, parent, false)
                WineViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.separator_item, parent, false)
                SeparatorViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == VIEW_TYPE_WINE) {
                val wineIndex = position - (position / (rps + 1)) // Adjust for separators
                val wine = wines[wineIndex]
                (holder as WineViewHolder).bind(wine)
            }
        }

        class WineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val wineImageView: ImageView = view.findViewById(R.id.wineImageView)

            fun bind(wine: FridgesFragment.Wine) {
                if (wine.name.equals("null")) {
                    wineImageView.setImageResource(R.drawable.ic_add)
                } else {
                    wineImageView.setImageResource(R.drawable.bottle_front)
                }
            }
        }

        class SeparatorViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

}

package com.example.projectp2_emmanuelchan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
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
}

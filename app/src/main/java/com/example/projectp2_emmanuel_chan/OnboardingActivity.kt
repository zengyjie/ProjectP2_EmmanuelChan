package com.example.projectp2_emmanuel_chan

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button

    private val images = listOf(
        R.mipmap.ic_launcher,
        R.drawable.onboarding0,
        R.drawable.onboarding1,
        R.drawable.onboarding2,
        R.drawable.onboarding3,
        R.drawable.onboarding4,
    )

    private val captions = listOf(
        "Welcome to WineWise!",
        "Create custom fridges for your wines",
        "Browse your collection efficiently",
        "Access a comprehensive database of wines",
        "Find the perfect wine for your taste",
        "Effortless syncing with Google"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_activity)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)

        val adapter = OnboardingAdapter(this, images, captions)
        viewPager.adapter = adapter

        btnNext.setOnClickListener {
            if (viewPager.currentItem < images.size - 1) {
                viewPager.currentItem += 1
            } else {
                finish()
            }
        }

        btnPrevious.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnPrevious.visibility = if (position > 0) View.VISIBLE else View.GONE
                btnNext.text = if (position == images.size - 1) "Get started!" else "Next"
            }
        })
    }

    class OnboardingAdapter(
        fragmentActivity: FragmentActivity,
        private val images: List<Int>,
        private val captions: List<String>
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = images.size

        override fun createFragment(position: Int): Fragment {
            return OnboardingFragment.newInstance(images[position], captions[position])
        }
    }

}

package com.example.projectp2_emmanuel_chan.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.projectp2_emmanuel_chan.R

class CustomSpinner : LinearLayout {
    private var countTextView: TextView? = null
    var count = 1

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.custom_spinner, this, true)
        findViewById<Button>(R.id.decrementButton).setOnClickListener { updateCount(-1) }
        findViewById<Button>(R.id.incrementButton).setOnClickListener { updateCount(1) }
        countTextView = findViewById(R.id.countTextView)
        updateCount(0)
    }

    internal fun updateCount(delta: Int) {
        count += delta
        count = count.coerceIn(1, 20)
        countTextView!!.text = count.toString()
    }

    fun count(count: Int) {
        this.count = count
        updateCount(0)
    }
}
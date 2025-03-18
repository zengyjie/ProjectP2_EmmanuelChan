package com.example.projectp2_emmanuel_chan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingFragment : Fragment() {

    private var imageResId: Int = 0
    private var captionText: String = ""

    companion object {
        private const val ARG_IMAGE = "arg_image"
        private const val ARG_CAPTION = "arg_caption"

        fun newInstance(imageResId: Int, captionText: String) =
            OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_IMAGE, imageResId)
                    putString(ARG_CAPTION, captionText)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageResId = it.getInt(ARG_IMAGE)
            captionText = it.getString(ARG_CAPTION, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val captionView = view.findViewById<TextView>(R.id.textViewCaption)

        imageView.setImageResource(imageResId)
        captionView.text = captionText

        return view
    }
}


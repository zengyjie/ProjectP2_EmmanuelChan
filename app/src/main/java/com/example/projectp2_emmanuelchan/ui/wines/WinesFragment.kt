package com.example.projectp2_emmanuelchan.ui.wines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.projectp2_emmanuelchan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val winesViewModel =
            ViewModelProvider(this).get(WinesViewModel::class.java)

        _binding = FragmentWinesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val allWines = mutableListOf<FridgesFragment.Wine>()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
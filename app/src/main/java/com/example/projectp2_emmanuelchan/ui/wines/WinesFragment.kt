package com.example.projectp2_emmanuelchan.ui.wines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.FridgeActivity
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentWinesBinding
import com.example.projectp2_emmanuelchan.ui.custom.CustomSpinner
import com.example.projectp2_emmanuelchan.ui.fridges.FridgesFragment

class WinesFragment : Fragment() {

    private var _binding: FragmentWinesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allWinesAdapter: AllWinesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWinesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val allWines = mutableListOf<FridgesFragment.Wine>()
        val allWinesRecyclerView = binding.allWinesRecyclerView
        allWinesRecyclerView.layoutManager = GridLayoutManager(context, 1)
        allWinesAdapter = AllWinesAdapter(allWines) { wine -> viewWine(wine) }
        allWinesRecyclerView.adapter = allWinesAdapter

        //dummy data
        allWines.add(FridgesFragment.Wine())
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun viewWine(wine: FridgesFragment.Wine) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.wine_info, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<Button>(R.id.editWineButton).setOnClickListener { editWine(wine) }
        dialog.show()
    }

    fun editWine(wine: FridgesFragment.Wine) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.wine_edit, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialog.show()
    }

    //adapter
    class AllWinesAdapter(
        private val wines: List<FridgesFragment.Wine>,
        private val onWineClick: (FridgesFragment.Wine) -> Unit,
    ) : RecyclerView.Adapter<AllWinesAdapter.AllWinesViewHolder>() {

        class AllWinesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fridgeImageView: ImageView = view.findViewById(R.id.wineImageView)
            val fridgeNameTextView: TextView = view.findViewById(R.id.wineNameTextView)
            val fridgeWineCountTextView: TextView = view.findViewById(R.id.wineDescTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllWinesViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.all_wines_card, parent, false)
            return AllWinesViewHolder(view)
        }

        override fun onBindViewHolder(holder: AllWinesViewHolder, i: Int) {
            holder.itemView.setOnClickListener { onWineClick(wines[i]) }
        }

        override fun getItemCount(): Int = wines.size
    }
}
package com.example.projectp2_emmanuelchan.ui.fridges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.FridgeActivity
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentFridgesBinding
import com.example.projectp2_emmanuelchan.ui.custom.CustomSpinner
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FridgesFragment : Fragment() {

    private var _binding: FragmentFridgesBinding? = null
    private val binding get() = _binding!!

    companion object {
        var fridges = mutableListOf<Fridge>()
        var selectedFridge: Fridge = Fridge()
    }

    private lateinit var fridgeAdapter: FridgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fridgesViewModel =
            ViewModelProvider(this).get(FridgesViewModel::class.java)

        _binding = FragmentFridgesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //RecyclerView
        val fridgeRecyclerView = binding.fridgeRecyclerView
        val spanCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) { 2 } else { 3 }
        fridgeRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        fridgeAdapter = FridgeAdapter(fridges, { fridge -> openFridge(fridge) }, { fridge -> showEditFridgePopup(fridge) })
        fridgeRecyclerView.adapter = fridgeAdapter

        //FAB
        val floatingActionButton: FloatingActionButton = binding.root.findViewById(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener { showAddFridgePopup() }

        //dummy values
        addFridge(Fridge())
        addWine(getFridge("New fridge"), Wine())
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //functions
    fun showAddFridgePopup() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.add_fridge, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = dialogBuilder.create()
        dialogView.findViewById<Button>(R.id.deleteButton).visibility = View.GONE

        dialogView.findViewById<Button>(R.id.confirmButton)?.setOnClickListener {
            val fridge = readFridgeData(dialogView)
            if (!fridge.name.startsWith("InvalidName")) { addFridge(fridge) } else { return@setOnClickListener}
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showEditFridgePopup(fridge: Fridge) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.add_fridge, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = dialogBuilder.create()

        dialogView.findViewById<TextView>(R.id.titleTextView)?.text = "Edit Fridge"
        dialogView.findViewById<TextView>(R.id.nameEditText).text = fridge.name
        dialogView.findViewById<CustomSpinner>(R.id.sectionsCustomSpinner).count(fridge.sections)
        dialogView.findViewById<CustomSpinner>(R.id.columnsCustomSpinner).count(fridge.columns)
        dialogView.findViewById<CustomSpinner>(R.id.rpsCustomSpinner).count(fridge.rps)
        dialogView.findViewById<ToggleButton>(R.id.depthToggleButton).isChecked = fridge.depth != 1

        dialogView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            val confirmDeleteView = LayoutInflater.from(context).inflate(R.layout.confirm_delete, null)
            val confirmDialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(confirmDeleteView)
            val deleteDialog = confirmDialogBuilder.create()

            confirmDeleteView.findViewById<TextView>(R.id.nameTextView).text = "Delete ${fridge.name}?"

            confirmDeleteView.findViewById<Button>(R.id.yesButton).setOnClickListener {
                removeFridge(fridge)
                deleteDialog.dismiss()
                dialog.dismiss()
            }

            confirmDeleteView.findViewById<Button>(R.id.noButton).setOnClickListener {
                deleteDialog.dismiss()
            }

            deleteDialog.show()
        }

        dialogView.findViewById<Button>(R.id.confirmButton)?.setOnClickListener {
            val newFridge = readFridgeData(dialogView)
            if (newFridge.name.equals("InvalidName")) { newFridge.name = fridge.name }
            editFridge(fridge, newFridge)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun openFridge(fridge: Fridge) {
        selectedFridge = fridge
        FridgeActivity.start(requireContext())
    }

    private fun toast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    //Fridge class
    data class Fridge (
        var name: String = "New fridge",
        var icon: Int = R.drawable.default_fridge,
        var sections: Int = 1,
        var columns: Int = 1,
        var rps: Int = 1,
        var depth: Int = 1,
        var capacity: Int = sections * columns * rps * depth,
        var wines: MutableList<Wine> = mutableListOf() //TODO create default full list
    )

    fun getFridge(name: String): Fridge {
        for (f in fridges) {
            if (f.name.equals(name)) {
                return f
            }
        }
        return Fridge(name="null")
    }

    fun addFridge(fridge: Fridge) {
        if (fridges.contains(fridge)) { return }
        if (fridges.any { it.name.equals(fridge.name, ignoreCase = true) }) { return }
        fridges.add(fridge)
        fridgeAdapter.notifyDataSetChanged()
    }

    fun removeFridge(fridge: Fridge) {
        fridges.remove(fridge)
        fridgeAdapter.notifyDataSetChanged()
    }

    fun editFridge(fridge: Fridge, newFridge: Fridge) {
        fridge.name = newFridge.name
        fridge.icon = newFridge.icon
        fridge.sections = newFridge.sections
        fridge.columns = newFridge.columns
        fridge.rps = newFridge.rps
        fridge.depth = newFridge.depth
        fridge.capacity = newFridge.capacity
        fridgeAdapter.notifyDataSetChanged()
    }

    fun readFridgeData(dialogView: View, edit: Boolean = false): Fridge {
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        var name = nameEditText?.text.toString().trim()

        if (name.isEmpty()) {
            toast("Please choose a name")
            name = "InvalidName"
        }
        if (name.equals("null", ignoreCase = true) || name.contains("\\")) {
            toast("Invalid name")
            name = "InvalidName"
        }
        if (fridges.any { it.name.equals(name, ignoreCase = true) }) {
            if (!edit) {
                toast("Name already in use")
                name = "InvalidName"
            }
        }

        var icon: Int = R.drawable.default_fridge
        var depth: Int
        if (dialogView.findViewById<RadioButton>(R.id.fridge1RadioButton)!!.isSelected) { icon = R.drawable.fridge_1 }
        if (dialogView.findViewById<RadioButton>(R.id.fridge2RadioButton)!!.isSelected) { icon = R.drawable.fridge_2 }
        if (dialogView.findViewById<ToggleButton>(R.id.depthToggleButton).isChecked) { depth = 2 } else { depth = 1 }

        return Fridge(
            name = name,
            icon = icon,
            sections = dialogView.findViewById<CustomSpinner>(R.id.sectionsCustomSpinner).count,
            columns = dialogView.findViewById<CustomSpinner>(R.id.columnsCustomSpinner).count,
            rps = dialogView.findViewById<CustomSpinner>(R.id.rpsCustomSpinner).count,
            depth = depth
        )
    }

    //Wine class
    data class Wine(
        val name: String = "null",
        val price: Int = 10,
        val year: Int = 2000,
        val type: String = "Red",
        val vineyard: String = "Vineyard",
        val region: String = "Bordeaux",
        val grapeVariety: String = "Cabernet Sauvignon",
        val rating: Double = 4.5,
        val tastingNotes: String = "Fruity with hints of oak and vanilla",
        val drinkBy: Int = 2050,
        val image: String? = null
    )

    fun addWine(fridge: Fridge, wine: Wine) {
        if (fridge.wines.contains(wine)) { return }
        fridge.wines.add(wine)
        fridgeAdapter.notifyDataSetChanged()
    }

    //adapter
    class FridgeAdapter(
        private val fridges: List<Fridge>,
        private val onFridgeClick: (Fridge) -> Unit,
        private val onFridgeLongClick: (Fridge) -> Unit
    ) : RecyclerView.Adapter<FridgeAdapter.FridgeViewHolder>() {

        class FridgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fridgeImageView: ImageView = view.findViewById(R.id.wineImageView)
            val fridgeNameTextView: TextView = view.findViewById(R.id.fridgeNameTextView)
            val fridgeWineCountTextView: TextView = view.findViewById(R.id.fridgeWineCountTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FridgeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fridge_card, parent, false)
            return FridgeViewHolder(view)
        }

        override fun onBindViewHolder(holder: FridgeViewHolder, i: Int) {
            val fridge = fridges[i]
            holder.fridgeImageView.setImageResource(fridge.icon)
            holder.fridgeNameTextView.text = fridge.name
            holder.fridgeWineCountTextView.text = "Wines: ${fridge.wines.size}/${fridge.capacity}"

            holder.itemView.setOnClickListener { onFridgeClick(fridge) }

            holder.itemView.setOnLongClickListener {
                onFridgeLongClick(fridge)
                true
            }
        }

        override fun getItemCount(): Int = fridges.size
    }
}

package com.example.projectp2_emmanuelchan.ui.fridges

import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectp2_emmanuelchan.FridgeActivity
import com.example.projectp2_emmanuelchan.R
import com.example.projectp2_emmanuelchan.databinding.FragmentFridgesBinding
import com.example.projectp2_emmanuelchan.ui.custom.CustomSpinner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.Serializable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FridgesFragment : Fragment() {

    private var _binding: FragmentFridgesBinding? = null
    private val binding get() = _binding!!

    companion object {
        var fridges = mutableListOf<Fridge>()
        var selectedFridge: Fridge = Fridge()
        var origSelectedFridge: Fridge = Fridge()
        var highlightedWine: Wine = Wine()
        lateinit var fridgeRecyclerView: RecyclerView

        fun getFridge(name: String): Fridge {
            for (f in fridges) {
                if (f.name.equals(name)) {
                    return f
                }
            }
            return Fridge(name="null")
        }

        fun openFridge(fridge: Fridge, context: Context, itemLayer: Int = 3) {
            selectedFridge = fridge
            val intent = Intent(context, FridgeActivity::class.java)
            intent.putExtra("itemLayer", itemLayer)
            context.startActivity(intent)
        }
    }

    private lateinit var fridgeAdapter: FridgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFridgesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        loadFridges()

        fridgeRecyclerView = binding.fridgeRecyclerView
        val spanCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) { 2 } else { 3 }
        fridgeRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        fridgeAdapter = FridgeAdapter(fridges, { fridge ->
            highlightedWine = Wine()
            origSelectedFridge = fridge
            openFridge(fridge, requireContext(), 3) },
            { fridge -> showEditFridgePopup(fridge) })
        fridgeRecyclerView.adapter = fridgeAdapter

        val floatingActionButton: FloatingActionButton = binding.root.findViewById(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener { showAddFridgePopup() }

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
            if (!fridge.name.startsWith("InvalidName")) { addFridge(fridge) } else { return@setOnClickListener }
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
        dialogView.findViewById<TextView>(R.id.searchBar).text = fridge.name
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
        var capacity: Int = sections * rps * columns * depth,
        var wines: MutableList<MutableList<MutableList<MutableList<Wine>>>> = mutableListOf()
    ) : Serializable

    private fun initWineArray(depth: Int, sections: Int, rps: Int, columns: Int): MutableList<MutableList<MutableList<MutableList<Wine>>>> {
        return MutableList(depth) { MutableList(sections) { MutableList(rps) { MutableList(columns) { Wine() } } } }
    }

    fun addFridge(fridge: Fridge) {
        if (fridges.contains(fridge)) { return }
        if (fridges.any { it.name.equals(fridge.name, ignoreCase = true) }) { return }
        fridges.add(fridge)
        fridge.wines = initWineArray(fridge.depth, fridge.sections, fridge.rps, fridge.columns)
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
        resizeWinesArray(fridge, newFridge.depth, newFridge.sections, newFridge.rps, newFridge.columns)
        fridgeAdapter.notifyDataSetChanged()
    }

    fun resizeWinesArray(fridge: Fridge, newDepth: Int, newSections: Int, newRows: Int, newColumns: Int) {
        val oldWines = fridge.wines

        val newWines = MutableList(newDepth) { layer -> MutableList(newSections) { section -> MutableList(newRows) { row -> MutableList(newColumns) { column ->
            if (layer < oldWines.size &&
                section < oldWines[layer].size &&
                row < oldWines[layer][section].size &&
                column < oldWines[layer][section][row].size)
            { oldWines[layer][section][row][column] } else { Wine() }
        } } } }

        fridge.wines = newWines
    }


    fun readFridgeData(dialogView: View, edit: Boolean = false): Fridge {
        val nameEditText = dialogView.findViewById<EditText>(R.id.searchBar)
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
        if (dialogView.findViewById<RadioButton>(R.id.fridge1RadioButton)!!.isSelected) { icon = R.drawable.fridge_1 }
        if (dialogView.findViewById<RadioButton>(R.id.fridge2RadioButton)!!.isSelected) { icon = R.drawable.fridge_2 }
        val depthToggle = dialogView.findViewById<ToggleButton>(R.id.depthToggleButton)
        val depth = if (depthToggle?.isChecked == true) 2 else 1

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
        var name: String = "null",
        var price: Int = 10,
        var year: Int = 2000,
        var type: String = "Red",
        var vineyard: String = "Vineyard",
        var region: String = "Bordeaux",
        var grapeVariety: String = "Cabernet Sauvignon",
        var rating: Double = 4.5,
        var tastingNotes: String = "Fruity with hints of oak and vanilla",
        var drinkBy: Int = 2050,
        var description: String = "desc",
        var imagePath: String = "null",
        var parentFridge: String = "New Fridge",
        var drunk: Boolean = false
    )

    fun addWine(fridge: Fridge, wine: Wine, layer: Int = 0, section: Int = 0, row: Int = 0, column: Int = 0) {
        if (fridge.wines[layer][section][row][column].name != "null") return

        for (l in 0 until fridge.wines.size) {
            for (s in 0 until fridge.wines[l].size) {
                for (r in 0 until fridge.wines[l][s].size) {
                    for (c in 0 until fridge.wines[l][s][r].size) {
                        if (fridge.wines[l][s][r][c].name == wine.name) return } } } }

        fridge.wines[layer][section][row][column] = wine
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
            val fridgeWineCountTextView: TextView = view.findViewById(R.id.wineDescTextView)
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

            // Correctly count total wine slots
            val totalWines = fridge.depth * fridge.sections * fridge.rps * fridge.columns
            val filledSlots = fridge.wines.sumOf { layer ->
                layer.sumOf { section ->
                    section.sumOf { row ->
                        row.count { it.name != "null" }
                    }
                }
            }

            holder.fridgeWineCountTextView.text = "Wines: $filledSlots/$totalWines"

            holder.itemView.setOnClickListener { onFridgeClick(fridge) }

            holder.itemView.setOnLongClickListener {
                onFridgeLongClick(fridge)
                true
            }
        }

        override fun getItemCount(): Int = fridges.size
    }

    override fun onResume() {
        super.onResume()
        fridgeRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        saveFridges()
    }

    override fun onStop() {
        super.onStop()
        saveFridges()
    }

    private fun saveFridges() {
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val gson = Gson()
        val json = gson.toJson(fridges)

        editor.putString("fridges_data", json)
        editor.apply()
    }

    private fun loadFridges() {
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("fridges_data", null)

        if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<Fridge>>() {}.type
            fridges = gson.fromJson(json, type) ?: mutableListOf()
        }
    }
}

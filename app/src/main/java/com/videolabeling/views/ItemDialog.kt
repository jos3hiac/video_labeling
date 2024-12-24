package com.videolabeling.views

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.videolabeling.R

abstract class ItemDialog(context: Context) : Dialog(context) {

    var onAccept: ((String) -> Unit)? = null
    var onRemove: ((Int,String) -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    protected var _items = mutableListOf<String>()
    protected var onAddItem: ((String) -> Unit)? = null
    protected lateinit var itemAdapter: ItemAdapter
    protected lateinit var inputEditText: EditText
    protected lateinit var buttonAccept: Button
    protected lateinit var buttonRemove: Button
    protected lateinit var buttonCancel: Button
    protected lateinit var recyclerView: RecyclerView

    init {
        initDialog()
    }

    open fun initDialog() {
        setContentView(R.layout.item_dialog)

        // Inicializamos los elementos del layout
        inputEditText = findViewById(R.id.inputEditText)
        buttonAccept = findViewById(R.id.buttonAccept)
        buttonRemove = findViewById(R.id.buttonRemove)
        buttonCancel = findViewById(R.id.buttonCancel)
        recyclerView = findViewById(R.id.recyclerView)
        // Inicializamos el RecyclerView con el adaptador
        itemAdapter = ItemAdapter(getItems()) { selectedItem ->
            inputEditText.setText(selectedItem)
            if(!buttonRemove.isEnabled) setButtonRemoveEnabled(true)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = itemAdapter

        setButtonEnabled(buttonAccept,true)
        setButtonEnabled(buttonRemove,false)
        setButtonEnabled(buttonCancel,true)
    }
    fun getItems(): MutableList<String> {
        return _items
    }
    fun getSelectedItem(): String? {
        return itemAdapter.getSelectedItem()
    }
    fun getSelectedPosition(): Int {
        return itemAdapter.getSelectedPosition()
    }
    fun removeItemAt(position: Int){
        itemAdapter.removeItemAt(position)
    }
    fun removeItem(item: String){
        itemAdapter.removeItem(item)
    }
    fun setItems(newItems: MutableList<String>) {
        _items = newItems
        itemAdapter.setItems(_items)
    }
    fun setItem(item: String){
        inputEditText.setText(item)
        itemAdapter.setSelectedItem(item)
    }
    fun setButtonAcceptEnabled(enabled: Boolean){
        setButtonEnabled(buttonAccept,enabled)
    }
    fun setButtonRemoveEnabled(enabled: Boolean){
        setButtonEnabled(buttonRemove,enabled)
    }
    fun setButtonEnabled(button: Button,enabled: Boolean){
        button.isEnabled = enabled
        if(enabled){
            button.alpha = 1f
            button.setOnClickListener {
                if(button == buttonAccept){
                    val newItem = inputEditText.text.toString().trim()
                    val foundItem = getItems().find { it.equals(newItem, ignoreCase = true) }
                    if (newItem.isNotEmpty()){
                        if(foundItem == null){
                            itemAdapter.addItem(newItem)
                            inputEditText.text.clear()
                            onAddItem?.invoke(newItem)
                        }
                        val item = foundItem ?: newItem
                        onAccept?.invoke(item)
                        dismiss()
                        setItem(item)
                    }
                }
                else if(button == buttonRemove){
                    onRemove?.invoke(itemAdapter.getSelectedPosition(),itemAdapter.getSelectedItem()!!)
                }
                else if(button == buttonCancel){
                    onCancel?.invoke()
                    dismiss()
                }
            }
        }
        else{
            button.alpha = 0.5f
            button.setOnClickListener(null)
        }
    }
}

class ItemAdapter(
    private var items: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var selectedPosition: Int = -1 // Índice del ítem seleccionado

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTextView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.itemTextView.text = item

        // Cambiar el estilo visual según si el ítem está seleccionado o no
        holder.itemTextView.setBackgroundColor(
            if (position == selectedPosition)
                holder.itemView.context.getColor(android.R.color.holo_blue_light)
            else
                holder.itemView.context.getColor(android.R.color.transparent)
        )
        holder.itemTextView.setTextColor(
            if (position == selectedPosition)
                holder.itemView.context.getColor(android.R.color.white)
            else
                holder.itemView.context.getColor(android.R.color.black)
        )

        // Configurar el evento de clic
        holder.itemView.setOnClickListener {
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) { // Verifica que sea válido
                val previousSelected = selectedPosition
                selectedPosition = adapterPosition
                if (previousSelected != -1) notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onItemClick(items[adapterPosition])
            }
        }
    }
    override fun getItemCount(): Int = items.size
    fun addItem(newItem: String) {
        items.add(newItem)
        notifyItemInserted(items.size - 1)
    }
    fun setItems(items: MutableList<String>){
        this.items = items
        notifyDataSetChanged()
    }
    fun setSelectedItem(item: String) {
        val position = items.indexOf(item)
        val previousSelected = selectedPosition
        selectedPosition = position
        if (previousSelected != -1) notifyItemChanged(previousSelected)
        notifyItemChanged(selectedPosition)
    }
    fun removeItemAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            selectedPosition = -1
            // Actualizar la posición seleccionada si es necesario
            /*if (position == selectedPosition) {
                selectedPosition = -1
            } else if (position < selectedPosition) {
                selectedPosition--
            }*/
        }
    }
    fun removeItem(item: String) {
        val position = items.indexOf(item)
        if (position != -1) {
            removeItemAt(position)
        }
    }
    fun getSelectedPosition(): Int {
        return selectedPosition
    }
    fun getSelectedItem(): String? {
        return if (selectedPosition != -1) items[selectedPosition] else null
    }
}

class LabelDialog(context: Context) : ItemDialog(context) {
    override fun initDialog() {
        super.initDialog()
        setTitle("Crear o editar etiqueta")
        inputEditText.hint = "Ingrese etiqueta"
        onAddItem = { item ->
            Toast.makeText(context, "Se agregó la etiqueta $item", Toast.LENGTH_SHORT).show()
        }
    }
}

class ProjectDialog(context: Context) : ItemDialog(context) {
    override fun initDialog() {
        super.initDialog()
        setTitle("Nuevo o abrir proyecto")
        inputEditText.hint = "Ingrese nombre del proyecto"
        onAddItem = { item ->
            Toast.makeText(context, "Se agregó el proyecto $item", Toast.LENGTH_SHORT).show()
        }
    }
}
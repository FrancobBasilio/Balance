package com.app.balance.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.R
import com.app.balance.model.Categoria
import java.io.File

class CategoriaSeleccionAdapter(
    private var categorias: List<Categoria>,
    private val onCategoriaClick: (Categoria) -> Unit,
    private val onCategoriaLongClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaSeleccionAdapter.CategoriaViewHolder>() {

    private var categoriaSeleccionadaId: Int = -1

    class CategoriaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIconoCategoria: ImageView = view.findViewById(R.id.ivIconoCategoria)
        val ivCheckMarca: ImageView = view.findViewById(R.id.ivCheckMarca)
        val tvNombreCategoria: TextView = view.findViewById(R.id.tvNombreCategoria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria_seleccion, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val categoria = categorias[position]

        holder.tvNombreCategoria.text = categoria.nombre

        // Cargar icono
        if (!categoria.rutaImagen.isNullOrEmpty()) {
            // Cargar imagen personalizada
            val file = File(categoria.rutaImagen)
            if (file.exists()) {
                holder.ivIconoCategoria.setImageURI(Uri.fromFile(file))
                holder.ivIconoCategoria.backgroundTintList = null
            }
        } else {
            // Cargar icono predeterminado
            val iconoRes = obtenerRecursoIcono(categoria.icono)
            holder.ivIconoCategoria.setImageResource(iconoRes)
        }

        // Mostrar check si está seleccionada
        if (categoria.id == categoriaSeleccionadaId) {
            holder.ivCheckMarca.visibility = View.VISIBLE
        } else {
            holder.ivCheckMarca.visibility = View.GONE
        }

        // Click normal - seleccionar
        holder.itemView.setOnClickListener {
            categoriaSeleccionadaId = categoria.id
            notifyDataSetChanged()
            onCategoriaClick(categoria)
        }

        // Long click - eliminar
        holder.itemView.setOnLongClickListener {
            onCategoriaLongClick(categoria)
            true
        }
    }

    override fun getItemCount() = categorias.size

    fun actualizarCategorias(nuevasCategorias: List<Categoria>) {
        categorias = nuevasCategorias
        notifyDataSetChanged()
    }

    fun getCategoriaSeleccionada(): Categoria? {
        return categorias.find { it.id == categoriaSeleccionadaId }
    }

    private fun obtenerRecursoIcono(nombreIcono: String): Int {
        return when (nombreIcono) {
            "comida" -> R.drawable.ic_comida
            "transporte" -> R.drawable.ic_transporte
            "casa" -> R.drawable.ic_casa
            "salud" -> R.drawable.ic_salud
            "educacion" -> R.drawable.ic_educacion
            "entretenimiento" -> R.drawable.ic_entretenimiento
            "compras" -> R.drawable.ic_compras
            "otros" -> R.drawable.ic_otros
            else -> R.drawable.ic_default
        }
    }
}
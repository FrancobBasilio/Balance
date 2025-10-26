package com.app.balance.adapters

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.R
import com.app.balance.model.Categoria
import com.bumptech.glide.Glide
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
        val context = holder.itemView.context

        holder.tvNombreCategoria.text = categoria.nombre

        //  Cargar icono con Glide
        if (!categoria.rutaImagen.isNullOrEmpty()) {
            // Cargar imagen personalizada circular
            val file = File(categoria.rutaImagen)
            if (file.exists()) {
                Glide.with(context)
                    .load(file)
                    .circleCrop()
                    .into(holder.ivIconoCategoria)

                // Quitar el background para evitar el borde
                holder.ivIconoCategoria.background = null
            } else {
                // Si el archivo no existe, cargar icono por defecto
                cargarIconoPredeterminado(holder, categoria)
            }
        } else {
            // Cargar icono predeterminado
            cargarIconoPredeterminado(holder, categoria)
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

    //  NUEVA FUNCIÓN: Cargar icono predeterminado con background circular
    // ✅ FUNCIÓN CORREGIDA
    private fun cargarIconoPredeterminado(holder: CategoriaViewHolder, categoria: Categoria) {
        val context = holder.itemView.context
        val iconoRes = obtenerRecursoIcono(categoria.icono)

        holder.ivIconoCategoria.setImageResource(iconoRes)
        // Fondo circular blanco
        holder.ivIconoCategoria.setBackgroundResource(R.drawable.fondo_circular_solido)

        // Usar el color guardado o negro por defecto
        val colorIcono = categoria.color ?: android.R.color.black
        holder.ivIconoCategoria.setColorFilter(
            context.getColor(colorIcono),
            PorterDuff.Mode.SRC_IN
        )
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
package com.app.balance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.R
import com.app.balance.model.Categoria
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import java.io.File

class CategoriaSeleccionAdapter(
    private var categorias: List<Categoria>,
    private val onCategoriaClick: (Categoria) -> Unit,
    private val onCategoriaLongClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaSeleccionAdapter.CategoriaViewHolder>() {

    private var categoriaSeleccionadaId: Int = -1

    class CategoriaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardCategoria: MaterialCardView = view.findViewById(R.id.cardCategoria)
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

        // Limpiar estado anterior
        holder.ivIconoCategoria.clearColorFilter()
        holder.cardCategoria.strokeWidth = 0
        holder.ivIconoCategoria.setPadding(0, 0, 0, 0)
        
        if (!categoria.rutaImagen.isNullOrEmpty()) {
            val file = File(categoria.rutaImagen)
            if (file.exists()) {
                // Imagen personalizada - llenar todo el círculo
                holder.ivIconoCategoria.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.cardCategoria.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                
                Glide.with(context)
                    .load(file)
                    .centerCrop()
                    .into(holder.ivIconoCategoria)
            } else {
                cargarIconoPredeterminado(holder, categoria)
            }
        } else {
            cargarIconoPredeterminado(holder, categoria)
        }

        // Check de selección
        holder.ivCheckMarca.visibility = if (categoria.id == categoriaSeleccionadaId) View.VISIBLE else View.GONE

        // Click
        holder.itemView.setOnClickListener {
            categoriaSeleccionadaId = categoria.id
            notifyDataSetChanged()
            onCategoriaClick(categoria)
        }

        // Long click
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

    fun setSelectedById(categoriaId: Int) {
        categoriaSeleccionadaId = categoriaId
        notifyDataSetChanged()
    }

    fun setCategoriasAndSelected(nuevasCategorias: List<Categoria>, selectedId: Int?) {
        categorias = nuevasCategorias
        categoriaSeleccionadaId = selectedId ?: -1
        notifyDataSetChanged()
    }

    private fun cargarIconoPredeterminado(holder: CategoriaViewHolder, categoria: Categoria) {
        val context = holder.itemView.context
        val iconoRes = obtenerRecursoIcono(categoria.icono)

        // Icono predeterminado centrado con padding
        holder.ivIconoCategoria.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val paddingPx = (14 * context.resources.displayMetrics.density).toInt()
        holder.ivIconoCategoria.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        
        // Usar color guardado o gold por defecto
        val colorInt = try {
            if (categoria.color != null) {
                ContextCompat.getColor(context, categoria.color)
            } else {
                ContextCompat.getColor(context, R.color.gold)
            }
        } catch (e: Exception) {
            ContextCompat.getColor(context, R.color.gold)
        }
        
        // Fondo más claro basado en el color
        holder.cardCategoria.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gold_soft))
        
        holder.ivIconoCategoria.setImageResource(iconoRes)
        holder.ivIconoCategoria.setColorFilter(colorInt)
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

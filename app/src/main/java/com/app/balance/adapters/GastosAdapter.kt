package com.app.balance.adapters

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.R
import com.app.balance.model.Categoria
import com.app.balance.model.TransaccionConDetalles
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class GastosAdapter(
    private var transacciones: List<TransaccionConDetalles>,
    private val codigoDivisa: String,
    private val onItemClick: (TransaccionConDetalles) -> Unit,
    private val onEliminarClick: (TransaccionConDetalles) -> Unit,
    private val onEditarClick: (TransaccionConDetalles) -> Unit
) : RecyclerView.Adapter<GastosAdapter.GastoViewHolder>() {

    class GastoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardIcono: MaterialCardView = view.findViewById(R.id.cardIcono)
        val ivIconoCategoria: ImageView = view.findViewById(R.id.ivIconoCategoria)
        val tvNombreCategoria: TextView = view.findViewById(R.id.tvNombreCategoria)
        val tvTipoCategoria: TextView = view.findViewById(R.id.tvTipoCategoria)
        val tvComentario: TextView = view.findViewById(R.id.tvComentario)
        val tvMontoGasto: TextView = view.findViewById(R.id.tvMontoGasto)
        val tvFechaGasto: TextView = view.findViewById(R.id.tvFechaGasto)
        val btnEditarGasto: ImageButton = view.findViewById(R.id.btnEditarGasto)
        val btnEliminarGasto: ImageButton = view.findViewById(R.id.btnEliminarGasto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        val item = transacciones[position]
        val context = holder.itemView.context

        holder.tvNombreCategoria.text = item.categoria.nombre
        holder.tvTipoCategoria.text = item.tipoCategoria.nombre
        holder.tvMontoGasto.text = "-$codigoDivisa ${String.format("%.2f", item.transaccion.monto)}"
        holder.tvFechaGasto.text = formatearFechaCorta(item.transaccion.fecha)

        // Comentario
        if (!item.transaccion.comentario.isNullOrEmpty()) {
            holder.tvComentario.visibility = View.VISIBLE
            holder.tvComentario.text = item.transaccion.comentario
        } else {
            holder.tvComentario.visibility = View.GONE
        }

        // Color del monto
        val colorMonto = when (item.tipoCategoria.nombre) {
            "Necesidad" -> ContextCompat.getColor(context, R.color.error_red)
            "Deseo" -> ContextCompat.getColor(context, R.color.warning_orange)
            "Ahorro" -> ContextCompat.getColor(context, R.color.success_green)
            else -> ContextCompat.getColor(context, R.color.text_primary)
        }
        holder.tvMontoGasto.setTextColor(colorMonto)

        // Cargar icono
        cargarIconoCategoria(holder, item.categoria, context)

        // Listeners
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnEditarGasto.setOnClickListener { onEditarClick(item) }
        holder.btnEliminarGasto.setOnClickListener { onEliminarClick(item) }
        
        // Click en imagen para ver completa
        if (!item.categoria.rutaImagen.isNullOrEmpty()) {
            holder.ivIconoCategoria.setOnClickListener {
                mostrarImagenCompleta(context, item.categoria.rutaImagen!!)
            }
        } else {
            holder.ivIconoCategoria.setOnClickListener(null)
            holder.ivIconoCategoria.isClickable = false
        }
    }

    override fun getItemCount() = transacciones.size

    fun actualizarDatos(nuevasTransacciones: List<TransaccionConDetalles>) {
        transacciones = nuevasTransacciones
        notifyDataSetChanged()
    }

    private fun formatearFechaCorta(fecha: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd MMM", Locale("es", "ES"))
            val date = formatoEntrada.parse(fecha)
            formatoSalida.format(date!!)
        } catch (e: Exception) {
            fecha
        }
    }

    private fun cargarIconoCategoria(holder: GastoViewHolder, categoria: Categoria, context: Context) {
        // Limpiar estado anterior
        holder.ivIconoCategoria.clearColorFilter()
        holder.ivIconoCategoria.setPadding(0, 0, 0, 0)
        holder.cardIcono.strokeWidth = 0
        
        if (!categoria.rutaImagen.isNullOrEmpty()) {
            val file = File(categoria.rutaImagen)
            if (file.exists()) {
                // Imagen de galería/cámara - llenar todo el círculo
                holder.ivIconoCategoria.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.cardIcono.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                
                Glide.with(context)
                    .load(file)
                    .centerCrop()
                    .into(holder.ivIconoCategoria)
                    
                holder.ivIconoCategoria.isClickable = true
                return
            }
        }
        
        // Icono predeterminado - con fondo y padding
        holder.ivIconoCategoria.scaleType = ImageView.ScaleType.CENTER_INSIDE
        holder.cardIcono.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gold_soft))
        val paddingPx = (12 * context.resources.displayMetrics.density).toInt()
        holder.ivIconoCategoria.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        
        val iconoRes = obtenerRecursoIcono(categoria.icono)
        holder.ivIconoCategoria.setImageResource(iconoRes)
        
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
        holder.ivIconoCategoria.setColorFilter(colorInt)
        holder.ivIconoCategoria.isClickable = false
    }

    private fun mostrarImagenCompleta(context: Context, rutaImagen: String) {
        val file = File(rutaImagen)
        if (!file.exists()) {
            Toast.makeText(context, "Imagen no encontrada", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_imagen_completa)

        val ivImagenCompleta = dialog.findViewById<ImageView>(R.id.ivImagenCompleta)
        val btnCerrar = dialog.findViewById<ImageButton>(R.id.btnCerrarImagen)

        // Cargar imagen a tamaño completo
        Glide.with(context)
            .load(file)
            .fitCenter()
            .into(ivImagenCompleta)

        ivImagenCompleta.setOnClickListener { dialog.dismiss() }
        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
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

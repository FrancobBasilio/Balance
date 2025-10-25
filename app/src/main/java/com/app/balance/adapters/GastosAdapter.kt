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
import com.app.balance.model.TransaccionConDetalles
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class GastosAdapter(
    private var transacciones: List<TransaccionConDetalles>,
    private val codigoDivisa: String,
    private val onItemClick: (TransaccionConDetalles) -> Unit,
    private val onEliminarClick: (TransaccionConDetalles) -> Unit // Cambiar nombre del callback
) : RecyclerView.Adapter<GastosAdapter.GastoViewHolder>() {

    class GastoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIconoCategoria: ImageView = view.findViewById(R.id.ivIconoCategoria)
        val tvNombreCategoria: TextView = view.findViewById(R.id.tvNombreCategoria)
        val tvTipoCategoria: TextView = view.findViewById(R.id.tvTipoCategoria)
        val tvComentario: TextView = view.findViewById(R.id.tvComentario)
        val tvMontoGasto: TextView = view.findViewById(R.id.tvMontoGasto)
        val tvFechaGasto: TextView = view.findViewById(R.id.tvFechaGasto)
        val btnEliminarGasto: MaterialButton = view.findViewById(R.id.btnEliminarGasto) // NUEVO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        val item = transacciones[position]

        holder.tvNombreCategoria.text = item.categoria.nombre
        holder.tvTipoCategoria.text = item.tipoCategoria.nombre
        holder.tvMontoGasto.text = "$codigoDivisa ${String.format("%.2f", item.transaccion.monto)}"

        // Formatear fecha
        holder.tvFechaGasto.text = formatearFecha(item.transaccion.fecha)

        // Mostrar comentario si existe
        if (!item.transaccion.comentario.isNullOrEmpty()) {
            holder.tvComentario.visibility = View.VISIBLE
            holder.tvComentario.text = item.transaccion.comentario
        } else {
            holder.tvComentario.visibility = View.GONE
        }

        // Cambiar color del monto según el tipo de categoría
        val colorMonto = when (item.tipoCategoria.nombre) {
            "Necesidad" -> holder.itemView.context.getColor(android.R.color.holo_red_dark)
            "Deseo" -> holder.itemView.context.getColor(android.R.color.holo_orange_dark)
            "Ahorro" -> holder.itemView.context.getColor(android.R.color.holo_green_dark)
            else -> holder.itemView.context.getColor(android.R.color.black)
        }
        holder.tvMontoGasto.setTextColor(colorMonto)

        // Cargar icono de categoría
        cargarIconoCategoria(holder, item.categoria)

        // Click listener normal
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        // NUEVO: Botón eliminar
        holder.btnEliminarGasto.setOnClickListener {
            onEliminarClick(item)
        }
    }

    override fun getItemCount() = transacciones.size

    fun actualizarDatos(nuevasTransacciones: List<TransaccionConDetalles>) {
        transacciones = nuevasTransacciones
        notifyDataSetChanged()
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = formatoEntrada.parse(fecha)
            formatoSalida.format(date!!)
        } catch (e: Exception) {
            fecha
        }
    }

    private fun cargarIconoCategoria(holder: GastoViewHolder, categoria: Categoria) {
        if (!categoria.rutaImagen.isNullOrEmpty()) {
            val file = File(categoria.rutaImagen)
            if (file.exists()) {
                holder.ivIconoCategoria.setImageURI(Uri.fromFile(file))
            } else {
                holder.ivIconoCategoria.setImageResource(obtenerRecursoIcono(categoria.icono))
            }
        } else {
            val iconoRes = obtenerRecursoIcono(categoria.icono)
            holder.ivIconoCategoria.setImageResource(iconoRes)
        }
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
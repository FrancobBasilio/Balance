package com.app.balance.adapters

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.R
import com.app.balance.model.Categoria
import com.app.balance.model.TransaccionConDetalles
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class GastosAdapter(
    private var transacciones: List<TransaccionConDetalles>,
    private val codigoDivisa: String,
    private val onItemClick: (TransaccionConDetalles) -> Unit,
    private val onEliminarClick: (TransaccionConDetalles) -> Unit
) : RecyclerView.Adapter<GastosAdapter.GastoViewHolder>() {

    class GastoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIconoCategoria: ImageView = view.findViewById(R.id.ivIconoCategoria)
        val tvNombreCategoria: TextView = view.findViewById(R.id.tvNombreCategoria)
        val tvTipoCategoria: TextView = view.findViewById(R.id.tvTipoCategoria)
        val tvComentario: TextView = view.findViewById(R.id.tvComentario)
        val tvMontoGasto: TextView = view.findViewById(R.id.tvMontoGasto)
        val tvFechaGasto: TextView = view.findViewById(R.id.tvFechaGasto)
        val btnEliminarGasto: MaterialButton = view.findViewById(R.id.btnEliminarGasto)
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
            "Necesidad" -> context.getColor(android.R.color.holo_red_dark)
            "Deseo" -> context.getColor(android.R.color.holo_orange_dark)
            "Ahorro" -> context.getColor(android.R.color.holo_green_dark)
            else -> context.getColor(android.R.color.black)
        }
        holder.tvMontoGasto.setTextColor(colorMonto)

        cargarIconoCategoria(holder, item.categoria, context)

        // Click en la imagen para ver en tamaño completo
        holder.ivIconoCategoria.setOnClickListener {
            if (!item.categoria.rutaImagen.isNullOrEmpty()) {
                mostrarImagenCompleta(context, item.categoria.rutaImagen)
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        // Botón eliminar
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

    // Cargar icono circular con Glide
    private fun cargarIconoCategoria(holder: GastoViewHolder, categoria: Categoria, context: Context) {
        if (!categoria.rutaImagen.isNullOrEmpty()) {
            val file = File(categoria.rutaImagen)
            if (file.exists()) {
                holder.ivIconoCategoria.background = null
                holder.ivIconoCategoria.clearColorFilter()

                Glide.with(context)
                    .load(file)
                    .circleCrop()
                    .error(R.drawable.ic_default)
                    .into(holder.ivIconoCategoria)

                holder.ivIconoCategoria.isClickable = true
                holder.ivIconoCategoria.isFocusable = true
            } else {
                cargarIconoPredeterminado(holder, categoria)
            }
        } else {
            cargarIconoPredeterminado(holder, categoria)
        }
    }

    //  Cargar icono predeterminado
    private fun cargarIconoPredeterminado(holder: GastoViewHolder, categoria: Categoria) {
        val context = holder.itemView.context
        val iconoRes = obtenerRecursoIcono(categoria.icono)

        holder.ivIconoCategoria.setImageResource(iconoRes)

        holder.ivIconoCategoria.setBackgroundResource(R.drawable.fondo_circular_solido)


        val colorIcono = categoria.color ?: android.R.color.black
        holder.ivIconoCategoria.setColorFilter(
            context.getColor(colorIcono),
            PorterDuff.Mode.SRC_IN
        )
        holder.ivIconoCategoria.isClickable = false
        holder.ivIconoCategoria.isFocusable = false
    }


    //  Mostrar imagen en tamaño completo
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

        Glide.with(context)
            .load(file)
            .centerInside()
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
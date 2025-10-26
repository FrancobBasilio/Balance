package com.app.balance.adapters

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

        // ✅ Cargar icono de categoría circular
        cargarIconoCategoria(holder, item.categoria, context)

        // ✅ Click en la imagen para ver en tamaño completo
        holder.ivIconoCategoria.setOnClickListener {
            if (!item.categoria.rutaImagen.isNullOrEmpty()) {
                mostrarImagenCompleta(context, item.categoria.rutaImagen)
            }
        }

        // Click listener normal
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

    // ✅ ACTUALIZADO: Cargar icono circular con Glide
    private fun cargarIconoCategoria(holder: GastoViewHolder, categoria: Categoria, context: Context) {
        if (!categoria.rutaImagen.isNullOrEmpty()) {
            val file = File(categoria.rutaImagen)
            if (file.exists()) {
                // Cargar imagen personalizada circular
                Glide.with(context)
                    .load(file)
                    .circleCrop()
                    .into(holder.ivIconoCategoria)

                // Hacer la imagen clickeable
                holder.ivIconoCategoria.isClickable = true
                holder.ivIconoCategoria.isFocusable = true
            } else {
                // Si el archivo no existe, cargar icono por defecto
                cargarIconoPredeterminado(holder, categoria) // ✅ Pasar categoria completa
            }
        } else {
            // Cargar icono predeterminado
            cargarIconoPredeterminado(holder, categoria) // ✅ Pasar categoria completa
        }
    }

    //  NUEVA FUNCIÓN: Cargar icono predeterminado
    //  ACTUALIZADO: Usar el color guardado en la categoría
    private fun cargarIconoPredeterminado(holder: GastoViewHolder, categoria: Categoria) {
        val context = holder.itemView.context
        val iconoRes = obtenerRecursoIcono(categoria.icono)

        holder.ivIconoCategoria.setImageResource(iconoRes)
        // Fondo circular blanco
        holder.ivIconoCategoria.setBackgroundResource(R.drawable.fondo_circular_solido)

        //  Usar el color guardado o negro por defecto
        val colorIcono = categoria.color ?: android.R.color.black
        holder.ivIconoCategoria.setColorFilter(
            context.getColor(colorIcono),
            PorterDuff.Mode.SRC_IN
        )
        holder.ivIconoCategoria.isClickable = false
        holder.ivIconoCategoria.isFocusable = false
    }

    //  NUEVA FUNCIÓN: Mostrar imagen en tamaño completo
    private fun mostrarImagenCompleta(context: Context, rutaImagen: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_imagen_completa, null)
        val ivImagenCompleta = dialogView.findViewById<ImageView>(R.id.ivImagenCompleta)
        val btnCerrar = dialogView.findViewById<ImageButton>(R.id.btnCerrarImagen)

        //  Cargar imagen en ALTA CALIDAD
        Glide.with(context)
            .load(File(rutaImagen))
            .override(2048, 2048) // Tamaño máximo para evitar OutOfMemory
            .fitCenter()
            .into(ivImagenCompleta)

        // Crear diálogo en pantalla completa
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        // Hacer el diálogo pantalla completa
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)

        // Cerrar al hacer click en la imagen o el botón
        ivImagenCompleta.setOnClickListener {
            dialog.dismiss()
        }

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

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
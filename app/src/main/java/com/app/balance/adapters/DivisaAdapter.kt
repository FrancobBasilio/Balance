package com.app.balance.adapters


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.R
import com.app.balance.model.Divisa
import com.bumptech.glide.Glide

class DivisaAdapter(
    private var divisas: List<Divisa>,
    private val onDivisaClick: (Divisa) -> Unit
) : RecyclerView.Adapter<DivisaAdapter.DivisaViewHolder>() {

    private var divisasFiltradas = divisas.toList()
    private var seleccionadoPosition = -1

    class DivisaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreDivisa)
        val tvCodigo: TextView = view.findViewById(R.id.tvCodigoDivisa)
        val ivBandera: ImageView = view.findViewById(R.id.ivBanderaDivisa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DivisaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_divisa, parent, false)
        return DivisaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DivisaViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val divisa = divisasFiltradas[position]
        holder.tvNombre.text = divisa.nombre
        holder.tvCodigo.text = divisa.codigo

        if (!divisa.bandera.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(divisa.bandera)
                .centerCrop()
                .placeholder(android.R.drawable.ic_dialog_map)
                .error(android.R.drawable.ic_dialog_map)
                .into(holder.ivBandera)
        }

        if (position == seleccionadoPosition) {
            holder.itemView.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.darker_gray)
            )
        } else {
            holder.itemView.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.white)
            )
        }

        holder.itemView.setOnClickListener {
            val posicionAnterior = seleccionadoPosition
            seleccionadoPosition = position

            if (posicionAnterior != -1 && posicionAnterior < divisasFiltradas.size) {
                notifyItemChanged(posicionAnterior)
            }
            notifyItemChanged(position)

            onDivisaClick(divisa)
        }
    }

    override fun getItemCount() = divisasFiltradas.size

    fun filtrar(query: String) {
        divisasFiltradas = if (query.isEmpty()) {
            divisas
        } else {
            divisas.filter { divisa ->
                divisa.nombre.contains(query, ignoreCase = true) ||
                        divisa.codigo.contains(query, ignoreCase = true)
            }
        }
        seleccionadoPosition = -1
        notifyDataSetChanged()
    }

    fun actualizarDatos(nuevosDivisas: List<Divisa>) {
        divisas = nuevosDivisas
        divisasFiltradas = nuevosDivisas
        seleccionadoPosition = -1
        notifyDataSetChanged()
    }

    fun getDivisaSeleccionada(): Divisa? {
        return if (seleccionadoPosition >= 0 && seleccionadoPosition < divisasFiltradas.size) {
            divisasFiltradas[seleccionadoPosition]
        } else {
            null
        }
    }
}
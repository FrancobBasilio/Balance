package com.app.balance.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.app.balance.R
import com.app.balance.model.CountryCode
import com.bumptech.glide.Glide


class CountryCodeAdapter(
    context: Context,
    countries: List<CountryCode>
) : ArrayAdapter<CountryCode>(context, 0, countries) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_country_spinner, parent, false)

        val country = getItem(position)
        if (country != null) {
            val tvCountry = view.findViewById<TextView>(R.id.tvCountryName)
            val tvCode = view.findViewById<TextView>(R.id.tvCountryCode)
            val ivFlag = view.findViewById<ImageView>(R.id.ivFlag)

            tvCountry.text = country.nombre
            tvCode.text = country.codigo

            // Cargar imagen con Glide
            if (!country.bandera.isNullOrEmpty()) {
                Glide.with(context)
                    .load(country.bandera)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_dialog_map)
                    .error(android.R.drawable.ic_dialog_map)
                    .into(ivFlag)
            }
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_country_dropdown, parent, false)

        val country = getItem(position)
        if (country != null) {
            val tvCountry = view.findViewById<TextView>(R.id.tvCountryName)
            val tvCode = view.findViewById<TextView>(R.id.tvCountryCode)
            val ivFlag = view.findViewById<ImageView>(R.id.ivFlag)

            tvCountry.text = country.nombre
            tvCode.text = country.codigo

            // Cargar imagen con Glide
            if (!country.bandera.isNullOrEmpty()) {
                Glide.with(context)
                    .load(country.bandera)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_dialog_map)
                    .error(android.R.drawable.ic_dialog_map)
                    .into(ivFlag)
            }
        }

        return view
    }
}
package com.app.balance.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.app.balance.R

class ConfiguracionFragment : Fragment() {

    private lateinit var swNotificaciones: Switch
    private lateinit var swTemaOscuro: Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_configuracion, container, false)

        initViews(view)
        cargarPreferencias()
        setupListeners()

        return view
    }

    private fun initViews(view: View) {
        swNotificaciones = view.findViewById(R.id.swNotificaciones)
        swTemaOscuro = view.findViewById(R.id.swTemaOscuro)
    }

    private fun cargarPreferencias() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Cargar estado de notificaciones
        swNotificaciones.isChecked = prefs.getBoolean("NOTIFICACIONES_ACTIVAS", true)

        // Cargar estado del tema oscuro
        val modoOscuro = prefs.getBoolean("TEMA_OSCURO", false)
        swTemaOscuro.isChecked = modoOscuro
    }

    private fun setupListeners() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Listener para notificaciones
        swNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean("NOTIFICACIONES_ACTIVAS", isChecked)
                .apply()

            Toast.makeText(
                requireContext(),
                if (isChecked) "Notificaciones activadas" else "Notificaciones desactivadas",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Listener para tema oscuro
        swTemaOscuro.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean("TEMA_OSCURO", isChecked)
                .apply()

            // Aplicar el tema
            aplicarTema(isChecked)

            Toast.makeText(
                requireContext(),
                if (isChecked) "Modo oscuro activado" else "Modo claro activado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun aplicarTema(modoOscuro: Boolean) {
        if (modoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // Recrear la actividad para aplicar el tema
        requireActivity().recreate()
    }
}
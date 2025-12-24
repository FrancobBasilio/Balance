package com.app.balance.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.app.balance.R
import com.google.android.material.materialswitch.MaterialSwitch

class ConfiguracionFragment : Fragment() {

    private lateinit var swNotificaciones: MaterialSwitch
    private lateinit var swTemaOscuro: MaterialSwitch

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
        }
    }

    private fun aplicarTema(modoOscuro: Boolean) {
        val nuevoModo = if (modoOscuro) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        
        // Solo recrear si el modo actual es diferente
        if (AppCompatDelegate.getDefaultNightMode() != nuevoModo) {
            AppCompatDelegate.setDefaultNightMode(nuevoModo)
            
            // Mostrar mensaje
            Toast.makeText(
                requireContext(),
                if (modoOscuro) "Modo oscuro activado" else "Modo claro activado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        /**
         * Aplica el tema guardado en las preferencias.
         * Llamar desde Application o SplashActivity.
         */
        fun aplicarTemaGuardado(context: Context) {
            val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val modoOscuro = prefs.getBoolean("TEMA_OSCURO", false)
            
            AppCompatDelegate.setDefaultNightMode(
                if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}

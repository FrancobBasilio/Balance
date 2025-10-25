package com.app.balance.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.balance.CrearCategoriaActivity
import com.app.balance.R
import com.app.balance.TransaccionGastoActivity
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.data.dao.UsuarioDAO
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class IngresosFragment : Fragment(R.layout.fragment_ingresos), BalanceUpdateListener {

    private lateinit var progressCircular: ProgressCircular
    private lateinit var btnAgregarGasto: MaterialButton
    private lateinit var fabCrearCategoria: FloatingActionButton

    private var balanceOriginal = 0.0
    private var balanceActual = 0.0
    private var usuarioId = 0
    private var codigoDivisa = "PEN"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressCircular = view.findViewById(R.id.progressCircular)
        btnAgregarGasto = view.findViewById(R.id.btnAgregarGasto)
        fabCrearCategoria = view.findViewById(R.id.fabCrearCategoria)

        obtenerDatos()
        actualizarGrafico()

        btnAgregarGasto.setOnClickListener {
            val intent = Intent(requireContext(), TransaccionGastoActivity::class.java)
            startActivity(intent)
        }

        fabCrearCategoria.setOnClickListener {
            val intent = Intent(requireContext(), CrearCategoriaActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onBalanceUpdated(nuevoBalance: Double, codigoDivisa: String) {
        this.codigoDivisa = codigoDivisa
        obtenerDatos()
        actualizarGrafico()
    }

    private fun verificarYCargarDivisaDesdeDB() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        var codigoDivisaActual = prefs.getString("DIVISA_CODIGO", "")

        // Si no hay divisa en SharedPreferences, cargarla desde BD
        if (codigoDivisaActual.isNullOrEmpty() || codigoDivisaActual == "PEN") {
            val userId = prefs.getInt("USER_ID", 0)

            if (userId > 0) {
                val dbHelper = AppDatabaseHelper(requireContext())
                val db = dbHelper.readableDatabase
                val usuarioDAO = UsuarioDAO(db, dbHelper)
                val divisaDAO = DivisaDAO(db, dbHelper)

                val usuario = usuarioDAO.obtenerUsuarioPorId(userId)

                if (usuario != null) {
                    val divisa = divisaDAO.obtenerDivisaPorId(usuario.divisaId)

                    if (divisa != null) {
                        // Restaurar divisa en SharedPreferences
                        prefs.edit()
                            .putInt("DIVISA_ID", divisa.id)
                            .putString("DIVISA_CODIGO", divisa.codigo)
                            .putString("DIVISA_NOMBRE", divisa.nombre)
                            .putString("DIVISA_BANDERA", divisa.bandera)
                            .apply()

                        // Actualizar variable local
                        codigoDivisa = divisa.codigo
                    }
                }

                db.close()
            }
        } else {
            codigoDivisa = codigoDivisaActual
        }
    }

    private fun obtenerDatos() {
        verificarYCargarDivisaDesdeDB() // ← AGREGAR ESTA LÍNEA PRIMERO

        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        balanceOriginal = prefs.getString("BALANCE_ORIGINAL", "0.00")?.toDoubleOrNull() ?: 0.0
        balanceActual = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0
        codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN" // Ya estará actualizado
        usuarioId = prefs.getInt("USER_ID", 0)
    }

    private fun actualizarGrafico() {
        // CÁLCULO: Porcentaje basado en lo que QUEDA respecto al ORIGINAL
        // Ejemplo: Balance Original = 100 ALL, Balance Actual = 50 ALL
        // Porcentaje de ahorro disponible = (50 / 100) * 100 = 50%

        val porcentajeAhorroDisponible = if (balanceOriginal > 0) {
            ((balanceActual / balanceOriginal) * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }

        val color = obtenerColorSegunAhorro(porcentajeAhorroDisponible)
        progressCircular.setValues(porcentajeAhorroDisponible, color)
    }

    private fun obtenerColorSegunAhorro(porcentajeAhorro: Float): Int {
        return when {
            porcentajeAhorro >= 30f -> requireContext().getColor(android.R.color.holo_blue_light)
            porcentajeAhorro >= 20f -> interpolarColor(
                requireContext().getColor(android.R.color.holo_blue_light),
                requireContext().getColor(android.R.color.holo_green_light),
                (30f - porcentajeAhorro) / 10f
            )
            porcentajeAhorro >= 15f -> interpolarColor(
                requireContext().getColor(android.R.color.holo_green_light),
                0xFFFFA500.toInt(),
                (20f - porcentajeAhorro) / 5f
            )
            porcentajeAhorro >= 10f -> interpolarColor(
                0xFFFFA500.toInt(),
                0xFFFF6347.toInt(),
                (15f - porcentajeAhorro) / 5f
            )
            porcentajeAhorro >= 5f -> interpolarColor(
                0xFFFF6347.toInt(),
                requireContext().getColor(android.R.color.holo_red_light),
                (10f - porcentajeAhorro) / 5f
            )
            porcentajeAhorro > 0f -> interpolarColor(
                requireContext().getColor(android.R.color.holo_red_light),
                requireContext().getColor(android.R.color.holo_red_dark),
                (5f - porcentajeAhorro) / 5f
            )
            else -> requireContext().getColor(android.R.color.holo_red_dark)
        }
    }

    private fun interpolarColor(colorInicio: Int, colorFin: Int, progreso: Float): Int {
        val progresoNormalizado = progreso.coerceIn(0f, 1f)

        val a1 = (colorInicio shr 24) and 0xff
        val r1 = (colorInicio shr 16) and 0xff
        val g1 = (colorInicio shr 8) and 0xff
        val b1 = colorInicio and 0xff

        val a2 = (colorFin shr 24) and 0xff
        val r2 = (colorFin shr 16) and 0xff
        val g2 = (colorFin shr 8) and 0xff
        val b2 = colorFin and 0xff

        val a = (a1 + (a2 - a1) * progresoNormalizado).toInt()
        val r = (r1 + (r2 - r1) * progresoNormalizado).toInt()
        val g = (g1 + (g2 - g1) * progresoNormalizado).toInt()
        val b = (b1 + (b2 - b1) * progresoNormalizado).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun onResume() {
        super.onResume()
        obtenerDatos()
        actualizarGrafico()
    }
}
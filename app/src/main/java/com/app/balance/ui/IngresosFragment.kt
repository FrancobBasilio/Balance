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
        verificarYCargarDivisaDesdeDB()

        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        balanceOriginal = prefs.getString("BALANCE_ORIGINAL", "0.00")?.toDoubleOrNull() ?: 0.0
        balanceActual = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0
        codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN" // Ya estará actualizado
        usuarioId = prefs.getInt("USER_ID", 0)
    }

    private fun actualizarGrafico() {
        // CÁLCULO: Porcentaje basado en lo que queda respecto al original
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
            porcentajeAhorro >= 70f -> {
                val progress = (100f - porcentajeAhorro) / 30f
                interpolarColor(
                    0xFF2196F3.toInt(),
                    0xFF00BCD4.toInt(),
                    progress
                )
            }

            porcentajeAhorro >= 50f -> {
                val progress = (70f - porcentajeAhorro) / 20f
                interpolarColor(
                    0xFF00BCD4.toInt(),
                    0xFF4CAF50.toInt(),
                    progress
                )
            }

            porcentajeAhorro >= 30f -> {
                val progress = (50f - porcentajeAhorro) / 20f
                interpolarColor(
                    0xFF4CAF50.toInt(),
                    0xFF8BC34A.toInt(),
                    progress
                )
            }

            porcentajeAhorro >= 20f -> {
                val progress = (30f - porcentajeAhorro) / 10f
                interpolarColor(
                    0xFF8BC34A.toInt(),
                    0xFFFFC107.toInt(),
                    progress
                )
            }

            porcentajeAhorro >= 10f -> {
                val progress = (20f - porcentajeAhorro) / 10f
                interpolarColor(
                    0xFFFFC107.toInt(),
                    0xFFFF9800.toInt(),
                    progress
                )
            }

            porcentajeAhorro >= 5f -> {
                val progress = (10f - porcentajeAhorro) / 5f
                interpolarColor(
                    0xFFFF9800.toInt(),
                    0xFFF44336.toInt(),
                    progress
                )
            }

            porcentajeAhorro > 0f -> {
                val progress = (5f - porcentajeAhorro) / 5f
                interpolarColor(
                    0xFFF44336.toInt(),
                    0xFFD32F2F.toInt(),
                    progress
                )
            }

            else -> 0xFFB71C1C.toInt()
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
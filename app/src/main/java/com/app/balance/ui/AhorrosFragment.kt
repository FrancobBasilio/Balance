package com.app.balance.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.balance.R
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.model.TransaccionConDetalles


class AhorrosFragment : Fragment(R.layout.fragment_ahorro), BalanceUpdateListener {

    private lateinit var progressNecesidad: ProgressBar
    private lateinit var progressDeseo: ProgressBar
    private lateinit var progressAhorro: ProgressBar

    private lateinit var tvMontoNecesidad: TextView
    private lateinit var tvMontoDeseo: TextView
    private lateinit var tvMontoAhorro: TextView

    private lateinit var tvPorcentajeNecesidad: TextView
    private lateinit var tvPorcentajeDeseo: TextView
    private lateinit var tvPorcentajeAhorro: TextView

    private lateinit var tvTotalGastado: TextView
    private lateinit var tvIngresosTotales: TextView

    private var balanceOriginal = 0.0
    private var usuarioId = 0
    private var codigoDivisa = "PEN"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        obtenerDatosUsuario()
        cargarTransacciones()
    }

    private fun initViews(view: View) {
        progressNecesidad = view.findViewById(R.id.progressNecesidad)
        progressDeseo = view.findViewById(R.id.progressDeseo)
        progressAhorro = view.findViewById(R.id.progressAhorro)

        tvMontoNecesidad = view.findViewById(R.id.tvMontoNecesidad)
        tvMontoDeseo = view.findViewById(R.id.tvMontoDeseo)
        tvMontoAhorro = view.findViewById(R.id.tvMontoAhorro)

        tvPorcentajeNecesidad = view.findViewById(R.id.tvPorcentajeNecesidad)
        tvPorcentajeDeseo = view.findViewById(R.id.tvPorcentajeDeseo)
        tvPorcentajeAhorro = view.findViewById(R.id.tvPorcentajeAhorro)

        tvTotalGastado = view.findViewById(R.id.tvTotalGastado)
        tvIngresosTotales = view.findViewById(R.id.tvIngresosTotales)
    }

    private fun obtenerDatosUsuario() {
        verificarYCargarDivisaDesdeDB()

        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        balanceOriginal = prefs.getString("BALANCE_ORIGINAL", "0.00")?.toDoubleOrNull() ?: 0.0
        codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN"
        usuarioId = prefs.getInt("USER_ID", 0)
    }

    override fun onBalanceUpdated(nuevoBalance: Double, codigoDivisa: String) {
        // Recargar datos desde SharedPreferences
        obtenerDatosUsuario()
        this.codigoDivisa = codigoDivisa
        cargarTransacciones()
    }
    private fun verificarYCargarDivisaDesdeDB() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val codigoDivisaActual = prefs.getString("DIVISA_CODIGO", "")

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

    private fun cargarTransacciones() {
        val dbHelper = AppDatabaseHelper(requireContext())
        val db = dbHelper.readableDatabase
        val transaccionDAO = TransaccionDAO(db, dbHelper)

        val transacciones = transaccionDAO.obtenerTransaccionesPorUsuario(usuarioId)
        calcularYActualizarVista(transacciones)

        db.close()
    }

    private fun calcularYActualizarVista(transacciones: List<TransaccionConDetalles>) {
        // Calcular gastos SOLO de Necesidad y Deseo
        var gastoNecesidad = 0.0
        var gastoDeseo = 0.0

        transacciones.forEach { transaccion ->
            when (transaccion.tipoCategoria.nombre) {
                "Necesidad" -> gastoNecesidad += transaccion.transaccion.monto
                "Deseo" -> gastoDeseo += transaccion.transaccion.monto
            }
        }

        // Total gastado (SOLO Necesidad + Deseo)
        val totalGastado = gastoNecesidad + gastoDeseo

        // AHORRO = Balance ORIGINAL - Total Gastado
        val ahorroDisponible = balanceOriginal - totalGastado

        // Calcular porcentaje de ahorro respecto al balance ORIGINAL
        val porcentajeAhorro = if (balanceOriginal > 0) {
            ((ahorroDisponible / balanceOriginal) * 100.0).toFloat()
        } else {
            0f
        }

        // Calcular los montos ideales según patrón 50/30/20 del BALANCE ORIGINAL
        val montoIdealNecesidad = balanceOriginal * 0.50
        val montoIdealDeseo = balanceOriginal * 0.30
        val montoIdealAhorro = balanceOriginal * 0.20

        // Calcular lo que queda disponible para gastar
        val disponibleNecesidad = (montoIdealNecesidad - gastoNecesidad).coerceAtLeast(0.0)
        val disponibleDeseo = (montoIdealDeseo - gastoDeseo).coerceAtLeast(0.0)

        // Calcular porcentajes gastados respecto al ideal
        val porcentajeGastadoNecesidad = if (montoIdealNecesidad > 0) {
            ((gastoNecesidad / montoIdealNecesidad) * 100).toInt().coerceIn(0, 100)
        } else 0

        val porcentajeGastadoDeseo = if (montoIdealDeseo > 0) {
            ((gastoDeseo / montoIdealDeseo) * 100).toInt().coerceIn(0, 100)
        } else 0

        // Porcentaje de cuanto del ahorro ideal se ha consumido
        val porcentajeAhorroConsumido = if (montoIdealAhorro > 0) {
            val deficitAhorro = (montoIdealAhorro - ahorroDisponible).coerceAtLeast(0.0)
            ((deficitAhorro / montoIdealAhorro) * 100).toInt().coerceIn(0, 100)
        } else 0

        // Actualizar UI - Montos disponibles
        tvMontoNecesidad.text = "$codigoDivisa ${String.format("%.2f", disponibleNecesidad)}"
        tvMontoDeseo.text = "$codigoDivisa ${String.format("%.2f", disponibleDeseo)}"
        tvMontoAhorro.text = "$codigoDivisa ${String.format("%.2f", ahorroDisponible)}"

        // Actualizar barras de progreso
        progressNecesidad.progress = porcentajeGastadoNecesidad
        progressDeseo.progress = porcentajeGastadoDeseo
        progressAhorro.progress = porcentajeAhorroConsumido

        // Actualizar textos
        tvPorcentajeNecesidad.text = "Gastado: $codigoDivisa ${String.format("%.2f", gastoNecesidad)} ($porcentajeGastadoNecesidad%)"
        tvPorcentajeDeseo.text = "Gastado: $codigoDivisa ${String.format("%.2f", gastoDeseo)} ($porcentajeGastadoDeseo%)"
        tvPorcentajeAhorro.text = "${String.format("%.1f", porcentajeAhorro)}% del monto total"

        // Total gastado e Ingresos
        tvTotalGastado.text = "$codigoDivisa ${String.format("%.2f", totalGastado)}"
        tvIngresosTotales.text = "$codigoDivisa ${String.format("%.2f", ahorroDisponible)}"

        // Cambiar color de las barras
        actualizarColoresProgreso(progressNecesidad, porcentajeGastadoNecesidad)
        actualizarColoresProgreso(progressDeseo, porcentajeGastadoDeseo)
        actualizarColoresProgreso(progressAhorro, porcentajeAhorroConsumido)
    }

    private fun actualizarColoresProgreso(progressBar: ProgressBar, porcentaje: Int) {
        val color = when {
            porcentaje < 50 -> requireContext().getColor(android.R.color.holo_green_light)
            porcentaje < 75 -> requireContext().getColor(android.R.color.holo_orange_light)
            else -> requireContext().getColor(android.R.color.holo_red_light)
        }
        progressBar.progressTintList = ColorStateList.valueOf(color)
    }


    override fun onResume() {
        super.onResume()
        obtenerDatosUsuario()
        cargarTransacciones()
    }
}
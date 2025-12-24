package com.app.balance.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.InicioActivity
import com.app.balance.R
import com.app.balance.TransaccionGastoActivity
import com.app.balance.adapters.GastosAdapter
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.model.FiltroTipo
import com.app.balance.model.TransaccionConDetalles
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GastosFragment : Fragment(R.layout.fragment_gastos), BalanceUpdateListener {

    private lateinit var rvGastos: RecyclerView
    private lateinit var adapter: GastosAdapter
    private lateinit var chipGroupFiltros: ChipGroup


    private var todasLasTransacciones = listOf<TransaccionConDetalles>()
    private var usuarioId = 0
    private var codigoDivisa = "PEN"
    private var balanceTotal = 0.0
    private var fechaDesde: String? = null
    private var fechaHasta: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        obtenerDatosUsuario()
        setupRecyclerView()
        cargarTransacciones()
        setupFiltros()
    }

    private fun initViews(view: View) {
        rvGastos = view.findViewById(R.id.rvGastos)
        chipGroupFiltros = view.findViewById(R.id.chipGroupFiltros)
    }

    private fun obtenerDatosUsuario() {
        verificarYCargarDivisaDesdeDB()

        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        usuarioId = prefs.getInt("USER_ID", 0)
        codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN"
        balanceTotal = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0
    }

    override fun onBalanceUpdated(nuevoBalance: Double, codigoDivisa: String) {
        balanceTotal = nuevoBalance
        this.codigoDivisa = codigoDivisa
        cargarTransacciones()
    }

    private fun setupRecyclerView() {
        adapter = GastosAdapter(
            emptyList(),
            codigoDivisa,
            onItemClick = { transaccion ->
                mostrarDetallesGasto(transaccion)
            },
            onEliminarClick = { transaccion ->
                mostrarDialogoEliminarGasto(transaccion)
            },
            onEditarClick = { transaccion ->
                // Lanzar Activity de edición pasando el id de la transacción
                val intent = Intent(requireContext(), TransaccionGastoActivity::class.java)
                intent.putExtra("EXTRA_EDITAR", true)
                intent.putExtra("EXTRA_TRANSACCION_ID", transaccion.transaccion.id)
                startActivity(intent)
            }
        )
        rvGastos.layoutManager = LinearLayoutManager(requireContext())
        rvGastos.adapter = adapter
    }

    private fun mostrarDetallesGasto(transaccion: TransaccionConDetalles) {
        val fechaFormateada = formatearFechaLegible(transaccion.transaccion.fecha)
        val mensaje = buildString {
            append("Categoría: ${transaccion.categoria.nombre}\n")
            append("Monto: $codigoDivisa ${String.format("%.2f", transaccion.transaccion.monto)}\n")
            append("Fecha: $fechaFormateada")
            if (!transaccion.transaccion.comentario.isNullOrEmpty()) {
                append("\nComentario: ${transaccion.transaccion.comentario}")
            }
        }

        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
    }

    private fun verificarYCargarDivisaDesdeDB() {
        val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val codigoDivisaActual = prefs.getString("DIVISA_CODIGO", "")

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
                        prefs.edit()
                            .putInt("DIVISA_ID", divisa.id)
                            .putString("DIVISA_CODIGO", divisa.codigo)
                            .putString("DIVISA_NOMBRE", divisa.nombre)
                            .putString("DIVISA_BANDERA", divisa.bandera)
                            .apply()

                        codigoDivisa = divisa.codigo
                    }
                }

                db.close()
            }
        } else {
            codigoDivisa = codigoDivisaActual
        }
    }

    private fun mostrarDialogoEliminarGasto(transaccion: TransaccionConDetalles) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar gasto")
            .setMessage("¿Deseas eliminar este gasto de $codigoDivisa ${String.format("%.2f", transaccion.transaccion.monto)}?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarGasto(transaccion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarGasto(transaccion: TransaccionConDetalles) {
        val dbHelper = AppDatabaseHelper(requireContext())
        val db = dbHelper.writableDatabase
        val transaccionDAO = TransaccionDAO(db, dbHelper)
        val usuarioDAO = UsuarioDAO(db, dbHelper)

        val resultado = transaccionDAO.eliminarTransaccion(transaccion.transaccion.id)

        if (resultado > 0) {
            val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val balanceActual = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0

            val montoDevuelto = transaccion.transaccion.monto
            val nuevoBalance = balanceActual + montoDevuelto

            prefs.edit()
                .putString("BALANCE_MONTO", nuevoBalance.toString())
                .apply()

            usuarioDAO.actualizarMontoTotal(usuarioId, nuevoBalance)

            balanceTotal = nuevoBalance

            (requireActivity() as? InicioActivity)?.recargarBalance()

            Toast.makeText(
                requireContext(),
                "Gasto eliminado. Se devolvió $codigoDivisa ${String.format("%.2f", montoDevuelto)}",
                Toast.LENGTH_SHORT
            ).show()

            cargarTransacciones()
        } else {
            Toast.makeText(
                requireContext(),
                "Error al eliminar el gasto",
                Toast.LENGTH_SHORT
            ).show()
        }

        db.close()
    }

    private fun cargarTransacciones() {
        val dbHelper = AppDatabaseHelper(requireContext())
        val db = dbHelper.readableDatabase
        val transaccionDAO = TransaccionDAO(db, dbHelper)

        todasLasTransacciones = transaccionDAO.obtenerTransaccionesPorUsuario(usuarioId)

        // Ordenar por fecha descendente (más reciente primero)
        todasLasTransacciones = todasLasTransacciones.sortedByDescending { it.transaccion.fecha }

        adapter.actualizarDatos(todasLasTransacciones)

        db.close()
    }

    private fun setupFiltros() {
        chipGroupFiltros.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipTodos -> filtrarTransacciones(FiltroTipo.TODOS)
                R.id.chipDia -> filtrarTransacciones(FiltroTipo.DIA)
                R.id.chipSemana -> filtrarTransacciones(FiltroTipo.SEMANA)
                R.id.chipMes -> filtrarTransacciones(FiltroTipo.MES)
                R.id.chipPersonalizado -> mostrarDialogoRangoFechas()
                else -> filtrarTransacciones(FiltroTipo.TODOS)
            }
        }
    }

    private fun mostrarDialogoRangoFechas() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialogo_rango_fechas, null)

        val layoutFechaDesde = dialogView.findViewById<LinearLayout>(R.id.layoutFechaDesde)
        val tvFechaDesde = dialogView.findViewById<TextView>(R.id.tvFechaDesde)
        val layoutFechaHasta = dialogView.findViewById<LinearLayout>(R.id.layoutFechaHasta)
        val tvFechaHasta = dialogView.findViewById<TextView>(R.id.tvFechaHasta)
        val btnAplicar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAplicarFiltro)
        val btnCancelar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelarFiltro)

        // Inicializar con fechas actuales
        if (fechaDesde != null && fechaHasta != null) {
            tvFechaDesde.text = formatearFecha(fechaDesde!!)
            tvFechaHasta.text = formatearFecha(fechaHasta!!)
        } else {
            // Establecer rango del mes actual
            fechaDesde = obtenerInicioMes()
            fechaHasta = obtenerFechaHoy()
            tvFechaDesde.text = formatearFecha(fechaDesde!!)
            tvFechaHasta.text = formatearFecha(fechaHasta!!)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Selector de fecha DESDE
        layoutFechaDesde.setOnClickListener {
            mostrarDatePickerParaRango(tvFechaDesde, true)
        }

        // Selector de fecha HASTA
        layoutFechaHasta.setOnClickListener {
            mostrarDatePickerParaRango(tvFechaHasta, false)
        }

        // Botón Aplicar

        btnAplicar.setOnClickListener {
            if (fechaDesde != null && fechaHasta != null) {
                if (fechaDesde!! > fechaHasta!!) {
                    Toast.makeText(
                        requireContext(),
                        "La fecha 'Desde' no puede ser mayor que 'Hasta'",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    filtrarPorRangoPersonalizado(fechaDesde!!, fechaHasta!!)
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Por favor selecciona ambas fechas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Botón Cancelar
        btnCancelar.setOnClickListener {
            chipGroupFiltros.check(R.id.chipTodos)
            fechaDesde = null
            fechaHasta = null
            dialog.dismiss()
        }

        dialog.show()
    }





    private fun mostrarDatePickerParaRango(textView: TextView, esDesde: Boolean) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

                if (esDesde) {
                    fechaDesde = fechaSeleccionada
                } else {
                    fechaHasta = fechaSeleccionada
                }

                textView.text = formatearFecha(fechaSeleccionada)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun filtrarPorRangoPersonalizado(desde: String, hasta: String) {
        val transaccionesFiltradas = todasLasTransacciones.filter {
            it.transaccion.fecha >= desde && it.transaccion.fecha <= hasta
        }

        val transaccionesOrdenadas = transaccionesFiltradas.sortedByDescending { it.transaccion.fecha }
        adapter.actualizarDatos(transaccionesOrdenadas)

        if (transaccionesOrdenadas.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No hay gastos en el rango seleccionado",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val total = transaccionesOrdenadas.sumOf { it.transaccion.monto }
            val desde = formatearFecha(fechaDesde!!)
            val hasta = formatearFecha(fechaHasta!!)
            Toast.makeText(
                requireContext(),
                "Rango: $desde - $hasta\n${transaccionesOrdenadas.size} gastos - Total: $codigoDivisa ${String.format("%.2f", total)}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun filtrarTransacciones(filtro: FiltroTipo) {
        if (filtro != FiltroTipo.TODOS) {
            fechaDesde = null
            fechaHasta = null
        }

        val transaccionesFiltradas = when (filtro) {
            FiltroTipo.TODOS -> todasLasTransacciones

            FiltroTipo.DIA -> {
                val hoy = obtenerFechaHoy()
                todasLasTransacciones.filter {
                    it.transaccion.fecha == hoy
                }
            }

            FiltroTipo.SEMANA -> {
                val inicioSemana = obtenerInicioSemana()
                val finSemana = obtenerFinSemana()
                todasLasTransacciones.filter {
                    it.transaccion.fecha >= inicioSemana && it.transaccion.fecha <= finSemana
                }
            }

            FiltroTipo.MES -> {
                val inicioMes = obtenerInicioMes()
                val finMes = obtenerFinMes()
                todasLasTransacciones.filter {
                    it.transaccion.fecha >= inicioMes && it.transaccion.fecha <= finMes
                }
            }
        }

        val transaccionesOrdenadas = transaccionesFiltradas.sortedByDescending { it.transaccion.fecha }
        adapter.actualizarDatos(transaccionesOrdenadas)

        if (transaccionesOrdenadas.isEmpty()) {
            val mensaje = when (filtro) {
                FiltroTipo.TODOS -> "No tienes gastos registrados"
                FiltroTipo.DIA -> "No tienes gastos hoy"
                FiltroTipo.SEMANA -> "No tienes gastos esta semana"
                FiltroTipo.MES -> "No tienes gastos este mes"
            }
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerFechaHoy(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(Date())
    }

    private fun obtenerInicioSemana(): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(calendar.time)
    }

    private fun obtenerFinSemana(): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(calendar.time)
    }

    private fun obtenerInicioMes(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(calendar.time)
    }

    private fun obtenerFinMes(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(calendar.time)
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

    private fun formatearFechaLegible(fecha: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
            val date = formatoEntrada.parse(fecha)
            formatoSalida.format(date!!)
        } catch (e: Exception) {
            fecha
        }
    }

    override fun onResume() {
        super.onResume()
        obtenerDatosUsuario()
        cargarTransacciones()
    }
}
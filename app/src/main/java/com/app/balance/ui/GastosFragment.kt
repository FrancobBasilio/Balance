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
import com.app.balance.model.TransaccionConDetalles
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GastosFragment : Fragment(R.layout.fragment_gastos), BalanceUpdateListener {

    private lateinit var rvGastos: RecyclerView
    private lateinit var adapter: GastosAdapter
    private lateinit var btnAgregarGasto: MaterialButton
    private lateinit var chipGroupFiltros: ChipGroup

    private var todasLasTransacciones = listOf<TransaccionConDetalles>()
    private var usuarioId = 0
    private var codigoDivisa = "PEN"
    private var balanceTotal = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        obtenerDatosUsuario()
        setupRecyclerView()
        cargarTransacciones()
        setupFiltros()
        setupBotonAgregarGasto()
    }

    private fun initViews(view: View) {
        rvGastos = view.findViewById(R.id.rvGastos)
        btnAgregarGasto = view.findViewById(R.id.btnAgregarGasto)
        chipGroupFiltros = view.findViewById(R.id.chipGroupFiltros)
    }

    private fun obtenerDatosUsuario() {
        verificarYCargarDivisaDesdeDB() // ← AGREGAR ESTA LÍNEA PRIMERO

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
            }
        )
        rvGastos.layoutManager = LinearLayoutManager(requireContext())
        rvGastos.adapter = adapter
    }

    private fun mostrarDetallesGasto(transaccion: TransaccionConDetalles) {
        Toast.makeText(
            requireContext(),
            "Gasto: ${transaccion.categoria.nombre} - ${transaccion.transaccion.comentario ?: "Sin comentario"}",
            Toast.LENGTH_SHORT
        ).show()
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

        // Eliminar la transacción de la BD
        val resultado = transaccionDAO.eliminarTransaccion(transaccion.transaccion.id)

        if (resultado > 0) {
            // Obtener balance actual
            val prefs = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val balanceActual = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0

            // INCREMENTAR el balance (devolver el dinero)
            val montoDevuelto = transaccion.transaccion.monto
            val nuevoBalance = balanceActual + montoDevuelto

            // Actualizar en SharedPreferences
            prefs.edit()
                .putString("BALANCE_MONTO", nuevoBalance.toString())
                .apply()

            // Actualizar en la base de datos
            usuarioDAO.actualizarMontoTotal(usuarioId, nuevoBalance)

            // Actualizar balance local
            balanceTotal = nuevoBalance

            // Notificar a InicioActivity para actualizar el header
            (requireActivity() as? InicioActivity)?.recargarBalance()

            Toast.makeText(
                requireContext(),
                "Gasto eliminado. Se devolvió $codigoDivisa ${String.format("%.2f", montoDevuelto)}",
                Toast.LENGTH_SHORT
            ).show()

            // Recargar transacciones
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
            }
        }
    }

    private fun filtrarTransacciones(filtro: FiltroTipo) {
        val transaccionesFiltradas = when (filtro) {
            FiltroTipo.TODOS -> todasLasTransacciones
            FiltroTipo.DIA -> {
                val hoy = obtenerFechaHoy()
                todasLasTransacciones.filter { it.transaccion.fecha == hoy }
            }
            FiltroTipo.SEMANA -> {
                val inicioSemana = obtenerInicioSemana()
                todasLasTransacciones.filter { it.transaccion.fecha >= inicioSemana }
            }
            FiltroTipo.MES -> {
                val inicioMes = obtenerInicioMes()
                todasLasTransacciones.filter { it.transaccion.fecha >= inicioMes }
            }
        }

        adapter.actualizarDatos(transaccionesFiltradas)
    }

    private fun setupBotonAgregarGasto() {
        btnAgregarGasto.setOnClickListener {
            val intent = Intent(requireContext(), TransaccionGastoActivity::class.java)
            startActivity(intent)
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
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(calendar.time)
    }

    private fun obtenerInicioMes(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(calendar.time)
    }

    override fun onResume() {
        super.onResume()
        obtenerDatosUsuario()
        cargarTransacciones()
    }

    enum class FiltroTipo {
        TODOS, DIA, SEMANA, MES
    }
}
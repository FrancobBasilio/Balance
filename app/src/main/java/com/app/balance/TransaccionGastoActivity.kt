package com.app.balance

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.adapters.CategoriaSeleccionAdapter
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.CategoriaDAO
import com.app.balance.data.dao.TipoCategoriaDAO
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.model.Categoria
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransaccionGastoActivity : AppCompatActivity() {

    private lateinit var btnRegresar: ImageButton
    private lateinit var etMontoGasto: EditText
    private lateinit var tvDivisaGasto: TextView
    private lateinit var rvCategorias: RecyclerView
    private lateinit var layoutFecha: LinearLayout
    private lateinit var tvFechaSeleccionada: TextView
    private lateinit var etComentario: TextInputEditText
    private lateinit var btnAnadirGasto: MaterialButton

    private lateinit var adapter: CategoriaSeleccionAdapter
    private var fechaSeleccionada: String = ""
    private var usuarioId: Int = 0
    private var codigoDivisa: String = "PEN"

    // Edit mode
    private var esModoEditar: Boolean = false
    private var editarTransaccionId: Int = -1
    private var transaccionOriginal: com.app.balance.model.TransaccionConDetalles? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transaccion_gasto)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        obtenerDatosUsuario()
        initViews()
        setupRecyclerView()
        cargarCategoriasSistema()
        // Leer extras para modo edición
        esModoEditar = intent.getBooleanExtra("EXTRA_EDITAR", false)
        if (esModoEditar) {
            editarTransaccionId = intent.getIntExtra("EXTRA_TRANSACCION_ID", -1)
        }
        setupFechaPicker()
        setupBotonAnadir()
        setupBotonRegresar()

        establecerFechaActual()

        // Si venimos en modo edición, cargar la transacción y precargar campos
        if (esModoEditar && editarTransaccionId != -1) {
            precargarTransaccionParaEdicion(editarTransaccionId)
        }
    }

    private fun obtenerDatosUsuario() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        usuarioId = prefs.getInt("USER_ID", 0)
        codigoDivisa = prefs.getString("DIVISA_CODIGO", "PEN") ?: "PEN"
    }

    private fun initViews() {
        btnRegresar = findViewById(R.id.btnRegresar)
        etMontoGasto = findViewById(R.id.etMontoGasto)
        tvDivisaGasto = findViewById(R.id.tvDivisaGasto)
        rvCategorias = findViewById(R.id.rvCategorias)
        layoutFecha = findViewById(R.id.layoutFecha)
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada)
        etComentario = findViewById(R.id.etComentario)
        btnAnadirGasto = findViewById(R.id.btnAnadirGasto)

        tvDivisaGasto.text = codigoDivisa
    }

    private fun setupBotonRegresar() {
        btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoriaSeleccionAdapter(
            emptyList(),
            onCategoriaClick = { categoria ->
            },
            onCategoriaLongClick = { categoria ->
                mostrarDialogoEliminarCategoria(categoria)
            }
        )

        rvCategorias.layoutManager = GridLayoutManager(this, 3)
        rvCategorias.adapter = adapter
    }

    private fun cargarCategoriasSistema() {
        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val categoriaSistemaDAO = CategoriaDAO(db, dbHelper)

        val categorias = categoriaSistemaDAO.obtenerCategoriasSistemaPorUsuario(usuarioId)
        adapter.actualizarCategorias(categorias)

        db.close()
    }

    private fun mostrarDialogoEliminarCategoria(categoria: Categoria) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar categoría")
            .setMessage("¿Estás seguro de que deseas eliminar la categoría '${categoria.nombre}'?\n\nEsto NO afectará los gastos ya registrados con esta categoría.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarCategoriaSistema(categoria)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCategoriaSistema(categoria: Categoria) {
        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val categoriaSistemaDAO = CategoriaDAO(db, dbHelper)

        val resultado = categoriaSistemaDAO.eliminarCategoriaSistema(categoria.id)

        if (resultado > 0) {
            Toast.makeText(
                this,
                "Categoría '${categoria.nombre}' eliminada",
                Toast.LENGTH_SHORT
            ).show()
            cargarCategoriasSistema()
        } else {
            Toast.makeText(
                this,
                "Error al eliminar la categoría",
                Toast.LENGTH_SHORT
            ).show()
        }

        db.close()
    }

    private fun establecerFechaActual() {
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        fechaSeleccionada = fechaActual
        tvFechaSeleccionada.text = formatearFecha(fechaActual)
    }

    private fun setupFechaPicker() {
        layoutFecha.setOnClickListener {
            mostrarDatePicker()
        }
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                tvFechaSeleccionada.text = formatearFecha(fechaSeleccionada)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
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

    private fun setupBotonAnadir() {
        btnAnadirGasto.setOnClickListener {
            validarYGuardarTransaccion()
        }
    }

    private fun validarYGuardarTransaccion() {
        val montoTexto = etMontoGasto.text.toString().trim()
        val categoriaSeleccionada = adapter.getCategoriaSeleccionada()
        val comentario = etComentario.text.toString().trim()

        if (montoTexto.isEmpty() || montoTexto == "0") {
            Toast.makeText(this, "Por favor ingresa un monto válido", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = montoTexto.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            Toast.makeText(this, "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoriaSeleccionada == null) {
            Toast.makeText(this, "Por favor selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        if (fechaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        if (esModoEditar && transaccionOriginal != null) {
            actualizarTransaccion(transaccionOriginal!!, monto, categoriaSeleccionada, comentario)
        } else {
            guardarTransaccion(monto, categoriaSeleccionada, comentario)
        }
    }

    private fun guardarTransaccion(monto: Double, categoria: Categoria, comentario: String) {
        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val transaccionDAO = TransaccionDAO(db, dbHelper)
        val usuarioDAO = UsuarioDAO(db, dbHelper)
        val tipoCategoriaDAO = TipoCategoriaDAO(db, dbHelper)

        val tipoCategoria = tipoCategoriaDAO.obtenerTipoPorId(categoria.tipoCategoriaId)

        if (tipoCategoria == null) {
            Toast.makeText(this, "Error: Tipo de categoría no encontrado", Toast.LENGTH_SHORT).show()
            db.close()
            return
        }

        val resultado = transaccionDAO.insertarTransaccion(
            categoriaNombre = categoria.nombre,
            categoriaIcono = categoria.icono,
            categoriaRutaImagen = categoria.rutaImagen,
            categoriaColor = categoria.color,
            tipoCategoriaId = tipoCategoria.id,
            tipoCategoriaNombre = tipoCategoria.nombre,
            monto = monto,
            fecha = fechaSeleccionada,
            comentario = comentario.ifEmpty { null },
            usuarioId = usuarioId
        )

        if (resultado > 0) {
            actualizarBalanceDespuesDeGasto(monto, usuarioDAO)

            Toast.makeText(
                this,
                "Gasto de $codigoDivisa ${String.format("%.2f", monto)} añadido",
                Toast.LENGTH_SHORT
            ).show()

            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(
                this,
                "Error al añadir el gasto",
                Toast.LENGTH_SHORT
            ).show()
        }

        db.close()
    }

    private fun actualizarTransaccion(original: com.app.balance.model.TransaccionConDetalles, monto: Double, categoria: Categoria, comentario: String) {
        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val transaccionDAO = com.app.balance.data.dao.TransaccionDAO(db, dbHelper)
        val usuarioDAO = UsuarioDAO(db, dbHelper)

        val resultado = transaccionDAO.actualizarTransaccion(
            transaccionId = original.transaccion.id,
            categoriaNombre = categoria.nombre,
            categoriaIcono = categoria.icono,
            categoriaRutaImagen = categoria.rutaImagen,
            categoriaColor = categoria.color,
            tipoCategoriaId = categoria.tipoCategoriaId,
            tipoCategoriaNombre = categoria.tipoCategoriaId.let { id ->
                // obtener nombre tipo desde la categoria que usamos en UI
                val tipoDAO = TipoCategoriaDAO(db, dbHelper)
                tipoDAO.obtenerTipoPorId(categoria.tipoCategoriaId)?.nombre ?: ""
            },
            monto = monto,
            fecha = fechaSeleccionada,
            comentario = comentario.ifEmpty { null }
        )

        if (resultado > 0) {
            // Ajustar balance según diferencia
            val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val balanceActual = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0
            val montoOriginal = original.transaccion.monto
            val delta = monto - montoOriginal // si positivo, restar más; si negativo, devolver
            val nuevoBalance = balanceActual - delta

            prefs.edit()
                .putString("BALANCE_MONTO", nuevoBalance.toString())
                .apply()

            usuarioDAO.actualizarMontoTotal(usuarioId, nuevoBalance)

            Toast.makeText(this, "Gasto actualizado", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Error al actualizar el gasto", Toast.LENGTH_SHORT).show()
        }

        db.close()
    }

    private fun actualizarBalanceDespuesDeGasto(montoGasto: Double, usuarioDAO: UsuarioDAO) {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val balanceActual = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0
        val nuevoBalance = balanceActual - montoGasto

        prefs.edit()
            .putString("BALANCE_MONTO", nuevoBalance.toString())
            .apply()

        usuarioDAO.actualizarMontoTotal(usuarioId, nuevoBalance)
    }

    private fun precargarTransaccionParaEdicion(transaccionId: Int) {
        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val transaccionDAO = TransaccionDAO(db, dbHelper)
        val categoriaDAO = CategoriaDAO(db, dbHelper)

        val tx = transaccionDAO.obtenerTransaccionPorId(transaccionId)
        if (tx != null) {
            transaccionOriginal = tx

            // Precargar campos
            etMontoGasto.setText(tx.transaccion.monto.toString())
            fechaSeleccionada = tx.transaccion.fecha
            tvFechaSeleccionada.text = formatearFecha(fechaSeleccionada)
            etComentario.setText(tx.transaccion.comentario ?: "")

            // Cargar categorías y seleccionar la que coincida por nombre
            val categorias = categoriaDAO.obtenerCategoriasSistemaPorUsuario(usuarioId)
            adapter.actualizarCategorias(categorias)

            val match = categorias.find { it.nombre == tx.categoria.nombre }
            if (match != null) {
                adapter.setSelectedById(match.id)
            }

            // Cambiar texto del botón
            btnAnadirGasto.text = "Actualizar gasto"
        }

        db.close()
    }
}
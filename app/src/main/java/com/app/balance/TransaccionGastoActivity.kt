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
        cargarCategoriasSistema() // CAMBIO
        setupFechaPicker()
        setupBotonAnadir()
        setupBotonRegresar()

        establecerFechaActual()
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
                // Categoría seleccionada
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

        guardarTransaccion(monto, categoriaSeleccionada, comentario)
    }

    private fun guardarTransaccion(monto: Double, categoria: Categoria, comentario: String) {
        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val transaccionDAO = TransaccionDAO(db, dbHelper)
        val usuarioDAO = UsuarioDAO(db, dbHelper)
        val tipoCategoriaDAO = TipoCategoriaDAO(db, dbHelper)

        // Obtener el tipo de categoría
        val tipoCategoria = tipoCategoriaDAO.obtenerTipoPorId(categoria.tipoCategoriaId)

        if (tipoCategoria == null) {
            Toast.makeText(this, "Error: Tipo de categoría no encontrado", Toast.LENGTH_SHORT).show()
            db.close()
            return
        }

        // Insertar transacción con datos embebidos
        val resultado = transaccionDAO.insertarTransaccion(
            categoriaNombre = categoria.nombre,
            categoriaIcono = categoria.icono,
            categoriaRutaImagen = categoria.rutaImagen,
            tipoCategoriaId = tipoCategoria.id,
            tipoCategoriaNombre = tipoCategoria.nombre,
            monto = monto,
            fecha = fechaSeleccionada,
            comentario = comentario.ifEmpty { null },
            usuarioId = usuarioId
        )

        if (resultado > 0) {
            // Actualizar el balance
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

    private fun actualizarBalanceDespuesDeGasto(montoGasto: Double, usuarioDAO: UsuarioDAO) {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val balanceActual = prefs.getString("BALANCE_MONTO", "0.00")?.toDoubleOrNull() ?: 0.0
        val nuevoBalance = balanceActual - montoGasto

        prefs.edit()
            .putString("BALANCE_MONTO", nuevoBalance.toString())
            .apply()

        usuarioDAO.actualizarMontoTotal(usuarioId, nuevoBalance)
    }
}
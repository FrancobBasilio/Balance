package com.app.balance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import androidx.core.content.edit
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.model.Divisa
import com.app.balance.model.Usuario

class BalanceActivity : AppCompatActivity() {

    private lateinit var etMonto: EditText
    private lateinit var tvDivisaSeleccionada: TextView
    private lateinit var btnSiguienteBalance: MaterialButton
    private lateinit var tvDescription: TextView
    private lateinit var tvSubText: TextView

    private var codigoDivisa = ""
    private var nombreDivisa = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_balance)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        obtenerDivisaSeleccionada()
        setupListeners()
    }

    private fun initViews() {
        etMonto = findViewById(R.id.etMonto)
        tvDivisaSeleccionada = findViewById(R.id.divisaSeleccionada)
        btnSiguienteBalance = findViewById(R.id.btnSiguienteBalance)
        tvDescription = findViewById(R.id.tvDescription)
        tvSubText = findViewById(R.id.tvSubText)
    }

    private fun crearCuentaCompleta(montoInicial: Double) {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Verificar que estamos en proceso de registro
        val enProceso = prefs.getBoolean("REGISTRO_EN_PROCESO", false)
        if (!enProceso) {
            Toast.makeText(this, "Error: No hay datos de registro", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener datos temporales
        val nombre = prefs.getString("TEMP_NOMBRE", "") ?: ""
        val apellido = prefs.getString("TEMP_APELLIDO", "") ?: ""
        val fechaNac = prefs.getString("TEMP_FECHA_NAC", "") ?: ""
        val genero = prefs.getString("TEMP_GENERO", "") ?: ""
        val celular = prefs.getString("TEMP_CELULAR", "") ?: ""
        val email = prefs.getString("TEMP_EMAIL", "") ?: ""
        val contrasena = prefs.getString("TEMP_CONTRASENA", "") ?: ""

        val codigoDivisaTemp = prefs.getString("TEMP_DIVISA_CODIGO", "") ?: ""
        val nombreDivisaTemp = prefs.getString("TEMP_DIVISA_NOMBRE", "") ?: ""
        val banderaDivisaTemp = prefs.getString("TEMP_DIVISA_BANDERA", "") ?: ""

        val dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        val divisaDAO = DivisaDAO(db, dbHelper)
        val usuarioDAO = UsuarioDAO(db, dbHelper)

        try {
            // 1. Insertar o obtener divisa
            val divisaExistente = divisaDAO.obtenerDivisaPorCodigo(codigoDivisaTemp)
            var divisaId = divisaExistente?.id ?: 0

            if (divisaId == 0) {
                val nuevaDivisa = Divisa(
                    id = 0,
                    codigo = codigoDivisaTemp,
                    nombre = nombreDivisaTemp,
                    bandera = banderaDivisaTemp
                )
                divisaId = divisaDAO.insertarDivisa(nuevaDivisa).toInt()
            }

            // 2. Crear usuario COMPLETO
            val nuevoUsuario = Usuario(
                id = 0,
                nombre = nombre,
                apellido = apellido,
                fechaNacimiento = fechaNac,
                genero = genero,
                celular = celular,
                email = email,
                contrasena = contrasena,
                divisaId = divisaId,
                montoTotal = montoInicial
            )

            val resultado = usuarioDAO.insertarUsuario(nuevoUsuario)

            if (resultado > 0) {
                val usuarioInsertado = usuarioDAO.obtenerUsuarioPorEmail(email)

                if (usuarioInsertado != null) {
                    // 3. Guardar sesión COMPLETA en SharedPreferences
                    prefs.edit()
                        .clear() // Limpiar datos temporales
                        .putBoolean("SESION_ACTIVA", true)
                        .putBoolean("ES_PRIMERA_VEZ", false)
                        .putInt("USER_ID", usuarioInsertado.id)
                        .putString("USER_EMAIL", usuarioInsertado.email)
                        .putString("USER_NOMBRE", usuarioInsertado.nombre)
                        .putString("USER_APELLIDO", usuarioInsertado.apellido)
                        .putString("USER_CELULAR", usuarioInsertado.celular)
                        .putString("USER_FECHA_NAC", usuarioInsertado.fechaNacimiento)
                        .putString("USER_GENERO", usuarioInsertado.genero)
                        .putInt("DIVISA_ID", divisaId)
                        .putString("DIVISA_CODIGO", codigoDivisaTemp)
                        .putString("DIVISA_NOMBRE", nombreDivisaTemp)
                        .putString("DIVISA_BANDERA", banderaDivisaTemp)
                        .putString("BALANCE_MONTO", montoInicial.toString())
                        .putString("BALANCE_ORIGINAL", montoInicial.toString())
                        .putString("BALANCE_DIVISA", codigoDivisaTemp)
                        .apply()

                    Toast.makeText(this, "¡Cuenta creada exitosamente!", Toast.LENGTH_LONG).show()

                    db.close()
                    navigateToInicio()
                } else {
                    Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                    db.close()
                }
            } else {
                Toast.makeText(this, "Error al crear la cuenta", Toast.LENGTH_SHORT).show()
                db.close()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            db.close()
        }
    }

    private fun obtenerDivisaSeleccionada() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Obtener desde las claves TEMPORALES que guardamos en DivisaActivity
        codigoDivisa = prefs.getString("TEMP_DIVISA_CODIGO", "") ?: ""
        nombreDivisa = prefs.getString("TEMP_DIVISA_NOMBRE", "") ?: ""

        if (codigoDivisa.isNotEmpty() && nombreDivisa.isNotEmpty()) {
            tvDivisaSeleccionada.text = codigoDivisa
            tvDescription.text = "Ingresa tu saldo inicial"
            tvSubText.text = "en $nombreDivisa ($codigoDivisa)"
        } else {
            tvDivisaSeleccionada.text = "Divisa no seleccionada"
            Toast.makeText(this, "Error: No se pudo obtener la divisa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() == "0" && count > 1) {
                    etMonto.setText("")
                    etMonto.setSelection(0)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnSiguienteBalance.setOnClickListener {
            val monto = etMonto.text.toString().trim()

            if (monto.isEmpty() || monto.toDoubleOrNull() == null) {
                Toast.makeText(
                    this,
                    "Por favor ingresa un monto válido",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val montoDouble = monto.toDouble()

            if (montoDouble <= 0) {
                Toast.makeText(
                    this,
                    "El monto debe ser mayor a 0",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }


            crearCuentaCompleta(montoDouble)
        }
    }

    private fun navigateToInicio() {
        val intent = Intent(this, InicioActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
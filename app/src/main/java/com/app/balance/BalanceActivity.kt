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
import com.app.balance.data.dao.UsuarioDAO

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

    private fun obtenerDivisaSeleccionada() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        codigoDivisa = prefs.getString("DIVISA_CODIGO", "") ?: ""
        nombreDivisa = prefs.getString("DIVISA_NOMBRE", "") ?: ""

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

            // GUARDAR TANTO BALANCE_MONTO COMO BALANCE_ORIGINAL
            val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("BALANCE_MONTO", montoDouble.toString())
                .putString("BALANCE_ORIGINAL", montoDouble.toString()) // ← CRÍTICO: Guardar balance original
                .putString("BALANCE_DIVISA", codigoDivisa)
                .apply()

            // Actualizar en la base de datos
            val userId = prefs.getInt("USER_ID", 0)
            if (userId > 0) {
                val dbHelper = AppDatabaseHelper(this)
                val db = dbHelper.writableDatabase
                val usuarioDAO = UsuarioDAO(db, dbHelper)
                usuarioDAO.actualizarMontoTotal(userId, montoDouble)
                db.close()
            }

            Toast.makeText(
                this,
                "Balance guardado: $montoDouble $codigoDivisa",
                Toast.LENGTH_SHORT
            ).show()

            navigateToInicio()
        }
    }

    private fun navigateToInicio() {
        val intent = Intent(this, InicioActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
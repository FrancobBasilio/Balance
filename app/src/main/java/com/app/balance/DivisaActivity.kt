package com.app.balance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.balance.adapters.DivisaAdapter
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.model.CountryCode
import com.app.balance.model.Divisa
import com.app.balance.network.apiClient.PaisesApiClientDivisa
import com.app.balance.network.apiClient.PaisesApiClientRegistro
import com.app.balance.respondApi.repository.DivisaRepository
import com.app.balance.respondApi.repository.PaisRepositoryRegistro
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class DivisaActivity : AppCompatActivity() {

    private lateinit var etBuscar: EditText
    private lateinit var rvDivisas: RecyclerView
    private lateinit var btnSiguienteDivisa: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: DivisaAdapter
    private lateinit var repository: DivisaRepository

    private var todasLasDivisas = mutableListOf<Divisa>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_divisa)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupAdapter()
        setupRepository()
        setupSearchListener()
        setupBoton()

        cargarDivisas()
    }

    private fun initViews() {
        etBuscar = findViewById(R.id.etBuscar)
        rvDivisas = findViewById(R.id.rvDivisas)
        btnSiguienteDivisa = findViewById(R.id.btnSiguienteDivisa)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupAdapter() {
        adapter = DivisaAdapter(emptyList()) { divisa ->
            Toast.makeText(
                this,
                "Seleccionaste: ${divisa.nombre} (${divisa.codigo})",
                Toast.LENGTH_SHORT
            ).show()
        }
        rvDivisas.layoutManager = LinearLayoutManager(this)
        rvDivisas.adapter = adapter
    }

    private fun setupRepository() {
        val divisaService = PaisesApiClientDivisa.crearServicio()
        repository = DivisaRepository(divisaService)
    }

    private fun setupSearchListener() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filtrar(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBoton() {
        btnSiguienteDivisa.setOnClickListener {
            val divisaSeleccionada = adapter.getDivisaSeleccionada()
            if (divisaSeleccionada == null) {
                Toast.makeText(
                    this,
                    "Por favor selecciona una divisa",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val userId = prefs.getInt("USER_ID", 0)

            if (userId > 0) {
                val dbHelper = AppDatabaseHelper(this)
                val db = dbHelper.writableDatabase
                val divisaDAO = DivisaDAO(db, dbHelper)
                val usuarioDAO = UsuarioDAO(db, dbHelper)

                // Verificar si la divisa ya existe en la BD
                val divisaExistente = divisaDAO.obtenerDivisaPorCodigo(divisaSeleccionada.codigo)
                var divisaId = divisaExistente?.id ?: 0

                if (divisaId == 0) {
                    // Insertar divisa COMPLETA con bandera
                    divisaId = divisaDAO.insertarDivisa(divisaSeleccionada).toInt()
                }

                // Actualizar usuario con la divisa
                usuarioDAO.actualizarDivisaUsuario(userId, divisaId)

                // Guardar en SharedPreferences
                prefs.edit()
                    .putInt("DIVISA_ID", divisaId)
                    .putString("DIVISA_CODIGO", divisaSeleccionada.codigo)
                    .putString("DIVISA_NOMBRE", divisaSeleccionada.nombre)
                    .putString("DIVISA_BANDERA", divisaSeleccionada.bandera)
                    .apply()

                db.close()

                Toast.makeText(
                    this,
                    "Divisa guardada: ${divisaSeleccionada.nombre}",
                    Toast.LENGTH_SHORT
                ).show()

                navigateToBalance()
            } else {
                Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarDivisas() {
        progressBar.visibility = ProgressBar.VISIBLE
        lifecycleScope.launch {
            val resultado = repository.cargarDivisas()

            resultado.onSuccess { divisas ->
                todasLasDivisas.clear()
                todasLasDivisas.addAll(divisas)
                adapter.actualizarDatos(divisas)
                progressBar.visibility = ProgressBar.GONE

                Toast.makeText(
                    this@DivisaActivity,
                    "Se cargaron ${divisas.size} divisas",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(
                    this@DivisaActivity,
                    "Error al cargar: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun navigateToBalance() {
        val intent = Intent(this, BalanceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
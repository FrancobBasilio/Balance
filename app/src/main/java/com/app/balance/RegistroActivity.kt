package com.app.balance

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.balance.adapters.CountryCodeAdapter
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.model.CountryCode
import com.app.balance.model.Usuario
import com.app.balance.network.apiClient.PaisesApiClientRegistro
import com.app.balance.respondApi.PaisRepositoryRegistro
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Calendar

class RegistroActivity : AppCompatActivity() {

    private lateinit var tilCorreo: TextInputLayout
    private lateinit var tietCorreo: TextInputEditText
    private lateinit var tilNombre: TextInputLayout
    private lateinit var tietNombre: TextInputEditText
    private lateinit var tilApellido: TextInputLayout
    private lateinit var tietApellido: TextInputEditText
    private lateinit var tilAnio: TextInputLayout
    private lateinit var tietAnio: TextInputEditText
    private lateinit var tilMes: TextInputLayout
    private lateinit var tietMes: TextInputEditText
    private lateinit var tilDia: TextInputLayout
    private lateinit var tietDia: TextInputEditText
    private lateinit var chkHombre: CheckBox
    private lateinit var chkMujer: CheckBox
    private lateinit var chkOtro: CheckBox
    private lateinit var tilCelular: TextInputLayout
    private lateinit var tietCelular: TextInputEditText
    private lateinit var tilClave: TextInputLayout
    private lateinit var tietClave: TextInputEditText
    private lateinit var swTerms: SwitchMaterial
    private lateinit var btnRegister: MaterialButton
    private lateinit var spinnerCountry: Spinner

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var paisRepository: PaisRepositoryRegistro

    private var paises = mutableListOf<CountryCode>()
    private var selectedCountryCode = "+51"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        usuarioDAO = UsuarioDAO(db, dbHelper)

        val paisService = PaisesApiClientRegistro.crearServicio()
        paisRepository = PaisRepositoryRegistro(paisService)

        initViews()
        setupGenderCheckboxes()
        loadPaises()
        setupValidation()
        setupRegisterButton()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        tilCorreo = findViewById(R.id.tilCorreo)
        tietCorreo = findViewById(R.id.tietCorreo)
        tilNombre = findViewById(R.id.tilNombre)
        tietNombre = findViewById(R.id.tietNombre)
        tilApellido = findViewById(R.id.tilApellido)
        tietApellido = findViewById(R.id.tietApellido)
        tilAnio = findViewById(R.id.tilAnio)
        tietAnio = findViewById(R.id.tietAnio)
        tilMes = findViewById(R.id.tilMes)
        tietMes = findViewById(R.id.tietMes)
        tilDia = findViewById(R.id.tilDia)
        tietDia = findViewById(R.id.tietDia)
        chkHombre = findViewById(R.id.chkHombre)
        chkMujer = findViewById(R.id.chkMujer)
        chkOtro = findViewById(R.id.chkOtro)
        tilCelular = findViewById(R.id.tilCelular)
        tietCelular = findViewById(R.id.tietCelular)
        tilClave = findViewById(R.id.tilClave)
        tietClave = findViewById(R.id.tietClave)
        swTerms = findViewById(R.id.swTerms)
        btnRegister = findViewById(R.id.btnRegister)
        spinnerCountry = findViewById(R.id.spinnerCountry)

        tilCelular.prefixText = selectedCountryCode
    }

    private fun setupGenderCheckboxes() {
        chkHombre.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chkMujer.isChecked = false
                chkOtro.isChecked = false
            }
        }
        chkMujer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chkHombre.isChecked = false
                chkOtro.isChecked = false
            }
        }
        chkOtro.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chkHombre.isChecked = false
                chkMujer.isChecked = false
            }
        }
    }

    private fun loadPaises() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(this@RegistroActivity, "Cargando países...", Toast.LENGTH_SHORT).show()
                val resultado = paisRepository.cargarPaises()

                resultado.onSuccess { paisesCargados ->
                    paises = paisesCargados.toMutableList()
                    if (paises.isEmpty()) {
                        Toast.makeText(
                            this@RegistroActivity,
                            "No se cargaron países",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        setupCountrySpinner()
                        Toast.makeText(
                            this@RegistroActivity,
                            "Se cargaron ${paises.size} países",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(
                        this@RegistroActivity,
                        "Error al cargar países: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegistroActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupCountrySpinner() {
        val adapter = CountryCodeAdapter(this, paises)
        spinnerCountry.adapter = adapter

        val peruIndex = paises.indexOfFirst { it.codigo == "+51" }
        if (peruIndex != -1) {
            spinnerCountry.setSelection(peruIndex)
        }

        spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCountryCode = paises[position].codigo
                tilCelular.prefixText = selectedCountryCode
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupValidation() {
        tietCorreo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateCorreo() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        tietNombre.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateNombre() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        tietApellido.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateApellido() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dateWatcher = createDateTextWatcher()
        tietAnio.addTextChangedListener(dateWatcher)
        tietMes.addTextChangedListener(dateWatcher)
        tietDia.addTextChangedListener(dateWatcher)

        tietCelular.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateCelular() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        tietClave.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateClave() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun createDateTextWatcher(): TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { validateFecha() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun validateNombre(): Boolean {
        val nombre = tietNombre.text.toString().trim()
        return when {
            nombre.isEmpty() -> {
                tilNombre.error = "El nombre es obligatorio"
                false
            }
            nombre.length < 2 -> {
                tilNombre.error = "El nombre debe tener al menos 2 caracteres"
                false
            }
            !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")) -> {
                tilNombre.error = "El nombre solo debe contener letras"
                false
            }
            else -> {
                tilNombre.error = null
                true
            }
        }
    }

    private fun validateApellido(): Boolean {
        val apellido = tietApellido.text.toString().trim()
        return when {
            apellido.isEmpty() -> {
                tilApellido.error = "El apellido es obligatorio"
                false
            }
            apellido.length < 2 -> {
                tilApellido.error = "El apellido debe tener al menos 2 caracteres"
                false
            }
            !apellido.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")) -> {
                tilApellido.error = "El apellido solo debe contener letras"
                false
            }
            else -> {
                tilApellido.error = null
                true
            }
        }
    }

    private fun validateFecha(): Boolean {
        val anio = tietAnio.text.toString().trim()
        val mes = tietMes.text.toString().trim()
        val dia = tietDia.text.toString().trim()
        var isValid = true

        if (anio.isEmpty()) {
            tilAnio.error = "Año requerido"
            isValid = false
        } else {
            val anioInt = anio.toIntOrNull()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            when {
                anioInt == null -> {
                    tilAnio.error = "Año inválido"
                    isValid = false
                }
                anioInt < 1900 || anioInt > currentYear -> {
                    tilAnio.error = "Año debe estar entre 1900 y $currentYear"
                    isValid = false
                }
                else -> tilAnio.error = null
            }
        }

        if (mes.isEmpty()) {
            tilMes.error = "Mes requerido"
            isValid = false
        } else {
            val mesInt = mes.toIntOrNull()
            when {
                mesInt == null -> {
                    tilMes.error = "Mes inválido"
                    isValid = false
                }
                mesInt < 1 || mesInt > 12 -> {
                    tilMes.error = "Mes debe estar entre 1 y 12"
                    isValid = false
                }
                else -> tilMes.error = null
            }
        }

        if (dia.isEmpty()) {
            tilDia.error = "Día requerido"
            isValid = false
        } else {
            val diaInt = dia.toIntOrNull()
            when {
                diaInt == null -> {
                    tilDia.error = "Día inválido"
                    isValid = false
                }
                diaInt < 1 || diaInt > 31 -> {
                    tilDia.error = "Día debe estar entre 1 y 31"
                    isValid = false
                }
                else -> tilDia.error = null
            }
        }

        if (isValid && anio.isNotEmpty() && mes.isNotEmpty() && dia.isNotEmpty()) {
            try {
                val calendar = Calendar.getInstance()
                calendar.isLenient = false
                calendar.set(anio.toInt(), mes.toInt() - 1, dia.toInt())
                calendar.time
                val today = Calendar.getInstance()
                val age = today.get(Calendar.YEAR) - calendar.get(Calendar.YEAR)
                if (age < 18) {
                    tilAnio.error = "Debes tener al menos 18 años"
                    isValid = false
                }
            } catch (e: Exception) {
                tilDia.error = "Fecha inválida"
                isValid = false
            }
        }
        return isValid
    }

    private fun validateGenero(): Boolean {
        return if (!chkHombre.isChecked && !chkMujer.isChecked && !chkOtro.isChecked) {
            Toast.makeText(this, "Debe seleccionar un género", Toast.LENGTH_SHORT).show()
            false
        } else true
    }

    private fun validateCelular(): Boolean {
        val celular = tietCelular.text.toString().trim()
        return when {
            celular.isEmpty() -> {
                tilCelular.error = "El celular es obligatorio"
                false
            }
            !celular.matches(Regex("^[0-9]+$")) -> {
                tilCelular.error = "El celular solo debe contener números"
                false
            }
            celular.length < 7 || celular.length > 15 -> {
                tilCelular.error = "El celular debe tener entre 7 y 15 dígitos"
                false
            }
            else -> {
                tilCelular.error = null
                true
            }
        }
    }

    private fun validateClave(): Boolean {
        val clave = tietClave.text.toString()
        return when {
            clave.isEmpty() -> {
                tilClave.error = "La contraseña es obligatoria"
                false
            }
            clave.length < 6 -> {
                tilClave.error = "La contraseña debe tener al menos 6 caracteres"
                false
            }
            !clave.any { it.isDigit() } -> {
                tilClave.error = "La contraseña debe contener al menos un número"
                false
            }
            !clave.any { it.isUpperCase() } -> {
                tilClave.error = "La contraseña debe contener al menos una mayúscula"
                false
            }
            else -> {
                tilClave.error = null
                true
            }
        }
    }

    private fun validateCorreo(): Boolean {
        val correo = tietCorreo.text.toString().trim()
        return when {
            correo.isEmpty() -> {
                tilCorreo.error = "El correo es obligatorio"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                tilCorreo.error = "El correo no tiene un formato válido"
                false
            }
            else -> {
                tilCorreo.error = null
                true
            }
        }
    }

    private fun validateTerms(): Boolean {
        return if (!swTerms.isChecked) {
            Toast.makeText(this, "Debe aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
            false
        } else true
    }

    private fun setupRegisterButton() {
        btnRegister.setOnClickListener {
            val isCorreoValid = validateCorreo()
            val isNombreValid = validateNombre()
            val isApellidoValid = validateApellido()
            val isFechaValid = validateFecha()
            val isGeneroValid = validateGenero()
            val isCelularValid = validateCelular()
            val isClaveValid = validateClave()
            val isTermsValid = validateTerms()

            if (isCorreoValid && isNombreValid && isApellidoValid && isFechaValid &&
                isGeneroValid && isCelularValid && isClaveValid && isTermsValid
            ) {
                registrarUsuario()
            }
        }
    }

    private fun registrarUsuario() {
        val nombre = tietNombre.text?.toString()?.trim().orEmpty()
        val apellido = tietApellido.text?.toString()?.trim().orEmpty()

        val anio = tietAnio.text?.toString()?.trim().orEmpty()
        val mes = tietMes.text?.toString()?.trim().orEmpty()
        val dia = tietDia.text?.toString()?.trim().orEmpty()
        val fechaNacimiento = "$dia/$mes/$anio"

        val genero = when {
            chkHombre.isChecked -> "Masculino"
            chkMujer.isChecked -> "Femenino"
            chkOtro.isChecked -> "Otro"
            else -> ""
        }

        val celularSinCodigo = tietCelular.text?.toString()?.trim().orEmpty()
        val celular = "$selectedCountryCode$celularSinCodigo"
        val email = tietCorreo.text?.toString()?.trim().orEmpty()
        val contrasena = tietClave.text?.toString()?.trim().orEmpty()

        if (!validateCorreo() || !validateNombre() || !validateApellido() ||
            !validateFecha() || !validateGenero() || !validateCelular() ||
            !validateClave() || !validateTerms()) {
            Toast.makeText(this, "Por favor completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioExistente = usuarioDAO.obtenerUsuarioPorEmail(email)
        if (usuarioExistente != null) {
            tilCorreo.error = "Este correo ya está registrado"
            Toast.makeText(this, "Este correo ya está registrado", Toast.LENGTH_SHORT).show()
            return
        }

        val contrasenaHasheada = hashearContrasena(contrasena)

        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("REGISTRO_EN_PROCESO", true)
            .putString("TEMP_NOMBRE", nombre)
            .putString("TEMP_APELLIDO", apellido)
            .putString("TEMP_FECHA_NAC", fechaNacimiento)
            .putString("TEMP_GENERO", genero)
            .putString("TEMP_CELULAR", celular)
            .putString("TEMP_EMAIL", email)
            .putString("TEMP_CONTRASENA", contrasenaHasheada)
            .apply()

        Toast.makeText(this, "Datos guardados temporalmente", Toast.LENGTH_SHORT).show()


        val intent = Intent(this, DivisaActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    /**
     * Hashea una contraseña usando SHA-256
     */
    private fun hashearContrasena(contrasena: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(contrasena.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
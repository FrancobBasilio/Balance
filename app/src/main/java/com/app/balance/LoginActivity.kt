package com.app.balance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.dao.DivisaDAO
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.data.dao.UsuarioDAO
import com.app.balance.security.BiometricAuthManager
import com.app.balance.security.PasswordManager
import com.app.balance.security.SecurePreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilCorreo: TextInputLayout
    private lateinit var tietCorreo: TextInputEditText
    private lateinit var tilClave: TextInputLayout
    private lateinit var tietClave: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnBiometric: MaterialButton
    private lateinit var tvClaveOlvidada: TextView
    private lateinit var tvRegistro: TextView
    private lateinit var tvLockoutMessage: TextView

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var divisaDAO: DivisaDAO
    
    // Seguridad
    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var securePrefs: SecurePreferences
    
    private var lockoutTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = AppDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        usuarioDAO = UsuarioDAO(db, dbHelper)
        divisaDAO = DivisaDAO(db, dbHelper)
        
        // Inicializar seguridad
        biometricManager = BiometricAuthManager(this)
        securePrefs = SecurePreferences.getInstance(this)

        incializarVistas()
        configurandoListeners()
        verificarEstadoBloqueo()
        configurarBiometria()
    }

    private fun incializarVistas() {
        tilCorreo = findViewById(R.id.tilCorreo)
        tietCorreo = findViewById(R.id.tietCorreo)
        tilClave = findViewById(R.id.tilClave)
        tietClave = findViewById(R.id.tietClave)
        btnLogin = findViewById(R.id.btnLogin)
        tvClaveOlvidada = findViewById(R.id.tvClaveOlvidada)
        tvRegistro = findViewById(R.id.tvRegistro)
        
        // Nuevos elementos (agregar al layout)
        btnBiometric = findViewById(R.id.btnBiometric) ?: MaterialButton(this).also { 
            it.visibility = View.GONE 
        }
        tvLockoutMessage = findViewById(R.id.tvLockoutMessage) ?: TextView(this).also {
            it.visibility = View.GONE
        }
    }

    private fun configurandoListeners() {
        btnLogin.setOnClickListener { 
            if (!securePrefs.isLockedOut()) {
                validarCampos() 
            }
        }
        
        btnBiometric.setOnClickListener {
            iniciarLoginBiometrico()
        }
        
        tvRegistro.setOnClickListener { cambioActivity(RegistroActivity::class.java) }
        tvClaveOlvidada.setOnClickListener {
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        tietCorreo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilCorreo.error = null
        }
        tietClave.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilClave.error = null
        }
    }
    
    /**
     * Configura la opción de biometría si está disponible
     */
    private fun configurarBiometria() {
        val savedEmail = securePrefs.getUserEmail()
        val biometricEnabled = securePrefs.isBiometricEnabled()
        
        if (biometricManager.isBiometricAvailable() && biometricEnabled && savedEmail != null) {
            btnBiometric.visibility = View.VISIBLE
            tietCorreo.setText(savedEmail)
            
            // Auto-iniciar biometría si hay sesión previa
            if (securePrefs.getLastLogin() > 0) {
                iniciarLoginBiometrico()
            }
        } else {
            btnBiometric.visibility = View.GONE
        }
    }
    
    /**
     * Inicia el proceso de login biométrico
     */
    private fun iniciarLoginBiometrico() {
        val savedEmail = securePrefs.getUserEmail()
        
        if (savedEmail == null) {
            Toast.makeText(this, "Primero inicia sesión con tu contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Usar authenticateWithDeviceCredential para permitir huella, rostro o PIN
        biometricManager.authenticateWithDeviceCredential(
            activity = this,
            title = "Iniciar sesión en Balance+",
            subtitle = "Verifica tu identidad con huella, rostro o PIN"
        ) { result ->
            when (result) {
                is BiometricAuthManager.AuthResult.Success -> {
                    // Autenticación exitosa - cargar usuario
                    loginConEmail(savedEmail)
                }
                is BiometricAuthManager.AuthResult.Cancelled -> {
                    // Usuario canceló - mostrar formulario normal
                    Toast.makeText(this, "Usa tu contraseña para continuar", Toast.LENGTH_SHORT).show()
                }
                is BiometricAuthManager.AuthResult.Failed -> {
                    Toast.makeText(this, "Verificación fallida. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
                is BiometricAuthManager.AuthResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * Verifica si la cuenta está bloqueada por intentos fallidos
     */
    private fun verificarEstadoBloqueo() {
        if (securePrefs.isLockedOut()) {
            mostrarBloqueo()
        }
    }
    
    /**
     * Muestra el mensaje de bloqueo con countdown
     */
    private fun mostrarBloqueo() {
        btnLogin.isEnabled = false
        tietCorreo.isEnabled = false
        tietClave.isEnabled = false
        tvLockoutMessage.visibility = View.VISIBLE
        
        val remainingSeconds = securePrefs.getRemainingLockoutSeconds()
        
        lockoutTimer?.cancel()
        lockoutTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                val minutes = seconds / 60
                val secs = seconds % 60
                tvLockoutMessage.text = "Demasiados intentos. Espera ${minutes}:${String.format("%02d", secs)}"
            }
            
            override fun onFinish() {
                ocultarBloqueo()
            }
        }.start()
    }
    
    /**
     * Oculta el mensaje de bloqueo
     */
    private fun ocultarBloqueo() {
        btnLogin.isEnabled = true
        tietCorreo.isEnabled = true
        tietClave.isEnabled = true
        tvLockoutMessage.visibility = View.GONE
        securePrefs.resetLoginAttempts()
    }

    private fun validarCampos() {
        val correo = tietCorreo.text?.toString()?.trim().orEmpty()
        val clave = tietClave.text?.toString()?.trim().orEmpty()
        var error = false

        if (correo.isEmpty()) {
            tilCorreo.error = "Ingrese un correo"
            error = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.error = "Ingrese un correo válido"
            error = true
        } else {
            tilCorreo.error = null
        }

        if (clave.isEmpty()) {
            tilClave.error = "Ingrese una contraseña"
            error = true
        } else if (clave.length < 6) {
            tilClave.error = "La contraseña debe tener al menos 6 caracteres"
            error = true
        } else {
            tilClave.error = null
        }

        if (!error) iniciarSesion(correo, clave)
    }
    
    /**
     * Login con solo email (para biometría)
     */
    private fun loginConEmail(email: String) {
        try {
            val usuario = usuarioDAO.obtenerUsuarioPorEmail(email)
            
            if (usuario != null) {
                completarLogin(usuario)
            } else {
                Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                securePrefs.setBiometricEnabled(false)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarSesion(correo: String, clave: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "Verificando..."

        try {
            val usuario = usuarioDAO.obtenerUsuarioPorEmail(correo)

            if (usuario != null) {
                // Verificar contraseña con el nuevo sistema
                val passwordValid = PasswordManager.verifyPassword(clave, usuario.contrasena)

                if (passwordValid) {
                    // Resetear intentos fallidos
                    securePrefs.resetLoginAttempts()
                    
                    // Migrar hash si es legacy
                    if (PasswordManager.isLegacyHash(usuario.contrasena)) {
                        migrarContrasena(usuario.id, clave)
                    }
                    
                    // Preguntar si quiere habilitar biometría
                    if (biometricManager.isBiometricAvailable() && !securePrefs.isBiometricEnabled()) {
                        preguntarHabilitarBiometria(correo) {
                            completarLogin(usuario)
                        }
                    } else {
                        // Guardar email para biometría
                        securePrefs.saveUserEmail(correo)
                        completarLogin(usuario)
                    }
                } else {
                    // Contraseña incorrecta - incrementar intentos
                    manejarIntentoFallido()
                }
            } else {
                tilCorreo.error = "Usuario no registrado"
                Toast.makeText(this, "Este correo no está registrado", Toast.LENGTH_SHORT).show()
                btnLogin.isEnabled = true
                btnLogin.text = "Iniciar sesión"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            btnLogin.isEnabled = true
            btnLogin.text = "Iniciar sesión"
            e.printStackTrace()
        }
    }
    
    /**
     * Maneja un intento de login fallido
     */
    private fun manejarIntentoFallido() {
        val attempts = securePrefs.incrementLoginAttempts()
        
        tilClave.error = "Contraseña incorrecta"
        btnLogin.isEnabled = true
        btnLogin.text = "Iniciar sesión"
        
        when {
            attempts >= 3 && attempts < 5 -> {
                Toast.makeText(this, "Contraseña incorrecta. ${5 - attempts} intentos restantes", Toast.LENGTH_SHORT).show()
            }
            attempts >= 5 -> {
                securePrefs.setLockoutTime(System.currentTimeMillis())
                mostrarBloqueo()
                Toast.makeText(this, "Cuenta bloqueada temporalmente", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Pregunta al usuario si quiere habilitar biometría
     */
    private fun preguntarHabilitarBiometria(email: String, onComplete: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Habilitar huella/rostro?")
            .setMessage("¿Deseas usar tu huella dactilar o rostro para iniciar sesión más rápido?")
            .setPositiveButton("Sí, habilitar") { _, _ ->
                securePrefs.setBiometricEnabled(true)
                securePrefs.saveUserEmail(email)
                Toast.makeText(this, "Biometría habilitada", Toast.LENGTH_SHORT).show()
                onComplete()
            }
            .setNegativeButton("No, gracias") { _, _ ->
                securePrefs.saveUserEmail(email)
                onComplete()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Migra una contraseña de SHA-256 a PBKDF2
     */
    private fun migrarContrasena(userId: Int, passwordPlain: String) {
        try {
            val newHash = PasswordManager.hashPassword(passwordPlain)
            val db = dbHelper.writableDatabase
            val values = android.content.ContentValues().apply {
                put(AppDatabaseHelper.COL_USUARIO_CONTRASENA, newHash)
            }
            db.update(
                AppDatabaseHelper.TABLE_USUARIOS,
                values,
                "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
                arrayOf(userId.toString())
            )
        } catch (e: Exception) {
            // Error silencioso - no crítico
            e.printStackTrace()
        }
    }
    
    /**
     * Completa el proceso de login después de autenticación exitosa
     */
    private fun completarLogin(usuario: com.app.balance.model.Usuario) {
        val divisa = divisaDAO.obtenerDivisaPorId(usuario.divisaId)

        val transaccionDAO = TransaccionDAO(dbHelper.readableDatabase, dbHelper)
        val transacciones = transaccionDAO.obtenerTransaccionesPorUsuario(usuario.id)

        var totalGastado = 0.0
        transacciones.forEach { transaccion ->
            totalGastado += transaccion.transaccion.monto
        }

        val balanceActual = usuario.montoTotal
        val balanceOriginal = balanceActual + totalGastado

        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("SESION_ACTIVA", true)
            .putInt("USER_ID", usuario.id)
            .putString("USER_EMAIL", usuario.email)
            .putString("USER_NOMBRE", usuario.nombre)
            .putString("USER_APELLIDO", usuario.apellido)
            .putString("USER_CELULAR", usuario.celular)
            .putString("USER_FECHA_NAC", usuario.fechaNacimiento)
            .putString("USER_GENERO", usuario.genero)
            .putInt("DIVISA_ID", usuario.divisaId)
            .putString("DIVISA_CODIGO", divisa?.codigo ?: "PEN")
            .putString("DIVISA_NOMBRE", divisa?.nombre ?: "Nuevo Sol")
            .putString("DIVISA_BANDERA", divisa?.bandera ?: "")
            .putString("BALANCE_MONTO", balanceActual.toString())
            .putString("BALANCE_ORIGINAL", balanceOriginal.toString())
            .putString("FOTO_PERFIL_PATH", usuario.fotoPerfil)
            .apply()
        
        // Guardar último login
        securePrefs.saveLastLogin()

        Toast.makeText(this, "¡Bienvenido ${usuario.nombre}!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, InicioActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun cambioActivity(activityDestino: Class<out AppCompatActivity>) {
        val intent = Intent(this, activityDestino)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
        dbHelper.close()
    }
}

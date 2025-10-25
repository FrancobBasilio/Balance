package com.app.balance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verificar estado después de 2 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            decidirPantallaSiguiente()
        }, 2000)
    }

    private fun decidirPantallaSiguiente() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // 1. Verificar si es la primera vez
        val esPrimeraVez = prefs.getBoolean("ES_PRIMERA_VEZ", true)

        if (esPrimeraVez) {
            // Primera instalación - mostrar Bienvenida
            irABienvenida()
            return
        }

        // 2. No es primera vez - verificar si hay sesión activa
        val tieneSesionActiva = prefs.getBoolean("SESION_ACTIVA", false)
        val userId = prefs.getInt("USER_ID", 0)

        if (tieneSesionActiva && userId > 0) {
            // Usuario tiene sesión activa - ir directo a Inicio
            irAInicio()
        } else {
            // Usuario cerró sesión o no ha iniciado - ir a Login
            irALogin()
        }
    }

    private fun irABienvenida() {
        val intent = Intent(this, BienvenidoActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun irAInicio() {
        val intent = Intent(this, InicioActivity::class.java)
        startActivity(intent)
        finish()
    }
}
package com.app.balance.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Preferencias encriptadas para almacenar datos sensibles
 * Usa EncryptedSharedPreferences de AndroidX Security
 */
class SecurePreferences(context: Context) {
    
    companion object {
        private const val TAG = "SecurePreferences"
        private const val PREFS_NAME = "balance_secure_prefs"
        
        // Claves de preferencias
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_SESSION_TOKEN = "session_token"
        const val KEY_LAST_LOGIN = "last_login"
        const val KEY_LOGIN_ATTEMPTS = "login_attempts"
        const val KEY_LOCKOUT_TIME = "lockout_time"
        
        @Volatile
        private var INSTANCE: SecurePreferences? = null
        
        fun getInstance(context: Context): SecurePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurePreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val securePrefs: SharedPreferences
    
    init {
        securePrefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creando EncryptedSharedPreferences, usando fallback: ${e.message}")
            // Fallback a SharedPreferences normales si hay error
            context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }
    
    // ============================================
    // BIOMETRÍA
    // ============================================
    
    /**
     * Verifica si la biometría está habilitada para el usuario
     */
    fun isBiometricEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Habilita o deshabilita la biometría
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
    
    // ============================================
    // SESIÓN
    // ============================================
    
    /**
     * Guarda el email del usuario para login biométrico
     */
    fun saveUserEmail(email: String) {
        securePrefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }
    
    /**
     * Obtiene el email guardado
     */
    fun getUserEmail(): String? {
        return securePrefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Guarda la última fecha de login
     */
    fun saveLastLogin() {
        securePrefs.edit().putLong(KEY_LAST_LOGIN, System.currentTimeMillis()).apply()
    }
    
    /**
     * Obtiene la última fecha de login
     */
    fun getLastLogin(): Long {
        return securePrefs.getLong(KEY_LAST_LOGIN, 0)
    }
    
    // ============================================
    // PROTECCIÓN CONTRA FUERZA BRUTA
    // ============================================
    
    /**
     * Incrementa el contador de intentos fallidos
     */
    fun incrementLoginAttempts(): Int {
        val attempts = getLoginAttempts() + 1
        securePrefs.edit().putInt(KEY_LOGIN_ATTEMPTS, attempts).apply()
        return attempts
    }
    
    /**
     * Obtiene el número de intentos fallidos
     */
    fun getLoginAttempts(): Int {
        return securePrefs.getInt(KEY_LOGIN_ATTEMPTS, 0)
    }
    
    /**
     * Resetea el contador de intentos
     */
    fun resetLoginAttempts() {
        securePrefs.edit().putInt(KEY_LOGIN_ATTEMPTS, 0).apply()
    }
    
    /**
     * Establece el tiempo de bloqueo
     */
    fun setLockoutTime(timeMs: Long) {
        securePrefs.edit().putLong(KEY_LOCKOUT_TIME, timeMs).apply()
    }
    
    /**
     * Obtiene el tiempo de bloqueo
     */
    fun getLockoutTime(): Long {
        return securePrefs.getLong(KEY_LOCKOUT_TIME, 0)
    }
    
    /**
     * Verifica si la cuenta está bloqueada
     */
    fun isLockedOut(): Boolean {
        val lockoutTime = getLockoutTime()
        if (lockoutTime == 0L) return false
        
        val elapsed = System.currentTimeMillis() - lockoutTime
        val lockoutDuration = getLockoutDuration()
        
        if (elapsed > lockoutDuration) {
            // El bloqueo ha expirado
            resetLoginAttempts()
            setLockoutTime(0)
            return false
        }
        
        return true
    }
    
    /**
     * Obtiene el tiempo restante de bloqueo en segundos
     */
    fun getRemainingLockoutSeconds(): Int {
        val lockoutTime = getLockoutTime()
        if (lockoutTime == 0L) return 0
        
        val elapsed = System.currentTimeMillis() - lockoutTime
        val remaining = getLockoutDuration() - elapsed
        
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }
    
    /**
     * Calcula la duración del bloqueo basado en intentos
     */
    private fun getLockoutDuration(): Long {
        val attempts = getLoginAttempts()
        return when {
            attempts >= 10 -> 30 * 60 * 1000L // 30 minutos
            attempts >= 7 -> 10 * 60 * 1000L  // 10 minutos
            attempts >= 5 -> 5 * 60 * 1000L   // 5 minutos
            attempts >= 3 -> 1 * 60 * 1000L   // 1 minuto
            else -> 0
        }
    }
    
    // ============================================
    // LIMPIEZA
    // ============================================
    
    /**
     * Limpia todos los datos seguros (logout completo)
     */
    fun clearAll() {
        securePrefs.edit().clear().apply()
    }
    
    /**
     * Limpia solo datos de sesión (mantiene configuración de biometría)
     */
    fun clearSession() {
        securePrefs.edit()
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_LAST_LOGIN)
            .apply()
    }
}

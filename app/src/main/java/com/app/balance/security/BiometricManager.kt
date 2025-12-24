package com.app.balance.security

import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gestor de autenticación biométrica
 * Soporta huella dactilar y reconocimiento facial
 */
class BiometricAuthManager(private val context: Context) {
    
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * Tipos de biometría disponibles
     */
    enum class BiometricStatus {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NOT_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNKNOWN
    }
    
    /**
     * Resultado de autenticación
     */
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val code: Int, val message: String) : AuthResult()
        object Cancelled : AuthResult()
        object Failed : AuthResult()
    }
    
    /**
     * Verifica si la biometría está disponible
     */
    fun checkBiometricAvailability(): BiometricStatus {
        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.UNKNOWN
        }
    }
    
    /**
     * Verifica si la biometría está disponible
     */
    fun isBiometricAvailable(): Boolean {
        return checkBiometricAvailability() == BiometricStatus.AVAILABLE
    }
    
    /**
     * Muestra el diálogo de autenticación biométrica
     * Android automáticamente detecta y usa la biometría disponible (huella o rostro)
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Autenticación requerida",
        subtitle: String = "Verifica tu identidad para continuar",
        negativeButtonText: String = "Cancelar",
        onResult: (AuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(AuthResult.Success)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(AuthResult.Cancelled)
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        onResult(AuthResult.Error(errorCode, "No hay datos biométricos registrados"))
                    }
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                        onResult(AuthResult.Error(errorCode, "Este dispositivo no soporta biometría"))
                    }
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        onResult(AuthResult.Error(errorCode, "Demasiados intentos. Intenta más tarde"))
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onResult(AuthResult.Error(errorCode, "Biometría bloqueada. Usa tu PIN para desbloquear"))
                    }
                    else -> {
                        onResult(AuthResult.Error(errorCode, errString.toString()))
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // No cerrar el diálogo, permitir reintentos
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        try {
            // Usar BIOMETRIC_WEAK para máxima compatibilidad
            // Android selecciona automáticamente la biometría disponible
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setConfirmationRequired(false)
                .build()
            
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Fallback: intentar con credenciales del dispositivo
            try {
                val promptInfoFallback = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .setConfirmationRequired(false)
                    .build()
                
                biometricPrompt.authenticate(promptInfoFallback)
            } catch (e2: Exception) {
                onResult(AuthResult.Error(-1, "Error al iniciar autenticación: ${e2.message}"))
            }
        }
    }
    
    /**
     * Autenticación con credenciales del dispositivo como fallback (PIN/patrón/contraseña)
     */
    fun authenticateWithDeviceCredential(
        activity: FragmentActivity,
        title: String = "Autenticación requerida",
        subtitle: String = "Usa tu huella, rostro o PIN para continuar",
        onResult: (AuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(AuthResult.Success)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(AuthResult.Cancelled)
                    }
                    else -> {
                        onResult(AuthResult.Error(errorCode, errString.toString()))
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .setConfirmationRequired(false)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Obtiene un mensaje descriptivo del estado de biometría
     */
    fun getBiometricStatusMessage(): String {
        return when (checkBiometricAvailability()) {
            BiometricStatus.AVAILABLE -> "Biometría disponible"
            BiometricStatus.NO_HARDWARE -> "Este dispositivo no soporta biometría"
            BiometricStatus.HARDWARE_UNAVAILABLE -> "La biometría no está disponible temporalmente"
            BiometricStatus.NOT_ENROLLED -> "No hay huella o rostro registrado. Configúralo en Ajustes"
            BiometricStatus.SECURITY_UPDATE_REQUIRED -> "Se requiere una actualización de seguridad"
            BiometricStatus.UNKNOWN -> "Estado de biometría desconocido"
        }
    }
}

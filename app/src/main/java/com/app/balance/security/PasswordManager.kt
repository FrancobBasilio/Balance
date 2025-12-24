package com.app.balance.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Gestor de contraseñas con hashing seguro usando PBKDF2
 * 
 * PBKDF2 es más seguro que SHA-256 simple porque:
 * - Usa salt único por contraseña
 * - Aplica múltiples iteraciones (costoso computacionalmente)
 * - Resistente a ataques de rainbow tables y fuerza bruta
 */
object PasswordManager {
    
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000 // Número de iteraciones
    private const val KEY_LENGTH = 256 // Bits
    private const val SALT_LENGTH = 16 // Bytes
    
    /**
     * Genera un hash seguro de la contraseña con salt
     * Formato de salida: salt:hash (ambos en Base64)
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = pbkdf2Hash(password, salt)
        
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        
        return "$saltBase64:$hashBase64"
    }
    
    /**
     * Verifica si una contraseña coincide con un hash almacenado
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            // Verificar si es un hash nuevo (con salt) o legacy (SHA-256)
            if (storedHash.contains(":")) {
                // Hash nuevo con PBKDF2
                val parts = storedHash.split(":")
                if (parts.size != 2) return false
                
                val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
                val actualHash = pbkdf2Hash(password, salt)
                
                // Comparación en tiempo constante para evitar timing attacks
                MessageDigest.isEqual(expectedHash, actualHash)
            } else {
                // Hash legacy (SHA-256 simple) - para usuarios existentes
                val legacyHash = sha256Hash(password)
                storedHash == legacyHash
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verifica si un hash es del formato legacy (SHA-256)
     * Útil para migrar contraseñas gradualmente
     */
    fun isLegacyHash(storedHash: String): Boolean {
        return !storedHash.contains(":")
    }
    
    /**
     * Hash SHA-256 legacy (para compatibilidad con usuarios existentes)
     */
    fun sha256Hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Genera salt aleatorio criptográficamente seguro
     */
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    /**
     * Aplica PBKDF2 para generar el hash
     */
    private fun pbkdf2Hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
    
    /**
     * Valida la fortaleza de una contraseña
     * Retorna una lista de problemas encontrados (vacía si es válida)
     */
    fun validatePasswordStrength(password: String): List<String> {
        val problems = mutableListOf<String>()
        
        if (password.length < 8) {
            problems.add("Debe tener al menos 8 caracteres")
        }
        if (!password.any { it.isUpperCase() }) {
            problems.add("Debe contener al menos una mayúscula")
        }
        if (!password.any { it.isLowerCase() }) {
            problems.add("Debe contener al menos una minúscula")
        }
        if (!password.any { it.isDigit() }) {
            problems.add("Debe contener al menos un número")
        }
        
        return problems
    }
    
    /**
     * Calcula la fortaleza de la contraseña (0-100)
     */
    fun calculatePasswordStrength(password: String): Int {
        var score = 0
        
        // Longitud
        score += minOf(password.length * 4, 40)
        
        // Mayúsculas
        if (password.any { it.isUpperCase() }) score += 15
        
        // Minúsculas
        if (password.any { it.isLowerCase() }) score += 15
        
        // Números
        if (password.any { it.isDigit() }) score += 15
        
        // Caracteres especiales
        if (password.any { !it.isLetterOrDigit() }) score += 15
        
        return minOf(score, 100)
    }
}

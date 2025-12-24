package com.app.balance.data.cache

import android.util.LruCache
import com.app.balance.model.TransaccionConDetalles

/**
 * Caché en memoria para transacciones
 * Implementa LRU (Least Recently Used) para gestión automática de memoria
 */
object TransaccionCache {
    
    // Tamaño máximo del caché: 1/8 de la memoria disponible
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    // Caché de listas de transacciones por usuario
    private val transaccionesCache = LruCache<String, CachedData<List<TransaccionConDetalles>>>(cacheSize)
    
    // Caché de transacciones individuales
    private val transaccionIndividualCache = LruCache<Int, CachedData<TransaccionConDetalles>>(100)
    
    // Tiempo de expiración del caché (5 minutos)
    private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
    
    /**
     * Clase wrapper que incluye timestamp para control de expiración
     */
    data class CachedData<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_MS
        }
    }
    
    /**
     * Obtiene transacciones del caché para un usuario
     */
    fun getTransacciones(usuarioId: Int, filtro: String = "TODOS"): List<TransaccionConDetalles>? {
        val key = generarClave(usuarioId, filtro)
        val cached = transaccionesCache.get(key)
        
        return if (cached != null && !cached.isExpired()) {
            cached.data
        } else {
            // Remover si está expirado
            if (cached != null) {
                transaccionesCache.remove(key)
            }
            null
        }
    }
    
    /**
     * Guarda transacciones en el caché
     */
    fun putTransacciones(usuarioId: Int, transacciones: List<TransaccionConDetalles>, filtro: String = "TODOS") {
        val key = generarClave(usuarioId, filtro)
        transaccionesCache.put(key, CachedData(transacciones))
    }
    
    /**
     * Obtiene una transacción individual del caché
     */
    fun getTransaccion(transaccionId: Int): TransaccionConDetalles? {
        val cached = transaccionIndividualCache.get(transaccionId)
        
        return if (cached != null && !cached.isExpired()) {
            cached.data
        } else {
            if (cached != null) {
                transaccionIndividualCache.remove(transaccionId)
            }
            null
        }
    }
    
    /**
     * Guarda una transacción individual en el caché
     */
    fun putTransaccion(transaccion: TransaccionConDetalles) {
        transaccionIndividualCache.put(transaccion.transaccion.id, CachedData(transaccion))
    }
    
    /**
     * Invalida el caché de un usuario (llamar después de insertar/actualizar/eliminar)
     */
    fun invalidarCacheUsuario(usuarioId: Int) {
        // Invalidar todas las variantes de filtro para este usuario
        val filtros = listOf("TODOS", "DIA", "SEMANA", "MES", "RANGO")
        filtros.forEach { filtro ->
            val key = generarClave(usuarioId, filtro)
            transaccionesCache.remove(key)
        }
    }
    
    /**
     * Invalida una transacción específica
     */
    fun invalidarTransaccion(transaccionId: Int) {
        transaccionIndividualCache.remove(transaccionId)
    }
    
    /**
     * Limpia todo el caché
     */
    fun limpiarCache() {
        transaccionesCache.evictAll()
        transaccionIndividualCache.evictAll()
    }
    
    /**
     * Genera clave única para el caché
     */
    private fun generarClave(usuarioId: Int, filtro: String): String {
        return "user_${usuarioId}_$filtro"
    }
    
    /**
     * Obtiene estadísticas del caché (para debugging)
     */
    fun getEstadisticas(): String {
        return """
            Caché Transacciones:
            - Hits: ${transaccionesCache.hitCount()}
            - Misses: ${transaccionesCache.missCount()}
            - Size: ${transaccionesCache.size()}
            
            Caché Individual:
            - Hits: ${transaccionIndividualCache.hitCount()}
            - Misses: ${transaccionIndividualCache.missCount()}
            - Size: ${transaccionIndividualCache.size()}
        """.trimIndent()
    }
}

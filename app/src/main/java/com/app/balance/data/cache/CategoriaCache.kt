package com.app.balance.data.cache

import android.util.LruCache
import com.app.balance.model.Categoria

/**
 * Caché en memoria para categorías
 */
object CategoriaCache {
    
    // Caché de listas de categorías por usuario
    private val categoriasCache = LruCache<Int, CachedData<List<Categoria>>>(20)
    
    // Tiempo de expiración del caché (10 minutos - las categorías cambian menos)
    private const val CACHE_EXPIRATION_MS = 10 * 60 * 1000L
    
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
     * Obtiene categorías del caché para un usuario
     */
    fun getCategorias(usuarioId: Int): List<Categoria>? {
        val cached = categoriasCache.get(usuarioId)
        
        return if (cached != null && !cached.isExpired()) {
            cached.data
        } else {
            if (cached != null) {
                categoriasCache.remove(usuarioId)
            }
            null
        }
    }
    
    /**
     * Guarda categorías en el caché
     */
    fun putCategorias(usuarioId: Int, categorias: List<Categoria>) {
        categoriasCache.put(usuarioId, CachedData(categorias))
    }
    
    /**
     * Invalida el caché de un usuario
     */
    fun invalidarCache(usuarioId: Int) {
        categoriasCache.remove(usuarioId)
    }
    
    /**
     * Limpia todo el caché
     */
    fun limpiarCache() {
        categoriasCache.evictAll()
    }
}

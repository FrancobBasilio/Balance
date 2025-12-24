package com.app.balance.data.repository

import android.content.Context
import android.util.Log
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.cache.TransaccionCache
import com.app.balance.data.dao.TransaccionDAO
import com.app.balance.model.TransaccionConDetalles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository para gestionar transacciones
 * Abstrae el acceso a datos y maneja operaciones asíncronas
 */
class TransaccionRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "TransaccionRepository"
        
        @Volatile
        private var INSTANCE: TransaccionRepository? = null
        
        fun getInstance(context: Context): TransaccionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransaccionRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val dbHelper by lazy { AppDatabaseHelper(context) }
    
    /**
     * Obtiene transacciones de forma asíncrona
     */
    suspend fun obtenerTransacciones(usuarioId: Int): Result<List<TransaccionConDetalles>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.readableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val transacciones = dao.obtenerTransaccionesPorUsuario(usuarioId)
                Result.success(transacciones)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener transacciones: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene transacciones paginadas
     */
    suspend fun obtenerTransaccionesPaginadas(
        usuarioId: Int, 
        pagina: Int,
        tamanioPagina: Int = TransaccionDAO.PAGE_SIZE
    ): Result<List<TransaccionConDetalles>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.readableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val transacciones = dao.obtenerTransaccionesPaginadas(usuarioId, pagina, tamanioPagina)
                Result.success(transacciones)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener transacciones paginadas: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene transacciones por rango de fechas
     */
    suspend fun obtenerTransaccionesPorRango(
        usuarioId: Int,
        fechaDesde: String,
        fechaHasta: String
    ): Result<List<TransaccionConDetalles>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.readableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val transacciones = dao.obtenerTransaccionesPorRango(usuarioId, fechaDesde, fechaHasta)
                Result.success(transacciones)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener transacciones por rango: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Inserta una transacción
     */
    suspend fun insertarTransaccion(
        categoriaNombre: String,
        categoriaIcono: String,
        categoriaRutaImagen: String?,
        categoriaColor: Int?,
        tipoCategoriaId: Int,
        tipoCategoriaNombre: String,
        monto: Double,
        fecha: String,
        comentario: String?,
        usuarioId: Int
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.writableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val id = dao.insertarTransaccion(
                    categoriaNombre, categoriaIcono, categoriaRutaImagen,
                    categoriaColor, tipoCategoriaId, tipoCategoriaNombre,
                    monto, fecha, comentario, usuarioId
                )
                Result.success(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error al insertar transacción: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Elimina una transacción
     */
    suspend fun eliminarTransaccion(transaccionId: Int): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.writableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val resultado = dao.eliminarTransaccion(transaccionId)
                Result.success(resultado)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar transacción: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene estadísticas de gastos
     */
    suspend fun obtenerEstadisticas(usuarioId: Int): Result<Map<String, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.readableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val estadisticas = dao.obtenerEstadisticasPorTipo(usuarioId)
                Result.success(estadisticas)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener estadísticas: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene el total gastado
     */
    suspend fun obtenerTotalGastado(
        usuarioId: Int, 
        fechaDesde: String? = null, 
        fechaHasta: String? = null
    ): Result<Double> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.readableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val total = dao.obtenerTotalGastado(usuarioId, fechaDesde, fechaHasta)
                Result.success(total)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener total gastado: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cuenta el total de transacciones
     */
    suspend fun contarTransacciones(usuarioId: Int): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.readableDatabase
                val dao = TransaccionDAO(db, dbHelper)
                val count = dao.contarTransacciones(usuarioId)
                Result.success(count)
            } catch (e: Exception) {
                Log.e(TAG, "Error al contar transacciones: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Limpia el caché
     */
    fun limpiarCache() {
        TransaccionCache.limpiarCache()
    }
    
    /**
     * Invalida el caché de un usuario
     */
    fun invalidarCache(usuarioId: Int) {
        TransaccionCache.invalidarCacheUsuario(usuarioId)
    }
}

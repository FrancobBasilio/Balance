package com.app.balance.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.cache.TransaccionCache
import com.app.balance.model.Categoria
import com.app.balance.model.TipoCategoria
import com.app.balance.model.Transaccion
import com.app.balance.model.TransaccionConDetalles

class TransaccionDAO(private val db: SQLiteDatabase, private val dbHelper: AppDatabaseHelper) {
    
    companion object {
        private const val TAG = "TransaccionDAO"
        const val PAGE_SIZE = 20
    }

    fun insertarTransaccion(
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
    ): Long {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_NOMBRE, categoriaNombre)
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_ICONO, categoriaIcono)
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_RUTA_IMAGEN, categoriaRutaImagen)
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_COLOR, categoriaColor)
            put(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_ID, tipoCategoriaId)
            put(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_NOMBRE, tipoCategoriaNombre)
            put(AppDatabaseHelper.COL_TRANSACCION_MONTO, monto)
            put(AppDatabaseHelper.COL_TRANSACCION_FECHA, fecha)
            put(AppDatabaseHelper.COL_TRANSACCION_COMENTARIO, comentario)
            put(AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID, usuarioId)
        }
        
        val resultado = db.insert(AppDatabaseHelper.TABLE_TRANSACCIONES, null, valores)
        
        // Invalidar caché después de insertar
        if (resultado > 0) {
            TransaccionCache.invalidarCacheUsuario(usuarioId)
        }
        
        return resultado
    }

    fun obtenerTransaccionesPorUsuario(usuarioId: Int): List<TransaccionConDetalles> {
        // Intentar obtener del caché primero
        val cached = TransaccionCache.getTransacciones(usuarioId)
        if (cached != null) {
            return cached
        }
        
        val transacciones = mutableListOf<TransaccionConDetalles>()
        val query = """
            SELECT * FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
            WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
            ORDER BY ${AppDatabaseHelper.COL_TRANSACCION_FECHA} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(usuarioId.toString()))

        while (cursor.moveToNext()) {
            transacciones.add(parsearTransaccion(cursor))
        }
        cursor.close()
        
        // Guardar en caché
        TransaccionCache.putTransacciones(usuarioId, transacciones)
        
        return transacciones
    }
    
    /**
     * Obtiene transacciones con paginación
     */
    fun obtenerTransaccionesPaginadas(
        usuarioId: Int, 
        pagina: Int, 
        tamanioPagina: Int = PAGE_SIZE
    ): List<TransaccionConDetalles> {
        val offset = pagina * tamanioPagina
        val transacciones = mutableListOf<TransaccionConDetalles>()
        
        val query = """
            SELECT * FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
            WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
            ORDER BY ${AppDatabaseHelper.COL_TRANSACCION_FECHA} DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val cursor = db.rawQuery(
            query, 
            arrayOf(usuarioId.toString(), tamanioPagina.toString(), offset.toString())
        )
        
        while (cursor.moveToNext()) {
            transacciones.add(parsearTransaccion(cursor))
        }
        cursor.close()
        
        return transacciones
    }
    
    /**
     * Obtiene transacciones filtradas por rango de fechas
     */
    fun obtenerTransaccionesPorRango(
        usuarioId: Int,
        fechaDesde: String,
        fechaHasta: String
    ): List<TransaccionConDetalles> {
        val transacciones = mutableListOf<TransaccionConDetalles>()
        
        val query = """
            SELECT * FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
            WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
            AND ${AppDatabaseHelper.COL_TRANSACCION_FECHA} BETWEEN ? AND ?
            ORDER BY ${AppDatabaseHelper.COL_TRANSACCION_FECHA} DESC
        """.trimIndent()

        val cursor = db.rawQuery(
            query, 
            arrayOf(usuarioId.toString(), fechaDesde, fechaHasta)
        )
        
        while (cursor.moveToNext()) {
            transacciones.add(parsearTransaccion(cursor))
        }
        cursor.close()
        
        return transacciones
    }
    
    /**
     * Cuenta el total de transacciones
     */
    fun contarTransacciones(usuarioId: Int): Int {
        val query = """
            SELECT COUNT(*) FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
            WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf(usuarioId.toString()))
        var count = 0
        
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        
        return count
    }
    
    /**
     * Obtiene estadísticas por tipo de categoría
     */
    fun obtenerEstadisticasPorTipo(usuarioId: Int): Map<String, Double> {
        val query = """
            SELECT ${AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_NOMBRE}, 
                   SUM(${AppDatabaseHelper.COL_TRANSACCION_MONTO}) as total
            FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
            WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
            GROUP BY ${AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_NOMBRE}
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf(usuarioId.toString()))
        val estadisticas = mutableMapOf<String, Double>()
        
        while (cursor.moveToNext()) {
            val tipo = cursor.getString(0)
            val total = cursor.getDouble(1)
            estadisticas[tipo] = total
        }
        cursor.close()
        
        return estadisticas
    }
    
    /**
     * Obtiene el total gastado
     */
    fun obtenerTotalGastado(usuarioId: Int, fechaDesde: String? = null, fechaHasta: String? = null): Double {
        val query = if (fechaDesde != null && fechaHasta != null) {
            """
                SELECT COALESCE(SUM(${AppDatabaseHelper.COL_TRANSACCION_MONTO}), 0)
                FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
                WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
                AND ${AppDatabaseHelper.COL_TRANSACCION_FECHA} BETWEEN ? AND ?
            """.trimIndent()
        } else {
            """
                SELECT COALESCE(SUM(${AppDatabaseHelper.COL_TRANSACCION_MONTO}), 0)
                FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
                WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
            """.trimIndent()
        }
        
        val args = if (fechaDesde != null && fechaHasta != null) {
            arrayOf(usuarioId.toString(), fechaDesde, fechaHasta)
        } else {
            arrayOf(usuarioId.toString())
        }
        
        val cursor = db.rawQuery(query, args)
        var total = 0.0
        
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        
        return total
    }

    fun eliminarTransaccion(transaccionId: Int): Int {
        // Obtener usuarioId antes de eliminar para invalidar caché
        val transaccion = obtenerTransaccionPorId(transaccionId)
        val usuarioId = transaccion?.transaccion?.usuarioId
        
        val resultado = db.delete(
            AppDatabaseHelper.TABLE_TRANSACCIONES,
            "${AppDatabaseHelper.COL_TRANSACCION_ID} = ?",
            arrayOf(transaccionId.toString())
        )
        
        // Invalidar caché
        if (resultado > 0 && usuarioId != null) {
            TransaccionCache.invalidarCacheUsuario(usuarioId)
            TransaccionCache.invalidarTransaccion(transaccionId)
        }
        
        return resultado
    }

    fun obtenerTransaccionPorId(transaccionId: Int): TransaccionConDetalles? {
        // Intentar obtener del caché
        val cached = TransaccionCache.getTransaccion(transaccionId)
        if (cached != null) {
            return cached
        }
        
        val query = """
            SELECT * FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
            WHERE ${AppDatabaseHelper.COL_TRANSACCION_ID} = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(transaccionId.toString()))
        var resultado: TransaccionConDetalles? = null

        if (cursor.moveToFirst()) {
            resultado = parsearTransaccion(cursor)
            // Guardar en caché
            TransaccionCache.putTransaccion(resultado)
        }

        cursor.close()
        return resultado
    }

    fun actualizarTransaccion(
        transaccionId: Int,
        categoriaNombre: String,
        categoriaIcono: String,
        categoriaRutaImagen: String?,
        categoriaColor: Int?,
        tipoCategoriaId: Int,
        tipoCategoriaNombre: String,
        monto: Double,
        fecha: String,
        comentario: String?
    ): Int {
        // Obtener usuarioId para invalidar caché
        val transaccionExistente = obtenerTransaccionPorId(transaccionId)
        val usuarioId = transaccionExistente?.transaccion?.usuarioId
        
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_NOMBRE, categoriaNombre)
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_ICONO, categoriaIcono)
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_RUTA_IMAGEN, categoriaRutaImagen)
            put(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_COLOR, categoriaColor)
            put(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_ID, tipoCategoriaId)
            put(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_NOMBRE, tipoCategoriaNombre)
            put(AppDatabaseHelper.COL_TRANSACCION_MONTO, monto)
            put(AppDatabaseHelper.COL_TRANSACCION_FECHA, fecha)
            put(AppDatabaseHelper.COL_TRANSACCION_COMENTARIO, comentario)
        }

        val resultado = db.update(
            AppDatabaseHelper.TABLE_TRANSACCIONES,
            valores,
            "${AppDatabaseHelper.COL_TRANSACCION_ID} = ?",
            arrayOf(transaccionId.toString())
        )
        
        // Invalidar caché
        if (resultado > 0 && usuarioId != null) {
            TransaccionCache.invalidarCacheUsuario(usuarioId)
            TransaccionCache.invalidarTransaccion(transaccionId)
        }

        return resultado
    }
    
    /**
     * Parsea una transacción desde el cursor
     */
    private fun parsearTransaccion(cursor: Cursor): TransaccionConDetalles {
        val transaccion = Transaccion(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_ID)),
            categoriaId = 0,
            monto = cursor.getDouble(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_MONTO)),
            fecha = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_FECHA)),
            comentario = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_COMENTARIO)),
            usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID))
        )

        val colorIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_COLOR)
        val color = if (cursor.isNull(colorIndex)) null else cursor.getInt(colorIndex)

        val categoria = Categoria(
            id = 0,
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_NOMBRE)),
            icono = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_ICONO)),
            usuarioId = transaccion.usuarioId,
            tipoCategoriaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_ID)),
            rutaImagen = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_RUTA_IMAGEN)),
            color = color
        )

        val tipoCategoria = TipoCategoria(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_ID)),
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_NOMBRE))
        )

        return TransaccionConDetalles(transaccion, categoria, tipoCategoria)
    }
}

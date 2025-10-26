package com.app.balance.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.model.Categoria
import com.app.balance.model.TipoCategoria
import com.app.balance.model.Transaccion
import com.app.balance.model.TransaccionConDetalles

class TransaccionDAO(private val db: SQLiteDatabase, private val dbHelper: AppDatabaseHelper) {

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
        return db.insert(AppDatabaseHelper.TABLE_TRANSACCIONES, null, valores)
    }

    fun obtenerTransaccionesPorUsuario(usuarioId: Int): List<TransaccionConDetalles> {
        val transacciones = mutableListOf<TransaccionConDetalles>()
        val query = """
        SELECT * FROM ${AppDatabaseHelper.TABLE_TRANSACCIONES}
        WHERE ${AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID} = ?
        ORDER BY ${AppDatabaseHelper.COL_TRANSACCION_FECHA} DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(usuarioId.toString()))

        while (cursor.moveToNext()) {
            val transaccion = Transaccion(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_ID)),
                categoriaId = 0,
                monto = cursor.getDouble(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_MONTO)),
                fecha = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_FECHA)),
                comentario = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_COMENTARIO)),
                usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_USUARIO_ID))
            )

            //  Leer el color
            val colorIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_COLOR)
            val color = if (cursor.isNull(colorIndex)) null else cursor.getInt(colorIndex)

            val categoria = Categoria(
                id = 0,
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_NOMBRE)),
                icono = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_ICONO)),
                usuarioId = usuarioId,
                tipoCategoriaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_ID)),
                rutaImagen = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_CATEGORIA_RUTA_IMAGEN)),
                color = color
            )

            val tipoCategoria = TipoCategoria(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TRANSACCION_TIPO_CATEGORIA_NOMBRE))
            )

            transacciones.add(TransaccionConDetalles(transaccion, categoria, tipoCategoria))
        }
        cursor.close()
        return transacciones
    }

    fun eliminarTransaccion(transaccionId: Int): Int {
        return db.delete(
            AppDatabaseHelper.TABLE_TRANSACCIONES,
            "${AppDatabaseHelper.COL_TRANSACCION_ID} = ?",
            arrayOf(transaccionId.toString())
        )
    }
}
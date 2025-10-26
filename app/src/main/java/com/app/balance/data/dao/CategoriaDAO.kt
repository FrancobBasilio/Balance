package com.app.balance.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.model.Categoria

class CategoriaDAO(private val db: SQLiteDatabase, private val dbHelper: AppDatabaseHelper) {

    fun insertarCategoriaSistema(categoria: Categoria): Long {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_CAT_SISTEMA_NOMBRE, categoria.nombre)
            put(AppDatabaseHelper.COL_CAT_SISTEMA_ICONO, categoria.icono)
            put(AppDatabaseHelper.COL_CAT_SISTEMA_USUARIO_ID, categoria.usuarioId)
            put(AppDatabaseHelper.COL_CAT_SISTEMA_TIPO_ID, categoria.tipoCategoriaId)
            put(AppDatabaseHelper.COL_CAT_SISTEMA_RUTA_IMAGEN, categoria.rutaImagen)
            put(AppDatabaseHelper.COL_CAT_SISTEMA_COLOR, categoria.color)
        }
        return db.insert(AppDatabaseHelper.TABLE_CATEGORIAS_SISTEMA, null, valores)
    }

    fun obtenerCategoriasSistemaPorUsuario(usuarioId: Int): List<Categoria> {
        val categorias = mutableListOf<Categoria>()
        val cursor = db.query(
            AppDatabaseHelper.TABLE_CATEGORIAS_SISTEMA,
            null,
            "${AppDatabaseHelper.COL_CAT_SISTEMA_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString()),
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            categorias.add(
                Categoria(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_NOMBRE)),
                    icono = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ICONO)),
                    usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_USUARIO_ID)),
                    tipoCategoriaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_TIPO_ID)),
                    rutaImagen = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_RUTA_IMAGEN)),
                    color = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_COLOR))
                )
            )
        }
        cursor.close()
        return categorias
    }

    fun eliminarCategoriaSistema(categoriaId: Int): Int {
        return db.delete(
            AppDatabaseHelper.TABLE_CATEGORIAS_SISTEMA,
            "${AppDatabaseHelper.COL_CAT_SISTEMA_ID} = ?",
            arrayOf(categoriaId.toString())
        )
    }
}
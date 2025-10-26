package com.app.balance.data.dao

import android.database.sqlite.SQLiteDatabase
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.model.TipoCategoria

class TipoCategoriaDAO(private val db: SQLiteDatabase, private val dbHelper: AppDatabaseHelper) {

    fun obtenerTipoPorId(tipoId: Int): TipoCategoria? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_TIPOS_CATEGORIA,
            null,
            "${AppDatabaseHelper.COL_TIPO_ID} = ?",
            arrayOf(tipoId.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val tipo = TipoCategoria(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TIPO_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_TIPO_NOMBRE))
            )
            cursor.close()
            tipo
        } else {
            cursor.close()
            null
        }
    }
}
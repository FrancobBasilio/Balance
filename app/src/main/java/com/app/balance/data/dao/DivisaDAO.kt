package com.app.balance.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.model.Divisa

class DivisaDAO(private val db: SQLiteDatabase, private val dbHelper: AppDatabaseHelper) {

    /**
     * Insertar una sola divisa
     */
    fun insertarDivisa(divisa: Divisa): Long {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_DIVISA_NOMBRE, divisa.nombre)
            put(AppDatabaseHelper.COL_DIVISA_CODIGO, divisa.codigo)
            put(AppDatabaseHelper.COL_DIVISA_BANDERA_URL, divisa.bandera)
        }
        return db.insert(AppDatabaseHelper.TABLE_DIVISAS, null, valores)
    }



    /**
     * Obtener divisa por ID
     */
    fun obtenerDivisaPorId(divisaId: Int): Divisa? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_DIVISAS,
            null,
            "${AppDatabaseHelper.COL_DIVISA_ID} = ?",
            arrayOf(divisaId.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            val divisa = Divisa(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_NOMBRE)),
                codigo = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_CODIGO)),
                bandera = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_BANDERA_URL)) ?: ""
            )
            cursor.close()
            divisa
        } else {
            cursor.close()
            null
        }
    }

    /**
     * Obtener divisa por código
     */
    fun obtenerDivisaPorCodigo(codigo: String): Divisa? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_DIVISAS,
            null,
            "${AppDatabaseHelper.COL_DIVISA_CODIGO} = ?",
            arrayOf(codigo),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            val divisa = Divisa(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_NOMBRE)),
                codigo = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_CODIGO)),
                bandera = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_BANDERA_URL)) ?: ""
            )
            cursor.close()
            divisa
        } else {
            cursor.close()
            null
        }
    }



}
package com.app.balance.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.model.CountryCode

class CountryCodeDAO(private val db: SQLiteDatabase, private val dbHelper: AppDatabaseHelper) {

    fun insertarDivisa(divisa: CountryCode): Long {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_COUNTRY_NOMBRE, divisa.nombre)
            put(AppDatabaseHelper.COL_COUNTRY_CODIGO, divisa.codigo)
        }
        return db.insert(AppDatabaseHelper.TABLE_COUNTRY_CODES, null, valores)
    }

    fun insertarDivisas(divisas: List<CountryCode>): Boolean {
        return try {
            divisas.forEach { divisa ->
                val valores = ContentValues().apply {
                    put(AppDatabaseHelper.COL_COUNTRY_NOMBRE, divisa.nombre)
                    put(AppDatabaseHelper.COL_COUNTRY_CODIGO, divisa.codigo)
                }
                db.insertWithOnConflict(
                    AppDatabaseHelper.TABLE_COUNTRY_CODES,
                    null,
                    valores,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun obtenerTodosPaises(): List<CountryCode> {
        val paises = mutableListOf<CountryCode>()
        val cursor = db.query(
            AppDatabaseHelper.TABLE_COUNTRY_CODES,
            null,
            null,
            null,
            null,
            null,
            AppDatabaseHelper.COL_COUNTRY_NOMBRE + " ASC"
        )
        while (cursor.moveToNext()) {
            paises.add(
                CountryCode(
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_COUNTRY_NOMBRE)),
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_COUNTRY_CODIGO))
                )
            )
        }
        cursor.close()
        return paises
    }

    fun obtenerPaisPorId(paisId: Int): CountryCode? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_COUNTRY_CODES,
            null,
            "${AppDatabaseHelper.COL_COUNTRY_ID} = ?",
            arrayOf(paisId.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val pais = CountryCode(
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_COUNTRY_NOMBRE)),
                codigo = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_COUNTRY_CODIGO))
            )
            cursor.close()
            pais
        } else {
            cursor.close()
            null
        }
    }

    fun obtenerPaisPorCodigo(codigo: String): CountryCode? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_COUNTRY_CODES,
            null,
            "${AppDatabaseHelper.COL_COUNTRY_CODIGO} = ?",
            arrayOf(codigo),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val pais = CountryCode(
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_COUNTRY_NOMBRE)),
                codigo = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_COUNTRY_CODIGO))
            )
            cursor.close()
            pais
        } else {
            cursor.close()
            null
        }
    }

    fun eliminarTodasLasDivisas(): Int {
        return db.delete(AppDatabaseHelper.TABLE_COUNTRY_CODES, null, null)
    }
}

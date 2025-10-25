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
     * Insertar múltiples divisas (usado cuando cargas desde la API)
     */
    fun insertarDivisas(divisas: List<Divisa>): Boolean {
        return try {
            divisas.forEach { divisa ->
                val valores = ContentValues().apply {
                    put(AppDatabaseHelper.COL_DIVISA_NOMBRE, divisa.nombre)
                    put(AppDatabaseHelper.COL_DIVISA_CODIGO, divisa.codigo)
                    put(AppDatabaseHelper.COL_DIVISA_BANDERA_URL, divisa.bandera)
                }
                db.insertWithOnConflict(
                    AppDatabaseHelper.TABLE_DIVISAS,
                    null,
                    valores,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Obtener todas las divisas
     */
    fun obtenerTodasDivisas(): List<Divisa> {
        val divisas = mutableListOf<Divisa>()
        val cursor = db.query(
            AppDatabaseHelper.TABLE_DIVISAS,
            null,
            null,
            null,
            null,
            null,
            "${AppDatabaseHelper.COL_DIVISA_NOMBRE} ASC"
        )

        while (cursor.moveToNext()) {
            divisas.add(
                Divisa(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_NOMBRE)),
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_CODIGO)),
                    bandera = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_DIVISA_BANDERA_URL)) ?: ""
                )
            )
        }
        cursor.close()
        return divisas
    }

    /**
     * Obtener divisa por ID (usado en Login para recuperar datos de divisa del usuario)
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
     * Obtener divisa por código (ejemplo: "PEN", "USD")
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

    /**
     * Eliminar todas las divisas (útil para testing o reset)
     */
    fun eliminarTodasLasDivisas(): Int {
        return db.delete(AppDatabaseHelper.TABLE_DIVISAS, null, null)
    }

    /**
     * Verificar si ya existen divisas en la base de datos
     */
    fun existenDivisas(): Boolean {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${AppDatabaseHelper.TABLE_DIVISAS}",
            null
        )
        val existe = if (cursor.moveToFirst()) {
            cursor.getInt(0) > 0
        } else {
            false
        }
        cursor.close()
        return existe
    }
}
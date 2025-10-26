package com.app.balance.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.model.Usuario

class UsuarioDAO(private val db: SQLiteDatabase, private val dbHelper: AppDatabaseHelper) {

    fun insertarUsuario(usuario: Usuario): Long {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_NOMBRE, usuario.nombre)
            put(AppDatabaseHelper.COL_USUARIO_APELLIDO, usuario.apellido)
            put(AppDatabaseHelper.COL_USUARIO_FECHA_NAC, usuario.fechaNacimiento)
            put(AppDatabaseHelper.COL_USUARIO_GENERO, usuario.genero)
            put(AppDatabaseHelper.COL_USUARIO_CELULAR, usuario.celular)
            put(AppDatabaseHelper.COL_USUARIO_EMAIL, usuario.email)
            put(AppDatabaseHelper.COL_USUARIO_CONTRASENA, usuario.contrasena)
            put(AppDatabaseHelper.COL_USUARIO_DIVISA_ID, usuario.divisaId)
            put(AppDatabaseHelper.COL_USUARIO_MONTO_TOTAL, usuario.montoTotal)
            put(AppDatabaseHelper.COL_USUARIO_FOTO_PERFIL, usuario.fotoPerfil)
        }
        return db.insert(AppDatabaseHelper.TABLE_USUARIOS, null, valores)
    }

    fun obtenerUsuarioPorEmail(email: String): Usuario? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_USUARIOS,
            null,
            "${AppDatabaseHelper.COL_USUARIO_EMAIL} = ?",
            arrayOf(email),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val usuario = Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_NOMBRE)),
                apellido = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_APELLIDO)),
                fechaNacimiento = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_FECHA_NAC)),
                genero = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_GENERO)),
                celular = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_CELULAR)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_EMAIL)),
                contrasena = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_CONTRASENA)),
                divisaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_DIVISA_ID)),
                montoTotal = cursor.getDouble(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_MONTO_TOTAL)),
                fotoPerfil = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_FOTO_PERFIL))
            )
            cursor.close()
            usuario
        } else {
            cursor.close()
            null
        }
    }

    fun obtenerUsuarioPorId(id: Int): Usuario? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_USUARIOS,
            null,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val usuario = Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_NOMBRE)),
                apellido = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_APELLIDO)),
                fechaNacimiento = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_FECHA_NAC)),
                genero = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_GENERO)),
                celular = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_CELULAR)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_EMAIL)),
                contrasena = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_CONTRASENA)),
                divisaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_DIVISA_ID)),
                montoTotal = cursor.getDouble(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_USUARIO_MONTO_TOTAL))
            )
            cursor.close()
            usuario
        } else {
            cursor.close()
            null
        }
    }

    fun actualizarMontoTotal(usuarioId: Int, nuevoMonto: Double): Int {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_MONTO_TOTAL, nuevoMonto)
        }
        return db.update(
            AppDatabaseHelper.TABLE_USUARIOS,
            valores,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        )
    }

    fun actualizarUsuario(usuario: Usuario): Int {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_NOMBRE, usuario.nombre)
            put(AppDatabaseHelper.COL_USUARIO_APELLIDO, usuario.apellido)
            put(AppDatabaseHelper.COL_USUARIO_CELULAR, usuario.celular)
            put(AppDatabaseHelper.COL_USUARIO_DIVISA_ID, usuario.divisaId)
        }
        return db.update(
            AppDatabaseHelper.TABLE_USUARIOS,
            valores,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
            arrayOf(usuario.id.toString())
        )
    }

    fun eliminarUsuario(usuarioId: Int): Int {
        return db.delete(
            AppDatabaseHelper.TABLE_USUARIOS,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        )
    }


    fun actualizarFotoPerfil(usuarioId: Int, rutaFoto: String?): Int {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_FOTO_PERFIL, rutaFoto)
        }
        return db.update(
            AppDatabaseHelper.TABLE_USUARIOS,
            valores,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        )
    }

    /**
     * Actualizar solo la divisa del usuario
     */
    fun actualizarDivisaUsuario(usuarioId: Int, divisaId: Int): Int {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_DIVISA_ID, divisaId)
        }
        return db.update(
            AppDatabaseHelper.TABLE_USUARIOS,
            valores,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        )
    }

    fun actualizarNombre(usuarioId: Int, nombre: String, apellido: String): Int {
        val valores = ContentValues().apply {
            put(AppDatabaseHelper.COL_USUARIO_NOMBRE, nombre)
            put(AppDatabaseHelper.COL_USUARIO_APELLIDO, apellido)
        }

        return db.update(
            AppDatabaseHelper.TABLE_USUARIOS,
            valores,
            "${AppDatabaseHelper.COL_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        )
    }
}
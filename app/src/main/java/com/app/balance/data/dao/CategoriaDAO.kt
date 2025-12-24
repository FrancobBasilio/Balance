package com.app.balance.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.app.balance.data.AppDatabaseHelper
import com.app.balance.data.cache.CategoriaCache
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
        
        val resultado = db.insert(AppDatabaseHelper.TABLE_CATEGORIAS_SISTEMA, null, valores)
        
        // Invalidar caché después de insertar
        if (resultado > 0) {
            CategoriaCache.invalidarCache(categoria.usuarioId)
        }
        
        return resultado
    }

    fun obtenerCategoriasSistemaPorUsuario(usuarioId: Int): List<Categoria> {
        // Intentar obtener del caché
        val cached = CategoriaCache.getCategorias(usuarioId)
        if (cached != null) {
            return cached
        }
        
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
            val colorIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_COLOR)
            val color = if (cursor.isNull(colorIndex)) null else cursor.getInt(colorIndex)
            
            categorias.add(
                Categoria(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_NOMBRE)),
                    icono = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ICONO)),
                    usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_USUARIO_ID)),
                    tipoCategoriaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_TIPO_ID)),
                    rutaImagen = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_RUTA_IMAGEN)),
                    color = color
                )
            )
        }
        cursor.close()
        
        // Guardar en caché
        CategoriaCache.putCategorias(usuarioId, categorias)
        
        return categorias
    }
    
    /**
     * Obtiene una categoría por ID
     */
    fun obtenerCategoriaPorId(categoriaId: Int): Categoria? {
        val cursor = db.query(
            AppDatabaseHelper.TABLE_CATEGORIAS_SISTEMA,
            null,
            "${AppDatabaseHelper.COL_CAT_SISTEMA_ID} = ?",
            arrayOf(categoriaId.toString()),
            null,
            null,
            null
        )
        
        var categoria: Categoria? = null
        
        if (cursor.moveToFirst()) {
            val colorIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_COLOR)
            val color = if (cursor.isNull(colorIndex)) null else cursor.getInt(colorIndex)
            
            categoria = Categoria(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ID)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_NOMBRE)),
                icono = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ICONO)),
                usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_USUARIO_ID)),
                tipoCategoriaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_TIPO_ID)),
                rutaImagen = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_RUTA_IMAGEN)),
                color = color
            )
        }
        
        cursor.close()
        return categoria
    }
    
    /**
     * Obtiene categorías por tipo
     */
    fun obtenerCategoriasPorTipo(usuarioId: Int, tipoCategoriaId: Int): List<Categoria> {
        val categorias = mutableListOf<Categoria>()
        
        val cursor = db.query(
            AppDatabaseHelper.TABLE_CATEGORIAS_SISTEMA,
            null,
            "${AppDatabaseHelper.COL_CAT_SISTEMA_USUARIO_ID} = ? AND ${AppDatabaseHelper.COL_CAT_SISTEMA_TIPO_ID} = ?",
            arrayOf(usuarioId.toString(), tipoCategoriaId.toString()),
            null,
            null,
            "${AppDatabaseHelper.COL_CAT_SISTEMA_NOMBRE} ASC"
        )
        
        while (cursor.moveToNext()) {
            val colorIndex = cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_COLOR)
            val color = if (cursor.isNull(colorIndex)) null else cursor.getInt(colorIndex)
            
            categorias.add(
                Categoria(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_NOMBRE)),
                    icono = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_ICONO)),
                    usuarioId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_USUARIO_ID)),
                    tipoCategoriaId = cursor.getInt(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_TIPO_ID)),
                    rutaImagen = cursor.getString(cursor.getColumnIndexOrThrow(AppDatabaseHelper.COL_CAT_SISTEMA_RUTA_IMAGEN)),
                    color = color
                )
            )
        }
        
        cursor.close()
        return categorias
    }

    fun eliminarCategoriaSistema(categoriaId: Int): Int {
        // Obtener usuarioId antes de eliminar para invalidar caché
        val categoria = obtenerCategoriaPorId(categoriaId)
        val usuarioId = categoria?.usuarioId
        
        val resultado = db.delete(
            AppDatabaseHelper.TABLE_CATEGORIAS_SISTEMA,
            "${AppDatabaseHelper.COL_CAT_SISTEMA_ID} = ?",
            arrayOf(categoriaId.toString())
        )
        
        // Invalidar caché
        if (resultado > 0 && usuarioId != null) {
            CategoriaCache.invalidarCache(usuarioId)
        }
        
        return resultado
    }
}

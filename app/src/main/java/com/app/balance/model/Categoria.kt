package com.app.balance.model

data class Categoria(
    val id: Int = 0,
    val nombre: String,
    val icono: String = "default",
    val usuarioId: Int,
    val tipoCategoriaId: Int,
    val rutaImagen: String? = null
)
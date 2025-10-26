package com.app.balance.model

data class Usuario(
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val fechaNacimiento: String,
    val genero: String,
    val celular: String,
    val email: String,
    val contrasena: String,
    val divisaId: Int,
    val montoTotal: Double = 0.0,
    val fotoPerfil: String? = null
)
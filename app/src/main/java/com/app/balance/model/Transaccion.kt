package com.app.balance.model

import java.time.LocalDate


data class Transaccion(
    val id: Int = 0,
    val categoriaId: Int,
    val monto: Double,
    val fecha: String,
    val comentario: String? = null,
    val usuarioId: Int
)
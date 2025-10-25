package com.app.balance.model

data class TransaccionConDetalles(
    val transaccion: Transaccion,
    val categoria: Categoria,
    val tipoCategoria: TipoCategoria
)
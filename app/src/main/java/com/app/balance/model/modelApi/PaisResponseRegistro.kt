package com.app.balance.model.modelApi

data class PaisResponseRegistro(
    val name: NameResponse,
    val idd: IddResponse,
    val flags: FlagsResponse,
    val cca2: String
)
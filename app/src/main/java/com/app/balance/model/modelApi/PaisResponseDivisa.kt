package com.app.balance.model.modelApi

data class PaisResponseDivisa(
    val name: NameResponse,
    val flags: FlagsResponse,
    val currencies: Map<String, Map<String, String>>? = null
)
package com.app.balance.respondApi

data class DivisaResponse(
    val currencies: Map<String, String>? = null, // Para API como exchangerate-api.com
    val rates: Map<String, Double>? = null        // Para otras APIs
)
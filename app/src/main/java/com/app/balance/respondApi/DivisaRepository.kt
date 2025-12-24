package com.app.balance.respondApi

import com.app.balance.model.Divisa
import com.app.balance.network.PaisesApiServiceDivisa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DivisaRepository(private val paisService: PaisesApiServiceDivisa) {

    suspend fun cargarDivisas(): Result<List<Divisa>> = withContext(Dispatchers.IO) {
        try {
            val response = paisService.obtenerPaises()

            if (response.isSuccessful) {
                val paises = response.body() ?: emptyList()

                val divisasFormato = paises.mapNotNull { pais ->
                    val nombre = pais.name.common
                    val bandera = pais.flags.png ?: pais.flags.svg ?: ""

                    if (bandera.isEmpty()) return@mapNotNull null

                    val codigoDivisa = obtenerCodigoDivisaDelPais(pais.currencies)

                    if (codigoDivisa.isEmpty()) return@mapNotNull null

                    Divisa(
                        nombre = nombre,
                        codigo = codigoDivisa,
                        bandera = bandera
                    )
                }.sortedBy { it.nombre }
                    .distinctBy { it.codigo }

                Result.success(divisasFormato)
            } else {
                Result.failure(Exception("Error en la API: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun obtenerCodigoDivisaDelPais(currencies: Map<String, Map<String, String>>?): String {
        if (currencies == null || currencies.isEmpty()) {
            return ""
        }
        return currencies.keys.firstOrNull() ?: ""
    }
}
package com.app.balance.respondApi.repository

import com.app.balance.model.CountryCode
import com.app.balance.network.PaisesApiServiceRegistro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaisRepositoryRegistro(private val paisService: PaisesApiServiceRegistro) {

    suspend fun cargarPaises(): Result<List<CountryCode>> = withContext(Dispatchers.IO) {
        try {
            val response = paisService.obtenerPaises()

            if (response.isSuccessful) {
                val paises = response.body() ?: emptyList()

                val paisesFormato = paises.mapNotNull { pais ->
                    val nombre = pais.name.common
                    val root = pais.idd.root ?: return@mapNotNull null
                    val suffixes = pais.idd.suffixes ?: return@mapNotNull null

                    if (suffixes.isEmpty()) return@mapNotNull null

                    val codigo = root + suffixes.first()
                    val bandera = pais.flags.png ?: pais.flags.svg ?: ""

                    if (bandera.isEmpty()) return@mapNotNull null

                    CountryCode(
                        nombre = nombre,
                        codigo = codigo,
                        bandera = bandera
                    )
                }.sortedBy { it.nombre }

                Result.success(paisesFormato)
            } else {
                Result.failure(Exception("Error en la API: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.app.balance.network

import com.app.balance.model.modelApi.PaisResponseRegistro
import retrofit2.http.GET
import retrofit2.Response

interface PaisesApiServiceRegistro {
    @GET("all?fields=name,idd,cca2,flags")
    suspend fun obtenerPaises(): Response<List<PaisResponseRegistro>>
}


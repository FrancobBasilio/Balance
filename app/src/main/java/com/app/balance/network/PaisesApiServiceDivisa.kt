package com.app.balance.network

import com.app.balance.model.modelApi.PaisResponseDivisa
import retrofit2.Response
import retrofit2.http.GET

interface PaisesApiServiceDivisa {
    @GET("all?fields=name,flags,currencies")
    suspend fun obtenerPaises(): Response<List<PaisResponseDivisa>>
}
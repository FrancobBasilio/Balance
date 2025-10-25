package com.app.balance.network.apiClient

import com.app.balance.network.PaisesApiServiceRegistro
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PaisesApiClientRegistro {
    private const val BASE_URL = "https://restcountries.com/v3.1/"

    fun crearServicio(): PaisesApiServiceRegistro {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(PaisesApiServiceRegistro::class.java)
    }
}
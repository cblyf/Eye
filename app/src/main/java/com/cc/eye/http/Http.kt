package com.cc.eye.http

import com.cc.eye.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object Http {

    private const val URL = "https://www.wanandroid.com/"
    private val mediaType = "application/json".toMediaType()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .build()
    }

    fun server(): Server {
        return instance.create(Server::class.java)
    }

}
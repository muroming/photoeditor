package com.muroming.postcardeditor.data

import retrofit2.http.GET

interface PresetsApi {
    @GET("/frames")
    suspend fun loadPresets(): List<String>
}
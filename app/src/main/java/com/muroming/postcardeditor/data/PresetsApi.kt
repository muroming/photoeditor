package com.muroming.postcardeditor.data

import com.muroming.postcardeditor.data.dto.PresetsResponse
import retrofit2.http.GET

interface PresetsApi {
    @GET("/")
    suspend fun loadPresets(): PresetsResponse
}
package com.muroming.postcardeditor.data

import android.net.Uri
import com.google.gson.GsonBuilder
import com.muroming.postcardeditor.data.dto.UserPicture
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PresetsRepository {
    private val gson = GsonBuilder()
    .setLenient()
    .create()

    private val presets = Retrofit.Builder()
        .baseUrl(PRESETS_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(PresetsApi::class.java)

    suspend fun loadPresets(): List<UserPicture> {
        return presets.loadPresets()
                .presets
                .map { UserPicture(Uri.parse(it)) }
    }

    companion object {
        private const val PRESETS_URL = "https://muroming.pythonanywhere.com/"
//        private const val PRESETS_KEY = "%20gDjRtEYOT"
    }
}
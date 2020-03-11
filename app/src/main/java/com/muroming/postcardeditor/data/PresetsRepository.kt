package com.muroming.postcardeditor.data

import android.net.Uri
import com.muroming.postcardeditor.data.dto.UriPicture
import com.muroming.postcardeditor.data.dto.UserPicture
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class PresetsRepository {
    private val presets = Retrofit.Builder()
        .baseUrl(PRESETS_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PresetsApi::class.java)

    suspend fun loadPresets(): List<UserPicture> {
        return presets.loadPresets()
                .map { UriPicture(Uri.parse(it)) }
    }

    companion object {
        private const val PRESETS_URL = "http://muroming.pythonanywhere.com/"
    }
}
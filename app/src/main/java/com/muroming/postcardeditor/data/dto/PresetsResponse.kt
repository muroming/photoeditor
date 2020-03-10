package com.muroming.postcardeditor.data.dto

data class PresetsResponse(
    val success: Int,
    val message: String,
    val presets: List<String>
)
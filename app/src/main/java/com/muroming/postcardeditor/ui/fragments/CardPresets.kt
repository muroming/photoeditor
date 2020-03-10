package com.muroming.postcardeditor.ui.fragments

import com.muroming.postcardeditor.data.dto.UserPicture

sealed class CardPresets(val presets: List<UserPicture>) {
    object Loading : CardPresets(emptyList())
    object Error : CardPresets(emptyList())

    class Loaded(presets: List<UserPicture>) : CardPresets(presets)
}
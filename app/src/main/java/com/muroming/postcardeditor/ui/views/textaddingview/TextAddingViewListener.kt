package com.muroming.postcardeditor.ui.views.textaddingview

import android.graphics.Typeface

interface TextAddingViewListener {
    fun onTextReady(
        id: String,
        text: String,
        gravity: Int,
        textSize: Float,
        currentColor: Int,
        textOutlineColor: Int?,
        typeface: Typeface?
    )
}
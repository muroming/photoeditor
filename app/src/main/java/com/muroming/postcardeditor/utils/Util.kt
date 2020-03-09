package com.muroming.postcardeditor.utils

import android.content.res.Resources
import android.view.View

fun Float.toSp() = this / Resources.getSystem().displayMetrics.scaledDensity

fun View.setSize(newWidth: Int = width, newHeight: Int = height) {
    layoutParams = layoutParams.apply {
        width = newWidth
        height = newHeight
    }
}

fun View.setVisibility(isVisible: Boolean) {
    visibility = if (!isVisible) View.GONE else View.VISIBLE
}
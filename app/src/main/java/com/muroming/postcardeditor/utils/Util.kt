package com.muroming.postcardeditor.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
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

fun Bitmap.rotateBitmap(angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
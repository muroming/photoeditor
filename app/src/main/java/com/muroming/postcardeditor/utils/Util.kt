package com.muroming.postcardeditor.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun Float.toSp() = this / Resources.getSystem().displayMetrics.scaledDensity

fun Uri.toBitmap(): Bitmap? = try {
    BitmapFactory.decodeFile(path)
} catch (e: Exception) {
    null
}

fun Context.getCompatColor(@ColorRes color: Int) = ContextCompat.getColor(this, color)

fun View.setSize(newWidth: Int = width, newHeight: Int = height) {
    layoutParams = layoutParams.apply {
        width = newWidth
        height = newHeight
    }
}

fun View.setVisibility(isVisible: Boolean) {
    visibility = if (!isVisible) View.GONE else View.VISIBLE
}

fun EditText.getFocusWithKeyboard(inputManger: InputMethodManager) {
    inputManger.showSoftInput(this, InputMethodManager.SHOW_FORCED)
    requestFocus()
}
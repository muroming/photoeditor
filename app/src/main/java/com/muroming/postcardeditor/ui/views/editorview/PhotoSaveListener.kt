package com.muroming.postcardeditor.ui.views.editorview

import android.graphics.Bitmap
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor

class PhotoSaveListener (
    private val onSaved: (String) -> Unit,
    private val onFailure: () -> Unit = {}
) : PhotoEditor.OnSaveListener {
    override fun onSuccess(imagePath: String) = onSaved(imagePath)

    override fun onFailure(exception: Exception) = onFailure()
}

class BitmapSaveListener (
    private val onSaved: (Bitmap) -> Unit,
    private val onFailure: () -> Unit = {}
) : OnSaveBitmap {
    override fun onFailure(e: Exception?) = onFailure()

    override fun onBitmapReady(saveBitmap: Bitmap?) = saveBitmap?.let(onSaved) ?: onFailure()
}
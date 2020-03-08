package com.muroming.postcardeditor.ui.views.editorview

import android.graphics.Bitmap
import com.fenchtose.nocropper.CropperCallback

class CropCallback(
    private val onCrop: (Bitmap?) -> Unit
): CropperCallback() {
    override fun onCropped(bitmap: Bitmap?) {
        onCrop(bitmap)
    }
}
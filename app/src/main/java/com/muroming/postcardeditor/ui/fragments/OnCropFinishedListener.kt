package com.muroming.postcardeditor.ui.fragments

import android.net.Uri

interface OnCropFinishedListener {
    fun onImageCropped(uri: Uri)
    fun onCropCanceled()
}
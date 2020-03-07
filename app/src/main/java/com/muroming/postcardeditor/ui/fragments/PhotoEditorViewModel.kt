package com.muroming.postcardeditor.ui.fragments

import android.content.ContentResolver
import android.content.res.Resources
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.data.UserPicture
import java.io.File

class PhotoEditorViewModel : ViewModel() {
    private val userPictures = MutableLiveData<List<UserPicture>>()
    private val presets = MutableLiveData<List<UserPicture>>()
    private val editorState = MutableLiveData<EditorState>(EditorState.PRESETS)

    fun observeUserPictures(): LiveData<List<UserPicture>> = userPictures
    fun observePresets(): LiveData<List<UserPicture>> = presets
    fun observeEditorState(): LiveData<EditorState> = editorState

    fun getEditorState() = editorState.value

    fun loadUserPictures(directory: File) = directory
        .listFiles()
        .filter { it.name.contains(".png") || it.name.contains(".jpg") }
        .map {
            UserPicture(it.toUri())
        }
        .let(userPictures::setValue)

    fun loadPresets(resources: Resources) {
        val resId = R.drawable.k
        List(USER_PICTURES_COUNT) {
            UserPicture(
                Uri.parse(
                    ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                            resources.getResourcePackageName(resId) + '/' +
                            resources.getResourceTypeName(resId) + '/' +
                            resources.getResourceEntryName(resId)
                )
            )
        }
            .let(presets::setValue)
    }

    fun onPresetClicked() {
        editorState.value = EditorState.EDITING
    }

    fun onSavingComplete() {
        editorState.value = EditorState.PRESETS
    }

    fun onSavingDialogDismissed(shouldCloseEditor: Boolean) {
        editorState.value = if (shouldCloseEditor) EditorState.PRESETS else EditorState.EDITING
    }

    fun generateFilePath(directory: File): String {
        val currentCount = directory.listFiles().size
        return "${directory.path}/myPicture${currentCount}.png"
    }

    companion object {
        private const val USER_PICTURES_COUNT = 20
        private val PRESET_REGEX = Regex("preset[0-9]+\\.png")
    }
}
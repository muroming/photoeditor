package com.muroming.postcardeditor.ui.fragments

import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.data.PresetsRepository
import com.muroming.postcardeditor.data.dto.DrawablePicture
import com.muroming.postcardeditor.data.dto.UriPicture
import com.muroming.postcardeditor.data.dto.UserPicture
import kotlinx.coroutines.launch
import java.io.File

class PhotoEditorViewModel : ViewModel() {
    private val presetsRepository = PresetsRepository()

    private val userPictures = MutableLiveData<List<UserPicture>>()
    private val presets = MutableLiveData<CardPresets>(CardPresets.Loading)
    private val editorState = MutableLiveData<EditorState>(EditorState.FRAME_PRESETS)

    fun observeUserPictures(): LiveData<List<UserPicture>> = userPictures
    fun observePresets(): LiveData<CardPresets> = presets
    fun observeEditorState(): LiveData<EditorState> = editorState

    fun getEditorState() = editorState.value

    fun loadUserPictures(directory: File) = directory
        .listFiles()
        .filter { it.name.contains(".png") || it.name.contains(".jpg") }
        .map {
            UriPicture(it.toUri())
        }
        .let(userPictures::setValue)

    fun reloadPresets() {
        viewModelScope.launch {
            presets.value = CardPresets.Loading
            try {
                presets.value = CardPresets.Loaded(presetsRepository.loadPresets())
            } catch (e: Exception) {
                presets.value = CardPresets.Error
            }
        }
    }

    private fun loadFills(resources: Resources) {
        val fills = listOf(
            R.drawable.fill_blue,
            R.drawable.fill_deep_orange,
            R.drawable.fill_green,
            R.drawable.fill_indigo,
            R.drawable.fill_orange,
            R.drawable.fill_pink,
            R.drawable.fill_purple,
            R.drawable.fill_red,
            R.drawable.fill_teal,
            R.drawable.fill_yellow
        ).shuffled()
            .map { resId ->
                DrawablePicture(
                    resources.getDrawable(resId, null) as GradientDrawable
                )
            }
        presets.value = CardPresets.Loaded(fills)
    }

    fun onPresetClicked() {
        editorState.value = EditorState.EDITING
    }

    fun onSavingComplete() {
        editorState.value = EditorState.FRAME_PRESETS
    }

    fun onFinishedEditing() {
        editorState.value = EditorState.FRAME_PRESETS
    }

    fun generateFilePath(directory: File): String {
        val currentCount = directory.listFiles().size
        return "${directory.path}/myPicture${currentCount}.png"
    }

    fun onFillClicked(resources: Resources) {
        editorState.value = EditorState.FILL_PRESETS
        loadFills(resources)
    }

    fun onPresetsClicked() {
        editorState.value = EditorState.FRAME_PRESETS
        reloadPresets()
    }
}
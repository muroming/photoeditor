package com.muroming.postcardeditor.ui.fragments

import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muroming.postcardeditor.data.PresetsRepository
import com.muroming.postcardeditor.data.dto.UserPicture
import kotlinx.coroutines.launch
import java.io.File

class PhotoEditorViewModel : ViewModel() {
    private val presetsRepository = PresetsRepository()

    private val userPictures = MutableLiveData<List<UserPicture>>()
    private val presets = MutableLiveData<CardPresets>(CardPresets.Loading)
    private val editorState = MutableLiveData<EditorState>(EditorState.PRESETS)

    fun observeUserPictures(): LiveData<List<UserPicture>> = userPictures
    fun observePresets(): LiveData<CardPresets> = presets
    fun observeEditorState(): LiveData<EditorState> = editorState

    fun getEditorState() = editorState.value

    fun loadUserPictures(directory: File) = directory
        .listFiles()
        .filter { it.name.contains(".png") || it.name.contains(".jpg") }
        .map {
            UserPicture(it.toUri())
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

    fun onPresetClicked() {
        editorState.value = EditorState.EDITING
    }

    fun onSavingComplete() {
        editorState.value = EditorState.PRESETS
    }

    fun onFinishedEdeting() {
        editorState.value = EditorState.PRESETS
    }

    fun generateFilePath(directory: File): String {
        val currentCount = directory.listFiles().size
        return "${directory.path}/myPicture${currentCount}.png"
    }
}
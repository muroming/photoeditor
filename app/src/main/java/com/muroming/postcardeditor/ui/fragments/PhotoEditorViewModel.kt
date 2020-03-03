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

    fun observeUserPictures(): LiveData<List<UserPicture>> = userPictures
    fun observerPresets(): LiveData<List<UserPicture>> = presets

    fun loadUserPictures(directory: File) {
//        directory
//            .listFiles()
//            .filter { it.name.contains(".png") || it.name.contains(".jpg") }
//            .map {
//                UserPicture(it.toUri())
//            }
        List(USER_PICTURES_COUNT) { UserPicture(Uri.parse("android.resource://com.muroming.postcardeditor/drawable/ic_add_circle")) }
            .let(userPictures::setValue)
    }

    fun loadPresets(resources: Resources) {
//        directory
//            .listFiles()
//            .filter { PRESET_REGEX.matches(it.name) }
//            .map {
//                UserPicture(it.toUri())
//            }
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
            .let(userPictures::setValue)
    }

    companion object {
        private const val USER_PICTURES_COUNT = 20
        private val PRESET_REGEX = Regex("preset[0-9]+\\.png")
    }
}
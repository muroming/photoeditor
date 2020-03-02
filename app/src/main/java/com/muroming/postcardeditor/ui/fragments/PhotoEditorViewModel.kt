package com.muroming.postcardeditor.ui.fragments

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.muroming.postcardeditor.data.UserPicture
import java.io.File

class PhotoEditorViewModel : ViewModel() {
    private val userPictures = MutableLiveData<List<UserPicture>>()

    fun observeUserPictures(): LiveData<List<UserPicture>> = userPictures

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

    companion object {
        private const val USER_PICTURES_COUNT = 20
    }
}
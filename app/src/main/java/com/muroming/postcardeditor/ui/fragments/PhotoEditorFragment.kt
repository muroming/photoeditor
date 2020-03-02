package com.muroming.postcardeditor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.muroming.postcardeditor.R
import ja.burhanrashid52.photoeditor.PhotoEditor
import kotlinx.android.synthetic.main.editor_activity.*
import kotlinx.android.synthetic.main.photo_editor_view.*

class PhotoEditorFragment : Fragment() {
    private lateinit var photoEditor: PhotoEditor
    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.editor_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPhotoEditor()

        viewModel.loadUserPictures(requireContext().filesDir)
        viewModel.observeUserPictures().observe(this, vUserPictures::update)
    }

    private fun initPhotoEditor() {
        photoEditor = PhotoEditor.Builder(requireContext(), photoEditorView)
            .build()
    }
}
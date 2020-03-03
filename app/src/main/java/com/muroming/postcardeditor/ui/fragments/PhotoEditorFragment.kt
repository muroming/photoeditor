package com.muroming.postcardeditor.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.data.UserPicture
import com.muroming.postcardeditor.ui.views.UserPicturesAdapter
import com.squareup.picasso.Picasso
import ja.burhanrashid52.photoeditor.PhotoEditor
import kotlinx.android.synthetic.main.fragment_editor.*
import kotlinx.android.synthetic.main.photo_editor_view.*

class PhotoEditorFragment : Fragment(R.layout.fragment_editor), OnBackPressedListener {
    private lateinit var photoEditor: PhotoEditor
    private val viewModel: PhotoEditorViewModel by viewModels()

    private val presetsAdapter: UserPicturesAdapter by lazy {
        UserPicturesAdapter(requireContext(), R.layout.item_user_big_picture, ::showEditor)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPhotoEditor()
        initPresetsRecycler()
        initListeners()

        viewModel.loadUserPictures(requireContext().filesDir)
        viewModel.observeUserPictures().observe(this, vUserPictures::update)

        viewModel.loadPresets(resources)
        viewModel.observerPresets().observe(this, ::updatePresets)
    }

    private fun initPhotoEditor() {
        photoEditor = PhotoEditor.Builder(requireContext(), photoEditorView)
            .build()
    }

    private fun initPresetsRecycler() {
        rvPresets.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = presetsAdapter
        }
    }

    private fun initListeners() {
        ivBack.setOnClickListener { activity?.onBackPressed() }
    }

    private fun updatePresets(presets: List<UserPicture>) {
        presetsAdapter.setItems(presets)
        presetsAdapter.notifyDataSetChanged()
    }

    private fun showEditor(uri: Uri) {
        rvPresets.visibility = View.GONE
        Picasso.get()
            .load(uri)
            .into(photoEditorView.source)
        vPhotoEditor.visibility = View.VISIBLE
        viewModel.editorState = EditorState.EDITING
    }

    private fun showPresets() {
        rvPresets.visibility = View.VISIBLE
        vPhotoEditor.visibility = View.GONE
        viewModel.editorState = EditorState.PRESETS
    }

    override fun onBackPressed(): Boolean = when (viewModel.editorState) {
        EditorState.EDITING -> {
            showPresets()
            true
        }
        else -> false
    }
}
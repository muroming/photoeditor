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
import ja.burhanrashid52.photoeditor.PhotoEditor
import kotlinx.android.synthetic.main.fragment_editor.*
import kotlinx.android.synthetic.main.photo_editor_view.*

class PhotoEditorFragment : Fragment(R.layout.fragment_editor), OnBackPressedListener {
    private val viewModel: PhotoEditorViewModel by viewModels()

    private val presetsAdapter: UserPicturesAdapter by lazy {
        UserPicturesAdapter(requireContext(), R.layout.item_user_big_picture, ::onPresetClicked)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPresetsRecycler()
        initListeners()

        viewModel.loadUserPictures(resources)
        viewModel.observeUserPictures().observe(this, vUserPictures::update)

        viewModel.loadPresets(resources)
        viewModel.observePresets().observe(this, ::updatePresets)
        viewModel.observeEditorState().observe(this, ::onEditorStateChanged)

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

    private fun onPresetClicked(uri: Uri) {
        vPhotoEditor.initEditor(uri)
        viewModel.onPresetClicked()
    }

    private fun showEditor() {
        rvPresets.visibility = View.GONE
        mockIcons.visibility = View.INVISIBLE
        editorMockIcons.visibility = View.VISIBLE
        vPhotoEditor.visibility = View.VISIBLE
    }

    private fun showPresets() {
        vPhotoEditor.clearEditor()
        rvPresets.visibility = View.VISIBLE
        vPhotoEditor.visibility = View.GONE
        mockIcons.visibility = View.VISIBLE
        editorMockIcons.visibility = View.INVISIBLE
    }

    private fun onEditorStateChanged(editorState: EditorState) {
        when(editorState) {
            EditorState.PRESETS -> showPresets()
            EditorState.EDITING -> showEditor()
        }
    }

    override fun onBackPressed(): Boolean = when (viewModel.getEditorState()) {
        EditorState.EDITING -> {
            viewModel.onBackFromEditingClicked()
            true
        }
        else -> false
    }
}
package com.muroming.postcardeditor.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.data.UserPicture
import com.muroming.postcardeditor.ui.views.UserPicturesAdapter
import com.muroming.postcardeditor.ui.views.editorview.CropStarter
import com.muroming.postcardeditor.utils.setVisibility
import com.muroming.postcardeditor.utils.toBitmap
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_editor.*
import java.io.File

class PhotoEditorFragment : Fragment(R.layout.fragment_editor), OnBackPressedListener,
    OnCropFinishedListener, CropStarter {
    private val viewModel: PhotoEditorViewModel by viewModels()

    private val presetsAdapter: UserPicturesAdapter by lazy {
        UserPicturesAdapter(requireContext(), R.layout.item_user_big_picture, ::onPresetClicked)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPresetsRecycler()
        initListeners()

        viewModel.loadUserPictures(requireContext().filesDir)
        viewModel.observeUserPictures().observe(this, vUserPictures::update)

        viewModel.loadPresets(resources)
        viewModel.observePresets().observe(this, ::updatePresets)
        viewModel.observeEditorState().observe(this, ::onEditorStateChanged)

        vPhotoEditor.apply {
            fragmentManager = childFragmentManager
            cropStarter = this@PhotoEditorFragment
        }
        vUserPictures.onUserPictureClicked = ::onUserPictureClicked
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
        val bitmap = uri.toBitmap(requireContext().contentResolver)
        vPhotoEditor.initEditor(bitmap)
        viewModel.onPresetClicked()
    }

    private fun onUserPictureClicked(uri: Uri) {
        if (viewModel.getEditorState() == EditorState.EDITING) {
            SavePictureDialog(
                onSavePicture = { saveEditedPicture { onPresetClicked(uri) } },
                onNotSavingPicture = { onPresetClicked(uri) }
            ).show(childFragmentManager, SavePictureDialog.DIALOG_TAG)
        } else {
            onPresetClicked(uri)
        }
    }

    private fun showEditor() {
        rvPresets.setVisibility(false)
        mockIcons.visibility = View.INVISIBLE
        editorMockIcons.setVisibility(true)
        vPhotoEditor.setVisibility(true)
    }

    private fun showPresets() {
        vPhotoEditor.clearEditor()
        viewModel.loadUserPictures(requireContext().filesDir)

        rvPresets.setVisibility(true)
        mockIcons.setVisibility(true)
        editorMockIcons.setVisibility(false)
        vPhotoEditor.setVisibility(false)
    }

    private fun showSavingDialog() {
        SavePictureDialog(
            onSavePicture = { saveEditedPicture { viewModel.onSavingComplete() } },
            onNotSavingPicture = { viewModel.onFinishedEdeting() }
        ).show(childFragmentManager, SavePictureDialog.DIALOG_TAG)
    }

    private fun saveEditedPicture(onSaved: () -> Unit) {
        vPhotoEditor.saveImage(
            viewModel.generateFilePath(requireContext().filesDir)
        ) { isSuccessful ->
            if (isSuccessful) {
                onSaved()
            }
        }
    }

    private fun onEditorStateChanged(editorState: EditorState) {
        when (editorState) {
            EditorState.PRESETS -> showPresets()
            EditorState.EDITING -> showEditor()
        }
    }

    override fun startCrop() {
        (activity as? AppCompatActivity)?.let {
            UCrop.of(
                Uri.fromFile(getTempSrc()),
                Uri.fromFile(getTempDest())
            ).start(it)
        }
    }

    override fun onImageCropped(uri: Uri) {
        val bitmap = uri.toBitmap(requireContext().contentResolver)
        vPhotoEditor.setCroppedImage(bitmap)
        getTempDest().takeIf(File::exists)?.delete()
        getTempSrc().takeIf(File::exists)?.delete()
    }

    override fun onCropCanceled() {
        onImageCropped(getTempSrc().toUri())
    }

    private fun getTempSrc() = File(requireContext().filesDir, tempCropSrcFilename)
    private fun getTempDest() = File(requireContext().filesDir, tempCropDestFilename)

    override fun onBackPressed(): Boolean = when (viewModel.getEditorState()) {
        EditorState.EDITING -> {
            showSavingDialog()
            true
        }
        else -> false
    }

    companion object {
        const val tempCropSrcFilename = "tempcrop.png"
        private const val tempCropDestFilename = "destcrop.png"
    }
}
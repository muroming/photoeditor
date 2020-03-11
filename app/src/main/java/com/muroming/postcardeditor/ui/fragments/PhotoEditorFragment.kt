package com.muroming.postcardeditor.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
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
import com.muroming.postcardeditor.data.dto.UserPicture
import com.muroming.postcardeditor.listeners.OnBackPressedListener
import com.muroming.postcardeditor.listeners.OnCropFinishedListener
import com.muroming.postcardeditor.listeners.OnImagePickedListener
import com.muroming.postcardeditor.ui.views.UserPicturesAdapter
import com.muroming.postcardeditor.ui.views.editorview.CropStarter
import com.muroming.postcardeditor.ui.views.editorview.PicassoPhotoTarget
import com.muroming.postcardeditor.utils.setVisibility
import com.muroming.postcardeditor.utils.toBitmap
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_editor.*
import java.io.File

class PhotoEditorFragment : Fragment(R.layout.fragment_editor),
    OnBackPressedListener,
    OnCropFinishedListener,
    OnImagePickedListener,
    CropStarter {
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

        viewModel.reloadPresets()
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
        btnReloadPresets.setOnClickListener { viewModel.reloadPresets() }
        btnPickImageFromGallery.setOnClickListener {
            Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ).let {
                activity?.startActivityForResult(it, RESULT_PICK_IMAGE)
            }
        }
    }

    private fun updatePresets(presets: CardPresets) {
        when (presets) {
            is CardPresets.Loading -> showPresetsLoading()
            is CardPresets.Error -> showPresetsError()
            is CardPresets.Loaded -> showPresets(presets.presets)
        }
    }

    private fun showPresetsLoading() {
        rvPresets.setVisibility(false)
        errorGroup.setVisibility(false)
        pbLoadingPresets.setVisibility(true)
    }

    private fun showPresetsError() {
        rvPresets.setVisibility(false)
        errorGroup.setVisibility(true)
        pbLoadingPresets.setVisibility(false)
    }

    private fun showPresets(presets: List<UserPicture>) {
        presetsAdapter.setItems(presets)
        presetsAdapter.notifyDataSetChanged()
        rvPresets.setVisibility(true)
        errorGroup.setVisibility(false)
        pbLoadingPresets.setVisibility(false)
    }

    private fun onPresetClicked(uri: Uri) {
        if (uri.toString().startsWith("http")) {
            Picasso.get()
                .load(uri)
                .into(PicassoPhotoTarget(::openEditor))
        } else {
            uri.toBitmap(requireContext().contentResolver).let(::openEditor)
        }
    }

    private fun openEditor(bitmap: Bitmap) {
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
        btnPickImageFromGallery.setVisibility(false)
        errorGroup.setVisibility(false)
    }

    private fun showPresets() {
        vPhotoEditor.clearEditor()
        viewModel.loadUserPictures(requireContext().filesDir)

        rvPresets.setVisibility(true)
        mockIcons.setVisibility(true)
        btnPickImageFromGallery.setVisibility(true)
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
            if (vPhotoEditor.onBackPressed().not()) {
                showSavingDialog()
            }
            true
        }
        else -> false
    }

    override fun onImagePicked(imageUri: Uri) {
        onPresetClicked(imageUri)
    }

    companion object {
        const val RESULT_PICK_IMAGE = 123

        const val tempCropSrcFilename = "tempcrop.png"
        private const val tempCropDestFilename = "destcrop.png"
    }
}
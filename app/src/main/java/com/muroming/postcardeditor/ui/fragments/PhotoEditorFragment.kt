package com.muroming.postcardeditor.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
import com.muroming.postcardeditor.listeners.OnKeyboardShownListener
import com.muroming.postcardeditor.ui.views.UserPicturesAdapter
import com.muroming.postcardeditor.ui.views.editorview.CropStarter
import com.muroming.postcardeditor.ui.views.editorview.PicassoPhotoTarget
import com.muroming.postcardeditor.utils.setVisibility
import com.muroming.postcardeditor.utils.toBitmap
import com.squareup.picasso.Picasso
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.fragment_editor.*
import kotlinx.android.synthetic.main.my_pictures_view.*
import kotlinx.android.synthetic.main.photo_editor_view.*
import java.io.File

class PhotoEditorFragment : Fragment(R.layout.fragment_editor),
    OnBackPressedListener,
    OnCropFinishedListener,
    OnImagePickedListener,
    OnKeyboardShownListener,
    CropStarter {
    private val viewModel: PhotoEditorViewModel by viewModels()

    private val presetsAdapter: UserPicturesAdapter by lazy {
        UserPicturesAdapter(
            requireContext(),
            R.layout.item_user_big_picture,
            ::onPresetUriClicked,
            ::openEditor
        )
    }

    override fun onResume() {
        super.onResume()
        clearTempFiles()
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
        vTextAddingView.fragmentManager = childFragmentManager
        vTextAddingView.keyboardListener = this
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
        ivShare.setOnClickListener {
            val srcFile = getTempSrc()
            vPhotoEditor.saveImage(srcFile.absolutePath) { isSuccessful ->
                if (isSuccessful) {
                    val context = requireContext()
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + ".provider",
                        srcFile
                    )
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        type = "image/*"
                    }
                    vPhotoEditor.setCroppedImage(srcFile.toUri().toBitmap(context.contentResolver))
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            resources.getText(R.string.send_to)
                        )
                    )
                }
            }
        }
        ivFill.setOnClickListener {
            viewModel.onFillClicked(requireContext().resources)
        }
        ivMultiply.setOnClickListener {
            viewModel.onPresetsClicked()
        }
        ivAllPictures.setOnClickListener {
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

    private fun onPresetUriClicked(uri: Uri) {
        Picasso.get()
            .load(uri)
            .into(PicassoPhotoTarget(::openEditor))
    }

    private fun openEditor(bitmap: Bitmap) {
        vPhotoEditor.initEditor(bitmap)
        viewModel.onPresetClicked()
    }

    private fun onUserPictureClicked(uri: Uri) {
        if (viewModel.getEditorState() == EditorState.EDITING) {
            SavePictureDialog(
                onSavePicture = { saveEditedPicture { onPresetUriClicked(uri) } },
                onNotSavingPicture = { onPresetUriClicked(uri) }
            ).show(childFragmentManager, SavePictureDialog.DIALOG_TAG)
        } else {
            onPresetUriClicked(uri)
        }
    }

    private fun showEditor() {
        rvPresets.setVisibility(false)
        mockIcons.visibility = View.INVISIBLE
        editorMockIcons.setVisibility(true)
        vPhotoEditor.setVisibility(true)
        errorGroup.setVisibility(false)
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
            EditorState.FRAME_PRESETS -> showPresets()
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
    }

    override fun onCropCanceled() {
        onImageCropped(getTempSrc().toUri())
    }

    private fun getTempSrc() = File(requireContext().filesDir, tempCropSrcFilename)
    private fun getTempDest() = File(requireContext().filesDir, tempCropDestFilename)

    private fun clearTempFiles() {
        getTempSrc().takeIf(File::exists)?.delete()
    }

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
        onPresetUriClicked(imageUri)
    }

    override fun onKeyboardVisible() {
        editorMockIcons.setVisibility(false)
        vUserPictures.setVisibility(false)
        ivPresets.setVisibility(false)
        tvPresets.setVisibility(false)
    }

    override fun onKeyboardHidden() {
        editorMockIcons.setVisibility(true)
        vUserPictures.setVisibility(true)
        ivPresets.setVisibility(true)
        tvPresets.setVisibility(true)
    }

    companion object {
        const val RESULT_PICK_IMAGE = 123

        const val tempCropSrcFilename = "tempcrop.png"
        private const val tempCropDestFilename = "destcrop.png"
    }
}
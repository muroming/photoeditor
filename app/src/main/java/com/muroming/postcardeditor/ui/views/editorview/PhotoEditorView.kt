package com.muroming.postcardeditor.ui.views.editorview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.muroming.postcardeditor.R
import ja.burhanrashid52.photoeditor.PhotoEditor
import kotlinx.android.synthetic.main.photo_editor_view.view.*

class PhotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var photoEditor: PhotoEditor

    private val EDITOR_ACTIONS = mapOf(
        R.drawable.ic_add_text to ::onAddTextClicked,
        R.drawable.ic_palette to ::onPaletteClicked,
        R.drawable.ic_frame to {},
        R.drawable.ic_brush to ::onBrushClicked,
        R.drawable.ic_eraser to {},
        R.drawable.ic_undo to ::onUndoClicked,
        R.drawable.ic_redo to ::onRedoClicked,
        R.drawable.ic_crop to ::onCropClicked,
        R.drawable.ic_wand to {}
    )

    init {
        LayoutInflater.from(context).inflate(R.layout.photo_editor_view, this, true)
        initActions()
        initPhotoEditor()
    }

    private fun initActions() {
        val guidelineId = glEditorActions.id
        val margin = resources.getDimensionPixelSize(R.dimen.editor_actions_margin)

        var prevId = LayoutParams.PARENT_ID
        var currId = View.generateViewId()
        var nextId = View.generateViewId()
        for ((icon, action) in EDITOR_ACTIONS) {
            ImageView(context).apply {
                id = currId
                setOnClickListener { action() }
                layoutParams = LayoutParams(0, 0).apply {
                    dimensionRatio = "1:1"

                    topToBottom = guidelineId
                    if (icon == R.drawable.ic_add_text) {
                        startToStart = LayoutParams.PARENT_ID
                    } else {
                        startToEnd = prevId
                    }
                    if (icon == R.drawable.ic_wand) {
                        endToEnd = LayoutParams.PARENT_ID
                    } else {
                        endToStart = nextId
                    }
                    marginStart = margin
                    marginEnd = margin
                }
                setBackgroundResource(icon)
            }.let(::addView)

            prevId = currId
            currId = nextId
            nextId = View.generateViewId()
        }
    }

    private fun initPhotoEditor() {
        photoEditor = PhotoEditor.Builder(context, photoEditorView)
            .build()
    }

    fun clearEditor() = photoEditor.clearAllViews()

    private fun onAddTextClicked() {

    }

    private fun onPaletteClicked() {

    }

    private fun onBrushClicked() {
        photoEditor.brushColor = Color.parseColor("#ff0000")
        photoEditor.brushSize = 20f
    }

    private fun onUndoClicked() {
        photoEditor.undo()
    }

    private fun onRedoClicked() {
        photoEditor.redo()
    }

    private fun onCropClicked() {

    }

    private fun onEraserClicked() {
        photoEditor.brushEraser()
    }
}
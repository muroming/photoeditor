package com.muroming.postcardeditor.ui.views.editorview

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.utils.getFocusWithKeyboard
import com.muroming.postcardeditor.utils.setSize
import com.muroming.postcardeditor.utils.setVisibility
import com.squareup.picasso.Picasso
import ja.burhanrashid52.photoeditor.PhotoEditor
import kotlinx.android.synthetic.main.photo_editor_view.view.*

class PhotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var photoEditor: PhotoEditor
    private val maxBrushSize = resources.getDimensionPixelSize(R.dimen.max_brush_size)
    private val minBrushSize = resources.getDimensionPixelSize(R.dimen.min_brush_size)

    private var isErasing = false
    private var isDrawing = false
    private val inputMethodManager: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private val editorActions: Map<Int, (ImageView) -> Unit> = mapOf(
        R.drawable.ic_add_text to ::onAddTextClicked,
        R.drawable.ic_palette to ::onPaletteClicked,
        R.drawable.ic_frame to ::onFrameClicked,
        R.drawable.ic_brush to ::onBrushClicked,
        R.drawable.ic_eraser to ::onEraserClicked,
        R.drawable.ic_undo to ::onUndoClicked,
        R.drawable.ic_redo to ::onRedoClicked,
        R.drawable.ic_crop to ::onCropClicked,
        R.drawable.ic_wand to ::onWandClicked
    )

    init {
        LayoutInflater.from(context).inflate(R.layout.photo_editor_view, this, true)
        initPhotoEditor()
    }

    fun initEditor(uri: Uri) {
        loadPicture(uri)
        initActions()
    }

    private fun loadPicture(uri: Uri) {
        Picasso.get()
            .load(uri)
            .into(photoEditorView.source)
    }

    private fun initActions() {
        val guidelineId = glEditorActions.id
        val margin = resources.getDimensionPixelSize(R.dimen.editor_actions_margin)

        var prevId = LayoutParams.PARENT_ID
        var currId = View.generateViewId()
        var nextId = View.generateViewId()
        for ((icon, action) in editorActions) {
            ImageView(context).apply {
                id = currId
                setOnClickListener { action(this) }
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

        photoEditor.brushSize = minBrushSize.toFloat()
        photoEditor.brushColor = Color.parseColor("#ff0000")
        photoEditor.setBrushDrawingMode(false)

        vSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.takeIf { fromUser }?.let {
                    val newSize =
                        minBrushSize + (progress.toFloat() / 100) * (maxBrushSize - minBrushSize)
                    ivBrushSize.setSize(newSize.toInt(), newSize.toInt())

                    photoEditor.setBrushEraserSize(newSize)
                    photoEditor.brushSize = newSize

                    if (isErasing) {
                        photoEditor.brushEraser()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                ivBrushSize.animate().alpha(1f)
                    .setDuration(BRUSH_SIZE_ANIMATION_DURATION)
                    .start()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                ivBrushSize.animate()
                    .alpha(0f)
                    .setDuration(BRUSH_SIZE_ANIMATION_DURATION)
                    .start()
            }
        })

        vInputTextBackground.setOnClickListener { hideTextInputAndInstantiateText() }
    }

    fun clearEditor() {
        vSlider.setVisibility(false)
        photoEditor.clearAllViews()
        photoEditor.setBrushDrawingMode(false)
        isErasing = false
        isDrawing = false
    }

    private fun onAddTextClicked(view: ImageView) {
        textAddingGroup.setVisibility(true)
        etTextInput.getFocusWithKeyboard(inputMethodManager)
    }

    private fun onPaletteClicked(view: ImageView) {

    }

    private fun onBrushClicked(view: ImageView) {
        isDrawing = !isDrawing
        isErasing = false

        view.setColorFilter(if (isDrawing) SELECTED_ACTION_TINT else 0, PorterDuff.Mode.MULTIPLY)
        vSlider.setVisibility(isDrawing)
        photoEditor.setBrushDrawingMode(isDrawing)
    }

    private fun onUndoClicked(view: ImageView) {
        photoEditor.undo()
    }

    private fun onRedoClicked(view: ImageView) {
        photoEditor.redo()
    }

    private fun onCropClicked(view: ImageView) {

    }

    private fun onFrameClicked(view: ImageView) {

    }

    private fun onWandClicked(view: ImageView) {

    }

    private fun onEraserClicked(view: ImageView) {
        isErasing = !isErasing
        isDrawing = false

        vSlider.setVisibility(isErasing)
        if (isErasing) {
            photoEditor.brushEraser()
        } else {
            photoEditor.setBrushDrawingMode(false)
        }
    }

    private fun hideTextInputAndInstantiateText() {
        val text = etTextInput.text.toString()
        if (text.isNotEmpty()) {
            photoEditor.addText(text, null)
        }
        textAddingGroup.setVisibility(false)
        etTextInput.setText("")
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        private const val BRUSH_SIZE_ANIMATION_DURATION = 150L
        private val SELECTED_ACTION_TINT =
            Color.parseColor("#FF0000")//Color.parseColor("#FFD32F2F")
    }
}
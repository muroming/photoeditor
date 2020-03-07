package com.muroming.postcardeditor.ui.views.editorview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.utils.getFocusWithKeyboard
import com.muroming.postcardeditor.utils.setSize
import com.muroming.postcardeditor.utils.setVisibility
import com.muroming.postcardeditor.utils.toSp
import com.squareup.picasso.Picasso
import dev.sasikanth.colorsheet.ColorSheet
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import kotlinx.android.synthetic.main.photo_editor_view.view.*

class PhotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    lateinit var fragmentManager: FragmentManager

    private lateinit var photoEditor: PhotoEditor
    private val minBrushSize = resources.getDimensionPixelSize(R.dimen.min_brush_size)
    private val maxBrushSize = resources.getDimensionPixelSize(R.dimen.max_brush_size)

    private var isErasing = false
    private var isDrawing = false

    private var isTextBold = false
    private var isTextItalic = false
    private val minTextSize = resources.getDimensionPixelSize(R.dimen.min_text_size)
    private val maxTextSize = resources.getDimensionPixelSize(R.dimen.max_text_size)

    private val inputMethodManager: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private lateinit var colorPalette: IntArray
    private var selectedColor = -1

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
        Picasso.get().load(uri).into(photoEditorView.source)
        initActions()
        initTextControls()
        initColorPalette()
    }

    fun initEditor(image: Bitmap) {
        photoEditorView.source.setImageBitmap(image)
        initActions()
        initTextControls()
        initColorPalette()
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

    private fun initTextControls() {
        ivTextToLeft.setOnClickListener { etTextInput.gravity = Gravity.LEFT }
        ivTextToRight.setOnClickListener { etTextInput.gravity = Gravity.RIGHT }
        ivTextToCenter.setOnClickListener { etTextInput.gravity = Gravity.CENTER }
        ivBoldText.setOnClickListener { setInputTextBold() }
        ivItalicText.setOnClickListener { setInputTextItalic() }
        vInputTextBackground.setOnClickListener { hideTextInputAndInstantiateText() }
        vTextSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.takeIf { fromUser }?.let {
                    etTextInput.textSize =
                        minTextSize + (progress.toFloat() / 100) * (maxTextSize - minTextSize)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initColorPalette() {
        colorPalette = resources.getIntArray(R.array.paletteColors)
        selectedColor = colorPalette.first()
        photoEditor.brushColor = selectedColor
        photoEditor.setBrushDrawingMode(false)
    }

    private fun initPhotoEditor() {
        photoEditor = PhotoEditor.Builder(context, photoEditorView)
            .build()

        photoEditor.brushSize = minBrushSize.toFloat()
        photoEditor.brushColor = Color.parseColor("#ff0000")
        photoEditor.setBrushDrawingMode(false)

        vBrushSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
    }

    fun clearEditor() {
        cropper_view.setVisibility(false)
        photoEditorView.setVisibility(true)
        vBrushSlider.setVisibility(false)

        photoEditor.clearAllViews()
        photoEditor.setBrushDrawingMode(false)

        isErasing = false
        isDrawing = false
        isTextBold = false
        isTextItalic = false
    }

    fun saveImage(filepath: String, onSuccess: (Boolean) -> Unit) {
        photoEditor.saveAsFile(
            filepath,
            PhotoSaveListener({ onSuccess(true) }, { onSuccess(false) })
        )
    }

    private fun onAddTextClicked(view: ImageView) {
        setInputTextGroupVisibility(true)
    }

    private fun onPaletteClicked(view: ImageView) {
        ColorSheet().colorPicker(
            colors = colorPalette,
            selectedColor = selectedColor,
            noColorOption = false
        ) { newColor ->
            selectedColor = newColor
            photoEditor.brushColor = selectedColor
            photoEditor.setBrushDrawingMode(isDrawing)
        }.show(fragmentManager)
    }

    private fun onUndoClicked(view: ImageView) {
        photoEditor.undo()
    }

    private fun onRedoClicked(view: ImageView) {
        photoEditor.redo()
    }

    private fun onCropClicked(view: ImageView) {
        TODO()
//        val imageBitmap = photoEditorView.source.drawToBitmap()
//        cropper_view.apply {
//            setImageBitmap(imageBitmap)
//            setVisibility(true)
//        }
//        photoEditorView.setVisibility(false)
    }

    private fun onFrameClicked(view: ImageView) {}

    private fun onWandClicked(view: ImageView) {}

    private fun onBrushClicked(view: ImageView) {
        isDrawing = !isDrawing
        isErasing = false

        view.setColorFilter(if (isDrawing) SELECTED_ACTION_TINT else 0, PorterDuff.Mode.MULTIPLY)
        vBrushSlider.setVisibility(isDrawing)
        photoEditor.setBrushDrawingMode(isDrawing)
    }

    private fun onEraserClicked(view: ImageView) {
        isErasing = !isErasing
        isDrawing = false

        vBrushSlider.setVisibility(isErasing)
        if (isErasing) {
            photoEditor.brushEraser()
        } else {
            photoEditor.setBrushDrawingMode(false)
        }
    }

    private fun hideTextInputAndInstantiateText() {
        val text = etTextInput.text.toString()
        val gravity = etTextInput.gravity
        if (text.isNotEmpty()) {
            val textStyle = TextStyleBuilder().apply {
                withGravity(gravity)
                withTextSize(etTextInput.textSize.toSp())
                withTextFont(etTextInput.typeface)
            }
            photoEditor.addText(text, textStyle)
        }
        setInputTextGroupVisibility(false)
    }

    private fun setInputTextBold() {
        isTextBold = !isTextBold
        etTextInput.setTypeface(null, getTextStyle())
    }

    private fun setInputTextItalic() {
        isTextItalic = !isTextItalic
        etTextInput.setTypeface(null, getTextStyle())
    }

    private fun getTextStyle(): Int = if (isTextBold) {
        if (isTextItalic) Typeface.BOLD_ITALIC else Typeface.BOLD
    } else {
        if (isTextItalic) Typeface.ITALIC else Typeface.NORMAL
    }

    private fun setInputTextGroupVisibility(isVisible: Boolean) {
        textAddingGroup.setVisibility(isVisible)
        etTextInput.apply {
            setText("")
            textSize = minTextSize.toFloat()
            gravity = Gravity.LEFT
            setTypeface(null, Typeface.NORMAL)
        }
        if (isVisible) {
            etTextInput.getFocusWithKeyboard(inputMethodManager)
        } else {
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
        vBrushSlider.setVisibility(!isVisible && (isDrawing || isErasing))
    }

    companion object {
        private const val BRUSH_SIZE_ANIMATION_DURATION = 150L
        private val SELECTED_ACTION_TINT = Color.parseColor("#FFD32F2F")
    }
}
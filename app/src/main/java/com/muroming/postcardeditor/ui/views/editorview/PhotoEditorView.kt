package com.muroming.postcardeditor.ui.views.editorview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.listeners.OnBackPressedListener
import com.muroming.postcardeditor.ui.fragments.PhotoEditorFragment
import com.muroming.postcardeditor.ui.views.textaddingview.TextAddingViewListener
import com.muroming.postcardeditor.ui.views.textaddingview.TextViewStyle
import com.muroming.postcardeditor.utils.setSize
import com.muroming.postcardeditor.utils.setVisibility
import dev.sasikanth.colorsheet.ColorSheet
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import kotlinx.android.synthetic.main.photo_editor_view.view.*
import java.io.File

class PhotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    TextAddingViewListener, OnBackPressedListener {

    lateinit var fragmentManager: FragmentManager
    lateinit var cropStarter: CropStarter

    private lateinit var photoEditor: PhotoEditor
    private val editorAddedViews by lazy {
        PhotoEditor::class.java.getDeclaredField("addedViews").apply {
            isAccessible = true
        }.get(photoEditor) as MutableList<View>
    }

    private val minBrushSize = resources.getDimensionPixelSize(R.dimen.min_brush_size)
    private val maxBrushSize = resources.getDimensionPixelSize(R.dimen.max_brush_size)

    private var isErasing = false
    private var isDrawing = false

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
        vTextAddingView.textListener = this
    }

    fun initEditor(image: Bitmap) {
        photoEditorView.source.setImageBitmap(image)
        initActions()
        initColorPalette()
        photoEditorView.setOnTouchListener { _, event ->
            val applyDrawing = event.action == MotionEvent.ACTION_UP
            if (applyDrawing) {
                photoEditor.setBrushDrawingMode(false)
                photoEditor.setBrushDrawingMode(isDrawing)
                if (isErasing) {
                    photoEditor.brushEraser()
                }
            }
            applyDrawing
        }
    }

    fun setCroppedImage(bitmap: Bitmap) {
        photoEditorView.source.setImageBitmap(bitmap)
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
        photoEditorView.setVisibility(true)
        vBrushSlider.setVisibility(false)

        photoEditor.clearAllViews()
        photoEditor.setBrushDrawingMode(false)
        photoEditorView.source.setImageBitmap(null)

        isErasing = false
        isDrawing = false
        vTextAddingView.setInputTextGroupVisibility(false)
    }

    fun saveImage(filepath: String, onSuccess: (Boolean) -> Unit) {
        photoEditor.saveAsFile(
            filepath,
            PhotoSaveListener({ onSuccess(true) }, { onSuccess(false) })
        )
    }

    private fun onAddTextClicked(view: ImageView) {
        vTextAddingView.setInputTextGroupVisibility(true)
        vBrushSlider.setVisibility(false)
    }

    private fun onPaletteClicked(view: ImageView) {
        ColorSheet().colorPicker(
            colors = colorPalette,
            selectedColor = selectedColor,
            noColorOption = false
        ) { newColor ->
            selectedColor = newColor
            photoEditor.brushColor = selectedColor
            vTextAddingView.setTextColor(selectedColor)
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
        pbCropLoading.setVisibility(true)
        photoEditor.saveAsFile(getTempSrcPath(), PhotoSaveListener(
            onSaved = {
                pbCropLoading.setVisibility(false)
                cropStarter.startCrop()
            },
            onFailure = {
                pbCropLoading.setVisibility(false)
            }
        ))
    }

    private fun getTempSrcPath() = File(
        context.filesDir,
        PhotoEditorFragment.tempCropSrcFilename
    ).absolutePath

    private fun onFrameClicked(view: ImageView) {}

    private fun onWandClicked(view: ImageView) {}

    private fun onBrushClicked(view: ImageView?) {
        isDrawing = !isDrawing
        isErasing = false

        vBrushSlider.setVisibility(isDrawing)
        photoEditor.setBrushDrawingMode(isDrawing)
    }

    private fun onEraserClicked(view: ImageView?) {
        isErasing = !isErasing
        isDrawing = false

        vBrushSlider.setVisibility(isErasing)
        if (isErasing) {
            photoEditor.brushEraser()
        } else {
            photoEditor.setBrushDrawingMode(false)
        }
    }

    override fun onTextReady(
        text: String,
        gravity: Int,
        textSize: Float,
        currentColor: Int,
        textOutlineColor: Int?,
        typeface: Typeface?,
        textStyle: TextViewStyle
    ) {
        if (text.isNotEmpty()) {
            val textStyle = TextStyleBuilder().apply {
                withGravity(gravity)
                withTextSize(textSize)
                withTextColor(currentColor)
                typeface?.let(::withTextFont)
            }
            photoEditor.addText(text, textStyle)
        }
        vTextAddingView.setInputTextGroupVisibility(false)
        vBrushSlider.setVisibility(isDrawing || isErasing)
        modifyAddedText(text, textStyle, textOutlineColor)
    }

    private fun modifyAddedText(text: String, style: TextViewStyle, outlineColor: Int?) {
        val textHolder = editorAddedViews.mapNotNull {
            ((it as? ViewGroup)?.children?.first() as? ViewGroup)
        }.first { (it.children.first() as? TextView)?.text == text }


        outlineColor?.let {
            val textView = textHolder.getChildAt(0) as TextView
            textHolder.removeViewAt(0)
            val outlinedText = copyTextWithOutline(textView, outlineColor)
            textHolder.addView(outlinedText, 0)
        }

        (textHolder.parent as? ViewGroup)?.let { holderParent ->
            val deleteImage = (holderParent.children.first { it is ImageView })

            val deleteImageParams = deleteImage.layoutParams as FrameLayout.LayoutParams

            val editImage = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(0, 0).apply {
                    width = deleteImageParams.width
                    height = deleteImageParams.height
                    gravity = Gravity.END or Gravity.TOP
                }
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_edit))
            }
            deleteImage.viewTreeObserver.addOnGlobalLayoutListener {
                editImage.visibility = deleteImage.visibility
            }
            editImage.setOnClickListener {
                photoEditorView.removeView(holderParent)
                editorAddedViews.remove(holderParent)
                vTextAddingView.editText(textHolder, style)
            }

            holderParent.addView(editImage)
        }
    }

    private fun copyTextWithOutline(textView: TextView, outlineColor: Int) =
        OutlinedText(context).apply {
            text = textView.text
            gravity = textView.gravity
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.textSize)
            setTextColor(textView.currentTextColor)
            typeface = textView.typeface
            layoutParams = textView.layoutParams
            strokeColor = outlineColor
            setStrokeWidth(
                TypedValue.COMPLEX_UNIT_PX,
                textView.textSize / TEXT_OUTLINE_RATIO
            )
        }

    override fun onBackPressed(): Boolean {
        val intercepting = isDrawing || isErasing || vTextAddingView.isVisible
        when {
            isDrawing -> onBrushClicked(null)
            isErasing -> onEraserClicked(null)
            vTextAddingView.isVisible -> vTextAddingView.onBackPressed()
        }

        return intercepting
    }

    companion object {
        const val TEXT_OUTLINE_RATIO = 30L
        private const val BRUSH_SIZE_ANIMATION_DURATION = 150L
    }
}
package com.muroming.postcardeditor.ui.views.editorview

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentManager
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.utils.getFocusWithKeyboard
import com.muroming.postcardeditor.utils.setSize
import com.muroming.postcardeditor.utils.setVisibility
import com.muroming.postcardeditor.utils.toSp
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
    private var isTextUnderlined = false

    private val minTextSize = resources.getDimensionPixelSize(R.dimen.min_text_size)
    private val maxTextSize = resources.getDimensionPixelSize(R.dimen.max_text_size)

    private val inputMethodManager: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private lateinit var colorPalette: IntArray
    private var selectedColor = -1

    private var currentTypeface: Typeface? = null

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

    private val editorFonts = mapOf(
        "Default" to null,
        "Amaranth" to R.font.amaranth,
        "Elysium" to R.font.elysium,
        "Honey Beeg" to R.font.honey_beeg,
        "Meamury" to R.font.meamury,
        "Reload" to R.font.reload,
        "Zabava" to R.font.zabava
    ).mapNotNull { (name, font) ->
        name to font?.let { ResourcesCompat.getFont(context, font) }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.photo_editor_view, this, true)
        initPhotoEditor()
    }

    fun initEditor(contentResolver: ContentResolver, uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        initEditor(bitmap)
    }

    fun initEditor(image: Bitmap) {
        photoEditorView.source.setImageBitmap(image)
        initActions()
        initTextControls()
        initCropControls()
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
        ivUnderlineText.setOnClickListener { setInputTextUnderlined() }

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

    private fun initCropControls() {
        ivCancelCrop.setOnClickListener { hideCroppingView() }
        ivConfirmCrop.setOnClickListener { applyCropping() }
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

        cropper_view.initWithFitToCenter(true)
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
        setupFontsAdapter()
    }

    private fun setupFontsAdapter() {
        ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            editorFonts.map(Pair<String, Typeface?>::first)
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            vTypefacesSpinner.adapter = this
        }

        vTypefacesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentTypeface = editorFonts[position].second
                updateTextTypeface()
            }
        }
    }

    fun clearEditor() {
        cropper_view.setVisibility(false)
        photoEditorView.setVisibility(true)
        vBrushSlider.setVisibility(false)

        photoEditor.clearAllViews()
        photoEditor.setBrushDrawingMode(false)
        photoEditorView.source.setImageBitmap(null)
        cropper_view.release()

        isErasing = false
        isDrawing = false
        setInputTextGroupVisibility(false)
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
        showCroppingView()
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

    private fun showCroppingView() {
        photoEditor.saveAsBitmap(BitmapSaveListener({ imageBitmap ->
            cropper_view.apply {
                setImageBitmap(imageBitmap)
                setVisibility(true)
            }
            croppingControls.setVisibility(true)
            setInputTextGroupVisibility(false)
            vBrushSlider.setVisibility(false)
            photoEditorView.setVisibility(false)
        }))
    }

    private fun hideCroppingView() {
        cropper_view.apply {
            setVisibility(false)
            release()
        }
        croppingControls.setVisibility(false)
        vBrushSlider.setVisibility(isDrawing || isErasing)
        photoEditorView.setVisibility(true)
    }

    private fun applyCropping() {
        cropper_view.getCroppedBitmapAsync(CropCallback {
            it?.let(::initEditor)
            hideCroppingView()
        })
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
        updateTextTypeface()
    }

    private fun setInputTextItalic() {
        isTextItalic = !isTextItalic
        updateTextTypeface()
    }

    private fun updateTextTypeface() {
        etTextInput.setTypeface(currentTypeface, getTextStyle())
    }

    private fun setInputTextUnderlined() {
        isTextUnderlined = !isTextUnderlined
        etTextInput.paintFlags = if (isTextUnderlined) {
            etTextInput.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else {
            etTextInput.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
        }
    }

    private fun getTextStyle(): Int = if (isTextBold) {
        if (isTextItalic) Typeface.BOLD_ITALIC else Typeface.BOLD
    } else {
        if (isTextItalic) Typeface.ITALIC else Typeface.NORMAL
    }

    private fun setInputTextGroupVisibility(isVisible: Boolean) {
        textAddingGroup.setVisibility(isVisible)
        etTextInput.resetInputText()
        vTypefacesSpinner.setSelection(0)
        if (isVisible) {
            etTextInput.getFocusWithKeyboard(inputMethodManager)
        } else {
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
        vBrushSlider.setVisibility(!isVisible && (isDrawing || isErasing))
    }

    private fun EditText.resetInputText() {
        setText("")
        textSize = minTextSize.toFloat()
        gravity = Gravity.LEFT
        currentTypeface = null
        isTextBold = false
        isTextItalic = false
        isTextUnderlined = false
        updateTextTypeface()
    }

    companion object {
        private const val BRUSH_SIZE_ANIMATION_DURATION = 150L
        private val SELECTED_ACTION_TINT = Color.parseColor("#FFD32F2F")
    }
}
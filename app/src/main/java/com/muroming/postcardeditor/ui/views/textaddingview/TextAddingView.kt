package com.muroming.postcardeditor.ui.views.textaddingview

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.listeners.OnKeyboardShownListener
import com.muroming.postcardeditor.ui.views.editorview.OutlinedText
import com.muroming.postcardeditor.utils.applyStyle
import com.muroming.postcardeditor.utils.setVisibility
import com.muroming.postcardeditor.utils.toSp
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.text_adding_view.view.*

class TextAddingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    lateinit var fragmentManager: FragmentManager
    lateinit var keyboardListener: OnKeyboardShownListener

    private var isTextBold = false
    private var isTextItalic = false
    private var isTextOutlined = false
    private var selectedOutlineColor = ContextCompat.getColor(context, R.color.red)

    private val minTextSize = resources.getDimensionPixelSize(R.dimen.min_text_size).toFloat()
    private val maxTextSize = resources.getDimensionPixelSize(R.dimen.max_text_size).toFloat()

    private val minTextHeightScale = 1f
    private val maxTextHeightScale = 2f

    private val minTextSpacing = 0f
    private val maxTextSpacing = 1f

    private var currentTypeface: Int? = null
    private var editedTextHolder: ViewGroup? = null

    private val inputMethodManager: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private val editorFonts = listOf(
        "Обычный" to null,
        "Amaranth" to R.font.amaranth,
        "Elysium" to R.font.elysium,
        "Honey Beeg" to R.font.honey_beeg,
        "Meamury" to R.font.meamury,
        "Reload" to R.font.reload,
        "Zabava" to R.font.zabava
    )
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    lateinit var textListener: TextAddingViewListener

    init {
        LayoutInflater.from(context).inflate(R.layout.text_adding_view, this, true)
        initTextControls()
        setupFontsAdapter()
    }

    private fun setupFontsAdapter() {
        spinnerAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            editorFonts.map(Pair<String, Int?>::first)
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

    private fun initTextControls() {
        ivTextToLeft.setOnClickListener { etTextInput.gravity = Gravity.LEFT }
        ivTextToRight.setOnClickListener { etTextInput.gravity = Gravity.RIGHT }
        ivTextToCenter.setOnClickListener { etTextInput.gravity = Gravity.CENTER }

        ivBoldText.setOnClickListener { setInputTextBold() }
        ivItalicText.setOnClickListener { setInputTextItalic() }
        ivOutlineText.setOnClickListener {
            isTextOutlined = !isTextOutlined
            setInputTextOutlined()
        }
        ivOutlineText.setOnLongClickListener { selectOutlineColor(); true; }

        vInputTextBackground.setOnClickListener {
            textListener.onTextReady(
                etTextInput.text.toString(),
                etTextInput.gravity,
                etTextInput.textSize.toSp(),
                etTextInput.currentTextColor,
                if (isTextOutlined) selectedOutlineColor else null,
                etTextInput.typeface,
                TextViewStyle(currentTypeface, isTextBold, isTextItalic, etTextInput.scaleY, etTextInput.letterSpacing)
            )
        }
        vTextSizeSlider.setOnSeekBarChangeListener(
            SeekBarListener(
                minTextSize,
                maxTextSize
            ) { value ->
                etTextInput.textSize = value
            })
        vSpacingSlider.setOnSeekBarChangeListener(
            SeekBarListener(
                minTextSpacing,
                maxTextSpacing
            ) { value ->
                etTextInput.letterSpacing = value
            }
        )

        vTextHeightSlider.setOnSeekBarChangeListener(
            SeekBarListener(
                minTextHeightScale,
                maxTextHeightScale
            ) { value ->
                etTextInput.scaleY = value
            }
        )
    }

    private fun selectOutlineColor() {
        ColorSheet().colorPicker(
            colors = resources.getIntArray(R.array.paletteColors),
            selectedColor = selectedOutlineColor,
            noColorOption = false
        ) { newColor ->
            selectedOutlineColor = newColor
            setInputTextOutlined()
        }.show(fragmentManager)
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
        etTextInput.setTypeface(
            currentTypeface?.let { ResourcesCompat.getFont(context, it) },
            getTextStyle()
        )
    }

    private fun setInputTextOutlined() {
        if (isTextOutlined) {
            etTextInput.setShadowLayer(
                etTextInput.textSize / 20,
                0f, 0f,
                selectedOutlineColor
            )
        } else {
            etTextInput.setShadowLayer(0f, 0f, 0f, 0)
        }
    }

    private fun getTextStyle(): Int = if (isTextBold) {
        if (isTextItalic) Typeface.BOLD_ITALIC else Typeface.BOLD
    } else {
        if (isTextItalic) Typeface.ITALIC else Typeface.NORMAL
    }

    fun setInputTextGroupVisibility(isVisible: Boolean) {
        setVisibility(isVisible)
        etTextInput.apply {
            resetInputText()
            manageFocusWithKeyboard(isVisible)
        }
        vTypefacesSpinner.setSelection(0)
    }

    fun setTextColor(color: Int) {
        etTextInput.setTextColor(color)
    }

    fun onBackPressed() {
        if (editedTextHolder == null) {
            setInputTextGroupVisibility(false)
        } else {
            textListener.onTextReady(
                etTextInput.text.toString(),
                etTextInput.gravity,
                etTextInput.textSize.toSp(),
                etTextInput.currentTextColor,
                if (isTextOutlined) selectedOutlineColor else null,
                etTextInput.typeface,
                TextViewStyle(currentTypeface, isTextBold, isTextItalic, etTextInput.scaleY, etTextInput.letterSpacing)
            )
        }
    }

    private fun EditText.manageFocusWithKeyboard(shouldRequestFocus: Boolean) {
        handler?.postDelayed(
            {
                if (shouldRequestFocus) {
                    if (requestFocus()) {
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                        keyboardListener.onKeyboardVisible()
                    }
                } else {
                    clearFocus()
                    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                    keyboardListener.onKeyboardHidden()
                }
            },
            EDITTEXT_FOCUS_REQUEST_DELAY
        )
    }

    private fun EditText.resetInputText() {
        setText("")
        textSize = minTextSize
        scaleY = 1f
        letterSpacing = 0f
        gravity = Gravity.START
        etTextInput.setShadowLayer(0f, 0f, 0f, 0)
        currentTypeface = null
        editedTextHolder = null
        isTextBold = false
        isTextItalic = false
        isTextOutlined = false
        setTypeface(null, Typeface.NORMAL)
        setSlidersValues()
    }

    fun editText(textHolder: ViewGroup, textStyle: TextViewStyle) {
        setInputTextGroupVisibility(true)
        val textView = textHolder.children.first() as TextView
        editedTextHolder = textHolder

        etTextInput.apply {
            setText(textView.text)
            textSize = textView.textSize.toSp()
            gravity = textView.gravity
            applyStyle(textStyle)
            if (textView is OutlinedText) {
                isTextOutlined = true
                selectedOutlineColor = textView.strokeColor
                etTextInput.setShadowLayer(
                    etTextInput.textSize / 20,
                    0f, 0f,
                    selectedOutlineColor
                )
            }
        }
        currentTypeface = textStyle.typeface
        isTextBold = textStyle.isBold
        isTextItalic = textStyle.isItalic

        val fontIndex = editorFonts.indexOfFirst { (_, font) ->
            currentTypeface == font
        }
        vTypefacesSpinner.setSelection(fontIndex)
        updateTextTypeface()
        setSlidersValues()
    }

    private fun setSlidersValues() {
        val textSizeValue = (etTextInput.textSize.toSp() - minTextSize) / (maxTextSize - minTextSize) * 100
        val spacingValue = (etTextInput.letterSpacing - minTextSpacing) / (maxTextSpacing - minTextSpacing) * 100
        val scaleYValue = (etTextInput.scaleY - minTextHeightScale) / (maxTextHeightScale - minTextHeightScale) * 100

        vTextSizeSlider.progress = textSizeValue.toInt()
        vSpacingSlider.progress = spacingValue.toInt()
        vTextHeightSlider.progress = scaleYValue.toInt()
    }

    companion object {
        private const val EDITTEXT_FOCUS_REQUEST_DELAY = 30L
    }
}
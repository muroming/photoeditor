package com.muroming.postcardeditor.ui.views.textaddingview

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.ui.views.editorview.PhotoEditorView
import com.muroming.postcardeditor.utils.setVisibility
import com.muroming.postcardeditor.utils.toSp
import kotlinx.android.synthetic.main.text_adding_view.view.*

class TextAddingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var isTextBold = false
    private var isTextItalic = false
    private var isTextOutlined = false

    private val minTextSize = resources.getDimensionPixelSize(R.dimen.min_text_size)
    private val maxTextSize = resources.getDimensionPixelSize(R.dimen.max_text_size)
    private var currentTypeface: Typeface? = null

    private val inputMethodManager: InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

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

    lateinit var textListener: TextAddingViewListener

    init {
        LayoutInflater.from(context).inflate(R.layout.text_adding_view, this, true)
        initTextControls()
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

    private fun initTextControls() {
        ivTextToLeft.setOnClickListener { etTextInput.gravity = Gravity.LEFT }
        ivTextToRight.setOnClickListener { etTextInput.gravity = Gravity.RIGHT }
        ivTextToCenter.setOnClickListener { etTextInput.gravity = Gravity.CENTER }

        ivBoldText.setOnClickListener { setInputTextBold() }
        ivItalicText.setOnClickListener { setInputTextItalic() }
        ivOutlineText.setOnClickListener { setInputTextOutlined() }

        vInputTextBackground.setOnClickListener {
            textListener.onTextReady(
                etTextInput.text.toString(),
                etTextInput.gravity,
                etTextInput.textSize.toSp(),
                etTextInput.currentTextColor,
                if (isTextOutlined) ContextCompat.getColor(context, R.color.red) else null,
                currentTypeface
            )
        }
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

    private fun setInputTextOutlined() {
        isTextOutlined = !isTextOutlined
        if (isTextOutlined) {
            etTextInput.setShadowLayer(
                etTextInput.textSize / PhotoEditorView.TEXT_OUTLINE_RATIO,
                0f,0f, ContextCompat.getColor(context, R.color.red)
            )
        } else {
            etTextInput.setShadowLayer(
                0f, 0f, 0f, 0
            )
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

    private fun EditText.manageFocusWithKeyboard(shouldRequestFocus: Boolean) {
        handler?.postDelayed(
            {
                if (shouldRequestFocus) {
                    if (requestFocus()) {
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                    }
                } else {
                    clearFocus()
                    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                }
            },
            EDITTEXT_FOCUS_REQUEST_DELAY
        )
    }

    private fun EditText.resetInputText() {
        setText("")
        textSize = minTextSize.toFloat()
        gravity = Gravity.LEFT
        currentTypeface = null
        isTextBold = false
        isTextItalic = false
        isTextOutlined = false
        isTextOutlined = false
        updateTextTypeface()
    }

    companion object {
        private const val EDITTEXT_FOCUS_REQUEST_DELAY = 30L
    }
}
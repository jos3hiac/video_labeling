package com.videolabeling.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import com.videolabeling.R

abstract class BaseInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    protected val textInput: EditText
    protected val buttonDecrement: ImageButton
    protected val buttonIncrement: ImageButton

    var onIncrementClick: (() -> Unit)? = null
    var onDecrementClick: (() -> Unit)? = null
    var onDonePressed: ((String) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.base_input, this, true)

        textInput = findViewById(R.id.textInput)
        buttonDecrement = findViewById(R.id.buttonDecrement)
        buttonIncrement = findViewById(R.id.buttonIncrement)

        textInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                onDonePressed?.invoke(textInput.text.toString())
                true
            } else {
                false
            }
        }
        buttonDecrement.setOnClickListener {
            onDecrementClick?.invoke()
            //decrement()
        }
        buttonIncrement.setOnClickListener {
            onIncrementClick?.invoke()
            //increment()
        }
    }

    fun setDecrementButtonEnabled(isEnabled: Boolean){
        setButtonEnabled(buttonDecrement,isEnabled)
    }
    fun setIncrementButtonEnabled(isEnabled: Boolean){
        setButtonEnabled(buttonIncrement,isEnabled)
    }
    private fun setButtonEnabled(button: ImageButton,isEnabled: Boolean){
        button.isEnabled = isEnabled
        button.alpha = if(isEnabled) 1f else 0.5f
    }
    /*protected abstract fun increment()
    protected abstract fun decrement()
    protected abstract fun validateInput()*/
}

class NumberInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseInput(context, attrs, defStyleAttr) {

    var value: Int? = null
        set(value) {
            field = value
            updateTextInput()
        }

    /*init {
        // Manejar cambios directamente desde el EditText
        textInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateInput()
            }
        }
    }*/
    /*private fun validateInput() {
        value = textInput.text.toString().toIntOrNull()
    }*/
    private fun updateTextInput() {
        if (value == null) {
            textInput.setText("")
        } else {
            textInput.setText(value.toString())
        }
    }
}

class TimeInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseInput(context, attrs, defStyleAttr) {

    var hours: Int = 0
    var minutes: Int = 0
    var seconds: Int = 0
    var milliseconds: Int = 0

    private fun updateText() {
        textInput.setText(
            String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, milliseconds)
        )
    }

    fun increment() {
        milliseconds += 100
        if (milliseconds >= 1000) {
            milliseconds = 0
            seconds++
        }
        if (seconds >= 60) {
            seconds = 0
            minutes++
        }
        if (minutes >= 60) {
            minutes = 0
            hours++
        }
        updateText()
    }

    fun decrement() {
        milliseconds -= 100
        if (milliseconds < 0) {
            milliseconds = 900
            seconds--
        }
        if (seconds < 0) {
            seconds = 59
            minutes--
        }
        if (minutes < 0) {
            minutes = 59
            hours--
        }
        updateText()
    }

    fun validateInput() {
        val parts = textInput.text.toString().split(":").map { it.toIntOrNull() ?: 0 }
        if (parts.size == 4) {
            hours = parts[0]
            minutes = parts[1]
            seconds = parts[2]
            milliseconds = parts[3]
        }
        updateText()
    }
}

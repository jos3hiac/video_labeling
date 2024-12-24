package com.videolabeling.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.videolabeling.R

class ButtonControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    protected val button: ImageButton
    protected val textView: TextView

    var onButtonClick: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.button_control, this, true)
        button = findViewById(R.id.imageButton)
        textView = findViewById(R.id.textView)
        setEnabled(true)

    }
    override fun setEnabled(enabled: Boolean){
        super.setEnabled(enabled)
        if(enabled){
            button.alpha = 1f
            button.setOnClickListener {
                onButtonClick?.invoke()
            }
        }
        else{
            button.alpha = 0.5f
            button.setOnClickListener(null)
        }
    }
    fun getText(): String {
        return textView.text.toString()
    }
    fun setIcon(resourceId: Int){
        button.setImageResource(resourceId)
    }
    fun setText(text: String){
        textView.text = text
    }
}
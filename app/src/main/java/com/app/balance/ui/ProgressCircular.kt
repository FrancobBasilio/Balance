package com.app.balance.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ProgressCircular @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)

    private var porcentajeAhorroDisponible = 100f
    private var colorPrincipal = context.getColor(android.R.color.holo_blue_light)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }


    fun setValues(
        porcentajeAhorroDisponible: Float,
        color: Int
    ) {
        this.porcentajeAhorroDisponible = porcentajeAhorroDisponible.coerceIn(0f, 100f)
        this.colorPrincipal = color

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(width, height) / 2.3f
        val strokeWidth = 126f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.ROUND

        paint.setShadowLayer(12f, 0f, 4f, 0x40000000)

        paint.color = colorPrincipal
        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.clearShadowLayer()

        paintText.style = Paint.Style.FILL
        paintText.textAlign = Paint.Align.CENTER
        paintText.typeface = android.graphics.Typeface.DEFAULT_BOLD

        paintText.color = 0xFF000000.toInt()
        paintText.textSize = 110f
        canvas.drawText("${porcentajeAhorroDisponible.toInt()}%", centerX, centerY + 35f, paintText)

        paintText.textSize = 28f
        paintText.color = 0xFF666666.toInt()
        paintText.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("ahorro disponible", centerX, centerY + 75f, paintText)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = Math.min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }
}
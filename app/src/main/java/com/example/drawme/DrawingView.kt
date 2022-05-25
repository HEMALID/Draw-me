package com.example.drawme

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class DrawingView(con: Context, attr: AttributeSet) : View(con, attr),
    GestureDetector.OnGestureListener {

    private var mDrawPath: CustomPath?=null
    private var mCanvasBitmap: Bitmap?=null
    private var mDrawPaint: Paint?=null
    private var mCanvasPaint: Paint?=null
    private var mBrashSize: Float=0.toFloat()
    private var color=Color.BLACK
    private var canvas: Canvas?=null
    private var mPaths=ArrayList<CustomPath>()

    private val undoPaths=ArrayList<CustomPath>()

    lateinit var gestureDetector: GestureDetector
    var x1: Float=0.0f
    var x2: Float=0.0f
    var y1: Float=0.0f
    var y2: Float=0.0f

    companion object {
        const val MinDistance=150
    }

    init {
        setUpDrawing()
    }

    private fun setUpDrawing() {

        mDrawPaint=Paint()
        mDrawPath=CustomPath(color, mBrashSize)
        mDrawPaint!!.color=color
        mDrawPaint!!.style=Paint.Style.STROKE
        mDrawPaint!!.strokeJoin=Paint.Join.ROUND
        mDrawPaint!!.strokeCap=Paint.Cap.ROUND
        mCanvasPaint=Paint(Paint.DITHER_FLAG)
        /*mBrashSize = 20.toFloat()*/

        gestureDetector=GestureDetector(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap=Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas=Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        for (path in mPaths) {
            mDrawPaint!!.strokeWidth=path.brashThickness
            mDrawPaint!!.color=path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        if (!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth=mDrawPath!!.brashThickness
            mDrawPaint!!.color=mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
        canvas.drawPath(mDrawPath!!, mDrawPaint!!)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        gestureDetector.onTouchEvent(event)

        val touchX=event?.x
        val touchY=event?.y

        when (event?.action) {

            0 -> {
                x1=event.x
                y1=event.y
            }
            1 -> {
                x2=event.x
                y2=event.y

                val valueX: Float=x2 - x1
                val valuey: Float=y2 - y1

                if (abs(valueX) > MinDistance) {
                    if (x2 > x1) {
                        Log.e("TAG","FROM TAG1")
                    } else {
                        Log.e("TAG","FROM TAG2")
                    }
                } else if (abs(valueX) > MinDistance) {
                    if (y2 > y1) {
                        Log.e("TAG","FROM TAG3")
                    } else {
                        Log.e("TAG","FROM TAG4")
                    }
                }
            }

            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color=color
                mDrawPath!!.brashThickness=mBrashSize

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!)
                mDrawPath=CustomPath(color, mBrashSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float) {
        mBrashSize=TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth=mBrashSize
    }

    fun setColor(newColor: String) {
        color=Color.parseColor(newColor)
        mDrawPaint!!.color=color
    }

    fun clear() {
        mDrawPath!!.reset()
        canvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mPaths.clear()
        invalidate()
    }

    fun undo() {
        if (mPaths.size > 0) {
            undoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }

    fun redo() {
        if (undoPaths.size > 0) {
            mPaths.add(undoPaths.removeAt(undoPaths.size - 1))
            invalidate()
        }
    }

    internal inner class CustomPath(
        var color: Int,
        var brashThickness: Float
    ) : Path() {}

    override fun onDown(p0: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onShowPress(p0: MotionEvent?) {
        TODO("Not yet implemented")
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun onLongPress(p0: MotionEvent?) {
        TODO("Not yet implemented")
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        TODO("Not yet implemented")
    }

}
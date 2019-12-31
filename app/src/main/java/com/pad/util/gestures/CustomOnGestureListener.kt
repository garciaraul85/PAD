package com.pad.util.gestures

import android.content.Context
import android.os.Handler
import android.os.Message
import android.view.GestureDetector.OnDoubleTapListener
import android.view.GestureDetector.OnGestureListener
import android.view.MotionEvent
import android.view.ViewConfiguration
import java.util.*

class CustomOnGestureListener(context: Context) : OnGestureListener, OnDoubleTapListener {

    val TAG = CustomOnGestureListener::class.java.simpleName

    private var mCurrentDownEvent: MotionEvent? = null

    private var mPtrCount = 0

    private var mPrimStartTouchEventX = 0f
    private var mPrimStartTouchEventY = 0f
    private var mSecStartTouchEventX = 0f
    private var mSecStartTouchEventY = 0f
    private var mPrimSecStartTouchDistance = 0f

    private var downTimestamp = System.currentTimeMillis()

    private val gesture: IGesture
    var nodeList = LinkedHashMap<Long, Int>()

    internal var mHandler: GestureHandler

    inner class GestureHandler : Handler {
        internal constructor() : super() {}

        internal constructor(handler: Handler) : super(handler.looper) {}

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                TAP ->
                    // prepend("TAP Finger Count "+mCurrentDownEvent.getPointerCount());
                    onSingleTapConfirmed(mCurrentDownEvent)
                DOUBLE_TAP ->
                    // prepend("Double Tap "+mCurrentDownEvent.getPointerCount());
                    onDoubleTapConfirmed(mCurrentDownEvent!!)
                else -> throw RuntimeException("Unknown message $msg") // never
            }
        }
    }

    init {
        mHandler = GestureHandler()
        gesture = context as IGesture

        val viewConfig = ViewConfiguration.get(context)
        mViewScaledTouchSlop = viewConfig.scaledTouchSlop.toFloat()
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action and MotionEvent.ACTION_MASK

        // prepend("onTouchEvent() ptrs:" + ev.getPointerCount() + " "
        // + actionToString(action));
        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                mPtrCount++
                if (ev.pointerCount > 1) {
                    mSecStartTouchEventX = ev.getX(1)
                    mSecStartTouchEventY = ev.getY(1)
                    mPrimSecStartTouchDistance = distance(ev, 0, 1)

                    if (mCurrentDownEvent != null)
                        mCurrentDownEvent!!.recycle()
                    mCurrentDownEvent = MotionEvent.obtain(ev)

                    if (System.currentTimeMillis() - downTimestamp > 50) {

                        if (!mHandler.hasMessages(TAP)) {
                            mHandler.sendEmptyMessageDelayed(
                                TAP,
                                    DOUBLE_TAP_TIMEOUT
                            )
                        } else {
                            mHandler.removeMessages(TAP)
                            mHandler.sendEmptyMessageDelayed(
                                DOUBLE_TAP,
                                    DOUBLE_TAP_TIMEOUT
                            )
                        }

                    }

                    downTimestamp = System.currentTimeMillis()

                    // return true to prevent other actions.
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> mPtrCount--
            MotionEvent.ACTION_DOWN -> mPtrCount++
            MotionEvent.ACTION_UP -> mPtrCount--
        }

        return false
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        // prepend("onDoubleTap() ptrs:" + e.getPointerCount());
        if (mCurrentDownEvent!!.pointerCount == 1) {
            println("onDoubleTap() ptrs:" + mCurrentDownEvent!!.pointerCount)
            mHandler.sendEmptyMessageDelayed(DOUBLE_TAP, DOUBLE_TAP_TIMEOUT)
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        // prepend("onDoubleTapEvent() ptrs:" + e.getPointerCount());

        return true
    }

    /**
     * We don't want to ignore the old double tap method. Or in onDoubleTap
     * ignore MotionEvent and use our mCurrentDownEvent and don't call double
     * tap in the handler
     *
     * @param e
     * @return
     */
    fun onDoubleTapConfirmed(e: MotionEvent): Boolean {
        println("onDoubleTapConfirmed() ptrs:" + e.pointerCount)

        if (mPtrCount == 1) {
            println("onDoubleTapConfirmed(): tap and a half")
        }
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        if (!mHandler.hasMessages(TAP) && mCurrentDownEvent!!.pointerCount == e!!.pointerCount)
            println("onSingleTapConfirmed() ptrs:" + e.pointerCount)

        if (mPtrCount == 1 && mCurrentDownEvent!!.pointerCount == 2) {
            // one finger is still down and a single tap occured
            println("onSingleTapConfirmed(): One finger down, one finger tap")
        }
        return true
    }

    override fun onDown(e: MotionEvent): Boolean {
        // prepend("onDown() ptrs:" + e.getPointerCount());
        if (mCurrentDownEvent != null)
            mCurrentDownEvent!!.recycle()
        mCurrentDownEvent = MotionEvent.obtain(e)

        mPrimStartTouchEventX = e.x
        mPrimStartTouchEventY = e.y

        return true
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                         velocityY: Float): Boolean {
        // prepend("onFling() ptrs:e1:" + e1.getPointerCount() + " e2:"
        // + e2.getPointerCount() +
        // " last: "+mCurrentDownEvent.getPointerCount());
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        gesture.onLongPressItem()
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float,
                          distanceY: Float): Boolean {
        // e1 The first down motion event that started the scrolling.
        // e2 The move motion event that triggered the current onScroll.

        val scroll = isScrollGesture(e2)
        val pinch = isPinchGesture(e2)

        if (scroll || pinch)
            cancelAll()

        return true
    }

    override fun onShowPress(e: MotionEvent) {
        // prepend("onShowPress() ptrs:" + e.getPointerCount());

    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // prepend("onSingleTapUp() ptrs:" + e.getPointerCount());
        return true
    }

    private fun cancelAll() {
        if (mHandler.hasMessages(TAP))
            mHandler.removeMessages(TAP)
    }

    private fun isPinchGesture(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            val distanceCurrent = distance(event, 0, 1)
            val diffPrimX = mPrimStartTouchEventX - event.getX(0)
            val diffPrimY = mPrimStartTouchEventY - event.getY(0)
            val diffSecX = mSecStartTouchEventX - event.getX(1)
            val diffSecY = mSecStartTouchEventY - event.getY(1)

            if (// if the distance between the two fingers has increased past
            // our threshold
                    Math.abs(distanceCurrent - mPrimSecStartTouchDistance) > mViewScaledTouchSlop
                    // and the fingers are moving in opposing directions
                    && diffPrimY * diffSecY <= 0
                    && diffPrimX * diffSecX <= 0) {
                // mPinchClamp = false; // don't clamp initially
                return true
            }
        }

        return false
    }

    fun distance(event: MotionEvent, first: Int, second: Int): Float {
        if (event.pointerCount >= 2) {
            val x = event.getX(first) - event.getX(second)
            val y = event.getY(first) - event.getY(second)

            return Math.sqrt((x * x + y * y).toDouble()).toFloat()
        } else {
            return 0f
        }
    }

    private fun isScrollGesture(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            val diffPrim = mPrimStartTouchEventY - event.getY(0)
            val diffSec = mSecStartTouchEventY - event.getY(1)

            if (// make sure both fingers are moving in the same direction
                    diffPrim * diffSec > 0
                    // make sure both fingers have moved past the scrolling
                    // threshold
                    && Math.abs(diffPrim) > mViewScaledTouchSlop
                    && Math.abs(diffSec) > mViewScaledTouchSlop
            ) {
                return true
            }
        } else if (event.pointerCount == 1) {
            val diffPrim = mPrimStartTouchEventY - event.getY(0)
            if (// make sure finger has moved past the scrolling threshold
                    Math.abs(diffPrim) > mViewScaledTouchSlop) {
                return true
            }
        }

        return false
    }

    companion object {

        val TAP = 0
        val DOUBLE_TAP = 1
        val LONG_PRESS = 1

        val DOUBLE_TAP_TIMEOUT = ViewConfiguration
                .getDoubleTapTimeout().toLong()
        var mViewScaledTouchSlop: Float = 0.toFloat()

        // Given an action int, returns a string description
        fun actionToString(action: Int): String {
            when (action) {
                MotionEvent.ACTION_DOWN -> return "*Down"
                MotionEvent.ACTION_MOVE -> return "*Move"
                MotionEvent.ACTION_POINTER_DOWN -> return "*Pointer Down"
                MotionEvent.ACTION_UP -> return "*Up"
                MotionEvent.ACTION_POINTER_UP -> return "*Pointer Up"
                MotionEvent.ACTION_OUTSIDE -> return "*Outside"
                MotionEvent.ACTION_CANCEL -> return "*Cancel"
            }
            return ""
        }
    }

}
package com.pad.util.gestures

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent

class CustomGestureDetector(context: Context, internal var mListener: CustomOnGestureListener?) : GestureDetector(context, mListener) {

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val consume = mListener != null && mListener!!.onTouchEvent(ev)
        return consume || super.onTouchEvent(ev)
    }
}
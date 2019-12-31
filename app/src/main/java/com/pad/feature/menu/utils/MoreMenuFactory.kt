package com.pad.feature.menu.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.pad.R
import com.skydoves.powermenu.*
import com.skydoves.powermenu.kotlin.createPowerMenu

class MoreMenuFactory : PowerMenu.Factory() {

    override fun create(context: Context, lifecycle: LifecycleOwner): PowerMenu {
        return createPowerMenu(context) {
            setAutoDismiss(true)
            setLifecycleOwner(lifecycle)
            setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
            setCircularEffect(CircularEffect.BODY)
            setMenuRadius(10f)
            setMenuShadow(10f)
            setTextColorResource(R.color.md_grey_800)
            setTextSize(12)
            setTextGravity(Gravity.CENTER)
            setTextTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
            setSelectedTextColor(Color.WHITE)
            setMenuColor(Color.WHITE)
            setShowBackground(false)
            setSelectedMenuColorResource(R.color.colorPrimary)
            setInitializeRule(Lifecycle.Event.ON_CREATE, 0)
        }
    }
}
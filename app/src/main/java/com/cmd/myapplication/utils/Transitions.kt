package com.cmd.myapplication.utils

import android.animation.TimeInterpolator
import android.content.Context
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.motion.MotionUtils
import com.google.android.material.R as MaterialResources

object Transitions {
    fun getInterpolator(context: Context, type: Type): TimeInterpolator {
        val interpolatorAttr: Int = when (type) {
            Type.BEGIN_END_ON_SCREEN -> {
                MaterialResources.attr.motionEasingEmphasizedInterpolator
            }

            Type.ENTER -> {
                MaterialResources.attr.motionEasingEmphasizedDecelerateInterpolator
            }

            else -> {
                MaterialResources.attr.motionEasingEmphasizedAccelerateInterpolator
            }
        }

        return MotionUtils.resolveThemeInterpolator(
            context,
            interpolatorAttr,
            FastOutSlowInInterpolator()
        )
    }

    fun getDuration(context: Context, type: Type): Long {
        val durationAttr: Int = when (type) {
            Type.BEGIN_END_ON_SCREEN -> {
                MaterialResources.attr.motionDurationLong2
            }

            Type.ENTER -> {
                MaterialResources.attr.motionDurationMedium4
            }

            else -> {
                MaterialResources.attr.motionDurationShort4
            }
        }

        return MotionUtils.resolveThemeDuration(
            context,
            durationAttr,
            0,
        ).toLong()
    }

    enum class Type {
        BEGIN_END_ON_SCREEN,
        ENTER,
        EXIT
    }
}
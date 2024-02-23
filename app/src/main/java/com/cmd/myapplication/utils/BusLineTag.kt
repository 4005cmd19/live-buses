package com.cmd.myapplication.utils

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.cmd.myapplication.R
import com.cmd.myapplication.toDp
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel

object BusLineTag {
    const val TAG_CORNER_SIZE_DP = 8
    const val TAG_GROUP_INNER_CORNER_SIZE_DP = 4

    private val LAYOUT_ID = R.layout.tag_view_layout
    private val ELLIPSIZED_LAYOUT_ID = R.layout.ellipsized_tag_view_layout

    fun create(
        context: Context,
        name: CharSequence,
        backgroundColor: Int = Color.RED,
        contentColor: Int = Color.WHITE,
    ): View {
        val view = View.inflate(context, LAYOUT_ID, null) as MaterialCardView
        view.layoutParams =
            MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
        view.setCardBackgroundColor(backgroundColor)

        val cornerSize = TAG_CORNER_SIZE_DP.toDp(context).toFloat()
        view.shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
            setTopLeftCornerSize(cornerSize)
            setBottomLeftCornerSize(cornerSize)
            setTopRightCornerSize(cornerSize)
            setBottomRightCornerSize(cornerSize)
        }.build()

        val nameView: TextView = view.findViewById(R.id.name)
        nameView.setTextColor(contentColor)

        nameView.text = name

        return view
    }

    fun createStyled(
        context: Context,
        name: CharSequence,
        @AttrRes backgroundColor: Int,
        @AttrRes contentColor: Int,
    ): View {
        @ColorInt
        val background = resolveColor(context, backgroundColor)

        @ColorInt
        val content = resolveColor(context, contentColor)

        return create(context, name, background, content)
    }

    fun createEllipsized(
        context: Context,
        backgroundColor: Int = Color.RED,
        contentColor: Int = Color.WHITE,
    ): Pair<View, Int> {
        val view = View.inflate(context, ELLIPSIZED_LAYOUT_ID, null) as MaterialCardView

        view.layoutParams =
            MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)

        view.setCardBackgroundColor(backgroundColor)

        val iconView: ImageView = view.findViewById(R.id.icon_view)
        iconView.setColorFilter(contentColor)

        val id = View.generateViewId().also { view.id = it }

        return Pair(view, id)
    }

    fun createStyledEllipsized(
        context: Context,
        @AttrRes backgroundColor: Int,
        @AttrRes contentColor: Int,
    ): Pair<View, Int> {
        @ColorInt
        val background = resolveColor(context, backgroundColor)

        @ColorInt
        val content = resolveColor(context, contentColor)

        return createEllipsized(context, background, content)
    }

    fun createGroup(
        context: Context,
        root: ViewGroup,
        names: List<CharSequence>,
        maxWidth: Int,
        tagBackgroundColor: Int,
        tagContentColor: Int,
        ellipsizedTagBackgroundColor: Int,
        ellipsizedTagContentColor: Int,
    ) {
        for (line in names) {
            val lineView = create(
                context,
                line,
                tagBackgroundColor,
                tagContentColor
            )

            lineView.updateLayoutParams<MarginLayoutParams> {
                leftMargin = 4.toDp(context)
                rightMargin = 4.toDp(context)
            }

            root.addView(lineView)
        }

        val (ellipsizedTag, id) = createEllipsized(
            context,
            ellipsizedTagBackgroundColor,
            ellipsizedTagContentColor
        )

        root.addView(ellipsizedTag)

        root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                root.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val ellipsizedTagWidth = ellipsizedTag.width

                val toRemove = mutableListOf<View>()

                val lineViews = root.children.toList().filterNot { it.id == id }

                for (i in 0..lineViews.lastIndex) {
                    val lineView = lineViews[i]

                    if (i != lineViews.lastIndex) {
                        if (lineView.right + ellipsizedTagWidth > maxWidth) {
                            toRemove.add(lineView)
                        }
                    } else {
                        if (lineView.right > maxWidth) {
                            toRemove.add(lineView)
                        }
                    }
                }

                toRemove.forEach { root.removeView(it) }
            }
        })
    }

    fun createStyledGroup(
        context: Context,
        root: ViewGroup,
        names: List<CharSequence>,
        maxWidth: Int,
        @AttrRes tagBackgroundColor: Int,
        @AttrRes tagContentColor: Int,
        @AttrRes ellipsizedTagBackgroundColor: Int,
        @AttrRes ellipsizedTagContentColor: Int,
    ) {
        val tagBackground = resolveColor(context, tagBackgroundColor)
        val tagContent = resolveColor(context, tagContentColor)
        val ellipsizedTagBackground = resolveColor(context, ellipsizedTagBackgroundColor)
        val ellipsizedTagContent = resolveColor(context, ellipsizedTagContentColor)

        return createGroup(
            context,
            root,
            names,
            maxWidth,
            tagBackground,
            tagContent,
            ellipsizedTagBackground,
            ellipsizedTagContent
        )
    }

    @ColorInt
    private fun resolveColor(context: Context, @AttrRes color: Int): Int = with(TypedValue()) {
        context.theme.resolveAttribute(color, this, true)
        this.data
    }
}
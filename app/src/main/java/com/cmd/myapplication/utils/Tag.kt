package com.cmd.myapplication.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.cmd.myapplication.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.Shapeable

@SuppressLint("RestrictedApi")
class Tag(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = R.attr.tagStyle,
    defStyleRes: Int = R.style.Tag,
) : CoordinatorLayout(
    context, attrs, defStyleAttr
), Shapeable {
    private var _text: CharSequence? = null

    var text: CharSequence?
        get() = _text
        set(value) {
            _text = value

            _icon = null
            tagIconView.visibility = GONE
            tagTextView.visibility = VISIBLE
            tagTextView.text = value

            invalidate()
            requestLayout()
            tagTextView.invalidate()
            tagTextView.requestLayout()
        }

    private var _icon: Drawable? = null
    var icon: Drawable?
        get() = _icon
        set(value) {
            _icon = value

            _text = null
            tagTextView.visibility = GONE
            tagIconView.visibility = VISIBLE
            tagIconView.setImageDrawable(value)

            invalidate()
            requestLayout()
        }

    private var _foregroundColor: Int = Color.BLACK
    var foregroundColor: Int
        get() = _foregroundColor
        set(value) {
            _foregroundColor = value
            tagTextView.setTextColor(value)
            tagIconView.setColorFilter(value)
        }

    private val cardView: MaterialCardView
    private val tagTextView: TextView
    private val tagIconView: ImageView

    init {
        inflate(context, LAYOUT_ID, this)

        cardView = findViewById(R.id.container)
        tagTextView = findViewById(R.id.text_view)
        tagIconView = findViewById(R.id.icon_view)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.Tag,
            R.attr.tagStyle, R.style.Tag
        ).apply {
            try {
                val minWidth = getDimension(R.styleable.Tag_minWidth, 0f)

                val backgroundColor =
                    getColor(R.styleable.Tag_backgroundColor, Color.RED)
                val foregroundColor = getColor(R.styleable.Tag_foregroundColor, Color.BLACK)

                val text = getText(R.styleable.Tag_text)
                val icon = getDrawable(R.styleable.Tag_icon)

                val shapeAppearanceModel =
                    ShapeAppearanceModel.builder(context, attrs, defStyleAttr, defStyleRes)
                        .build()

                val textAppearanceResId = getResourceId(R.styleable.Tag_android_textAppearance, -1)

                tagTextView.minWidth = minWidth.toInt()

                Log.e("TAG", "ta - $textAppearanceResId")

                if (textAppearanceResId != -1) {
                    tagTextView.setTextAppearance(textAppearanceResId)
                }

                setShapeAppearanceModel(shapeAppearanceModel)

                setBackgroundColor(backgroundColor)
                this@Tag.foregroundColor = foregroundColor

                if (text != null) {
                    this@Tag._text = text
                } else if (icon != null) {
                    this@Tag._icon = icon
                }
            } finally {
                recycle()
            }
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        R.style.Tag
    )

    constructor(context: Context, attributeSet: AttributeSet?) : this(
        context,
        attributeSet,
        R.attr.tagStyle,
    )

    constructor(context: Context) : this(context, null)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (changed) {
            tagIconView.apply {
                layoutParams.height = tagTextView.height

                if (_text != null) {
                    text = _text
                } else {
                    icon = _icon
                }

                layoutParams.let {
                    if (icon != null) {
                        Log.e(TAG, "w=${it.width} h=${it.height} t=${tagTextView.height}")
                    }
                }
            }
        }
    }

    override fun setBackgroundColor(color: Int) {
        cardView.setCardBackgroundColor(color)
    }

    companion object {
        private const val TAG = "Tag"
        private val LAYOUT_ID = R.layout.tag_view_layout
    }

    override fun setShapeAppearanceModel(shapeAppearanceModel: ShapeAppearanceModel) {
        cardView.shapeAppearanceModel = shapeAppearanceModel
    }

    override fun getShapeAppearanceModel(): ShapeAppearanceModel = cardView.shapeAppearanceModel
}

//object BusLineTag {
//    const val TAG_CORNER_SIZE_DP = 8
//    const val TAG_GROUP_INNER_CORNER_SIZE_DP = 4
//
//    private val LAYOUT_ID = R.layout.tag_view_layout
//    private val ELLIPSIZED_LAYOUT_ID = R.layout.ellipsized_tag_view_layout
//
//    fun create(
//        context: Context,
//        name: CharSequence,
//        backgroundColor: Int = Color.RED,
//        contentColor: Int = Color.WHITE,
//    ): View {
//        val view = View.inflate(context, LAYOUT_ID, null) as MaterialCardView
//        view.layoutParams =
//            MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
//        view.setCardBackgroundColor(backgroundColor)
//
//        val cornerSize = TAG_CORNER_SIZE_DP.toDp(context).toFloat()
//        view.shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
//            setTopLeftCornerSize(cornerSize)
//            setBottomLeftCornerSize(cornerSize)
//            setTopRightCornerSize(cornerSize)
//            setBottomRightCornerSize(cornerSize)
//        }.build()
//
//        val nameView: TextView = view.findViewById(R.id.name)
//        nameView.setTextColor(contentColor)
//
//        nameView.text = name
//
//        return view
//    }
//
//    fun createStyled(
//        context: Context,
//        name: CharSequence,
//        @AttrRes backgroundColor: Int,
//        @AttrRes contentColor: Int,
//    ): View {
//        @ColorInt
//        val background = resolveColor(context, backgroundColor)
//
//        @ColorInt
//        val content = resolveColor(context, contentColor)
//
//        return create(context, name, background, content)
//    }
//
//    fun createEllipsized(
//        context: Context,
//        backgroundColor: Int = Color.RED,
//        contentColor: Int = Color.WHITE,
//    ): Pair<View, Int> {
//        val view = View.inflate(context, ELLIPSIZED_LAYOUT_ID, null) as MaterialCardView
//
//        view.layoutParams =
//            MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
//
//        view.setCardBackgroundColor(backgroundColor)
//
//        val iconView: ImageView = view.findViewById(R.id.icon_view)
//        iconView.setColorFilter(contentColor)
//
//        val id = View.generateViewId().also { view.id = it }
//
//        return Pair(view, id)
//    }
//
//    fun createStyledEllipsized(
//        context: Context,
//        @AttrRes backgroundColor: Int,
//        @AttrRes contentColor: Int,
//    ): Pair<View, Int> {
//        @ColorInt
//        val background = resolveColor(context, backgroundColor)
//
//        @ColorInt
//        val content = resolveColor(context, contentColor)
//
//        return createEllipsized(context, background, content)
//    }
//
//    fun createGroup(
//        context: Context,
//        root: ViewGroup,
//        names: List<CharSequence>,
//        maxWidth: Int,
//        tagBackgroundColor: Int,
//        tagContentColor: Int,
//        ellipsizedTagBackgroundColor: Int,
//        ellipsizedTagContentColor: Int,
//    ) {
//        for (line in names) {
//            val lineView = create(
//                context,
//                line,
//                tagBackgroundColor,
//                tagContentColor
//            )
//
//            lineView.updateLayoutParams<MarginLayoutParams> {
//                leftMargin = 4.toDp(context)
//                rightMargin = 4.toDp(context)
//            }
//
//            root.addView(lineView)
//        }
//
//        val (ellipsizedTag, id) = createEllipsized(
//            context,
//            ellipsizedTagBackgroundColor,
//            ellipsizedTagContentColor
//        )
//
//        root.addView(ellipsizedTag)
//
//        root.viewTreeObserver.addOnGlobalLayoutListener(object :
//            ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                root.viewTreeObserver.removeOnGlobalLayoutListener(this)
//
//                val ellipsizedTagWidth = ellipsizedTag.width
//
//                val toRemove = mutableListOf<View>()
//
//                val lineViews = root.children.toList().filterNot { it.id == id }
//
//                for (i in 0..lineViews.lastIndex) {
//                    val lineView = lineViews[i]
//
//                    if (i != lineViews.lastIndex) {
//                        if (lineView.right + ellipsizedTagWidth > maxWidth) {
//                            toRemove.add(lineView)
//                        }
//                    } else {
//                        if (lineView.right > maxWidth) {
//                            toRemove.add(lineView)
//                        }
//                    }
//                }
//
//                toRemove.forEach { root.removeView(it) }
//            }
//        })
//    }
//
//    fun createStyledGroup(
//        context: Context,
//        root: ViewGroup,
//        names: List<CharSequence>,
//        maxWidth: Int,
//        @AttrRes tagBackgroundColor: Int,
//        @AttrRes tagContentColor: Int,
//        @AttrRes ellipsizedTagBackgroundColor: Int,
//        @AttrRes ellipsizedTagContentColor: Int,
//    ) {
//        val tagBackground = resolveColor(context, tagBackgroundColor)
//        val tagContent = resolveColor(context, tagContentColor)
//        val ellipsizedTagBackground = resolveColor(context, ellipsizedTagBackgroundColor)
//        val ellipsizedTagContent = resolveColor(context, ellipsizedTagContentColor)
//
//        return createGroup(
//            context,
//            root,
//            names,
//            maxWidth,
//            tagBackground,
//            tagContent,
//            ellipsizedTagBackground,
//            ellipsizedTagContent
//        )
//    }
//
//    @ColorInt
//    private fun resolveColor(context: Context, @AttrRes color: Int): Int = with(TypedValue()) {
//        context.theme.resolveAttribute(color, this, true)
//        this.data
//    }
//}
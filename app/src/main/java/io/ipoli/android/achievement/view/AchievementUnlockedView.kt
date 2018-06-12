package io.ipoli.android.achievement.view

import android.animation.*
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Build.VERSION
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.*
import android.view.View.GONE
import android.view.View.MeasureSpec
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import io.ipoli.android.achievement.view.AchievementIconView.AchievementIconViewStates.FADE_DRAWABLE
import io.ipoli.android.achievement.view.AchievementIconView.AchievementIconViewStates.SAME_DRAWABLE
import timber.log.Timber


class AchievementUnlocked(private val context: Context) {
    //dimens
    private var smallSize: Int = 0
    private var largeSize: Int = 0
    private var elevation: Int = 0
    private var paddingLarge: Int = 0
    private var paddingSmall: Int = 0
    private var translationY: Int = 0
    private var margin: Int = 0
    private var initialSize = -1
    //indices of data iterator
    private var index = 0
    private var dismissible = false
    private var added = false
    //achievements data
    private var achievements = mutableListOf<AchievementData>()
    private val overshootInterpolator = OvershootInterpolator()
    //save previous width of popup just in case measureWidth returns 0 when using multiline popup
    private var readingDelay = 1500

    private val interpolator = DeceleratingInterpolator(50)
    private val anticipateInterpolator = AnticipateInterpolator()
    private val accelerateInterpolator = AccelerateInterpolator(50f)
    private var matchParent: Int = 0
    private var dismissed = false
    private var listener: AchievementListener? = null
    private var isPowerSavingModeOn = false
    private var isLarge = true
    private var alignTop = true
    private var isRounded = true
    private var container: ViewGroup? = null
    private var icon: AchievementIconView? = null
    var titleTextView: TextView? = null
        private set
    private var subtitleTextView: ScrollTextView? = null
    private val focusable =
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    private val nonFocusable =
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

    var achievementParent: ViewGroup? = null
        private set

    private var mainViewLP: WindowManager.LayoutParams? = null

    private var initiatedGlobalFields = false

    private var currentContainerWidth: Int = 0

    private val containerBg: GradientDrawableWithColors
        get() {
            if ((container!!.background) is GradientDrawableWithColors)
                return (container!!.background) as GradientDrawableWithColors
            val iconBackground = GradientDrawableWithColors()
            if (isRounded)
                iconBackground.cornerRadius = (initialSize / 2).toFloat()
            else
                iconBackground.cornerRadius = convertDpToPixel(2f).toFloat()
            return iconBackground
        }

    private val iconBg: GradientDrawableWithColors
        get() {
            if (((icon!!.parent as View).background) is GradientDrawable)
                return ((icon!!.parent as View).background) as GradientDrawableWithColors
            val iconBackground = GradientDrawableWithColors()
            if (isRounded)
                iconBackground.shape = GradientDrawable.OVAL
            else
                iconBackground.cornerRadius = convertDpToPixel(2f).toFloat()
            return iconBackground
        }

    private val exitAnimation: AnimatorSet
        get() {
            val containerScale = ObjectAnimator.ofFloat(container, View.SCALE_X, 1f, 0f)
            containerScale.addUpdateListener { animation ->
                if (!dismissed)
                    container!!.scaleY = animation.animatedValue as Float
            }
            containerScale.duration = (animationMultiplier * 250).toLong()
            containerScale.startDelay = 100
            containerScale.interpolator = anticipateInterpolator
            val scrimIsAvailable = alignTop && achievementParent!!.background != null
            var scrim: ObjectAnimator? = null
            if (scrimIsAvailable) {
                scrim = ObjectAnimator.ofInt(achievementParent!!.background, "alpha", 255, 0)
            }
            val out = AnimatorSet()
            if (scrim != null)
                out.playTogether(containerScale, scrim)
            else
                out.play(containerScale)
            val set = AnimatorSet()
            set.playSequentially(
                getContainerStretchAnimation(
                    Math.min(
                        container!!.measuredWidth,
                        matchParent
                    ), initialSize
                ), out
            )
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dismissWithoutAnimation()
                }
            })
            return set
        }

    private var hasBeenDismissed = false

    init {

        initGlobalFields()
    }

    fun setTopAligned(alignTop: Boolean): AchievementUnlocked {
        this.alignTop = alignTop
        return this
    }

    fun setReadingDelay(readingDelay: Int): AchievementUnlocked {
        this.readingDelay = readingDelay
        return this

    }

    fun setRounded(rounded: Boolean): AchievementUnlocked {
        isRounded = rounded
        return this
    }

    fun setLarge(large: Boolean): AchievementUnlocked {
        this.isLarge = large
        return this
    }

    private fun convertDpToPixel(dp: Float): Int {
        val metrics = Resources.getSystem().displayMetrics
        val px = dp * (metrics.densityDpi / 160f)
        return Math.round(px)
    }

    @SuppressLint("SetTextI18n")
    private fun initGlobalFields() {
        if (!initiatedGlobalFields) {
            initiatedGlobalFields = true
            margin = convertDpToPixel(16f)
            elevation = convertDpToPixel(10f)
            paddingLarge = convertDpToPixel(10f)
            paddingSmall = convertDpToPixel(5f)
            smallSize = convertDpToPixel(50f)
            largeSize = convertDpToPixel(65f)
            translationY = convertDpToPixel(20f)

            achievementParent = RelativeLayout(context)
            achievementParent!!.clipToPadding = false
            val motherLayoutLP = LayoutParams(-2, -2)
            achievementParent!!.layoutParams = motherLayoutLP
            achievementParent!!.tag = "motherLayout"
            val textContainerFake = LinearLayout(context)
            textContainerFake.orientation = VERTICAL
            textContainerFake.setPadding(convertDpToPixel(10f), 0, convertDpToPixel(20f), 0)
            textContainerFake.visibility = View.INVISIBLE
            val textContainerFakeLP = LayoutParams(-2, -2)
            textContainerFakeLP.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            textContainerFake.layoutParams = textContainerFakeLP
            textContainerFake.tag = "textContainerFake"
            val titleFake = TextView(context)
            titleFake.text = "Title"
            val titleFakeLP = LayoutParams(-2, -2)
            titleFake.layoutParams = titleFakeLP
            titleFake.tag = "titleFake"
            titleFake.maxLines = 1
            val subtitleFake = ScrollTextView(context)
            subtitleFake.text = "Subtitle"
            subtitleFake.visibility = GONE
            subtitleFake.maxLines = 1
            val subtitleFakeLP = LayoutParams(-2, -2)
            subtitleFake.layoutParams = subtitleFakeLP
            subtitleFake.tag = "subtitleFake"
            textContainerFake.addView(titleFake)
            textContainerFake.addView(subtitleFake)
            achievementParent!!.addView(textContainerFake)
            container = RelativeLayout(context)
            container!!.clipToPadding = false
            container!!.clipChildren = false

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

                achievementParent!!.clipToOutline = true
            }

            val achievementBodyLP = LayoutParams(-2, largeSize)
            achievementBodyLP.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            achievementBodyLP.addRule(Gravity.CENTER_HORIZONTAL, RelativeLayout.TRUE)
            achievementBodyLP.topMargin = convertDpToPixel(10f)
            achievementBodyLP.bottomMargin = achievementBodyLP.topMargin
            container!!.layoutParams = achievementBodyLP
            container!!.tag = "achievementBody"
            val achievementIconBg = LinearLayout(context)
            val achievementIconBgLP = LayoutParams(largeSize, largeSize)
            achievementIconBg.layoutParams = achievementIconBgLP
            achievementIconBg.tag = "achievementIconBg"
            container!!.addView(achievementIconBg)
            icon = AchievementIconView(context)
            icon!!.setPadding(
                convertDpToPixel(7f),
                convertDpToPixel(7f),
                convertDpToPixel(7f),
                convertDpToPixel(7f)
            )
            val achievementIconLP = LayoutParams(largeSize, largeSize)
            icon!!.maxWidth = largeSize
            icon!!.layoutParams = achievementIconLP
            icon!!.tag = "achievementIcon"
            achievementIconBg.addView(icon)
            val textContainer = LinearLayout(context)
            textContainer.clipToPadding = false
            textContainer.clipChildren = false
            textContainer.orientation = VERTICAL
            textContainer.tag = "textContainer"
            val textContainerLP = LayoutParams(-2, -2)
            textContainer.layoutParams = textContainerLP
            textContainerLP.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            container!!.addView(textContainer)

            container!!.tag = "achievementBody"
            titleTextView = TextView(context)
            titleTextView!!.text = "Title"
            titleTextView!!.maxLines = 1
            val titleLP = LayoutParams(-2, -2)
            titleTextView!!.layoutParams = titleLP
            titleTextView!!.tag = "title"
            subtitleTextView = ScrollTextView(context)
            subtitleTextView!!.text = "Subtitle"
            subtitleTextView!!.visibility = GONE
            subtitleTextView!!.layoutParams = titleLP
            subtitleTextView!!.maxLines = 1
            subtitleTextView!!.tag = "subtitle"
            textContainer.addView(titleTextView)
            textContainer.addView(subtitleTextView)
            achievementParent!!.addView(container)


            if (mainViewLP == null) {
                mainViewLP = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowOverlayCompat.TYPE_SYSTEM_ERROR,
                    focusable,
                    PixelFormat.TRANSLUCENT
                )
            }

            if (titleTextView == null) {
                if (isLarge) {
                    titleTextView!!.gravity = View.TEXT_ALIGNMENT_CENTER
                }
                titleTextView!!.setSingleLine(true)
                titleTextView!!.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {
                        (achievementParent!!.findViewWithTag<View>("titleFake") as TextView).text =
                            titleTextView!!.text
                    }
                })
            }
            if (subtitleTextView == null) {

                subtitleTextView!!.setSingleLine(true)
                subtitleTextView!!.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {
                        (achievementParent!!.findViewWithTag<View>("subtitleFake") as TextView).text =
                            subtitleTextView!!.text
                    }
                })
            }

        }
    }

    fun setDismissible(dismissible: Boolean) {
        this.dismissible = dismissible
        if (dismissible) {
            achievementParent!!.setOnTouchListener(SwipeDismissTouchListener())
            container!!.setOnTouchListener(SwipeDismissTouchListener())
        } else {
            achievementParent!!.setOnTouchListener(null)
            container!!.setOnTouchListener(null)
        }
    }

    private fun getTargetWidth(data: AchievementData): Int {

        val textContainerFake = achievementParent!!.findViewWithTag<ViewGroup>("textContainerFake")
        textContainerFake.findViewWithTag<TextView>("titleFake").text = data.title
        textContainerFake.findViewWithTag<TextView>("subtitleFake").text = data.subtitle
        textContainerFake.measure(MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return textContainerFake.measuredWidth
    }

    private fun buildAchievement() {
        initGlobalFields()
        val padding: Int
        if (isLarge) {
            initialSize = largeSize
            padding = paddingLarge
        } else {
            initialSize = smallSize
            padding = paddingSmall
        }
        (icon!!.parent as View).invalidate()
        icon!!.setPadding(padding, padding, padding, padding)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            container!!.elevation = elevation.toFloat()
        }
        titleTextView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.isEmpty()) {
                    titleTextView!!.visibility = GONE
                } else {
                    titleTextView!!.visibility = View.VISIBLE
                }
            }
        })
        val fakeTitle = (achievementParent!!.findViewWithTag<View>("titleFake") as TextView)
        fakeTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.isEmpty()) {
                    fakeTitle.visibility = GONE
                } else {
                    fakeTitle.visibility = View.VISIBLE
                }
            }
        })
        val fakeSubTitle = (achievementParent!!.findViewWithTag<View>("subtitleFake") as TextView)
        fakeSubTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.isEmpty()) {
                    fakeSubTitle.visibility = GONE
                } else {
                    fakeSubTitle.visibility = View.VISIBLE
                }
            }
        })
        subtitleTextView!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.isEmpty()) {
                    subtitleTextView!!.visibility = GONE
                } else
                    subtitleTextView!!.visibility = View.VISIBLE
            }
        })
        titleTextView!!.alpha = 0f
        titleTextView!!.translationY = translationY.toFloat()
        subtitleTextView!!.translationY = translationY.toFloat()
        subtitleTextView!!.alpha = 0f
        container!!.scaleY = 0f
        container!!.scaleX = 0f
        container!!.visibility = GONE

        val displayMetrics = context.resources.displayMetrics

        matchParent = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) - margin
        //stretched = 900;
        //  textContainer.setVisibility(View.GONE);

        val textContainer = achievementParent!!.findViewWithTag<View>("textContainer")
        if (textContainer != null) {
            textContainer.setPadding(
                convertDpToPixel(10f) + (initialSize),
                0,
                convertDpToPixel(20f),
                0
            )
            achievementParent!!.findViewWithTag<View>("textContainerFake").setPadding(
                textContainer.paddingLeft,
                textContainer.paddingTop,
                textContainer.paddingRight,
                textContainer.paddingBottom
            )
        }
        icon!!.maxWidth = initialSize
        (icon!!.parent as View).layoutParams.width = initialSize
        (icon!!.parent as View).layoutParams.height =
            (icon!!.parent as View).layoutParams.width
        icon!!.layoutParams.width = (icon!!.parent as View).layoutParams.height
        icon!!.layoutParams.height = icon!!.layoutParams.width
        container!!.layoutParams.height = icon!!.layoutParams.height
        container!!.layoutParams.width = container!!.layoutParams.height
        container!!.requestLayout()


        if (alignTop) {
            mainViewLP!!.gravity = Gravity.TOP
        } else {
            mainViewLP!!.gravity = Gravity.BOTTOM
        }
        // mainViewLP.width = stretched + (int) elevation;
        if (alignTop && (achievementParent!!.background == null || achievementParent!!.background !is GradientDrawable)) {
            val scrim = GradientDrawable()
            scrim.shape = GradientDrawable.RECTANGLE
            scrim.colors = intArrayOf(0x40000000, 0)
            scrim.alpha = 0
            achievementParent!!.background = scrim
            achievementParent!!.clipToPadding = false
        } else if (!alignTop) {
            achievementParent!!.background = null
        }
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(
            achievementParent,
            mainViewLP
        )
        added = true
    }

    private fun setTextColor(textColor: Int) {
        subtitleTextView!!.setTextColor(
            Color.rgb(
                Color.red(textColor),
                Color.green(textColor),
                Color.blue(textColor)
            )
        )
        titleTextView!!.setTextColor(
            Color.rgb(
                Color.red(textColor),
                Color.green(textColor),
                Color.blue(textColor)
            )
        )
    }

    private fun setBackground(v: View?, d: Drawable?) {
        v!!.background = d
    }

    fun show(data: List<AchievementData>) {
        //Check permission first
        if (VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
            Timber.d("canDrawOverlays not granted")
            return

        }
        if (data.isEmpty()) {
            return
        }
        if (added) {

            achievements.addAll(data)

            return
        }
        dismissWithoutAnimation()
        achievements.clear()
        achievements.addAll(data)
        buildAchievement()
        setContainerBg(achievements[0].backgroundColor)
        if (listener != null)
            listener!!.onViewCreated(this, data)
        prepareMorphism()
    }

    fun dismissWithoutAnimation() {
        removeView()
        if (listener != null)
            listener!!.onAchievementDismissed(this)
    }

    private fun removeView() {
        if (!added) return
        index = 0
        setSwipeEffect(0f)

        hasBeenDismissed = false
        isPowerSavingModeOn = false
        icon!!.visibility = View.VISIBLE
        setBackground((icon!!.parent as View), null)
        setBackground(container, null)
        setBackground(icon, null)
        isLarge = true
        alignTop = true
        isRounded = true
        icon!!.setOnClickListener(null)
        container!!.setOnClickListener(null)
        achievementParent!!.setOnClickListener(null)
        achievementParent!!.visibility = View.VISIBLE
        //    container.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        container!!.setOnTouchListener(null)
        container!!.visibility = View.VISIBLE
        container!!.translationX = 0f
        container!!.alpha = 1f
        achievementParent!!.alpha = 1f
        setDismissible(false)
        listener = null
        (icon!!.parent as View).background = null
        dismissed = false
        try {
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(
                achievementParent
            )
            added = false
        } catch (e: Exception) {
            e.printStackTrace()
            // *shrug emoji*
            //there's no way to check if view is already added to windowManager or not, probably the exception is nullPointerException where achievementLayout is null
            //best thing we could do is to check added boolean
        }

    }

    private fun clamp(`val`: Int, min: Int, max: Int): Int {
        return Math.max(min, Math.min(max, `val`))
    }

    private fun getStartValue(start: Int): Int {
        return clamp(start, initialSize, matchParent)
    }

    private fun getEndValue(end: Int): Int {
        return Math.min(end, matchParent)
    }

    private fun getContainerStretchAnimation(start: Int, end: Int): ValueAnimator {
        val containerStretch = ValueAnimator.ofInt(getStartValue(start), getEndValue(end))
        containerStretch.addUpdateListener { valueAnimator ->
            if (!dismissed) {
                val `val` = valueAnimator.animatedValue as Int
                val layoutParams = container!!.layoutParams
                layoutParams.width = `val`
                currentContainerWidth = `val`
                container!!.layoutParams = layoutParams
            }
        }
        containerStretch.interpolator = interpolator
        containerStretch.duration = (animationMultiplier * 300).toLong()
        return containerStretch
    }

    private fun setContainerBg(color: Int) {
        val bgDrawable = container!!.background
        if (bgDrawable != null && bgDrawable is GradientDrawable)
            (bgDrawable as GradientDrawableWithColors).setColor(color)
        else {
            val iconBackground = containerBg
            iconBackground.setColor(color)
            setBackground(container, iconBackground)
        }
    }

    private fun getIconBgColor(defaultColor: Int): Int {
        val bgDrawable = (icon!!.parent as View).background
        return if (bgDrawable != null && bgDrawable is GradientDrawable) (bgDrawable as GradientDrawableWithColors).gradientColor else defaultColor
    }

    private fun getContainerBgColor(defaultColor: Int): Int {
        val bgDrawable = container!!.background
        return if (bgDrawable != null && bgDrawable is GradientDrawable) (bgDrawable as GradientDrawableWithColors).gradientColor else defaultColor
    }

    private fun setIconBg(color: Int) {
        val bgDrawable = ((icon!!.parent as View).background)
        if (bgDrawable != null && bgDrawable is GradientDrawable)
            bgDrawable.setColorFilter(
                Color.argb(
                    bgDrawable.getAlpha(),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
                ), PorterDuff.Mode.SRC_IN
            )
        else {
            val iconBackground = iconBg
            iconBackground.setColor(color)
            setBackground((icon!!.parent as View), iconBackground)
        }
    }

    private fun morphData(): AnimatorSet {
        val sets = AnimatorSet()

        sets.play(animateData(achievements[index]))
        sets.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (!hasBeenDismissed && this@AchievementUnlocked.achievements.size > 0 && index + 1 < this@AchievementUnlocked.achievements.size) {
                    index++
                    morphData().start()
                } else
                    exitAnimation.start()
            }
        })
        return sets

    }


    private fun animateData(data: AchievementData): AnimatorSet {
        val backgroundAnimators = AnimatorSet()
        val inAnimation = AnimatorSet()
        val outAnimation = AnimatorSet()
        val result = AnimatorSet()

        var subtitleIn: ObjectAnimator? = null
        var subtitleOut: ObjectAnimator? = null
        if ((container!!.tag != null && container!!.tag !== data)) {
            var previousBgColor = -0x1
            var previousIconBgColor = 0x30ffffff
            if (index == 0) {
                previousBgColor = data.backgroundColor
                previousIconBgColor = data.iconBackgroundColor
            } else if (index > 0 && index < achievements.size) {
                previousBgColor = achievements[index - 1].backgroundColor
                previousIconBgColor = achievements[index - 1].iconBackgroundColor
            }
            val iconBgColor = ValueAnimator.ofInt(
                getIconBgColor(previousIconBgColor),
                data.iconBackgroundColor
            )
            iconBgColor.setEvaluator(ArgbEvaluator())
            iconBgColor.addUpdateListener { animation ->
                if (!dismissed)
                    setIconBg(animation.animatedValue as Int)
            }
            val bgColor =
                ValueAnimator.ofInt(getContainerBgColor(previousBgColor), data.backgroundColor)
            bgColor.setEvaluator(ArgbEvaluator())
            bgColor.addUpdateListener { animation ->
                if (!dismissed)
                    setContainerBg(animation.animatedValue as Int)
            }
            bgColor.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    if (index > 0) setIcon(data)
                }
            })

            backgroundAnimators.play(iconBgColor).with(bgColor)
            backgroundAnimators.interpolator = interpolator
            backgroundAnimators.duration = (animationMultiplier * 300).toLong()


        }
        val titleIn =
            ObjectAnimator.ofFloat(titleTextView, View.TRANSLATION_Y, translationY.toFloat(), 0f)
        titleIn!!.addUpdateListener(AnimatorUpdateListener { animation ->
            if (dismissed) return@AnimatorUpdateListener
            titleTextView!!.alpha = animation.animatedFraction
        })
        titleIn.duration = (animationMultiplier * 300).toLong()
        titleIn.interpolator = interpolator

        val titleOut =
            ObjectAnimator.ofFloat(titleTextView, View.TRANSLATION_Y, 0f, translationY.toFloat())
        titleOut!!.addUpdateListener(AnimatorUpdateListener { animation ->
            if (dismissed) return@AnimatorUpdateListener
            titleTextView!!.alpha = 1f - animation.animatedFraction
        })
        titleOut.interpolator = accelerateInterpolator


        val startScrollingDelay = 800
        val stretch = getContainerStretchAnimation(container!!.measuredWidth, getTargetWidth(data))

        if (data.subtitle != null) {
            subtitleIn = ObjectAnimator.ofFloat(
                subtitleTextView,
                View.TRANSLATION_Y,
                translationY.toFloat(),
                0f
            )
            subtitleIn!!.addUpdateListener(AnimatorUpdateListener { animation ->
                if (dismissed) return@AnimatorUpdateListener
                subtitleTextView!!.alpha = animation.animatedFraction
            })
            subtitleIn.interpolator = interpolator
            subtitleIn.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    subtitleTextView!!.stopScrolling()

                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    subtitleTextView!!.stopScrolling()
                    if ((matchParent - initialSize) < getTargetWidth(data))
                        android.os.Handler().postDelayed(
                            { subtitleTextView!!.startScrolling() },
                            startScrollingDelay.toLong()
                        )


                }
            })
            subtitleIn.startDelay = 150
            subtitleIn.interpolator = interpolator
            subtitleIn.duration = (animationMultiplier * 300).toLong()
        }
        //use previousWidth better than real-time measuring to increase performance

        if (data.subtitle != null) {
            val textViews = AnimatorSet()
            textViews.playTogether(titleIn, subtitleIn)

            inAnimation.play(stretch).with(backgroundAnimators).before(textViews)
        } else {
            inAnimation.play(stretch).with(backgroundAnimators).before(titleIn)

        }
        // inAnimation.setInterpolator(interpolator);
        if (data.subtitle != null) {
            subtitleOut = ObjectAnimator.ofFloat(
                subtitleTextView,
                View.TRANSLATION_Y,
                0f,
                translationY.toFloat()
            )
            subtitleOut!!.addUpdateListener(AnimatorUpdateListener { animation ->
                if (dismissed) return@AnimatorUpdateListener
                subtitleTextView!!.alpha = 1f - animation.animatedFraction
            })
            subtitleOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    subtitleTextView!!.stopScrolling()

                }
            })
            subtitleOut.interpolator = accelerateInterpolator

        }

        if (data.subtitle != null) {
            titleOut.startDelay = (150 * animationMultiplier).toLong()
            outAnimation.playTogether(subtitleOut, titleOut)
        } else {
            outAnimation.play(titleOut)
        }
        val title = data.title
        val subtitle = data.subtitle
        result.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                if (listener != null)
                    listener!!.onAchievementMorphed(this@AchievementUnlocked, data)
                if (data.popUpOnClickListener != null || dismissible) {
                    mainViewLP!!.flags = focusable
                } else {
                    mainViewLP!!.flags = nonFocusable
                }
                container!!.setOnClickListener(data.popUpOnClickListener)
                if (added)
                    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).updateViewLayout(
                        achievementParent,
                        mainViewLP
                    )


                subtitleTextView!!.text = subtitle
                titleTextView!!.text = title
                setTextColor(data.textColor)
            }


        })


        val fake = (achievementParent!!.findViewWithTag<View>("subtitleFake") as ScrollTextView)
        fake.text = data.subtitle
        var duration = readingDelay
        if ((matchParent) < getTargetWidth(data))
            duration = fake.duration + startScrollingDelay
        outAnimation.startDelay = duration.toLong()
        outAnimation.duration = (animationMultiplier * 300).toLong()
        result.playSequentially(inAnimation, outAnimation)
        result.interpolator = interpolator
        container!!.tag = data
        return result
    }

    private fun prepareMorphism() {
        if (achievements.isEmpty())
            return
        index = 0
        val scene = AnimatorSet()
        scene.playSequentially(getEntranceAnimation(achievements[0]), morphData())

        scene.start()
    }

    private fun getEntranceAnimation(data: AchievementData): AnimatorSet {
        val iconBG = data.iconBackgroundColor
        // final Drawable iconDrawable = data.getIcon();
        // final int bg = data.getBackgroundColor();
        //ValueAnimator stretch = getContainerStretchAnimation(initialSize, getTargetWidth(data));
        val containerScale = ObjectAnimator.ofFloat(container, View.SCALE_X, 0f, 1f)
        containerScale.addUpdateListener(AnimatorUpdateListener { animation ->
            if (dismissed) return@AnimatorUpdateListener
            container!!.scaleY = animation.animatedValue as Float
        })
        containerScale.duration = (animationMultiplier * 250).toLong()
        containerScale.interpolator = overshootInterpolator
        val scrimIsAvailable = alignTop && achievementParent!!.background != null
        var scrim: ObjectAnimator? = null
        if (scrimIsAvailable) {
            scrim = ObjectAnimator.ofInt(achievementParent!!.background, "alpha", 0, 255)
        }
        val set = AnimatorSet()
        if (scrim != null)
            set.playTogether(containerScale, scrim)
        else
            set.play(containerScale)
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                if (Color.alpha(iconBG) > 0) {
                    setIconBg(iconBG)
                } else {
                    val textContainer = titleTextView!!.parent as View
                    textContainer.setPadding(
                        (if (isLarge) largeSize else smallSize),
                        textContainer.paddingTop,
                        textContainer.paddingRight,
                        textContainer.paddingBottom
                    )
                    achievementParent!!.findViewWithTag<View>("textContainerFake").setPadding(
                        textContainer.paddingLeft,
                        textContainer.paddingTop,
                        textContainer.paddingRight,
                        textContainer.paddingBottom
                    )
                }
                container!!.visibility = View.VISIBLE
                setIcon(data)
            }
        })
        return set
    }

    private fun setIcon(data: AchievementData?) {
        if (data == null) {
            return
        }
        if (data.state === SAME_DRAWABLE)
            return
        val d = data.icon
        if (d != null) {

            if (data.state === FADE_DRAWABLE)
                icon!!.fadeDrawable(d)
            else
                icon!!.drawable = d

        } else
            icon!!.drawable = null
    }

    private inner class SwipeDismissTouchListener internal constructor() : View.OnTouchListener {
        private val mSlop: Int
        private val mMinFlingVelocity: Int
        private val mMaxFlingVelocity: Int
        private val mAnimationTime: Long
        private var mDownX: Float = 0.toFloat()
        private var mSwiping: Boolean = false
        private var mTranslationX: Float = 0.toFloat()
        private val end: Runnable?

        init {
            val vc = ViewConfiguration.get(container!!.context)
            mSlop = vc.scaledTouchSlop
            mMinFlingVelocity = vc.scaledMinimumFlingVelocity
            mMaxFlingVelocity = vc.scaledMaximumFlingVelocity
            mAnimationTime = container!!.context.resources.getInteger(
                android.R.integer.config_shortAnimTime
            ).toLong()
            end = Runnable {
                hasBeenDismissed = true
                if (alignTop)
                    achievementParent!!.animate().alpha(0f).setListener(object :
                        AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            achievementParent!!.visibility = GONE


                        }
                    }).start()
                else
                    achievementParent!!.visibility = GONE//no scrim in bottom-aligned achievements
            }
        }


        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            motionEvent.offsetLocation(mTranslationX, 0f)
            val deltaX = (motionEvent.rawX - mDownX)

            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mDownX = motionEvent.rawX
                    view.onTouchEvent(motionEvent)
                    return false
                }
                MotionEvent.ACTION_UP -> {

                    if (container!!.alpha == 0f) {
                        dismissWithoutAnimation()
                        return true
                    }

                    var dismiss = false
                    var dismissRight = false

                    val spaceToEdge = ((achievementParent!!.width - container!!.width) / 2)
                    val swipePercentage = Math.abs(mTranslationX / spaceToEdge)


                    if (swipePercentage >= 0.5f) {
                        dismiss = true
                        dismissRight = deltaX > 0
                    }
                    if (dismiss) {
                        val translation = ObjectAnimator.ofFloat(
                            container,
                            View.TRANSLATION_X,
                            container!!.translationX,
                            if (dismissRight) container!!.measuredWidth.toFloat() else -container!!.measuredWidth.toFloat()
                        )
                        translation.addUpdateListener({ animation ->
                            setSwipeEffect(
                                animation.animatedValue as Float
                            )
                        })
                        translation.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                end?.run()
                            }
                        })
                        translation.interpolator = interpolator
                        translation.duration = mAnimationTime
                        translation.start()
                        dismissed = true
                    } else {
                        val translation = ObjectAnimator.ofFloat(
                            container,
                            View.TRANSLATION_X,
                            container!!.translationX,
                            0f
                        )
                        translation.addUpdateListener({ animation ->
                            setSwipeEffect(
                                animation.animatedValue as Float
                            )
                        })
                        translation.duration = mAnimationTime
                        translation.interpolator = interpolator
                        translation.start()

                        dismissed = false
                    }
                    mTranslationX = 0f
                    mDownX = 0f
                    mSwiping = false
                }
                MotionEvent.ACTION_MOVE -> {

                    if (Math.abs(deltaX) > mSlop) {
                        mSwiping = true
                        container!!.parent.requestDisallowInterceptTouchEvent(true)
                        val cancelEvent = MotionEvent.obtain(motionEvent)
                        cancelEvent.action =
                            (MotionEvent.ACTION_CANCEL or (motionEvent.actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT))
                        container!!.onTouchEvent(cancelEvent)
                    }
                    if (mSwiping) {
                        mTranslationX = deltaX
                        setSwipeEffect(mTranslationX)
                        return true
                    }
                }
            }
            return false
        }
    }


    private fun setSwipeEffect(amount: Float) {

        container!!.translationX = amount
    }

    companion object {
        //for debugging purpose
        internal const val animationMultiplier = 1
    }

}

data class AchievementData(
    val title: String = "",
    val subtitle: String? = null,
    val icon: Drawable? = null,
    val textColor: Int = -0x1000000,
    val backgroundColor: Int = -0x1,
    val iconBackgroundColor: Int = 0x0,
    val popUpOnClickListener: View.OnClickListener? = null,
    val state: AchievementIconView.AchievementIconViewStates? = null
)

/*
Ticker textView
 */
@SuppressLint("AppCompatCustomView")
internal class ScrollTextView : TextView {

    val duration: Int
        get() {
            if (layout == null) measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            val density = context.resources.displayMetrics.density
            val dpPerSec = 30 * density
            val textWidth = layout.getLineWidth(0)
            val gap = textWidth / 3.0f
            val result = Math.round(
                (textWidth - gap) / dpPerSec
            )
            return if (result > 0) result * 1000 else 2000
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        setSingleLine()
        ellipsize = TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = 3
        isHorizontalFadingEdgeEnabled = true
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        isSelected = visibility == View.VISIBLE
    }


    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        if (alpha <= 0.1f) {
            stopScrolling()
        }
    }

    fun stopScrolling() {
        (parent as View).requestFocus()
        isSelected = false
    }

    fun startScrolling() {
        requestFocus()
        isSelected = true

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopScrolling()
    }
}

/**
 * GradientDrawable that saves the drawable colors; used for AU background
 */
internal class GradientDrawableWithColors : GradientDrawable() {
    var gradientColor: Int = 0
        private set

    override fun setColor(argb: Int) {
        super.setColors(intArrayOf(argb, argb))
        gradientColor = argb
    }

    override fun setColors(colors: IntArray) {
        super.setColors(colors)
        gradientColor = colors[0]
    }
}

/**
 * ImageView that animates drawable change. It also scales
 * according to the drawable size to make sure it doesn't clip
 */
@SuppressLint("AppCompatCustomView")
class AchievementIconView(context: Context) : ImageView(context) {

    enum class AchievementIconViewStates {
        FADE_DRAWABLE, SAME_DRAWABLE
    }


    fun setDrawable(drawable: Drawable?) {
        if (drawable == null) {
            setImageDrawable(null)
            return
        }
        if (scaleType !== ScaleType.CENTER_CROP) scaleType = ScaleType.CENTER_CROP

        if (getDrawable() == null) {
            setImageDrawable(drawable)
        } else {
            if (drawable.alpha < 255)
                drawable.alpha = 255

            animate().scaleX(0f)
                .setDuration((200 * AchievementUnlocked.animationMultiplier).toLong()).scaleY(0f)
                .alpha(0f).withEndAction {
                    animate().setDuration((200 * AchievementUnlocked.animationMultiplier).toLong())
                        .scaleX(1 / Math.max(scaleX, scaleY)).scaleY(1 / Math.max(scaleX, scaleY))
                        .alpha(1f).withStartAction { setImageDrawable(drawable) }
                        .start()
                }.start()
        }
    }

    fun fadeDrawable(drawable: Drawable?) {
        if (drawable == null) {
            setImageDrawable(null)
            return
        }
        if (scaleType !== ScaleType.CENTER_CROP) scaleType = ScaleType.CENTER_CROP

        val scaleX = 3.5f / (maxWidth / drawable.intrinsicWidth)
        val scaleY = 3.5f / (maxWidth / drawable.intrinsicHeight)

        if (getDrawable() == null) {
            setImageDrawable(drawable)
            setScaleX(1 / Math.max(scaleX, scaleY))
            setScaleY(1 / Math.max(scaleX, scaleY))
        } else {
            if (drawable.alpha < 255)
                drawable.alpha = 255

            animate().setDuration((50 * AchievementUnlocked.animationMultiplier).toLong()).alpha(0f)
                .withEndAction {
                    animate().setDuration((50 * AchievementUnlocked.animationMultiplier).toLong())
                        .alpha(1f).withStartAction { setImageDrawable(drawable) }.start()
                }.start()
        }
    }

}

interface AchievementListener {
    fun onViewCreated(achievement: AchievementUnlocked, data: List<AchievementData>)
    fun onAchievementMorphed(achievement: AchievementUnlocked, data: AchievementData)
    fun onAchievementDismissed(achievement: AchievementUnlocked)
}

class DeceleratingInterpolator(base: Int) : TimeInterpolator {
    private val mBase: Int = base
    private val mLogScale: Float

    init {
        mLogScale = 1f / computeLog(1f, mBase)
    }

    private fun computeLog(t: Float, base: Int): Float {
        return (-Math.pow(base.toDouble(), (-t).toDouble())).toFloat() + 1
    }

    override fun getInterpolation(t: Float): Float {
        return computeLog(t, mBase) * mLogScale
    }
}

object WindowOverlayCompat {
    private const val ANDROID_OREO = 26
    private const val TYPE_APPLICATION_OVERLAY = 2038
    @Suppress("DEPRECATION")
    val TYPE_SYSTEM_ERROR =
        if (Build.VERSION.SDK_INT < ANDROID_OREO) WindowManager.LayoutParams.TYPE_SYSTEM_ERROR else TYPE_APPLICATION_OVERLAY
}
package io.ipoli.android.common.redux.android

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import com.github.florent37.tutoshowcase.TutoShowcase
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import io.ipoli.android.Constants
import io.ipoli.android.common.*
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.di.Module
import io.ipoli.android.common.mvi.ViewState
import io.ipoli.android.common.navigation.Navigator
import io.ipoli.android.common.redux.Action
import io.ipoli.android.common.redux.StateStore
import io.ipoli.android.common.redux.ViewStateReducer
import io.ipoli.android.common.view.*
import io.ipoli.android.myPoliApp
import io.ipoli.android.player.data.Player
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.withContext
import org.commonmark.node.Heading
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableBuilder
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.renderer.SpannableMarkdownVisitor
import ru.noties.markwon.spans.SpannableTheme
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required


/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 1/18/18.
 */

abstract class BaseViewController<A : Action, VS : ViewState> protected constructor(
    args: Bundle? = null
) : RestoreViewOnCreateController(args), Injects<Module>,
    StateStore.StateChangeSubscriber<AppState> {

    private val stateStore by required { stateStore }
    private val eventLogger by required { eventLogger }
    private val sharedPreferences by required { sharedPreferences }

    protected open var namespace: String? = null

    protected abstract val stateKey: String

    private var eventActor: SendChannel<ViewAction>? = null

    private var permissionsToRationale: Map<String, String> = mapOf()

    private var use24HourFormat: Boolean? = null

    @Volatile
    private var currentState: VS? = null

    init {
        val lifecycleListener = object : LifecycleListener() {

            override fun postAttach(controller: Controller, view: View) {
                eventLogger.logCurrentScreen(
                    activity!!,
                    this@BaseViewController.javaClass.simpleName.replace("ViewController", "")
                )
                stateStore.subscribe(this@BaseViewController)
                onSubscribedToStore()
                onCreateLoadAction()?.let {
                    dispatch(it)
                }
                eventActor = createEventActor()

            }

            override fun preDetach(controller: Controller, view: View) {
                stateStore.unsubscribe(this@BaseViewController)
                eventActor!!.close()
            }

            override fun preDestroyView(controller: Controller, view: View) {
                currentState = null
            }
        }
        addLifecycleListener(lifecycleListener)
    }

    protected open fun onSubscribedToStore() {}

    override fun onContextAvailable(context: Context) {
        inject(myPoliApp.module(context))
    }

    override fun onAttach(view: View) {
        colorLayoutBars()
        super.onAttach(view)
    }

    protected open fun colorLayoutBars() {
        activity?.window?.navigationBarColor = attrData(io.ipoli.android.R.attr.colorPrimary)
        activity?.window?.statusBarColor = attrData(io.ipoli.android.R.attr.colorPrimaryDark)
    }

    private fun createEventActor() = actor<ViewAction>(UI, start = CoroutineStart.UNDISPATCHED) {
        for (va in this) {
            va.action(va.view)
            delay(400)
        }
    }

    data class ViewAction(val view: View, val action: suspend (View) -> Unit)

    fun View.onDebounceClick(action: suspend (View) -> Unit) {
        setOnClickListener {
            eventActor!!.offer(ViewAction(it, action))
        }
    }

    fun dispatch(action: A) {
        val a = namespace?.let {
            NamespaceAction(action, it)
        } ?: action
        stateStore.dispatch(a)
    }

    override suspend fun onStateChanged(newState: AppState) {
        if (newState.keys.contains(stateKey)) {

            val viewState = newState.stateFor<VS>(stateKey)

            if (currentState == null || !viewState.hasSameStateId(currentState as VS)) {
                currentState = viewState
                renderViewState(viewState)
            }
        }
    }

    private suspend fun renderViewState(viewState: VS) {
        withContext(UI) {
            onRenderViewState(viewState)
        }
    }

    protected open fun onRenderViewState(state: VS) {
        view?.let {
            render(state, it)
        }
    }

    protected open fun onCreateLoadAction(): A? {
        return null
    }

    abstract fun render(state: VS, view: View)

    fun View.dispatchOnClick(block: () -> A) {
        onDebounceClick {
            dispatch(block())
        }
    }

    protected fun navigateFromRoot() = Navigator(rootRouter)

    protected fun requestPermissions(
        permissionsToRationale: Map<String, String>,
        requestCode: Int
    ) {
        this.permissionsToRationale = permissionsToRationale
        requestPermissions(permissionsToRationale.keys.toTypedArray(), requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = mutableListOf<String>()
        val denied = mutableListOf<String>()

        grantResults.forEachIndexed { index, res ->
            val perm = permissions[index]
            if (res == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm)
            } else {
                denied.add(perm)
            }
        }
        if (granted.isNotEmpty()) {
            onPermissionsGranted(requestCode, granted)
        }

        if (denied.isNotEmpty()) {
            onPermissionsDenied(requestCode, denied)
        }
    }

    protected open fun onPermissionsDenied(requestCode: Int, permissions: List<String>) {
        permissions.forEach {
            if (shouldShowRequestPermissionRationale(it)) {
                val message = if (permissionsToRationale.containsKey(it))
                    permissionsToRationale[it]!!
                else
                    ""
                PermissionRationaleDialogController(
                    message,
                    { requestPermissions(permissions.toTypedArray(), requestCode) },
                    {}
                ).show(router)
            }
        }
    }

    protected open fun onPermissionsGranted(requestCode: Int, permissions: List<String>) {

    }

    protected fun showcaseRect(
        @LayoutRes layout: Int,
        @IdRes view: Int,
        @IdRes containerView: Int,
        onClick: (TutoShowcase) -> Unit = {}
    ): TutoShowcase {
        val showcase = TutoShowcase.from(activity!!)
        showcase
            .setContentView(layout)
            .on(view)
            .addRoundRect()
            .withBorder()
            .onClick {
                onClick(showcase)
            }
            .show()
            .onClickContentView(containerView, null)
        return showcase
    }

    protected fun showcaseRect(
        @LayoutRes layout: Int,
        view: View,
        @IdRes containerView: Int,
        onClick: (TutoShowcase) -> Unit = {}
    ): TutoShowcase {
        val showcase = TutoShowcase.from(activity!!)
        showcase
            .setContentView(layout)
            .on(view)
            .addRoundRect()
            .withBorder()
            .onClick {
                onClick(showcase)
            }
            .show()
            .onClickContentView(containerView, null)
        return showcase
    }

    protected fun showcaseCircle(
        @LayoutRes layout: Int,
        @IdRes view: Int,
        @IdRes containerView: Int,
        onClick: (TutoShowcase) -> Unit = {}
    ): TutoShowcase {
        val showcase = TutoShowcase.from(activity!!)
        showcase
            .setContentView(layout)
            .on(view)
            .addCircle()
            .withBorder()
            .onClick {
                onClick(showcase)
            }
            .show()
            .onClickContentView(containerView, null)
        return showcase
    }

    protected fun createTimePickerDialog(
        startTime: Time,
        onTimePicked: (Time?) -> Unit,
        showNeutral: Boolean = true
    ) =
        TimePickerDialogViewController(
            time = startTime,
            shouldUse24HourFormat = shouldUse24HourFormat,
            listener = { time ->
                onTimePicked(time)
            },
            showNeutral = showNeutral
        )

    protected val shouldUse24HourFormat: Boolean
        get() {
            if (use24HourFormat == null) {
                val timeFormat = Player.Preferences.TimeFormat.valueOf(
                    sharedPreferences.getString(
                        Constants.KEY_TIME_FORMAT,
                        Player.Preferences.TimeFormat.DEVICE_DEFAULT.name
                    )
                )
                use24HourFormat =
                    if (timeFormat == Player.Preferences.TimeFormat.DEVICE_DEFAULT)
                        DateFormat.is24HourFormat(activity)
                    else
                        timeFormat != Player.Preferences.TimeFormat.TWELVE_HOURS
            }
            return use24HourFormat!!
        }

    protected fun ProgressBar.animateProgressFromCurrentValue(to: Int) {
        animateProgress(progress, to)
    }

    protected fun ProgressBar.animateProgressFromZero(to: Int) {
        animateProgress(0, to)
    }

    protected fun ProgressBar.animateProgress(
        from: Int,
        to: Int,
        endListener: (() -> Unit)? = null
    ) {
        val animator = ObjectAnimator.ofInt(this, "progress", from, to)
        animator.duration = intRes(android.R.integer.config_mediumAnimTime).toLong()
        animator.interpolator = AccelerateDecelerateInterpolator()

        endListener?.let {
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    endListener()
                }
            })
        }

        animator.start()
    }

    protected fun TextView.animateToValueFromZero(value: Int) {
        val anim = ValueAnimator.ofInt(0, value)
        anim.addUpdateListener {
            text = "${it.animatedValue}"
        }
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.duration = longAnimTime
        anim.start()
    }

    protected val Color.androidColor: AndroidColor
        get() = AndroidColor.valueOf(this.name)

    protected val AndroidColor.color: Color
        get() = Color.valueOf(this.name)

    protected val Icon.androidIcon: AndroidIcon
        get() = AndroidIcon.valueOf(this.name)

    protected val AndroidIcon.toIcon: Icon
        get() = Icon.valueOf(this.name)

    fun listItemIcon(icon: IIcon): IconicsDrawable =
        IconicsDrawable(activity!!).listItemIcon(icon)

    fun smallListItemIcon(icon: IIcon): IconicsDrawable =
        IconicsDrawable(activity!!).smallListItemIcon(icon)

    fun TextView.setMarkdown(markdown: String) {
        val parser = Markwon.createParser()

        val theme = SpannableTheme.builderWithDefaults(activity!!)
            .headingBreakHeight(0)
            .thematicBreakColor(attrData(io.ipoli.android.R.attr.colorAccent))
            .listItemColor(attrData(io.ipoli.android.R.attr.colorAccent))
            .linkColor(attrData(io.ipoli.android.R.attr.colorAccent))
            .blockQuoteColor(attrData(io.ipoli.android.R.attr.colorAccent))
            .codeBackgroundColor(colorRes(io.ipoli.android.R.color.sourceCodeBackground))
            .codeTextColor(colorRes(io.ipoli.android.R.color.sourceCodeText))
            .codeTextSize(ViewUtils.spToPx(14, activity!!))
            .build()
        val configuration = SpannableConfiguration.builder(activity!!)
            .theme(theme)
            .build()

        val builder = SpannableBuilder()

        val node = parser.parse(markdown)

        val headlineVisitor = HeadlineColorVisitor(configuration, builder)

        node.accept(headlineVisitor)

        val text = builder.text()

        movementMethod = LinkMovementMethod.getInstance()

        Markwon.unscheduleDrawables(this)
        Markwon.unscheduleTableRows(this)

        setText(text)

        Markwon.scheduleDrawables(this)
        Markwon.scheduleTableRows(this)
    }

    inner class HeadlineColorVisitor(
        config: SpannableConfiguration,
        private val builder: SpannableBuilder
    ) : SpannableMarkdownVisitor(config, builder) {

        override fun visit(heading: Heading) {

            val startLength = builder.length()

            super.visit(heading)

            builder.setSpan(
                ForegroundColorSpan(attrData(io.ipoli.android.R.attr.colorAccent)),
                startLength,
                builder.length()
            )
        }
    }
}

abstract class ReduxViewController<A : Action, VS : ViewState, out R : ViewStateReducer<AppState, VS>>
protected constructor(args: Bundle? = null) : BaseViewController<A, VS>(args) {

    private val stateStore by required { stateStore }

    protected abstract val reducer: R

    override val stateKey get() = reducer.stateKey

    init {
        val lifecycleListener = object : LifecycleListener() {

            override fun postCreateView(controller: Controller, view: View) {
                stateStore.dispatch(UiAction.Attach(reducer))
            }

            override fun preDestroyView(controller: Controller, view: View) {
                stateStore.dispatch(UiAction.Detach(reducer))
            }
        }
        addLifecycleListener(lifecycleListener)
    }

    override fun onSubscribedToStore() {
        onRenderViewState(reducer.defaultState())
    }
}
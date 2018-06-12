package io.ipoli.android.quest.show

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.transition.ChangeBounds
import android.support.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.view.*
import kotlinx.android.synthetic.main.controller_quest_detail.view.*

class QuestDetailViewController : ReduxViewController<QuestAction, QuestViewState, QuestReducer> {

    override val reducer = QuestReducer

    private var questId = ""

    private val handler = Handler(Looper.getMainLooper())

    var updateTimer = {}

    @Suppress("unused")
    constructor(args: Bundle? = null) : super(args)

    constructor(questId: String) : super() {
        this.questId = questId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = container.inflate(R.layout.controller_quest_detail)

        setToolbar(view.toolbar)
        view.collapsingToolbarContainer.isTitleEnabled = false

        view.appbar.addOnOffsetChangedListener(object :
            AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {

                appBarLayout.post {
                    if (state == State.EXPANDED) {
                        val supportActionBar = (activity as MainActivity).supportActionBar
                        supportActionBar?.setDisplayShowTitleEnabled(false)
                    } else if (state == State.COLLAPSED) {
                        val supportActionBar = (activity as MainActivity).supportActionBar
                        supportActionBar?.setDisplayShowTitleEnabled(true)
                    }
                }
            }
        })

        view.questStartTimer.onDebounceClick {
            it.gone()
            TransitionManager.beginDelayedTransition(view.appbar, ChangeBounds())
            val p = view.appbar.layoutParams
            p.height = CoordinatorLayout.LayoutParams.MATCH_PARENT
            view.appbar.layoutParams = p

            view.questPomodoroCountText.gone()


            view.postDelayed({
                //                val tp = view.timerProgress.layoutParams
//                tp.height = LinearLayout.LayoutParams.WRAP_CONTENT
//                view.timerProgress.layoutParams = tp
                view.timerProgressLayout.visible()

                val iconImage = Ionicons.Icon.ion_stop

                val icon = IconicsDrawable(view.startStop.context)
                    .icon(iconImage)
                    .color(attrData(R.attr.colorAccent))
                    .sizeDp(22)

                view.startStop.setImageDrawable(icon)

                updateTimer = {
                    dispatch(QuestAction.Tick)
                    handler.postDelayed(updateTimer, 1000)
                }

                handler.postDelayed(updateTimer, 1000)
//                view.timerProgressLayout.requestLayout()
            }, 300)
        }

        handler.postDelayed(updateTimer, 1000)

        return view
    }

    override fun onCreateLoadAction() = QuestAction.Load(questId)

    override fun onAttach(view: View) {
        super.onAttach(view)
        showBackButton()
    }

    private fun colorLayout(
        state: QuestViewState,
        view: View
    ) {
        view.appbar.setBackgroundColor(colorRes(state.color500))
        view.toolbar.setBackgroundColor(colorRes(state.color500))
        view.collapsingToolbarContainer.setContentScrimColor(colorRes(state.color500))
        activity?.window?.navigationBarColor = colorRes(state.color500)
        activity?.window?.statusBarColor = colorRes(state.color700)
    }

    override fun render(state: QuestViewState, view: View) {
        when (state.type) {
            QuestViewState.StateType.SHOW_POMODORO -> {
                colorLayout(state, view)
                view.questName.text = state.questName
                view.questNote.setMarkdown(state.note)
            }

            QuestViewState.StateType.RUNNING -> {
                view.timerLabel.text = state.timerLabel
                view.timerProgress.progress = state.timerProgress
                renderTimerProgress(view, state)
            }

            else -> {
            }
        }
    }

    private fun renderTimerProgress(
        view: View,
        state: QuestViewState
    ) {
        view.timerProgress.max = state.maxTimerProgress
        view.timerProgress.secondaryProgress = state.maxTimerProgress
        view.timerProgress.progress = state.timerProgress
    }

    private val QuestViewState.color500
        get() = color.androidColor.color500

    private val QuestViewState.color700
        get() = color.androidColor.color700

}
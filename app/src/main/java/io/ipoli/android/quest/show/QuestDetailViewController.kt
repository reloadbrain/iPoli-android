package io.ipoli.android.quest.show

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.transition.ChangeBounds
import android.support.transition.TransitionManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.BaseRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.common.view.recyclerview.ReorderItemHelper
import io.ipoli.android.common.view.recyclerview.SimpleViewHolder
import kotlinx.android.synthetic.main.controller_quest_detail.view.*
import kotlinx.android.synthetic.main.item_quest_sub_quest.view.*

class QuestDetailViewController : ReduxViewController<QuestAction, QuestViewState, QuestReducer> {

    override val reducer = QuestReducer

    private var questId = ""

    private val handler = Handler(Looper.getMainLooper())

    var updateTimer = {}

    private lateinit var touchHelper: ItemTouchHelper

    private lateinit var newSubQuestWatcher: TextWatcher

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

        view.questSubQuestList.layoutManager = LinearLayoutManager(activity!!)
        view.questSubQuestList.adapter = SubQuestAdapter()

        val dragHelper =
            ReorderItemHelper(
                onItemMoved = { oldPosition, newPosition ->
                    (view.questSubQuestList.adapter as SubQuestAdapter).move(
                        oldPosition,
                        newPosition
                    )
                },
                onItemReordered = { oldPosition, newPosition ->
                    dispatch(QuestAction.ReorderSubQuest(oldPosition, newPosition))
                }
            )

        touchHelper = ItemTouchHelper(dragHelper)
        touchHelper.attachToRecyclerView(view.questSubQuestList)

        newSubQuestWatcher = object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
//                if (editable.isBlank()) {
//                    view.newSubQuestName.invisible()
//                } else {
//                    view.newSubQuestName.visible()
//                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }
        }

        view.newSubQuestName.addTextChangedListener(newSubQuestWatcher)
        view.newSubQuestName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                disableEditForAllSubQuests()
            }
        }

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

        renderSubQuests(state, view)

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

    private fun renderSubQuests(state: QuestViewState, view: View) {
        val adapter = view.questSubQuestList.adapter as SubQuestAdapter
        adapter.updateAll(state.subQuestViewModels)
        view.newSubQuestName.onDebounceClick {
            addSubQuest(view)
        }

        view.newSubQuestName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addSubQuest(view)
            }
            true
        }

        view.subQuestListProgress.animateProgress(
            view.subQuestListProgress.progress,
            state.subQuestListProgressPercent,
            endListener = {
                val drawable = view.subQuestListProgress.progressDrawable as LayerDrawable
                val backgroundDrawable = drawable.getDrawable(0)
                val backgroundColor =
                    if
                        (state.allSubQuestsDone) colorRes(R.color.md_green_700)
                    else
                        attrData(R.attr.colorPrimaryDark)

                backgroundDrawable.setColorFilter(
                    backgroundColor,
                    PorterDuff.Mode.SRC_ATOP
                )

                val progressDrawable = drawable.getDrawable(1)
                val progressColor =
                    if
                        (state.allSubQuestsDone) colorRes(R.color.md_green_500)
                    else
                        attrData(R.attr.colorPrimary)
                progressDrawable.setColorFilter(
                    progressColor,
                    PorterDuff.Mode.SRC_ATOP
                )
            }
        )

        if (state.hasSubQuests) {
            view.subQuestListProgressLabel.text = "${state.subQuestListProgressPercent}%"
            view.subQuestListProgressLabel.visible()
            view.doneLabel.setText(R.string.done)
            view.doneLabel.setAllCaps(true)
        } else {
            view.subQuestListProgressLabel.gone()
            view.doneLabel.text = stringRes(R.string.empty_sub_quests)
            view.doneLabel.setAllCaps(false)
        }

    }

    private fun addSubQuest(view: View) {
        val name = view.newSubQuestName.text.toString()
        dispatch(QuestAction.AddSubQuest(name))
    }

    private fun renderTimerProgress(
        view: View,
        state: QuestViewState
    ) {
        view.timerProgress.max = state.maxTimerProgress
        view.timerProgress.secondaryProgress = state.maxTimerProgress
        view.timerProgress.progress = state.timerProgress
    }

    data class SubQuestViewModel(
        val name: String,
        val isCompleted: Boolean
    ) : RecyclerViewViewModel {
        override val id: String
            get() = name + isCompleted
    }

    inner class SubQuestAdapter :
        BaseRecyclerViewAdapter<SubQuestViewModel>(R.layout.item_quest_sub_quest) {

        override fun onBindViewModel(vm: SubQuestViewModel, view: View, holder: SimpleViewHolder) {

            view.subQuestCheckBox.setOnCheckedChangeListener(null)

            view.subQuestCheckBox.isChecked = vm.isCompleted

            view.subQuestCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    dispatch(QuestAction.CompleteSubQuest(holder.adapterPosition))
                } else {
                    dispatch(QuestAction.UndoCompletedSubQuest(holder.adapterPosition))
                }
            }

            view.editSubQuestName.setText(vm.name)

            if (vm.isCompleted) {
                view.editSubQuestName.paintFlags = view.editSubQuestName.paintFlags or
                    Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                view.editSubQuestName.paintFlags = view.editSubQuestName.paintFlags and
                    Paint.STRIKE_THRU_TEXT_FLAG.inv()

            }

            view.editSubQuestName.setOnFocusChangeListener { _, hasFocus ->

                val adapterPosition = holder.adapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnFocusChangeListener
                }

                if (hasFocus) {
                    startEdit(view)
                } else {
                    dispatch(
                        QuestAction.SaveSubQuestName(
                            view.editSubQuestName.text.toString(),
                            adapterPosition
                        )
                    )
                }

            }

            view.editSubQuestName.setOnEditTextImeBackListener(object : EditTextImeBackListener {
                override fun onImeBack(ctrl: EditTextBackEvent, text: String) {
//                    enterFullScreen()
                }
            })

            view.reorderButton.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(holder)
                }
                false
            }

            view.removeButton.setOnClickListener {
                removeAt(holder.adapterPosition)
                dispatch(QuestAction.RemoveSubQuest(holder.adapterPosition))
            }
        }

        private fun startEdit(view: View) {
            disableEditForAllSubQuests()
            view.reorderButton.gone()
            view.removeButton.visible()
            view.editSubQuestName.requestFocus()
            ViewUtils.showKeyboard(view.context, view.editSubQuestName)
            view.editSubQuestName.setSelection(view.editSubQuestName.length())
        }
    }


    private fun disableEditForAllSubQuests() {
        view!!.questSubQuestList.children.forEach {
            it.removeButton.gone()
            it.reorderButton.visible()
        }
    }

    private val QuestViewState.color500
        get() = color.androidColor.color500

    private val QuestViewState.color700
        get() = color.androidColor.color700

    private val QuestViewState.subQuestViewModels: List<SubQuestViewModel>
        get() = subQuests.map {
            SubQuestViewModel(
                name = it.name,
                isCompleted = it.completedAtDate != null
            )
        }
}
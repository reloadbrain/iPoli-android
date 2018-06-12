package io.ipoli.android.challenge.show

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import io.ipoli.android.MainActivity
import io.ipoli.android.R
import io.ipoli.android.challenge.QuestPickerViewController
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.text.DateFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.anim.AccelerateDecelerateEasingFunction
import io.ipoli.android.common.view.recyclerview.SimpleSwipeCallback
import io.ipoli.android.common.view.recyclerview.SimpleViewHolder
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.RepeatingQuest
import io.ipoli.android.tag.Tag
import kotlinx.android.synthetic.main.controller_challenge.view.*
import kotlinx.android.synthetic.main.item_challenge_quest.view.*
import kotlinx.android.synthetic.main.item_quest_tag_list.view.*


/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/05/2018.
 */
class ChallengeViewController(args: Bundle? = null) :
    ReduxViewController<ChallengeAction, ChallengeViewState, ChallengeReducer>(args) {

    override val reducer = ChallengeReducer

    private lateinit var challengeId: String
    private var showEdit = true
    private var showComplete = true

    private constructor(
        challengeId: String
    ) : this() {
        this.challengeId = challengeId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.controller_challenge, container, false)
        setToolbar(view.toolbar)
        view.collapsingToolbarContainer.isTitleEnabled = false

        setupAppBar(view)

        setupHistoryChart(view.progressChart)

        view.questList.layoutManager =
            LinearLayoutManager(container.context, LinearLayoutManager.VERTICAL, false)
        view.questList.adapter = QuestAdapter()

        val swipeHandler = object : SimpleSwipeCallback(
            view.context,
            R.drawable.ic_done_white_24dp,
            R.color.md_green_500,
            R.drawable.ic_delete_white_24dp,
            R.color.md_red_500
        ) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.START) {
                    dispatch(ChallengeAction.RemoveQuestFromChallenge(viewHolder.adapterPosition))
                }
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) = ItemTouchHelper.START
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(view.questList)

        view.addQuests.setOnClickListener {
            val changeHandler = FadeChangeHandler()
            rootRouter.pushController(
                RouterTransaction.with(QuestPickerViewController(challengeId))
                    .pushChangeHandler(changeHandler)
                    .popChangeHandler(changeHandler)
            )
        }

        return view
    }

    private fun setupAppBar(view: View) {
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
    }

    private fun setupHistoryChart(chart: LineChart) {
        with(chart) {
            description = null
            setTouchEnabled(false)
            setPinchZoom(false)
            extraTopOffset = 16f
            extraBottomOffset = 16f
            extraLeftOffset = 28f

            setDrawGridBackground(false)

            axisRight.axisMinimum = 0f
            axisRight.axisMaximum = 100f
            axisRight.spaceTop = 0f
            axisRight.textSize = ViewUtils.spToPx(5, activity!!).toFloat()
            axisRight.textColor = colorRes(R.color.md_dark_text_87)
            axisRight.setValueFormatter { value, _ -> "${value.toInt()}%" }

            axisLeft.isEnabled = false

            xAxis.yOffset = ViewUtils.dpToPx(4f, activity!!)
            xAxis.isGranularityEnabled = true
            xAxis.granularity = 1f
            xAxis.textSize = ViewUtils.spToPx(5, activity!!).toFloat()
            xAxis.textColor = colorRes(R.color.md_dark_text_87)

            legend.isEnabled = false

        }

    }

    override fun onCreateLoadAction() = ChallengeAction.Load(challengeId)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.challenge_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.actionComplete).isVisible = showComplete
        menu.findItem(R.id.actionEdit).isVisible = showEdit
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {

            android.R.id.home ->
                router.handleBack()

            R.id.actionComplete -> {
                dispatch(ChallengeAction.Complete(challengeId))
                router.handleBack()
            }

            R.id.actionEdit -> {
                showEdit()
                true
            }
            R.id.actionDelete -> {
                dispatch(ChallengeAction.Remove(challengeId))
                router.handleBack()
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun showEdit() {
        navigateFromRoot().toEditChallenge(challengeId)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        showBackButton()
    }

    override fun onDetach(view: View) {
        (activity as MainActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        super.onDetach(view)
    }

    class XAxisValueFormatter(private val labels: List<String>) : IAxisValueFormatter {

        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            val idx = value.toInt()
            return if (idx < 0 || idx >= labels.size) {
                ""
            } else labels[idx]
        }
    }

    override fun render(state: ChallengeViewState, view: View) {
        when (state.type) {
            ChallengeViewState.StateType.DATA_CHANGED -> {
                showComplete = state.canComplete
                showEdit = state.canEdit
                activity!!.invalidateOptionsMenu()

                colorLayout(state, view)

                renderName(state.name, view)
                renderTags(state.tags, view)

                view.progress.animateProgressFromZero(state.progressPercent)

                view.progressText.text = state.progressText

                view.difficulty.setCompoundDrawablesWithIntrinsicBounds(
                    IconicsDrawable(view.context)
                        .icon(GoogleMaterial.Icon.gmd_fitness_center)
                        .colorRes(R.color.md_white)
                        .sizeDp(24),
                    null, null, null
                )

                view.difficulty.text = state.difficulty

                view.endDate.setCompoundDrawablesWithIntrinsicBounds(
                    IconicsDrawable(view.context)
                        .icon(MaterialDesignIconic.Icon.gmi_hourglass_outline)
                        .colorRes(R.color.md_white)
                        .sizeDp(24),
                    null, null, null
                )

                view.endDate.text = state.endText
                view.nextDate.text = state.nextText

                renderChart(state, view)
                renderMotivations(state, view)
                renderQuests(state, view)

                renderNote(state, view)
            }

            ChallengeViewState.StateType.REMOVED ->
                router.handleBack()

            else -> {
            }
        }
    }

    private fun renderTags(
        tags: List<Tag>,
        view: View
    ) {
        view.tagList.removeAllViews()

        val inflater = LayoutInflater.from(activity!!)
        tags.forEach { tag ->
            val item = inflater.inflate(R.layout.item_quest_tag_list, view.tagList, false)
            renderTag(item, tag)
            view.tagList.addView(item)
        }
    }

    private fun renderTag(view: View, tag: Tag) {
        view.tagName.text = tag.name
        val indicator = view.tagName.compoundDrawablesRelative[0] as GradientDrawable
        indicator.setColor(colorRes(tag.color.androidColor.color500))
    }

    private fun renderNote(
        state: ChallengeViewState,
        view: View
    ) {
        if (state.note != null) {
            view.note.setMarkdown(state.note)
        } else {
            view.note.setText(R.string.tap_to_add_note)
            view.note.setTextColor(colorRes(R.color.md_dark_text_54))
        }
        view.note.onDebounceClick { showEdit() }
    }

    private fun renderChart(state: ChallengeViewState, view: View) {
        view.progressChart.xAxis.setLabelCount(state.xAxisLabelCount, true)

        view.progressChart.xAxis.valueFormatter = XAxisValueFormatter(state.xAxisLabels)

        view.progressChart.axisRight.axisMaximum = state.yAxisMax.toFloat()

        view.progressChart.data = createLineData(state.chartEntries)
        view.progressChart.invalidate()
        view.progressChart.animateX(1400, AccelerateDecelerateEasingFunction)
    }

    private fun renderMotivations(state: ChallengeViewState, view: View) {
        val motivationsViews = listOf(view.motivation1, view.motivation2, view.motivation3)
        motivationsViews.forEach { it.gone() }
        state.motivations.forEachIndexed { index, text ->
            val mView = motivationsViews[index]
            mView.visible()
            mView.text = "${index + 1}. $text"
        }
    }

    private fun renderQuests(state: ChallengeViewState, view: View) {
        (view.questList.adapter as QuestAdapter).updateAll(state.questViewModels)
    }

    private fun createLineData(entries: List<Entry>): LineData {

        val set = LineDataSet(entries, "")
        set.color = attrData(R.attr.colorAccent)
        set.lineWidth = ViewUtils.dpToPx(1f, activity!!)
        set.setDrawCircles(false)
        set.setDrawFilled(true)
        set.fillColor = attrData(R.attr.colorAccent)
        set.fillAlpha = 160
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setDrawValues(false)

        set.axisDependency = YAxis.AxisDependency.RIGHT

        val d = LineData()
        d.addDataSet(set)

        return d
    }

    private fun colorLayout(
        state: ChallengeViewState,
        view: View
    ) {
        view.appbar.setBackgroundColor(colorRes(state.color500))
        view.toolbar.setBackgroundColor(colorRes(state.color500))
        view.collapsingToolbarContainer.setContentScrimColor(colorRes(state.color500))
        activity?.window?.navigationBarColor = colorRes(state.color500)
        activity?.window?.statusBarColor = colorRes(state.color700)
    }

    private fun renderName(
        name: String,
        view: View
    ) {
        toolbarTitle = name
        view.name.text = name
    }

    data class QuestViewModel(
        val id: String,
        val name: String,
        @ColorRes val color: Int,
        @ColorRes val textColor: Int,
        val icon: IIcon,
        val isRepeating: Boolean,
        val isCompleted: Boolean
    )

    inner class QuestAdapter(private var viewModels: List<QuestViewModel> = listOf()) :
        RecyclerView.Adapter<SimpleViewHolder>() {
        override fun getItemCount() = viewModels.size

        override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
            val vm = viewModels[position]
            val view = holder.itemView
            view.questName.text = vm.name
            view.questName.setTextColor(colorRes(vm.textColor))

            view.questIcon.backgroundTintList =
                ColorStateList.valueOf(colorRes(vm.color))
            view.questIcon.setImageDrawable(
                IconicsDrawable(view.context)
                    .icon(vm.icon)
                    .colorRes(R.color.md_white)
                    .sizeDp(22)
            )
            view.questRepeatIndicator.visible = vm.isRepeating
        }

        fun updateAll(viewModels: List<QuestViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SimpleViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_challenge_quest,
                    parent,
                    false
                )
            )
    }

    private val ChallengeViewState.xAxisLabels
        get() = chartData.keys.map {
            DateFormatter.formatWithoutYear(activity!!, it)
        }

    private val ChallengeViewState.chartEntries
        get() = chartData.values.mapIndexed { i, value ->
            Entry(i.toFloat(), value)
        }

    private val ChallengeViewState.color500
        get() = color.androidColor.color500

    private val ChallengeViewState.color700
        get() = color.androidColor.color700

    private val ChallengeViewState.progressText
        get() = "$completedCount of $totalCount ($progressPercent%) done"

    private val ChallengeViewState.endText
        get() = DateFormatter.formatWithoutYear(activity!!, endDate)

    private val ChallengeViewState.nextText
        get() = nextDate?.let { DateFormatter.formatWithoutYear(activity!!, it) }
            ?: stringRes(R.string.unscheduled)

    private val ChallengeViewState.questViewModels
        get() = quests.map {
            when (it) {
                is Quest -> QuestViewModel(
                    id = it.id,
                    name = it.name,
                    color = if (it.isCompleted) R.color.md_grey_300 else it.color.androidColor.color500,
                    textColor = if (it.isCompleted) R.color.md_dark_text_38 else R.color.md_dark_text_54,
                    icon = it.icon?.androidIcon?.icon ?: GoogleMaterial.Icon.gmd_local_florist,
                    isRepeating = false,
                    isCompleted = it.isCompleted
                )
                is RepeatingQuest -> QuestViewModel(
                    id = it.id,
                    name = it.name,
                    color = it.color.androidColor.color500,
                    textColor = if (it.isCompleted) R.color.md_dark_text_38 else R.color.md_dark_text_54,
                    icon = it.icon?.androidIcon?.icon ?: GoogleMaterial.Icon.gmd_local_florist,
                    isRepeating = true,
                    isCompleted = it.isCompleted
                )
            }

        }

    companion object {
        fun routerTransaction(challengeId: String) =
            RouterTransaction.with(ChallengeViewController(challengeId))
                .pushChangeHandler(VerticalChangeHandler())
                .popChangeHandler(VerticalChangeHandler())
    }
}
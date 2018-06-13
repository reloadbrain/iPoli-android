package io.ipoli.android.quest.show

import io.ipoli.android.Constants
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.datetime.*
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.TimeRange
import io.ipoli.android.quest.show.QuestDetailViewController.PomodoroProgress
import io.ipoli.android.quest.show.QuestViewState.StateType.*
import io.ipoli.android.quest.show.sideeffect.TimerStartedAction
import io.ipoli.android.quest.show.view.formatter.TimerFormatter
import io.ipoli.android.quest.subquest.SubQuest
import io.ipoli.android.tag.Tag
import org.threeten.bp.Instant

/**
 * Created by Venelin Valkov <venelin@io.ipoli.io>
 * on 6.01.18.
 */

sealed class QuestAction : Action {
    data class Load(val questId: String) : QuestAction()
    data class Changed(val quest: Quest) : QuestAction()
    data class CompleteSubQuest(val position: Int) : QuestAction()
    data class UndoCompletedSubQuest(val position: Int) : QuestAction()
    data class SaveSubQuestName(val name: String, val position: Int) : QuestAction()
    data class AddSubQuest(val name: String) : QuestAction()
    data class RemoveSubQuest(val position: Int) : QuestAction()
    data class ReorderSubQuest(val oldPosition: Int, val newPosition: Int) : QuestAction()
    data class SaveNote(val note: String) : QuestAction()
    data class UpdateNote(val note: String) : QuestAction()

    object Start : QuestAction()
    object Stop : QuestAction()
    object Tick : QuestAction()
    object CompletePomodoro : QuestAction()
    object ShowCountDownTimer : QuestAction()
    object ShowPomodoroTimer : QuestAction()
    object CompleteQuest : QuestAction()
    object AddPomodoro : QuestAction()
    object RemovePomodoro : QuestAction()
}

object QuestReducer : BaseViewStateReducer<QuestViewState>() {

    override val stateKey = key<QuestViewState>()

    override fun reduce(state: AppState, subState: QuestViewState, action: Action) =
        when (action) {

            is DataLoadedAction.QuestChanged -> {
                if (action.quest.isCompleted) {
                    subState.copy(
                        type = QUEST_COMPLETED
                    )
                } else {
                    createQuestChangedState(subState.copy(quest = action.quest))
                }
            }

            is TimerStartedAction -> {
                val type =
                    if (action.otherTimerStopped)
                        TIMER_REPLACED
                    else
                        TIMER_STARTED
                subState.copy(
                    type = type
                )
            }

            QuestAction.Stop -> {
                subState.copy(
                    type = TIMER_STOPPED
                )
            }

            QuestAction.Tick -> {
                val remainingTime = subState.remainingTime!! - 1.seconds
                val shouldShowCompletePomodoroButton =
                    subState.timerType == QuestViewState.TimerType.POMODORO && remainingTime < 0.seconds
                subState.copy(
                    type = RUNNING,
                    timerProgress = subState.timerProgress + 1,
                    timerLabel = formatDuration(remainingTime),
                    remainingTime = remainingTime,
                    showCompletePomodoroButton = shouldShowCompletePomodoroButton
                )
            }

            QuestAction.CompletePomodoro -> {
                subState.copy(
                    type = TIMER_STOPPED
                )
            }

            QuestAction.ShowPomodoroTimer -> {
                createStateForInitialPomodoroTimer(subState, subState.quest!!)
            }

            QuestAction.ShowCountDownTimer -> {
                createStateForInitialCountDownTimer(subState, subState.quest!!)
            }

            QuestAction.CompleteQuest -> {
                subState.copy(
                    type = TIMER_STOPPED
                )
            }

            QuestAction.AddPomodoro -> {
                subState.copy(
                    type = POMODORO_ADDED
                )
            }

            QuestAction.RemovePomodoro -> {
                subState.copy(
                    type = POMODORO_REMOVED
                )
            }

            is QuestAction.AddSubQuest -> {
                subState.copy(
                    type = SUB_QUEST_ADDED
                )
            }

            else -> subState
        }

    override fun defaultState() = QuestViewState(type = LOADING, tags = listOf())

    private fun formatDuration(duration: Duration<Second>): String {
        return if (duration >= 0.seconds) {
            TimerFormatter.format(duration.millisValue)
        } else {
            "+" + TimerFormatter.format(Math.abs(duration.millisValue))
        }
    }

    private fun createQuestChangedState(
        state: QuestViewState
    ): QuestViewState {

        val quest = state.quest!!

        val completedSubQuestsCount = quest.subQuests.count { it.completedAtDate != null }

        val hasSubQuests = quest.subQuests.isNotEmpty()

        val newState = state.copy(
            questName = quest.name,
            color = quest.color,
            subQuests = quest.subQuests,
            subQuestListProgressPercent = ((completedSubQuestsCount.toFloat() / quest.subQuests.size) * 100).toInt(),
            hasSubQuests = hasSubQuests,
            allSubQuestsDone = hasSubQuests && completedSubQuestsCount == quest.subQuests.size,
            tags = quest.tags,
            note = quest.note
        )


        if (quest.hasCountDownTimer) {
            return createStateForRunningCountdownTimer(quest, newState)
        }

        if (quest.hasPomodoroTimer) {
            return createStateForRunningPomodoroTimer(quest, newState)
        }

        return if (quest.duration < MIN_INITIAL_POMODORO_TIMER_DURATION) {
            createStateForInitialCountDownTimer(newState, quest)
        } else {
            createStateForInitialPomodoroTimer(newState, quest)
        }
    }

    private fun createStateForInitialPomodoroTimer(
        state: QuestViewState,
        quest: Quest
    ) =
        state.copy(
            type = SHOW_POMODORO,
            timerType = QuestViewState.TimerType.POMODORO,
            showTimerTypeSwitch = true,
            pomodoroProgress = quest.timeRangesToComplete.map { createPomodoroProgress(it) },
            timerLabel = formatDuration(Constants.DEFAULT_POMODORO_WORK_DURATION.minutes.asSeconds),
            remainingTime = Constants.DEFAULT_POMODORO_WORK_DURATION.minutes.asSeconds,
            currentProgressIndicator = 0,
            timerProgress = 0,
            maxTimerProgress = Constants.DEFAULT_POMODORO_WORK_DURATION.minutes.asSeconds.intValue
        )

    private fun createStateForInitialCountDownTimer(
        state: QuestViewState,
        quest: Quest
    ) =
        state.copy(
            type = SHOW_COUNTDOWN,
            timerType = QuestViewState.TimerType.COUNTDOWN,
            showTimerTypeSwitch = quest.duration >= MIN_POMODORO_TIMER_DURATION,
            timerLabel = formatDuration(quest.duration.minutes.asSeconds),
            remainingTime = quest.duration.minutes.asSeconds,
            timerProgress = 0,
            maxTimerProgress = quest.duration.minutes.asSeconds.intValue
        )

    private fun createStateForRunningPomodoroTimer(
        quest: Quest,
        state: QuestViewState
    ): QuestViewState {
        val currentProgressIndicator =
            findCurrentProgressIndicator(quest.timeRangesToComplete)

        val currentTimeRange = if (currentProgressIndicator >= 0) {
            quest.timeRanges.last()
        } else {
            quest.timeRangesToComplete[quest.timeRanges.size]
        }

        val duration = currentTimeRange.duration
        val passed: Duration<Millisecond> = if (currentTimeRange.start != null)
            (Instant.now() - currentTimeRange.start).milliseconds
        else
            0.milliseconds

        val remainingTime = duration.minutes - passed

        val type =
            if (quest.timeRanges.last().end == null) RESUMED
            else SHOW_POMODORO

        return state.copy(
            type = type,
            timerType = QuestViewState.TimerType.POMODORO,
            showTimerTypeSwitch = false,
            pomodoroProgress = quest.timeRangesToComplete.map { createPomodoroProgress(it) },
            timerLabel = formatDuration(remainingTime.asSeconds),
            remainingTime = remainingTime.asSeconds,
            currentProgressIndicator = currentProgressIndicator,
            timerProgress = passed.asSeconds.intValue,
            maxTimerProgress = duration.minutes.asSeconds.intValue
        )
    }

    private fun createStateForRunningCountdownTimer(
        quest: Quest,
        state: QuestViewState
    ): QuestViewState {

        val passed = Instant.now() - quest.actualStart!!
        val remainingTime =
            quest.duration.minutes - passed.milliseconds
        return state.copy(
            type = RESUMED,
            timerType = QuestViewState.TimerType.COUNTDOWN,
            showTimerTypeSwitch = false,
            timerLabel = formatDuration(remainingTime.asSeconds),
            remainingTime = remainingTime.asSeconds,
            timerProgress = passed.milliseconds.asSeconds.intValue,
            maxTimerProgress = quest.duration.minutes.asSeconds.intValue
        )
    }

    private fun findCurrentProgressIndicator(timeRanges: List<TimeRange>): Int =
        timeRanges.indexOfFirst { it.start != null && it.end == null }

    private fun createPomodoroProgress(timeRange: TimeRange): QuestDetailViewController.PomodoroProgress {
        return when (timeRange.type) {
            TimeRange.Type.POMODORO_SHORT_BREAK -> {
                if (timeRange.end != null) {
                    PomodoroProgress.COMPLETE_SHORT_BREAK
                } else {
                    PomodoroProgress.INCOMPLETE_SHORT_BREAK
                }
            }

            TimeRange.Type.POMODORO_LONG_BREAK -> {
                if (timeRange.end != null) {
                    PomodoroProgress.COMPLETE_LONG_BREAK
                } else {
                    PomodoroProgress.INCOMPLETE_LONG_BREAK
                }
            }

            TimeRange.Type.POMODORO_WORK -> {
                if (timeRange.end != null) {
                    PomodoroProgress.COMPLETE_WORK
                } else {
                    PomodoroProgress.INCOMPLETE_WORK
                }
            }
            else -> PomodoroProgress.COMPLETE_WORK
        }
    }

    const val MIN_INITIAL_POMODORO_TIMER_DURATION =
        Constants.DEFAULT_POMODORO_WORK_DURATION * 2 + Constants.DEFAULT_POMODORO_BREAK_DURATION * 2

    const val MIN_POMODORO_TIMER_DURATION =
        Constants.DEFAULT_POMODORO_WORK_DURATION + Constants.DEFAULT_POMODORO_BREAK_DURATION

}

data class QuestViewState(
    val type: StateType,
    val quest: Quest? = null,
    val color: Color = Color.BLUE,
    val showTimerTypeSwitch: Boolean = false,
    val timerLabel: String = "",
    val remainingTime: Duration<Second>? = null,
    val timerType: TimerType = TimerType.COUNTDOWN,
    val questName: String = "",
    val subQuests: List<SubQuest> = listOf(),
    val subQuestListProgressPercent: Int = 0,
    val hasSubQuests: Boolean = false,
    val allSubQuestsDone: Boolean = false,
    val timerProgress: Int = 0,
    val maxTimerProgress: Int = 0,
    val pomodoroProgress: List<PomodoroProgress> = listOf(),
    val currentProgressIndicator: Int = 0,
    val showCompletePomodoroButton: Boolean = false,
    val tags: List<Tag>,
    val note: String = ""
) : BaseViewState() {

    enum class StateType {
        LOADING,
        RESUMED,
        SHOW_POMODORO,
        SHOW_COUNTDOWN,
        TIMER_STARTED,
        TIMER_REPLACED,
        TIMER_STOPPED,
        RUNNING,
        POMODORO_ADDED,
        POMODORO_REMOVED,
        QUEST_COMPLETED,
        SUB_QUEST_ADDED
    }

    enum class TimerType {
        COUNTDOWN,
        POMODORO
    }
}
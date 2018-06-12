package io.ipoli.android.repeatingquest.add

import io.ipoli.android.Constants
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.Validator
import io.ipoli.android.common.datetime.Duration
import io.ipoli.android.common.datetime.Minute
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.datetime.minutes
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.quest.Reminder
import io.ipoli.android.quest.reminder.picker.ReminderViewModel
import io.ipoli.android.repeatingquest.add.EditRepeatingQuestViewState.DurationOption.*
import io.ipoli.android.repeatingquest.add.EditRepeatingQuestViewState.RepeatPatternOption.*
import io.ipoli.android.repeatingquest.add.EditRepeatingQuestViewState.StateType.*
import io.ipoli.android.repeatingquest.entity.RepeatPattern
import io.ipoli.android.tag.Tag
import org.threeten.bp.DayOfWeek

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 04/09/2018.
 */
sealed class EditRepeatingQuestAction : Action {
    data class ChangeColor(val color: Color) : EditRepeatingQuestAction()
    data class ChangeIcon(val icon: Icon?) : EditRepeatingQuestAction()
    data class ShowRepeatPatternPicker(val name: String) : EditRepeatingQuestAction()
    data class PickRepeatPattern(val repeatPatternOption: EditRepeatingQuestViewState.RepeatPatternOption) :
        EditRepeatingQuestAction()

    data class RepeatPatternPicked(val repeatPattern: RepeatPattern) :
        EditRepeatingQuestAction()

    data class PickDuration(val durationOption: EditRepeatingQuestViewState.DurationOption) :
        EditRepeatingQuestAction()

    data class DurationPicked(val minutes: Int) : EditRepeatingQuestAction()
    data class AddSubQuest(val name: String) : EditRepeatingQuestAction()
    data class ChangeChallenge(val challenge: Challenge?) : EditRepeatingQuestAction()
    data class ChangeReminder(val reminder: ReminderViewModel?) : EditRepeatingQuestAction()
    data class ChangeRepeatPattern(val repeatPattern: RepeatPattern?) : EditRepeatingQuestAction()
    data class ChangeStartTime(val time: Time?) : EditRepeatingQuestAction()
    data class ChangeDuration(val minutes: Int) : EditRepeatingQuestAction()
    data class ChangeNote(val text: String) : EditRepeatingQuestAction()

    data class RemoveTag(val tag: Tag) : EditRepeatingQuestAction()
    data class AddTag(val tagName: String) : EditRepeatingQuestAction()
    data class ValidateName(val name: String) : EditRepeatingQuestAction()
    data class Load(val repeatingQuestId: String) : EditRepeatingQuestAction()

    object Back : EditRepeatingQuestAction()

    object Save : EditRepeatingQuestAction()
    object LoadName : EditRepeatingQuestAction()
    object LoadSummary : EditRepeatingQuestAction()
    object SaveNew : EditRepeatingQuestAction()
}

object EditRepeatingQuestReducer : BaseViewStateReducer<EditRepeatingQuestViewState>() {

    override val stateKey = key<EditRepeatingQuestViewState>()

    override fun reduce(
        state: AppState,
        subState: EditRepeatingQuestViewState,
        action: Action
    ) =
        when (action) {

            is EditRepeatingQuestAction.ShowRepeatPatternPicker -> {
                val name = action.name
                val errors = Validator.validate(action).check<ValidationError> {
                    "name" {
                        given { name.isBlank() } addError ValidationError.EMPTY_NAME
                    }
                }

                if (errors.isEmpty()) {
                    subState.copy(
                        type = NEXT_PAGE,
                        name = action.name,
                        adapterPosition = subState.adapterPosition + 1
                    )
                } else {
                    subState.copy(
                        type = VALIDATION_ERROR_EMPTY_NAME
                    )
                }
            }

            is EditRepeatingQuestAction.ValidateName -> {
                val name = action.name
                val errors = Validator.validate(action).check<ValidationError> {
                    "name" {
                        given { name.isBlank() } addError ValidationError.EMPTY_NAME
                    }
                }
                if (errors.isEmpty()) {
                    subState.copy(
                        type = VALID_NAME,
                        name = name
                    )
                } else {
                    subState.copy(
                        type = VALIDATION_ERROR_EMPTY_NAME
                    )
                }
            }

            is EditRepeatingQuestAction.ChangeIcon -> {
                subState.copy(
                    type = ICON_CHANGED,
                    icon = action.icon
                )
            }

            is EditRepeatingQuestAction.ChangeColor -> {
                subState.copy(
                    type = COLOR_CHANGED,
                    color = action.color
                )
            }

            is EditRepeatingQuestAction.PickDuration ->
                subState.copy(
                    type = NEXT_PAGE,
                    duration = createDurationFromOption(action.durationOption),
                    adapterPosition = subState.adapterPosition + 1
                )

            is EditRepeatingQuestAction.DurationPicked ->
                subState.copy(
                    type = NEXT_PAGE,
                    duration = action.minutes.minutes,
                    adapterPosition = subState.adapterPosition + 1
                )

            is EditRepeatingQuestAction.PickRepeatPattern ->
                subState.copy(
                    type = NEXT_PAGE,
                    repeatPattern = createRepeatingPatternFromOption(action.repeatPatternOption),
                    adapterPosition = subState.adapterPosition + 1
                )

            is EditRepeatingQuestAction.RepeatPatternPicked ->
                subState.copy(
                    type = NEXT_PAGE,
                    repeatPattern = action.repeatPattern,
                    adapterPosition = subState.adapterPosition + 1
                )

            EditRepeatingQuestAction.Back -> {
                val adapterPosition = subState.adapterPosition - 1
                if (adapterPosition < 0) {
                    subState.copy(
                        type = CLOSE
                    )
                } else {
                    subState.copy(
                        type = PREVIOUS_PAGE,
                        adapterPosition = adapterPosition
                    )
                }
            }

            EditRepeatingQuestAction.LoadSummary ->
                subState.copy(
                    type = SUMMARY_DATA_LOADED
                )

            is EditRepeatingQuestAction.Load -> {
                val dataState = state.dataState
                val rq = dataState.repeatingQuests!!.first { it.id == action.repeatingQuestId }

                subState.copy(
                    type = SUMMARY_DATA_LOADED,
                    id = rq.id,
                    name = rq.name,
                    questTags = rq.tags,
                    tags = state.dataState.tags - rq.tags,
                    subQuestNames = rq.subQuests.map { it.name },
                    startTime = rq.startTime,
                    repeatPattern = rq.repeatPattern,
                    duration = rq.duration.minutes,
                    reminder = rq.reminders.firstOrNull()?.let {
                        if (it is Reminder.Relative) {
                            ReminderViewModel(
                                it.message,
                                it.minutesFromStart.toLong()
                            )
                        } else null
                    },
                    icon = rq.icon,
                    color = rq.color,
                    note = rq.note,
                    maxTagsReached = rq.tags.size >= Constants.MAX_TAGS_PER_ITEM
                )
            }

            EditRepeatingQuestAction.LoadName ->
                subState.copy(
                    type = NAME_DATA_LOADED,
                    tags = state.dataState.tags
                )

            is EditRepeatingQuestAction.AddSubQuest ->
                subState.copy(
                    type = SUB_QUEST_ADDED,
                    subQuestNames = subState.subQuestNames + action.name,
                    newSubQuestName = action.name
                )

            is EditRepeatingQuestAction.ChangeRepeatPattern ->
                subState.copy(
                    type = REPEAT_PATTERN_CHANGED,
                    repeatPattern = action.repeatPattern
                )

            is EditRepeatingQuestAction.ChangeStartTime ->
                subState.copy(
                    type = START_TIME_CHANGED,
                    startTime = action.time
                )

            is EditRepeatingQuestAction.ChangeDuration ->
                subState.copy(
                    type = DURATION_CHANGED,
                    duration = action.minutes.minutes
                )

            is EditRepeatingQuestAction.ChangeReminder ->
                subState.copy(
                    type = REMINDER_CHANGED,
                    reminder = action.reminder
                )

            is EditRepeatingQuestAction.ChangeChallenge ->
                subState.copy(
                    type = CHALLENGE_CHANGED,
                    challenge = action.challenge
                )

            is EditRepeatingQuestAction.ChangeNote -> {
                val note = action.text.trim()
                subState.copy(
                    type = NOTE_CHANGED,
                    note = note
                )
            }

            is DataLoadedAction.TagsChanged ->
                subState.copy(
                    type = TAGS_CHANGED,
                    tags = action.tags - subState.questTags
                )

            is EditRepeatingQuestAction.RemoveTag -> {
                val questTags = subState.questTags - action.tag
                subState.copy(
                    type = TAGS_CHANGED,
                    questTags = questTags,
                    tags = subState.tags + action.tag,
                    maxTagsReached = questTags.size >= Constants.MAX_TAGS_PER_ITEM
                )
            }

            is EditRepeatingQuestAction.AddTag -> {
                val tag = subState.tags.first { it.name == action.tagName }
                val questTags = subState.questTags + tag
                subState.copy(
                    type = TAGS_CHANGED,
                    questTags = questTags,
                    tags = subState.tags - tag,
                    maxTagsReached = questTags.size >= Constants.MAX_TAGS_PER_ITEM
                )
            }

            EditRepeatingQuestAction.Save ->
                subState.copy(
                    type = CLOSE
                )

            else -> subState
        }

    private fun createRepeatingPatternFromOption(
        option: EditRepeatingQuestViewState.RepeatPatternOption
    ) =
        when (option) {
            EVERY_DAY -> RepeatPattern.Daily()
            ONCE_PER_WEEK -> RepeatPattern.Flexible.Weekly(1)
            THREE_PER_WEEK -> RepeatPattern.Flexible.Weekly(3)
            FIVE_PER_WEEK -> RepeatPattern.Flexible.Weekly(5)
            WORK_DAYS -> RepeatPattern.Weekly(
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                )
            )
            WEEKEND_DAYS -> RepeatPattern.Weekly(
                setOf(
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                )
            )

            else -> throw IllegalArgumentException("Unknown repeat option $option")
        }

    private fun createDurationFromOption(option: EditRepeatingQuestViewState.DurationOption) =
        when (option) {
            TEN_MINUTES ->
                10.minutes
            FIFTEEN_MINUTES -> 15.minutes
            TWENTY_FIVE_MINUTES -> 25.minutes
            THIRTY_MINUTES -> 30.minutes
            ONE_HOUR -> 60.minutes
            TWO_HOURS -> 120.minutes
            else -> throw IllegalArgumentException("Unknown duration option $option")
        }

    override fun defaultState() =
        EditRepeatingQuestViewState(
            type = INITIAL,
            id = "",
            adapterPosition = 0,
            name = "",
            tags = emptyList(),
            questTags = emptyList(),
            subQuestNames = emptyList(),
            newSubQuestName = "",
            startTime = null,
            color = Color.GREEN,
            icon = null,
            repeatPattern = null,
            reminder = ReminderViewModel(
                message = "",
                minutesFromStart = Constants.DEFAULT_RELATIVE_REMINDER_MINUTES_FROM_START
            ),
            challenge = null,
            note = "",
            maxTagsReached = false
        )

    enum class ValidationError {
        EMPTY_NAME
    }
}

data class EditRepeatingQuestViewState(
    val type: StateType,
    val id: String,
    val adapterPosition: Int,
    val name: String,
    val tags: List<Tag>,
    val questTags: List<Tag>,
    val subQuestNames: List<String>,
    val newSubQuestName: String,
    val startTime: Time?,
    val color: Color,
    val icon: Icon?,
    val repeatPattern: RepeatPattern?,
    val reminder: ReminderViewModel?,
    val challenge: Challenge?,
    val duration: Duration<Minute> = Constants.DEFAULT_QUEST_DURATION.minutes,
    val note: String,
    val maxTagsReached: Boolean
) : BaseViewState() {

    enum class StateType {
        INITIAL,
        VALID_NAME,
        VALIDATION_ERROR_EMPTY_NAME,
        TAGS_CHANGED,
        NEXT_PAGE,
        PREVIOUS_PAGE,
        SUB_QUEST_ADDED,
        CLOSE,
        REPEAT_PATTERN_CHANGED,
        START_TIME_CHANGED,
        DURATION_CHANGED,
        REMINDER_CHANGED,
        CHALLENGE_CHANGED,
        COLOR_CHANGED,
        ICON_CHANGED,
        NOTE_CHANGED,
        SUMMARY_DATA_LOADED,
        NAME_DATA_LOADED
    }

    enum class RepeatPatternOption {
        EVERY_DAY,
        ONCE_PER_WEEK,
        THREE_PER_WEEK,
        FIVE_PER_WEEK,
        WORK_DAYS,
        WEEKEND_DAYS,
        MORE_OPTIONS
    }

    enum class DurationOption {
        TEN_MINUTES,
        FIFTEEN_MINUTES,
        TWENTY_FIVE_MINUTES,
        THIRTY_MINUTES,
        ONE_HOUR,
        TWO_HOURS,
        MORE_OPTIONS
    }
}
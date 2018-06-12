package io.ipoli.android.challenge.add

import io.ipoli.android.Constants
import io.ipoli.android.challenge.QuestPickerAction
import io.ipoli.android.challenge.QuestPickerViewState
import io.ipoli.android.challenge.add.EditChallengeViewState.StateType.*
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.Validator
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.quest.BaseQuest
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.tag.Tag
import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 3/8/18.
 */
sealed class EditChallengeAction : Action {
    data class ChangeColor(val color: Color) : EditChallengeAction()
    data class ChangeIcon(val icon: Icon?) : EditChallengeAction()
    data class ChangeDifficulty(val position: Int) : EditChallengeAction()
    data class ValidateName(val name: String) : EditChallengeAction()
    data class ValidateMotivation(val motivationList: List<String>) : EditChallengeAction()
    data class SelectDate(val date: LocalDate) : EditChallengeAction()
    data class ChangeNote(val note: String) : EditChallengeAction()
    data class RemoveTag(val tag: Tag) : EditChallengeAction()
    data class AddTag(val tagName: String) : EditChallengeAction()
    data class Load(val challengeId: String) : EditChallengeAction()
    data class ChangeEndDate(val date: LocalDate) : EditChallengeAction()
    data class ChangeMotivations(
        val motivation1: String,
        val motivation2: String,
        val motivation3: String
    ) : EditChallengeAction()

    object ShowNext : EditChallengeAction()
    object UpdateSummary : EditChallengeAction()
    object LoadSummary : EditChallengeAction()
    object LoadTags : EditChallengeAction()

    object Back : EditChallengeAction()
    object Save : EditChallengeAction()
    object LoadFirstPage : EditChallengeAction()
    object SaveNew : EditChallengeAction()
}

object EditChallengeReducer : BaseViewStateReducer<EditChallengeViewState>() {

    override val stateKey = key<EditChallengeViewState>()


    override fun reduce(
        state: AppState,
        subState: EditChallengeViewState,
        action: Action
    ) =
        when (action) {
            EditChallengeAction.LoadFirstPage -> {
                subState.copy(
                    type = LOADING
                )
            }

            is EditChallengeAction.Load -> {
                val dataState = state.dataState
                val c = dataState.challenges!!.first { it.id == action.challengeId }
                subState.copy(
                    type = DATA_CHANGED,
                    id = action.challengeId,
                    name = c.name,
                    challengeTags = c.tags,
                    tags = state.dataState.tags - c.tags,
                    icon = c.icon,
                    color = c.color,
                    difficulty = c.difficulty,
                    end = c.endDate,
                    motivation1 = c.motivation1,
                    motivation2 = c.motivation2,
                    motivation3 = c.motivation3,
                    note = c.note,
                    quests = c.baseQuests,
                    maxTagsReached = c.tags.size >= Constants.MAX_TAGS_PER_ITEM
                )
            }

            is EditChallengeAction.ShowNext ->
                if (subState.adapterPosition + 1 > AddChallengeViewController.SUMMARY_INDEX)
                    subState
                else
                    subState.copy(
                        type = NEXT_PAGE,
                        adapterPosition = subState.adapterPosition + 1
                    )

            is EditChallengeAction.LoadTags -> {
                subState.copy(
                    type = TAGS_CHANGED,
                    tags = state.dataState.tags - subState.challengeTags
                )
            }

            is DataLoadedAction.TagsChanged -> {
                subState.copy(
                    type = TAGS_CHANGED,
                    tags = action.tags - subState.challengeTags
                )
            }

            is EditChallengeAction.RemoveTag -> {
                val challengeTags = subState.challengeTags - action.tag
                subState.copy(
                    type = TAGS_CHANGED,
                    challengeTags = challengeTags,
                    tags = subState.tags + action.tag,
                    maxTagsReached = challengeTags.size >= Constants.MAX_TAGS_PER_ITEM
                )
            }

            is EditChallengeAction.AddTag -> {
                val tag = subState.tags.first { it.name == action.tagName }
                val challengeTags = subState.challengeTags + tag
                subState.copy(
                    type = TAGS_CHANGED,
                    challengeTags = challengeTags,
                    tags = subState.tags - tag,
                    maxTagsReached = challengeTags.size >= Constants.MAX_TAGS_PER_ITEM
                )
            }

            is EditChallengeAction.ChangeColor -> {
                subState.copy(
                    type = COLOR_CHANGED,
                    color = action.color
                )
            }

            is EditChallengeAction.ChangeIcon ->
                subState.copy(
                    type = ICON_CHANGED,
                    icon = action.icon
                )

            is EditChallengeAction.ChangeDifficulty ->
                subState.copy(
                    type = DIFFICULTY_CHANGED,
                    difficulty = Challenge.Difficulty.values()[action.position]
                )

            is EditChallengeAction.ValidateName -> {
                val errors = Validator.validate(action).check<ValidationError> {
                    "name" {
                        given { name.isEmpty() } addError ValidationError.EMPTY_NAME
                    }
                }
                subState.copy(
                    type = if (errors.isEmpty()) {
                        VALIDATION_NAME_SUCCESSFUL
                    } else {
                        VALIDATION_ERROR_EMPTY_NAME
                    },
                    name = action.name
                )
            }

            is EditChallengeAction.ValidateMotivation -> {
                val errors = Validator.validate(action).check<ValidationError> {
                    "name" {
                        given {
                            motivationList.isEmpty()
                                || motivationList.none { it.isNotBlank() }
                        } addError ValidationError.EMPTY_MOTIVATION
                    }
                }
                val motivationList = action.motivationList
                subState.copy(
                    type = if (errors.isEmpty()) {
                        VALIDATION_MOTIVATION_SUCCESSFUL
                    } else {
                        VALIDATION_ERROR_EMPTY_MOTIVATION
                    },
                    motivation1 = if (motivationList.isNotEmpty()) motivationList[0] else "",
                    motivation2 = if (motivationList.size > 1) motivationList[1] else "",
                    motivation3 = if (motivationList.size > 2) motivationList[2] else ""
                )
            }

            is EditChallengeAction.SelectDate -> {
                subState.copy(
                    type = NEXT_PAGE,
                    adapterPosition = subState.adapterPosition + 1,
                    end = action.date
                )
            }

            is QuestPickerAction.Next -> {
                val s = state.stateFor(QuestPickerViewState::class.java)
                subState.copy(
                    type = NEXT_PAGE,
                    adapterPosition = subState.adapterPosition + 1,
                    allQuests = s.allQuests.map {
                        it.baseQuest
                    },
                    selectedQuestIds = s.selectedQuests
                )
            }

            EditChallengeAction.Back -> {
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

            is EditChallengeAction.LoadSummary -> {
                subState.copy(
                    type = SUMMARY_DATA_LOADED,
                    quests = subState.allQuests.filter { subState.selectedQuestIds.contains(it.id) }
                )
            }

            is EditChallengeAction.ChangeNote ->
                subState.copy(
                    type = NOTE_CHANGED,
                    note = action.note.trim()
                )

            is EditChallengeAction.ChangeEndDate -> {
                subState.copy(
                    type = END_DATE_CHANGED,
                    end = action.date
                )
            }

            is EditChallengeAction.ChangeMotivations -> {
                if (action.motivation1.isEmpty() && action.motivation2.isEmpty() && action.motivation3.isEmpty()) {
                    subState
                } else {
                    subState.copy(
                        type = MOTIVATIONS_CHANGED,
                        motivation1 = action.motivation1,
                        motivation2 = action.motivation2,
                        motivation3 = action.motivation3
                    )
                }
            }

            EditChallengeAction.Save ->
                subState.copy(
                    type = CLOSE
                )


            else -> subState
        }

    override fun defaultState() =
        EditChallengeViewState(
            type = INITIAL,
            adapterPosition = 0,
            id = "",
            name = "",
            challengeTags = emptyList(),
            tags = emptyList(),
            color = Color.GREEN,
            icon = null,
            difficulty = Challenge.Difficulty.NORMAL,
            end = LocalDate.now(),
            motivation1 = "",
            motivation2 = "",
            motivation3 = "",
            allQuests = emptyList(),
            quests = emptyList(),
            selectedQuestIds = emptySet(),
            note = "",
            maxTagsReached = false
        )

    enum class ValidationError {
        EMPTY_NAME, EMPTY_MOTIVATION
    }
}


data class EditChallengeViewState(
    val type: EditChallengeViewState.StateType,
    val adapterPosition: Int,
    val id: String,
    val name: String,
    val challengeTags: List<Tag>,
    val tags: List<Tag>,
    val color: Color,
    val icon: Icon?,
    val difficulty: Challenge.Difficulty,
    val end: LocalDate,
    val motivation1: String,
    val motivation2: String,
    val motivation3: String,
    val allQuests: List<BaseQuest>,
    val quests: List<BaseQuest>,
    val selectedQuestIds: Set<String>,
    val note: String,
    val maxTagsReached: Boolean
) : BaseViewState() {
    enum class StateType {
        INITIAL,
        LOADING,
        DATA_CHANGED,
        NEXT_PAGE,
        PREVIOUS_PAGE,
        CLOSE,
        COLOR_CHANGED,
        ICON_CHANGED,
        NOTE_CHANGED,
        DIFFICULTY_CHANGED,
        VALIDATION_ERROR_EMPTY_NAME,
        VALIDATION_ERROR_EMPTY_MOTIVATION,
        VALIDATION_NAME_SUCCESSFUL,
        VALIDATION_MOTIVATION_SUCCESSFUL,
        SUMMARY_DATA_LOADED,
        TAGS_CHANGED,
        END_DATE_CHANGED,
        MOTIVATIONS_CHANGED
    }
}
package io.ipoli.android.common

import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.common.datetime.Duration
import io.ipoli.android.common.datetime.Minute
import io.ipoli.android.common.redux.Action
import io.ipoli.android.common.redux.Reducer
import io.ipoli.android.common.redux.State
import io.ipoli.android.event.Calendar
import io.ipoli.android.event.Event
import io.ipoli.android.growth.usecase.CalculateGrowthStatsUseCase
import io.ipoli.android.planday.data.Weather
import io.ipoli.android.planday.persistence.MotivationalImage
import io.ipoli.android.planday.persistence.Quote
import io.ipoli.android.player.data.Player
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.RepeatingQuest
import io.ipoli.android.quest.schedule.agenda.usecase.CreateAgendaItemsUseCase
import io.ipoli.android.quest.usecase.Schedule
import io.ipoli.android.repeatingquest.usecase.CreateRepeatingQuestHistoryUseCase
import io.ipoli.android.store.purchase.GemPack
import io.ipoli.android.tag.Tag
import io.ipoli.android.tag.usecase.CreateTagItemsUseCase
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 01/24/2018.
 */

sealed class DataLoadedAction : Action {

    data class PlayerChanged(val player: Player) : DataLoadedAction()
    data class TodayQuestsChanged(val quests: List<Quest>) : DataLoadedAction()
    data class RepeatingQuestsChanged(val repeatingQuests: List<RepeatingQuest>) :
        DataLoadedAction()

    data class ChallengesChanged(val challenges: List<Challenge>) :
        DataLoadedAction()

    data class AgendaItemsChanged(
        val start: LocalDate,
        val end: LocalDate,
        val agendaItems: List<CreateAgendaItemsUseCase.AgendaItem>,
        val currentAgendaItemDate: LocalDate?
    ) : DataLoadedAction()

    data class CalendarScheduleChanged(val schedule: Map<LocalDate, Schedule>) :
        DataLoadedAction()

    data class RepeatingQuestHistoryChanged(
        val repeatingQuestId: String,
        val history: CreateRepeatingQuestHistoryUseCase.History
    ) : DataLoadedAction()

    data class EventsChanged(val events: List<Event>) : DataLoadedAction()
    data class CalendarsChanged(val calendars: List<Calendar>) : DataLoadedAction()
    data class QuestChanged(val quest: Quest) : DataLoadedAction()
    data class TagsChanged(val tags: List<Tag>) : DataLoadedAction()
    data class TagItemsChanged(val tagId: String, val items: List<CreateTagItemsUseCase.TagItem>) :
        DataLoadedAction()

    data class UnscheduledQuestsChanged(val quests: List<Quest>) : DataLoadedAction()

    data class GemPacksLoaded(val gemPacks: List<GemPack>) : DataLoadedAction()
    data class ReviewDayQuestsChanged(
        val quests: List<Quest>,
        val awesomenessScore: Double
    ) : DataLoadedAction()

    data class WeatherChanged(val weather: Weather?) : DataLoadedAction()

    data class SuggestionsChanged(val quests: List<Quest>) : DataLoadedAction()
    data class MotivationalImageChanged(val motivationalImage: MotivationalImage?) :
        DataLoadedAction()

    data class QuoteChanged(val quote: Quote?) : DataLoadedAction()

    data class GrowthChanged(
        val dailyGrowth: CalculateGrowthStatsUseCase.Growth.Today,
        val weeklyGrowth: CalculateGrowthStatsUseCase.Growth.Week,
        val monthlyGrowth: CalculateGrowthStatsUseCase.Growth.Month,
        val includesAppUsageData: Boolean
    ) : DataLoadedAction()

    data class ProfileDataChanged(
        val unlockedAchievements: List<CreateAchievementItemsUseCase.AchievementItem>,
        val streak: Int,
        val averageProductiveDuration: Duration<Minute>
    ) : DataLoadedAction()

    data class AchievementItemsChanged(val achievementListItems: List<CreateAchievementItemsUseCase.AchievementListItem>) :
        DataLoadedAction()
}

data class AppDataState(
    val player: Player?,
    val todayQuests: List<Quest>,
    val unscheduledQuests: List<Quest>,
    val calendarSchedule: Map<LocalDate, Schedule>,
    val repeatingQuests: List<RepeatingQuest>?,
    val challenges: List<Challenge>?,
    val events: List<Event>,
    val tags: List<Tag>
) : State

object AppDataReducer : Reducer<AppState, AppDataState> {

    override val stateKey: String = AppDataState::class.java.simpleName

    override fun reduce(state: AppState, subState: AppDataState, action: Action) =
        when (action) {

            is DataLoadedAction.PlayerChanged -> {
                subState.copy(
                    player = action.player
                )
            }

            is DataLoadedAction.CalendarScheduleChanged ->
                subState.copy(
                    calendarSchedule = action.schedule
                )

            is DataLoadedAction.TodayQuestsChanged ->
                subState.copy(
                    todayQuests = action.quests
                )

            is DataLoadedAction.RepeatingQuestsChanged ->
                subState.copy(
                    repeatingQuests = action.repeatingQuests
                )

            is DataLoadedAction.ChallengesChanged ->
                subState.copy(
                    challenges = action.challenges
                )

            is DataLoadedAction.EventsChanged ->
                subState.copy(
                    events = action.events
                )

            is DataLoadedAction.TagsChanged ->
                subState.copy(
                    tags = action.tags
                )

            is DataLoadedAction.UnscheduledQuestsChanged ->
                subState.copy(
                    unscheduledQuests = action.quests
                )

            else -> subState
        }

    override fun defaultState() =
        AppDataState(
            player = null,
            todayQuests = listOf(),
            unscheduledQuests = emptyList(),
            calendarSchedule = mapOf(),
            repeatingQuests = null,
            challenges = null,
            events = listOf(),
            tags = listOf()
        )
}
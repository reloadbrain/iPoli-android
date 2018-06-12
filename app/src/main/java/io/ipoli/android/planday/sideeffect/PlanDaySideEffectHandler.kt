package io.ipoli.android.planday.sideeffect

import io.ipoli.android.common.AppSideEffectHandler
import io.ipoli.android.common.AppState
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.async.ChannelRelay
import io.ipoli.android.common.redux.Action
import io.ipoli.android.planday.PlanDayAction
import io.ipoli.android.planday.PlanDayViewState
import io.ipoli.android.planday.usecase.CalculateAwesomenessScoreUseCase
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.usecase.RescheduleQuestUseCase
import org.threeten.bp.LocalDate
import space.traversal.kapsule.required

object PlanDaySideEffectHandler : AppSideEffectHandler() {

    private val questRepository by required { questRepository }
    private val quoteRepository by required { quoteRepository }
    private val motivationalImageRepository by required { motivationalImageRepository }
    private val weatherRepository by required { weatherRepository }
    private val calculateAwesomenessScoreUseCase by required { calculateAwesomenessScoreUseCase }
    private val rescheduleQuestUseCase by required { rescheduleQuestUseCase }

    private val yesterdayQuestsChannelRelay = ChannelRelay<List<Quest>, Unit>(
        producer = { c, _ ->
            questRepository.listenForScheduledAt(LocalDate.now().minusDays(1), c)
        },
        consumer = { qs, _ ->
            dispatch(
                DataLoadedAction.ReviewDayQuestsChanged(
                    quests = qs,
                    awesomenessScore = calculateAwesomenessScoreUseCase.execute(
                        CalculateAwesomenessScoreUseCase.Params.WithQuests(qs)
                    )
                )
            )
        }
    )

    override suspend fun doExecute(action: Action, state: AppState) {
        when (action) {
            is PlanDayAction.Load -> {
                val vs = state.stateFor(PlanDayViewState::class.java)
                yesterdayQuestsChannelRelay.listen(Unit)
                if (vs.suggestedQuests == null) {
                    dispatch(
                        DataLoadedAction.SuggestionsChanged(
                            questRepository.findRandomUnscheduled(
                                3
                            )
                        )
                    )
                }
                if (!vs.quoteLoaded) {
                    dispatch(DataLoadedAction.QuoteChanged(quoteRepository.findRandomQuote()))
                }
                if (!vs.imageLoaded) {
                    dispatch(DataLoadedAction.MotivationalImageChanged(motivationalImageRepository.findRandomImage()))
                }
            }

            is PlanDayAction.GetWeather ->
                try {
                    dispatch(DataLoadedAction.WeatherChanged(weatherRepository.getCurrentWeather()))
                } catch (e: Throwable) {
                    dispatch(DataLoadedAction.WeatherChanged(null))
                }

            is PlanDayAction.Done -> {
                val yesterday = LocalDate.now().minusDays(1)
                val yesterdayQuests = questRepository.findScheduledAt(yesterday)
                yesterdayQuests.forEach {
                    rescheduleQuestUseCase.execute(RescheduleQuestUseCase.Params(it.id, null))
                }
            }
        }
    }

    override fun canHandle(action: Action) = action is PlanDayAction

}
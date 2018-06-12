package io.ipoli.android.planday.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.common.datetime.Duration
import io.ipoli.android.common.datetime.Minute
import io.ipoli.android.common.datetime.minutes
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.data.persistence.QuestRepository
import org.threeten.bp.LocalDate

open class CalculateFocusDurationUseCase(private val questRepository: QuestRepository) :
    UseCase<CalculateFocusDurationUseCase.Params, Duration<Minute>> {

    override fun execute(parameters: Params): Duration<Minute> {
        val quests = when (parameters) {
            is Params.WithQuests -> parameters.quests
            is Params.WithoutQuests -> questRepository.findScheduledAt(
                parameters.currentDate
            )
        }

        return quests.sumBy {
            if (it.hasTimer) {
                it.actualDuration.asMinutes.intValue
            } else {
                0
            }
        }.minutes
    }

    sealed class Params {
        data class WithQuests(val quests: List<Quest>) : Params()
        data class WithoutQuests(val currentDate: LocalDate = LocalDate.now()) : Params()
    }
}
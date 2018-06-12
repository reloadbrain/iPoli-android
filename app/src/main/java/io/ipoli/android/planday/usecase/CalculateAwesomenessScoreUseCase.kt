package io.ipoli.android.planday.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.data.persistence.QuestRepository
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 05/16/2018.
 */
open class CalculateAwesomenessScoreUseCase(private val questRepository: QuestRepository) :
    UseCase<CalculateAwesomenessScoreUseCase.Params, Double> {

    override fun execute(parameters: Params): Double {
        val qs = when (parameters) {
            is Params.WithQuests -> parameters.quests
            is Params.WithoutQuests -> questRepository.findScheduledAt(parameters.currentDate)
        }
        val completeCount = qs.count { it.isCompleted }
        if (qs.size <= 5) {
            return calculateSimpleAwesomenessScore(completeCount)
        }

        val percentComplete = completeCount / qs.size.toFloat()
        return when {
            percentComplete >= .85 -> 5.0
            percentComplete >= .7 -> 4.0
            percentComplete >= .5 -> 3.0
            percentComplete >= .35 -> 2.0
            else -> 0.0
        }
    }

    private fun calculateSimpleAwesomenessScore(completeCount: Int) =
        when (completeCount) {
            5 -> 5.0
            4 -> 4.0
            3 -> 3.0
            2 -> 2.0
            else -> 0.0
        }

    sealed class Params {
        data class WithQuests(val quests: List<Quest>) : Params()
        data class WithoutQuests(val currentDate: LocalDate = LocalDate.now()) : Params()
    }
}
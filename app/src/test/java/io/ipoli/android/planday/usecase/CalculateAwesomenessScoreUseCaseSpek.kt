package io.ipoli.android.planday.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.common.datetime.Time
import org.amshove.kluent.`should be in range`
import org.amshove.kluent.mock
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 05/16/2018.
 */
class CalculateAwesomenessScoreUseCaseSpek : Spek({

    describe("CalculateAwesomenessScoreUseCase") {

        fun executeUseCase(
            completeCount: Int,
            totalCount: Int = completeCount
        ): Double {

            val cq = TestUtil.quest.copy(
                completedAtTime = Time.now(),
                completedAtDate = LocalDate.now()
            )

            val iq = TestUtil.quest

            val incompleteCount = totalCount - completeCount
            return CalculateAwesomenessScoreUseCase(mock()).execute(
                CalculateAwesomenessScoreUseCase.Params.WithQuests(
                    quests = 0.until(completeCount).map { cq } +
                        0.until(incompleteCount).map { iq }
                )
            )
        }

        it("should give 0 when no quests are completed") {
            executeUseCase(completeCount = 0).`should be in range`(-0.001, 0.001)
        }

        it("should give 0 when 1 quest is completed and 1 is added") {
            executeUseCase(completeCount = 1).`should be in range`(-0.001, 0.001)
        }

        it("should give 2 when 2 quests are completed and 2 are added") {
            executeUseCase(completeCount = 2).`should be in range`(1.999, 2.001)
        }

        it("should give 3 when 3 quests are completed and 3 are added") {
            executeUseCase(completeCount = 3).`should be in range`(2.999, 3.001)
        }

        it("should give 4 when 4 quests are completed and 4 are added") {
            executeUseCase(completeCount = 4).`should be in range`(3.999, 4.001)
        }

        it("should give 5 when 5 quests are completed and 5 are added") {
            executeUseCase(completeCount = 5).`should be in range`(4.999, 5.001)
        }

        it("should give 4 when 5 quests are completed and 6 are added") {
            executeUseCase(completeCount = 5, totalCount = 6).`should be in range`(3.999, 4.001)
        }

        it("should give 5 when 6 quests are completed and 6 are added") {
            executeUseCase(completeCount = 6, totalCount = 6).`should be in range`(4.999, 5.001)
        }

        it("should give 0 when 0 quests are completed and 15 are added") {
            executeUseCase(completeCount = 0, totalCount = 15).`should be in range`(-0.001, 0.001)
        }
    }
})
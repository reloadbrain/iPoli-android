package mypoli.android.repeatingquest

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import mypoli.android.common.datetime.toStartOfDay
import mypoli.android.common.di.Module
import mypoli.android.myPoliApp
import mypoli.android.repeatingquest.usecase.SaveQuestsForRepeatingQuestUseCase
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.Kapsule

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/03/2018.
 */

class SaveQuestsForRepeatingQuestJob : Job(), Injects<Module> {

    override fun onRunJob(params: Params): Result {
        val kap = Kapsule<Module>()
        val saveQuestsForRepeatingQuestScheduler by kap.required { saveQuestsForRepeatingQuestScheduler }
        val repeatingQuestRepository by kap.required { repeatingQuestRepository }
        val saveQuestsForRepeatingQuestUseCase by kap.required { saveQuestsForRepeatingQuestUseCase }
        kap.inject(myPoliApp.module(context))

        val rqs = repeatingQuestRepository.findAllActive()
        rqs.forEach {
            val currentPeriod = it.repeatingPattern.periodRangeFor(LocalDate.now())
            val nextPeriodFirstDate = currentPeriod.end.plusDays(1)
            val end = it.repeatingPattern.periodRangeFor(nextPeriodFirstDate).end
            saveQuestsForRepeatingQuestUseCase.execute(
                SaveQuestsForRepeatingQuestUseCase.Params(
                    repeatingQuest = it,
                    start = LocalDate.now(),
                    end = end
                )
            )
        }

        saveQuestsForRepeatingQuestScheduler.schedule()
        return Result.SUCCESS
    }

    companion object {
        const val TAG = "save_quests_for_repeating_quest_tag"
    }
}

class AndroidSaveQuestsForRepeatingQuestScheduler : SaveQuestsForRepeatingQuestScheduler {
    override fun schedule() {
        val startOfTomorrow = LocalDate.now().plusDays(1).toStartOfDay().time

        JobRequest.Builder(SaveQuestsForRepeatingQuestJob.TAG)
            .setUpdateCurrent(true)
            .setExact(startOfTomorrow - System.currentTimeMillis())
            .build()
            .schedule()
    }

}

interface SaveQuestsForRepeatingQuestScheduler {
    fun schedule()
}
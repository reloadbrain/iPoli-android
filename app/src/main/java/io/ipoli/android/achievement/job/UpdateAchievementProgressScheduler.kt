package io.ipoli.android.achievement.job

import io.ipoli.android.achievement.usecase.UpdateAchievementProgressUseCase
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.di.Module
import io.ipoli.android.common.job.FixedDailyJob
import io.ipoli.android.common.job.FixedDailyJobScheduler
import io.ipoli.android.myPoliApp
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import space.traversal.kapsule.Kapsule

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 06/08/2018.
 */

class UpdateAchievementProgressJob : FixedDailyJob(UpdateAchievementProgressJob.TAG) {

    override fun doRunJob(params: Params): Result {
        val kap = Kapsule<Module>()
        val updateAchievementProgressUseCase by kap.required { updateAchievementProgressUseCase }
        kap.inject(myPoliApp.module(context))
        updateAchievementProgressUseCase.execute(UpdateAchievementProgressUseCase.Params())
        return Result.SUCCESS
    }

    companion object {
        const val TAG = "job_update_achievement_progress"
    }
}

interface UpdateAchievementProgressScheduler {
    fun schedule()
}

class AndroidUpdateAchievementProgressScheduler : UpdateAchievementProgressScheduler {

    override fun schedule() {
        launch(CommonPool) {
            FixedDailyJobScheduler.schedule(UpdateAchievementProgressJob.TAG, Time.at(0, 1))
        }
    }

}
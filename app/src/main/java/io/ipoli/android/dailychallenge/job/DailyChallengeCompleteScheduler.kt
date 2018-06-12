package io.ipoli.android.dailychallenge.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import io.ipoli.android.achievement.usecase.UnlockAchievementsUseCase
import io.ipoli.android.common.SimpleReward
import io.ipoli.android.common.datetime.seconds
import io.ipoli.android.common.di.Module
import io.ipoli.android.common.view.asThemedWrapper
import io.ipoli.android.dailychallenge.DailyChallengeCompletePopup
import io.ipoli.android.myPoliApp
import io.ipoli.android.quest.Quest
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import space.traversal.kapsule.Kapsule

class DailyChallengeCompleteJob : Job() {

    override fun onRunJob(params: Params): Result {

        val kap = Kapsule<Module>()
        val rewardPlayerUseCase by kap.required { rewardPlayerUseCase }
        val unlockAchievement by kap.required { unlockAchievementsUseCase }
        val playerRepository by kap.required { playerRepository }
        kap.inject(myPoliApp.module(context))

        val coins = params.extras.getInt(KEY_COINS, -1)
        val experience = params.extras.getInt(KEY_EXPERIENCE, -1)

        require(coins > 0, { "DailyChallengeCompleteJob has incorrect coins param $coins" })
        require(
            experience > 0,
            { "DailyChallengeCompleteJob has incorrect experience param $experience" })

        val c = context.asThemedWrapper()
        launch(UI) {
            val popup = DailyChallengeCompletePopup(experience, coins)
            popup.hideListener = {
                launch(CommonPool) {
                    rewardPlayerUseCase.execute(SimpleReward(experience, coins, Quest.Bounty.None))
                }
            }
            popup.show(c)
        }

        unlockAchievement.execute(
            UnlockAchievementsUseCase.Params(
                playerRepository.find()!!,
                UnlockAchievementsUseCase.Params.EventType.DailyChallengeCompleted
            )
        )

        return Result.SUCCESS
    }

    companion object {
        const val TAG = "job_daily_challenge_complete_tag"

        const val KEY_EXPERIENCE = "EXPERIENCE"
        const val KEY_COINS = "COINS"
    }

}

interface DailyChallengeCompleteScheduler {
    fun schedule(experience: Int, coins: Int)
}

class AndroidDailyChallengeCompleteScheduler : DailyChallengeCompleteScheduler {

    override fun schedule(experience: Int, coins: Int) {

        val params = PersistableBundleCompat()
        params.putInt(DailyChallengeCompleteJob.KEY_EXPERIENCE, experience)
        params.putInt(DailyChallengeCompleteJob.KEY_COINS, coins)

        JobRequest.Builder(DailyChallengeCompleteJob.TAG)
            .setExtras(params)
            .setUpdateCurrent(true)
            .setExact(3.seconds.millisValue)
            .build()
            .schedule()
    }

}
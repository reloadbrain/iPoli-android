package io.ipoli.android.store.powerup.job

import com.evernote.android.job.DailyJob
import com.evernote.android.job.JobRequest
import io.ipoli.android.common.di.Module
import io.ipoli.android.myPoliApp
import io.ipoli.android.player.data.Membership
import io.ipoli.android.store.powerup.usecase.RemoveExpiredPowerUpsUseCase
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.Kapsule
import java.util.concurrent.TimeUnit

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/20/2018.
 */
class RemoveExpiredPowerUpsJob : DailyJob(), Injects<Module> {

    override fun onRunDailyJob(params: Params): DailyJobResult {
        val kap = Kapsule<Module>()
        val removeExpiredPowerUpsUseCase by kap.required { removeExpiredPowerUpsUseCase }
        val playerRepository by kap.required { playerRepository }
        kap.inject(myPoliApp.module(context))

        val p = playerRepository.find()
        requireNotNull(p)

        if (p!!.membership != Membership.NONE) {
            return DailyJobResult.SUCCESS
        }

        removeExpiredPowerUpsUseCase.execute(RemoveExpiredPowerUpsUseCase.Params(LocalDate.now()))

        return DailyJobResult.SUCCESS
    }

    companion object {
        const val TAG = "remove_expired_power_ups_tag"
    }
}

class AndroidRemoveExpiredPowerUpsScheduler : RemoveExpiredPowerUpsScheduler {
    override fun schedule() {
        DailyJob
            .schedule(
                JobRequest.Builder(RemoveExpiredPowerUpsJob.TAG).setUpdateCurrent(true),
                0,
                TimeUnit.HOURS.toMillis(8)
            )
    }
}

interface RemoveExpiredPowerUpsScheduler {
    fun schedule()
}
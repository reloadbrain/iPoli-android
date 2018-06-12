package io.ipoli.android.planday.job

import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.common.IntentUtil
import io.ipoli.android.common.datetime.Duration
import io.ipoli.android.common.datetime.Minute
import io.ipoli.android.common.datetime.minutes
import io.ipoli.android.common.datetime.startOfDayUTC
import io.ipoli.android.common.di.Module
import io.ipoli.android.common.job.FixedDailyJob
import io.ipoli.android.common.job.FixedDailyJobScheduler
import io.ipoli.android.common.notification.NotificationUtil
import io.ipoli.android.common.notification.ScreenUtil
import io.ipoli.android.common.view.asThemedWrapper
import io.ipoli.android.dailychallenge.data.DailyChallenge
import io.ipoli.android.myPoliApp
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.pet.Pet
import io.ipoli.android.quest.reminder.PetNotificationPopup
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Kapsule
import java.util.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 05/18/2018.
 */

object PlanDayNotification {

    fun show(
        context: Context,
        pet: Pet,
        planDayScheduler: PlanDayScheduler
    ) {
        val vm = PetNotificationPopup.ViewModel(
            headline = "Time to plan your day",
            title = null,
            body = null,
            petAvatar = pet.avatar,
            petState = pet.state
        )
        val c = context.asThemedWrapper()

        val icon = BitmapFactory.decodeResource(
            c.resources,
            AndroidPetAvatar.valueOf(pet.avatar.name).headImage
        )

        val sound =
            Uri.parse("android.resource://" + c.packageName + "/" + R.raw.notification)

        val notification = NotificationUtil.createDefaultNotification(
            context = c,
            icon = icon,
            title = "Time to plan your day",
            message = "Amazing new day ahead!",
            sound = sound,
            channelId = Constants.PLAN_DAY_NOTIFICATION_CHANNEL_ID,
            contentIntent = IntentUtil.getActivityPendingIntent(c, IntentUtil.startPlanDay(c))
        )

        val notificationId = Random().nextInt()

        val notificationManager =
            c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(notificationId, notification)

        ScreenUtil.awakeScreen(c)

        PetNotificationPopup(vm,
            onDismiss = {
                notificationManager.cancel(notificationId)
            },
            onSnooze = {
                notificationManager.cancel(notificationId)
                launch(CommonPool) {
                    planDayScheduler.scheduleAfter(15.minutes)
                }
                Toast
                    .makeText(c, c.getString(R.string.remind_in_15), Toast.LENGTH_SHORT)
                    .show()
            },
            onStart = {
                notificationManager.cancel(notificationId)
                c.startActivity(IntentUtil.startPlanDay(c))
            }).show(c)
    }

}

class SnoozedPlanDayJob : Job() {

    override fun onRunJob(params: Params): Result {

        val kap = Kapsule<Module>()
        val playerRepository by kap.required { playerRepository }
        val planDayScheduler by kap.required { planDayScheduler }
        kap.inject(myPoliApp.module(context))

        val p = playerRepository.find()
        requireNotNull(p)

        val pet = p!!.pet
        launch(UI) {
            PlanDayNotification.show(getContext(), pet, planDayScheduler)
        }

        return Result.SUCCESS
    }

    companion object {
        const val TAG = "job_snoozed_plan_day_tag"
    }
}

class PlanDayJob : FixedDailyJob(PlanDayJob.TAG) {

    override fun doRunJob(params: Params): Result {
        val kap = Kapsule<Module>()
        val playerRepository by kap.required { playerRepository }
        val planDayScheduler by kap.required { planDayScheduler }
        val dailyChallengeRepository by kap.required { dailyChallengeRepository }
        kap.inject(myPoliApp.module(context))

        val p = playerRepository.find()
        requireNotNull(p)

        if (!p!!.preferences.planDays.contains(LocalDate.now().dayOfWeek)) {
            return Job.Result.SUCCESS
        }

        dailyChallengeRepository.findById(LocalDate.now().startOfDayUTC().toString())
            ?: dailyChallengeRepository.save(DailyChallenge())

        val pet = p.pet
        launch(UI) {
            PlanDayNotification.show(getContext(), pet, planDayScheduler)
        }

        return Job.Result.SUCCESS
    }

    companion object {
        const val TAG = "job_plan_day_tag"
    }
}

interface PlanDayScheduler {
    fun scheduleAfter(minutes: Duration<Minute>)
    fun schedule()
}

class AndroidPlanDayScheduler(private val context: Context) : PlanDayScheduler {

    override fun scheduleAfter(minutes: Duration<Minute>) {
        JobRequest.Builder(SnoozedPlanDayJob.TAG)
            .setUpdateCurrent(true)
            .setExact(minutes.millisValue)
            .build()
            .schedule()
    }

    override fun schedule() {
        launch(CommonPool) {

            val kap = Kapsule<Module>()
            val playerRepository by kap.required { playerRepository }
            kap.inject(myPoliApp.module(this@AndroidPlanDayScheduler.context))

            val p = playerRepository.find()

            requireNotNull(p)

            val pTime = p!!.preferences.planDayTime

            FixedDailyJobScheduler.schedule(PlanDayJob.TAG, pTime)
        }
    }
}
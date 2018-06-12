package io.ipoli.android.common.notification

import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.common.IntentUtil
import io.ipoli.android.common.text.DurationFormatter
import io.ipoli.android.common.view.AndroidColor
import io.ipoli.android.common.view.AndroidIcon
import io.ipoli.android.common.view.largeIcon
import io.ipoli.android.player.data.Player
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.receiver.CompleteQuestReceiver

object QuickDoNotificationUtil {

    fun update(context: Context, quests: List<Quest>, player: Player) {
        if (!player.preferences.isQuickDoNotificationEnabled) {
            return
        }

        val nextQuest = quests.firstOrNull { !it.isCompleted }

        val totalCount = quests.size
        val completedCount = quests.count { it.isCompleted }

        val title = when {
            nextQuest != null -> nextQuest.name
            totalCount == 0 -> context.getString(R.string.ongoing_notification_no_quests_title)
            else -> context.getString(R.string.ongoing_notification_done_title)
        }

        val text =
            if (totalCount == 0)
                context.getString(R.string.ongoing_notification_no_quests_text)
            else
                context.getString(
                    R.string.ongoing_notification_progress_text,
                    completedCount,
                    totalCount
                )

        val showWhen = nextQuest != null && nextQuest.isScheduled

        val whenMillis = (if (showWhen) nextQuest!!.startMillisecond!! else 0).toLong()
        val contentInfo = if (nextQuest == null) "" else String.format(
            context.getString(R.string.notification_for_time),
            DurationFormatter.format(context, nextQuest.duration)
        )

        val iconColor =
            when {
                quests.isEmpty() -> R.color.md_blue_500
                nextQuest == null -> R.color.md_green_500
                else -> AndroidColor.valueOf(nextQuest.color.name).color500
            }

        val iconicsDrawable = IconicsDrawable(context)

        val largeIcon = when {
            quests.isEmpty() -> iconicsDrawable.largeIcon(
                GoogleMaterial.Icon.gmd_wb_sunny,
                R.color.md_amber_500
            )
            nextQuest == null -> iconicsDrawable.largeIcon(
                GoogleMaterial.Icon.gmd_done_all,
                R.color.md_green_500
            )
            else -> nextQuest.icon?.let {
                val androidIcon = AndroidIcon.valueOf(it.name)
                iconicsDrawable.largeIcon(
                    androidIcon.icon,
                    androidIcon.color
                )
            } ?: iconicsDrawable.largeIcon(
                GoogleMaterial.Icon.gmd_notifications_active,
                AndroidColor.valueOf(nextQuest.color.name).color500
            )
        }

        val builder =
            NotificationCompat.Builder(context, Constants.PLAN_DAY_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setShowWhen(showWhen)
                .setWhen(whenMillis)
                .setContentInfo(contentInfo)
                .setContentIntent(
                    IntentUtil.getActivityPendingIntent(
                        context,
                        IntentUtil.startApp(context)
                    )
                )
                .setLargeIcon(largeIcon.toBitmap())
                .setSmallIcon(R.drawable.ic_notification_small)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(
                    R.drawable.ic_add_white_24dp,
                    context.getString(R.string.add),
                    IntentUtil.getActivityPendingIntent(
                        context,
                        IntentUtil.showQuickAdd(context)
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setColor(ContextCompat.getColor(context, iconColor))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (nextQuest != null) {
            if (nextQuest.isStarted) {
//                builder.addAction(
//                    R.drawable.ic_stop_black_24dp,
//                    context.getString(R.string.stop).toUpperCase(),
//                    getStopPendingIntent(quest.getId())
//                )
            } else {
                builder.addAction(
                    R.drawable.ic_play_arrow_black_24dp,
                    context.getString(R.string.start).toUpperCase(),
                    IntentUtil.getActivityPendingIntent(
                        context,
                        IntentUtil.showTimer(
                            nextQuest.id,
                            context
                        )
                    )
                )
            }

            val intent = Intent(context, CompleteQuestReceiver::class.java)
            intent.putExtra(Constants.QUEST_ID_EXTRA_KEY, nextQuest.id)

            builder.addAction(
                R.drawable.ic_done_24dp,
                context.getString(R.string.done).toUpperCase(),
                IntentUtil.getBroadcastPendingIntent(context, intent)
            )
        }

        NotificationManagerCompat.from(context)
            .notify(Constants.ONGOING_NOTIFICATION_ID, builder.build())
    }

    fun disable(context: Context) {
        NotificationManagerCompat.from(context).cancel(Constants.ONGOING_NOTIFICATION_ID)
    }
}
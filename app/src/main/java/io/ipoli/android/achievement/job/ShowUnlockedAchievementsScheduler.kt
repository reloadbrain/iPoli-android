package io.ipoli.android.achievement.job

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import io.ipoli.android.R
import io.ipoli.android.achievement.Achievement
import io.ipoli.android.achievement.AndroidAchievement
import io.ipoli.android.achievement.view.AchievementData
import io.ipoli.android.achievement.view.AchievementUnlocked
import io.ipoli.android.common.datetime.seconds
import io.ipoli.android.common.view.asThemedWrapper
import io.ipoli.android.common.view.attrData
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 06/09/2018.
 */

interface ShowUnlockedAchievementsScheduler {
    fun schedule(achievements: List<Achievement>)
}

class AndroidShowUnlockedAchievementsScheduler(private val context: Context) :
    ShowUnlockedAchievementsScheduler {

    override fun schedule(achievements: List<Achievement>) {

        val c = context.asThemedWrapper()

        val androidAchievements = achievements
            .map { AndroidAchievement.valueOf(it.name) }.map {
                AchievementData(
                    title = c.getString(R.string.achievement_unlocked),
                    subtitle = c.getString(it.title),
                    textColor = Color.WHITE,
                    backgroundColor = c.attrData(R.attr.colorAccent),
                    icon = ContextCompat.getDrawable(c, it.icon),
                    iconBackgroundColor = ContextCompat.getColor(c, it.color)
                )
            }

        launch(UI) {
            AchievementUnlocked(c)
                .setRounded(true)
                .setLarge(true)
                .setTopAligned(true)
                .setReadingDelay(3.seconds.millisValue.toInt())
                .show(androidAchievements)
        }
    }

}
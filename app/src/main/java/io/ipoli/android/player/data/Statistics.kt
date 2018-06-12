package io.ipoli.android.player.data

import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/7/18.
 */
data class Statistics(
    val questCompletedCount: Long = 0,
    val questCompletedCountForToday: Long = 0,
    val questCompletedStreak: StreakStatistic = StreakStatistic(),
    val dailyChallengeCompleteStreak: StreakStatistic = StreakStatistic(),
    val petHappyStateStreak: Long = 0,
    val awesomenessScoreStreak: Long = 0,
    val planDayStreak: StreakStatistic = StreakStatistic(),
    val focusHoursStreak: Long = 0,
    val repeatingQuestCreatedCount: Long = 0,
    val challengeCompletedCount: Long = 0,
    val challengeCreatedCount: Long = 0,
    val gemConvertedCount: Long = 0,
    val friendInvitedCount: Long = 0,
    val experienceForToday: Long = 0,
    val petItemEquippedCount: Long = 0,
    val avatarChangeCount: Long = 0,
    val petChangeCount: Long = 0,
    val petFedWithPoopCount: Long = 0,
    val petFedCount: Long = 0,
    val feedbackSentCount: Long = 0,
    val joinMembershipCount: Long = 0,
    val powerUpActivatedCount: Long = 0,
    val petRevivedCount: Long = 0,
    val petDiedCount: Long = 0
) {
    data class StreakStatistic(val count: Long = 0, val lastDate: LocalDate? = null)
}
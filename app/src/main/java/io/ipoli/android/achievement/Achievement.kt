package io.ipoli.android.achievement

import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import io.ipoli.android.R
import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase

enum class Achievement {
    FIRST_QUEST_COMPLETED,
    COMPLETE_10_QUESTS_IN_A_DAY,

    KEEP_PET_HAPPY_5_DAY_STREAK,
    KEEP_PET_HAPPY_15_DAY_STREAK,
    KEEP_PET_HAPPY_40_DAY_STREAK,

    // >= 4
    AWESOMENESS_SCORE_5_DAY_STREAK,
    AWESOMENESS_SCORE_20_DAY_STREAK,
    AWESOMENESS_SCORE_50_DAY_STREAK,

    // only on plan days
    FOCUS_HOURS_5_DAY_STREAK,
    FOCUS_HOURS_20_DAY_STREAK,
    FOCUS_HOURS_50_DAY_STREAK,

    PLAN_DAY_5_DAY_STREAK,
    PLAN_DAY_20_DAY_STREAK,
    PLAN_DAY_50_DAY_STREAK,

    CONVERT_1_GEM,
    CONVERT_20_GEMS,
    CONVERT_50_GEMS,

    REACH_LEVEL_10,
    REACH_LEVEL_30,
    REACH_LEVEL_50,

    HAVE_1K_LIFE_COINS_IN_INVENTORY,
    HAVE_5K_LIFE_COINS_IN_INVENTORY,
    HAVE_10K_LIFE_COINS_IN_INVENTORY,

    INVITE_1_FRIEND,
    INVITE_5_FRIENDS,
    INVITE_20_FRIENDS,

    GAIN_999_XP_IN_A_DAY,
    FIRST_PET_ITEM_EQUIPPED,

    FIRST_REPEATING_QUEST_CREATED,

    FIRST_CHALLENGE_CREATED,

    COMPLETE_DAILY_CHALLENGE,
    COMPLETE_DAILY_CHALLENGE_10_DAY_STREAK,
    COMPLETE_DAILY_CHALLENGE_30_DAY_STREAK,

    COMPLETE_CHALLENGE,
    COMPLETE_5_CHALLENGES,
    COMPLETE_15_CHALLENGES,

    PET_FED_WITH_POOP,
    PET_FED,
    BECAME_PRO,
    PET_REVIVED,

    FIRST_AVATAR_CHANGED,
    FIRST_POWER_UP_ACTIVATED,
    FIRST_PET_CHANGED,
    PET_DIED,
    COMPLETE_QUEST_FOR_100_DAYS_IN_A_ROW,
    FEEDBACK_SENT;
}

enum class AndroidAchievement(
    @StringRes val title: Int, val levelDescriptions: Map<Int, Int>, @ColorRes val color: Int, @DrawableRes val icon: Int,
    val isHidden: Boolean = false
) {
    FIRST_QUEST_COMPLETED(
        R.string.achievement_first_quest_completed_name,
        mapOf(1 to R.string.achievement_first_quest_completed_desc),
        R.color.achievement_blue,
        R.drawable.achievement_first_quest_completed
    ),
    COMPLETE_10_QUESTS_IN_A_DAY(
        R.string.achievement_complete_10_quests_in_a_day_name,
        mapOf(1 to R.string.achievement_complete_10_quests_in_a_day_desc),
        R.color.achievement_green,
        R.drawable.ic_armor
    ),
    KEEP_PET_HAPPY(
        R.string.achievement_keep_pet_happy_name,
        mapOf(
            1 to R.string.achievement_keep_pet_happy_name_level_1_desc,
            2 to R.string.achievement_keep_pet_happy_name_level_2_desc,
            3 to R.string.achievement_keep_pet_happy_name_level_3_desc
        ),
        R.color.achievement_purple,
        R.drawable.achievement_keep_pet_happy
    ),
    AWESOMENESS_STREAK(
        R.string.achievement_awesomeness_streak_name,
        mapOf(
            1 to R.string.achievement_awesomeness_streak_level_1_desc,
            2 to R.string.achievement_awesomeness_streak_level_2_desc,
            3 to R.string.achievement_awesomeness_streak_level_3_desc
        ),
        R.color.achievement_orange,
        R.drawable.achievement_awesomeness_streak
    ),
    FOCUS_HOURS_STREAK(
        R.string.achievement_focus_hours_streak_name,
        mapOf(
            1 to R.string.achievement_focus_hours_streak_level_1_desc,
            2 to R.string.achievement_focus_hours_streak_level_2_desc,
            3 to R.string.achievement_focus_hours_streak_level_3_desc
        ),
        R.color.achievement_blue,
        R.drawable.achievement_focus_hour_streak
    ),
    PLAN_DAY_STREAK(
        R.string.achievement_plan_day_streak_name,
        mapOf(
            1 to R.string.achievement_plan_day_streak_level_1_desc,
            2 to R.string.achievement_plan_day_streak_level_2_desc,
            3 to R.string.achievement_plan_day_streak_level_3_desc
        ),
        R.color.achievement_green,
        R.drawable.achievement_plan_day_streak
    ),
    GEMS_CONVERTED(
        R.string.achievement_gems_converted_name,
        mapOf(
            1 to R.string.achievement_gems_converted_level_1_desc,
            2 to R.string.achievement_gems_converted_level_2_desc,
            3 to R.string.achievement_gems_converted_level_3_desc
        ),
        R.color.achievement_purple,
        R.drawable.achievement_gems_converted
    ),
    LEVEL_UP(
        R.string.achievement_level_up_name,
        mapOf(
            1 to R.string.achievement_level_up_level_1_desc,
            2 to R.string.achievement_level_up_level_2_desc,
            3 to R.string.achievement_level_up_level_3_desc
        ),
        R.color.achievement_purple,
        R.drawable.achievement_level_up
    ),
    COINS_IN_INVENTORY(
        R.string.achievement_coins_in_inventory_name,
        mapOf(
            1 to R.string.achievement_coins_in_inventory_level_1_desc,
            2 to R.string.achievement_coins_in_inventory_level_2_desc,
            3 to R.string.achievement_coins_in_inventory_level_3_desc
        ),
        R.color.achievement_brown,
        R.drawable.achievement_coins_in_inventory
    ),
    INVITE_FRIEND(
        R.string.achievement_invite_friend_name,
        mapOf(
            1 to R.string.achievement_invite_friend_level_1_desc,
            2 to R.string.achievement_invite_friend_level_2_desc,
            3 to R.string.achievement_invite_friend_level_3_desc
        ),
        R.color.achievement_orange,
        R.drawable.achievement_invite_friend
    ),
    GAIN_999_XP_IN_A_DAY(
        R.string.achievement_gain_xp_in_a_day_name,
        mapOf(1 to R.string.achievement_gain_xp_in_a_day_desc),
        R.color.achievement_red,
        R.drawable.achievement_gain_999_xp_in_a_day
    ),
    FIRST_PET_ITEM_EQUIPPED(
        R.string.achievement_first_pet_item_equipped_name,
        mapOf(1 to R.string.achievement_first_pet_item_equipped_desc),
        R.color.achievement_green,
        R.drawable.achievement_first_pet_item_equipped
    ),
    FIRST_REPEATING_QUEST_CREATED(
        R.string.achievement_first_repeating_quest_created_name,
        mapOf(1 to R.string.achievement_first_repeating_quest_created_desc),
        R.color.achievement_orange,
        R.drawable.achievement_first_repeating_quest_created
    ),
    FIRST_CHALLENGE_CREATED(
        R.string.achievement_first_challenge_created_name,
        mapOf(1 to R.string.achievement_first_challenge_created_desc),
        R.color.achievement_green,
        R.drawable.achievement_challenge_created
    ),
    COMPLETE_DAILY_CHALLENGE_STREAK(
        R.string.achievement_complete_daily_challenge_name,
        mapOf(
            1 to R.string.achievement_complete_daily_challenge_level_1_desc,
            2 to R.string.achievement_complete_daily_challenge_level_2_desc,
            3 to R.string.achievement_complete_daily_challenge_level_3_desc
        ),
        R.color.achievement_red,
        R.drawable.achievement_daily_challenge_completed
    ),
    COMPLETE_CHALLENGE(
        R.string.achievement_challenge_completed_name,
        mapOf(
            1 to R.string.achievement_challenge_completed_level_1_desc,
            2 to R.string.achievement_challenge_completed_level_2_desc,
            3 to R.string.achievement_challenge_completed_level_3_desc
        ),
        R.color.achievement_green,
        R.drawable.achievement_challenge_completed
    ),
    PET_FED_WITH_POOP(
        R.string.achievement_pet_fed_with_poop_name,
        mapOf(
            1 to R.string.achievement_pet_fed_with_poop_desc
        ),
        R.color.achievement_orange,
        R.drawable.achievement_feed_pet_with_poop,
        true
    ),
    PET_FED(
        R.string.achievement_pet_fed_name,
        mapOf(
            1 to R.string.achievement_pet_fed_desc
        ),
        R.color.achievement_purple,
        R.drawable.achievement_pet_fed
    ),
    BECAME_PRO(
        R.string.achievement_became_pro_name,
        mapOf(
            1 to R.string.achievement_became_pro_desc
        ),
        R.color.achievement_pink,
        R.drawable.achievement_became_pro
    ),
    PET_REVIVED(
        R.string.achievement_pet_revived_name,
        mapOf(
            1 to R.string.achievement_pet_revived_desc
        ),
        R.color.achievement_brown,
        R.drawable.achievement_pet_revived
    ),
    FIRST_AVATAR_CHANGED(
        R.string.achievement_first_avatar_changed_name,
        mapOf(
            1 to R.string.achievement_first_avatar_changed_desc
        ),
        R.color.achievement_blue,
        R.drawable.achievement_first_avatar_changed,
        true
    ),
    FIRST_POWER_UP_ACTIVATED(
        R.string.achievement_first_power_up_name,
        mapOf(
            1 to R.string.achievement_first_power_up_desc
        ),
        R.color.md_blue_500,
        R.drawable.achievement_first_power_up_activated,
        true
    ),
    FIRST_PET_CHANGED(
        R.string.achievement_change_pet_name,
        mapOf(
            1 to R.string.achievement_change_pet_desc
        ),
        R.color.achievement_pink,
        R.drawable.achievement_first_pet_changed
    ),
    PET_DIED(
        R.string.achievement_first_pet_died_name,
        mapOf(
            1 to R.string.achievement_first_pet_died_desc
        ),
        R.color.achievement_black,
        R.drawable.achievement_pet_died
    ),
    COMPLETE_QUEST_FOR_100_DAYS_IN_A_ROW(
        R.string.achievement_complete_quests_in_row_name,
        mapOf(
            1 to R.string.achievement_complete_quests_in_row_desc
        ),
        R.color.achievement_blue,
        R.drawable.achievement_complete_quest_for_100_days_in_a_row
    ),
    FEEDBACK_SENT(
        R.string.achievement_send_feedback_name,
        mapOf(
            1 to R.string.achievement_send_feedback_desc
        ),
        R.color.achievement_red,
        R.drawable.achievement_feedback_sent
    )
}

val CreateAchievementItemsUseCase.AchievementItem.androidAchievement
    get() =
        when (this) {
            is CreateAchievementItemsUseCase.AchievementItem.QuestCompleted ->
                AndroidAchievement.FIRST_QUEST_COMPLETED

            is CreateAchievementItemsUseCase.AchievementItem.CompletedQuestsInADay ->
                AndroidAchievement.COMPLETE_10_QUESTS_IN_A_DAY

            is CreateAchievementItemsUseCase.AchievementItem.KeepPetHappyStreak ->
                AndroidAchievement.KEEP_PET_HAPPY

            is CreateAchievementItemsUseCase.AchievementItem.AwesomenessScoreStreak ->
                AndroidAchievement.AWESOMENESS_STREAK

            is CreateAchievementItemsUseCase.AchievementItem.FocusHoursStreak ->
                AndroidAchievement.FOCUS_HOURS_STREAK

            is CreateAchievementItemsUseCase.AchievementItem.PlanDayStreak ->
                AndroidAchievement.PLAN_DAY_STREAK

            is CreateAchievementItemsUseCase.AchievementItem.GemsConverted ->
                AndroidAchievement.GEMS_CONVERTED

            is CreateAchievementItemsUseCase.AchievementItem.LevelUp ->
                AndroidAchievement.LEVEL_UP

            is CreateAchievementItemsUseCase.AchievementItem.CoinsInInventory ->
                AndroidAchievement.COINS_IN_INVENTORY

            is CreateAchievementItemsUseCase.AchievementItem.InviteFriend ->
                AndroidAchievement.INVITE_FRIEND

            is CreateAchievementItemsUseCase.AchievementItem.Gain999XPInADay ->
                AndroidAchievement.GAIN_999_XP_IN_A_DAY

            is CreateAchievementItemsUseCase.AchievementItem.FirstPetItemEquipped ->
                AndroidAchievement.FIRST_PET_ITEM_EQUIPPED

            is CreateAchievementItemsUseCase.AchievementItem.FirstRepeatingQuestCreated ->
                AndroidAchievement.FIRST_REPEATING_QUEST_CREATED

            is CreateAchievementItemsUseCase.AchievementItem.FirstChallengeCreated ->
                AndroidAchievement.FIRST_CHALLENGE_CREATED

            is CreateAchievementItemsUseCase.AchievementItem.CompleteDailyChallengeStreak ->
                AndroidAchievement.COMPLETE_DAILY_CHALLENGE_STREAK

            is CreateAchievementItemsUseCase.AchievementItem.CompleteChallenge ->
                AndroidAchievement.COMPLETE_CHALLENGE

            is CreateAchievementItemsUseCase.AchievementItem.FeedPet ->
                AndroidAchievement.PET_FED

            is CreateAchievementItemsUseCase.AchievementItem.FeedPetWithPoop ->
                AndroidAchievement.PET_FED_WITH_POOP

            is CreateAchievementItemsUseCase.AchievementItem.BecomePro ->
                AndroidAchievement.BECAME_PRO

            is CreateAchievementItemsUseCase.AchievementItem.RevivePet ->
                AndroidAchievement.PET_REVIVED

            is CreateAchievementItemsUseCase.AchievementItem.FirstAvatarChanged ->
                AndroidAchievement.FIRST_AVATAR_CHANGED

            is CreateAchievementItemsUseCase.AchievementItem.FirstPowerUpActivated ->
                AndroidAchievement.FIRST_POWER_UP_ACTIVATED

            is CreateAchievementItemsUseCase.AchievementItem.FirstPetChanged ->
                AndroidAchievement.FIRST_PET_CHANGED

            is CreateAchievementItemsUseCase.AchievementItem.PetDied ->
                AndroidAchievement.PET_DIED

            is CreateAchievementItemsUseCase.AchievementItem.CompleteQuestFor100DaysInARow ->
                AndroidAchievement.COMPLETE_QUEST_FOR_100_DAYS_IN_A_ROW

            is CreateAchievementItemsUseCase.AchievementItem.FeedbackSent ->
                AndroidAchievement.FEEDBACK_SENT

        }

val Achievement.androidAchievement
    get() =
        when (this) {

            Achievement.FIRST_QUEST_COMPLETED -> AndroidAchievement.FIRST_QUEST_COMPLETED

            Achievement.COMPLETE_10_QUESTS_IN_A_DAY -> AndroidAchievement.COMPLETE_10_QUESTS_IN_A_DAY

            Achievement.KEEP_PET_HAPPY_5_DAY_STREAK,
            Achievement.KEEP_PET_HAPPY_15_DAY_STREAK,
            Achievement.KEEP_PET_HAPPY_40_DAY_STREAK -> AndroidAchievement.KEEP_PET_HAPPY

        // >= 4
            Achievement.AWESOMENESS_SCORE_5_DAY_STREAK,
            Achievement.AWESOMENESS_SCORE_20_DAY_STREAK,
            Achievement.AWESOMENESS_SCORE_50_DAY_STREAK -> AndroidAchievement.AWESOMENESS_STREAK

        // only on plan days
            Achievement.FOCUS_HOURS_5_DAY_STREAK,
            Achievement.FOCUS_HOURS_20_DAY_STREAK,
            Achievement.FOCUS_HOURS_50_DAY_STREAK -> AndroidAchievement.FOCUS_HOURS_STREAK

            Achievement.PLAN_DAY_5_DAY_STREAK,
            Achievement.PLAN_DAY_20_DAY_STREAK,
            Achievement.PLAN_DAY_50_DAY_STREAK -> AndroidAchievement.PLAN_DAY_STREAK

            Achievement.CONVERT_1_GEM,
            Achievement.CONVERT_20_GEMS,
            Achievement.CONVERT_50_GEMS -> AndroidAchievement.GEMS_CONVERTED

            Achievement.REACH_LEVEL_10,
            Achievement.REACH_LEVEL_30,
            Achievement.REACH_LEVEL_50 -> AndroidAchievement.LEVEL_UP

            Achievement.HAVE_1K_LIFE_COINS_IN_INVENTORY,
            Achievement.HAVE_5K_LIFE_COINS_IN_INVENTORY,
            Achievement.HAVE_10K_LIFE_COINS_IN_INVENTORY -> AndroidAchievement.COINS_IN_INVENTORY

            Achievement.INVITE_1_FRIEND,
            Achievement.INVITE_5_FRIENDS,
            Achievement.INVITE_20_FRIENDS -> AndroidAchievement.INVITE_FRIEND

            Achievement.GAIN_999_XP_IN_A_DAY -> AndroidAchievement.GAIN_999_XP_IN_A_DAY
            Achievement.FIRST_PET_ITEM_EQUIPPED -> AndroidAchievement.FIRST_PET_ITEM_EQUIPPED

            Achievement.FIRST_REPEATING_QUEST_CREATED -> AndroidAchievement.FIRST_REPEATING_QUEST_CREATED

            Achievement.FIRST_CHALLENGE_CREATED -> AndroidAchievement.FIRST_CHALLENGE_CREATED

            Achievement.COMPLETE_DAILY_CHALLENGE,
            Achievement.COMPLETE_DAILY_CHALLENGE_10_DAY_STREAK,
            Achievement.COMPLETE_DAILY_CHALLENGE_30_DAY_STREAK -> AndroidAchievement.COMPLETE_DAILY_CHALLENGE_STREAK

            Achievement.COMPLETE_CHALLENGE,
            Achievement.COMPLETE_5_CHALLENGES,
            Achievement.COMPLETE_15_CHALLENGES -> AndroidAchievement.COMPLETE_CHALLENGE

            Achievement.PET_FED_WITH_POOP -> AndroidAchievement.PET_FED_WITH_POOP
            Achievement.PET_FED -> AndroidAchievement.PET_FED
            Achievement.BECAME_PRO -> AndroidAchievement.BECAME_PRO

            Achievement.PET_REVIVED -> AndroidAchievement.PET_REVIVED

            Achievement.FIRST_AVATAR_CHANGED -> AndroidAchievement.FIRST_AVATAR_CHANGED
            Achievement.FIRST_POWER_UP_ACTIVATED -> AndroidAchievement.FIRST_POWER_UP_ACTIVATED
            Achievement.FIRST_PET_CHANGED -> AndroidAchievement.FIRST_PET_CHANGED
            Achievement.PET_DIED -> AndroidAchievement.PET_DIED

            Achievement.COMPLETE_QUEST_FOR_100_DAYS_IN_A_ROW -> AndroidAchievement.COMPLETE_QUEST_FOR_100_DAYS_IN_A_ROW

            Achievement.FEEDBACK_SENT -> AndroidAchievement.FEEDBACK_SENT

        }
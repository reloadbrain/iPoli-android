package io.ipoli.android

import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.datetime.TimeOfDay
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.Theme
import io.ipoli.android.player.data.Player
import org.threeten.bp.DayOfWeek

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 8/20/17.
 */
interface Constants {
    companion object {

        val DISCORD_CHAT_LINK = "https://discord.gg/7AMKs28"

        val FACEBOOK_APP_LINK = "https://fb.me/1609677589354576"
        val IPOLI_LOGO_URL = "https://i.imgur.com/Gz3rUi1.png"
        val INVITE_IMAGE_URL = "https://i.imgur.com/fLToavB.png"
        val SHARE_URL = "http://bit.ly/ipoli-android"
        val TWITTER_USERNAME = "@myPoliHQ"

        val REMIND_DAILY_CHALLENGE_NOTIFICATION_ID = 101
        val ONGOING_NOTIFICATION_ID = 102
        val MEMBERSHIP_EXPIRATION_NOTIFICATION_ID = 103

        val QUEST_TIMER_NOTIFICATION_ID = 201
        val QUEST_COMPLETE_NOTIFICATION_ID = 202

        val PET_STATE_CHANGED_NOTIFICATION_ID = 301

        val DEFAULT_SNOOZE_TIME_MINUTES = 10

        val QUEST_WITH_NO_DURATION_TIMER_MINUTES = 30
        val MAX_QUEST_DURATION_HOURS = 4

        val PLAYER_ID_EXTRA_KEY = "player_id"

        val PROFILE_ID_EXTRA_KEY = "profile_id"

        val QUEST_ID_EXTRA_KEY = "quest_id"

        val REPEATING_QUEST_ID_EXTRA_KEY = "repeating_quest_id"

        val CHALLENGE_ID_EXTRA_KEY = "challenge_id"

        val REWARD_ID_EXTRA_KEY = "reward_id"

        val CURRENT_SELECTED_DAY_EXTRA_KEY = "CURRENT_SELECTED_DAY"

        val DISPLAY_NAME_EXTRA_KEY = "display_name"

        val SHOW_TRIAL_MESSAGE_EXTRA_KEY = "show_trial_message"

        val CALENDAR_EVENT_MIN_DURATION = 15

        val CALENDAR_EVENT_MIN_SINGLE_LINE_DURATION = 20

        val CALENDAR_EVENT_MIN_TWO_LINES_DURATION = 30

        val QUEST_MIN_DURATION = 10

        val DEFAULT_QUEST_DURATION = 60

        val MAX_UNSCHEDULED_QUEST_VISIBLE_COUNT = 3
        val RESULT_REMOVED = 100

        val KEY_APP_RUN_COUNT = "APP_RUN_COUNT"

        val KEY_PRIVACY_ACCEPTED_VERSION = "PRIVACY_ACCEPTED_VERSION"

        val PRIVACY_POLICY_VERSION = 1

        val KEY_SHOULD_SHOW_RATE_DIALOG = "SHOULD_SHOW_RATE_DIALOG"

        val KEY_APP_VERSION_CODE = "APP_VERSION_CODE"

        val KEY_PLAYER_ID = "PLAYER_ID"
        val KEY_TIME_FORMAT = "TIME_FORMAT"
        val KEY_SCHEMA_VERSION = "SCHEMA_VERSION"

        val KEY_DAILY_CHALLENGE_DAYS = "DAILY_CHALLENGE_DAYS"

        val KEY_DAILY_CHALLENGE_REMINDER_START_MINUTE = "DAILY_CHALLENGE_REMINDER_START_MINUTE"

        val KEY_DAILY_CHALLENGE_ENABLE_REMINDER = "DAILY_CHALLENGE_ENABLE_REMINDER"

        val KEY_DAILY_CHALLENGE_LAST_COMPLETED = "DAILY_CHALLENGE_LAST_COMPLETED"

        val KEY_ONGOING_NOTIFICATION_ENABLED = "ONGOING_NOTIFICATION_ENABLED"

        val KEY_SHOULD_SHOW_TUTORIAL = "SHOULD_SHOW_TUTORIAL"

        val MYPOLI_EMAIL = "hi@mypoli.fun"

        val API_RESOURCE_SOURCE = "io.ipoli-android"
        val DEFAULT_PLAYER_XP: Long = 20
        val DEFAULT_PLAYER_LEVEL = 1
        val DEFAULT_PLAYER_COINS = 10
        val DEFAULT_PLAYER_GEMS = 1
        val GEM_COINS_PRICE = 100
        //        val DEFAULT_PLAYER_AVATAR = Avatar.IPOLI_CLASSIC
        val DEFAULT_PLAYER_PRODUCTIVE_TIMES = setOf(TimeOfDay.MORNING)
        val DEFAULT_PLAYER_WORK_START_TIME by lazy { Time.atHours(9) }
        val DEFAULT_PLAYER_WORK_END_TIME by lazy { Time.atHours(18) }
        val DEFAULT_PLAYER_SLEEP_START_TIME by lazy { Time.atHours(23) }
        val DEFAULT_PLAYER_SLEEP_END_TIME by lazy { Time.atHours(8) }
        val DEFAULT_PLAYER_COMPLETE_DAILY_QUESTS_MINUTE = 0

        val DEFAULT_TIME_FORMAT = Player.Preferences.TimeFormat.DEVICE_DEFAULT
        val DEFAULT_TEMPERATURE_UNIT = Player.Preferences.TemperatureUnit.FAHRENHEIT

        val DEFAULT_PLAYER_WORK_DAYS by lazy {
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
        }

        val CHANGE_PET_STATS_MORNING_TIME by lazy { Time.atHours(9) }
        val CHANGE_PET_STATS_AFTERNOON_TIME by lazy { Time.atHours(14) }
        val CHANGE_PET_STATS_EVENING_TIME by lazy { Time.atHours(19) }

        val DURATIONS = listOf(10, 15, 20, 25, 30, 45, 60, 90, 120, 180, 240)

        val REWARD_COINS = arrayOf(10, 20, 50, 100, 200, 500, 1000)

        val DEFAULT_REWARD_PRICE = 10

        val REWARD_MAX_PRICE = 10000

        val REWARD_MIN_PRICE = 1

        const val DEFAULT_PLAN_DAY_REMINDER_START_MINUTE = 10 * 60

        val DEFAULT_DAILY_CHALLENGE_ENABLE_REMINDER = true

        const val DEFAULT_QUICK_DO_NOTIFICATION_ENABLED = true
        val REMINDER_PREDEFINED_MINUTES = intArrayOf(0, 10, 15, 30, 60)
        val MIN_FLEXIBLE_TIMES_A_WEEK_COUNT = 1
        val MAX_FLEXIBLE_TIMES_A_WEEK_COUNT = 6

        val MIN_FLEXIBLE_TIMES_A_MONTH_COUNT = 1
        val MAX_FLEXIBLE_TIMES_A_MONTH_COUNT = 15

        val DEFAULT_PLAN_DAYS by lazy {
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
        }

        val DAILY_CHALLENGE_QUEST_COUNT = 3
        val DEFAULT_CHALLENGE_DEADLINE_DAY_DURATION = 30
        val DEFAULT_BAR_COUNT = 4
        val REMINDER_START_TIME = "reminder_start_time"

        val QUICK_ADD_ADDITIONAL_TEXT = "quick_add_additional_text"
        val DEFAULT_PET_NAME = "Flopsy"
        val DEFAULT_PET_AVATAR = PetAvatar.ELEPHANT
        val DEFAULT_PET_BACKGROUND_PICTURE = "pet_background_1"

        val DEFAULT_PET_HP = 80
        val DEFAULT_PET_MP = 80
        val XP_BONUS_PERCENTAGE_OF_HP = 20.0
        val COINS_BONUS_PERCENTAGE_OF_HP = 10.0
        val QUEST_BOUNTY_DROP_PERCENTAGE = 30.0

        val XP_TO_PET_HP_RATIO = 13.2
        val XP_TO_PET_MOOD_RATIO = 11.1
        val REVIVE_PET_GEM_PRICE = 3
        val PREDEFINED_CHALLENGE_INDEX = "predefined_challenge_index"
        val RANDOM_SEED = 42 // duh!
        val MAX_TIMES_A_DAY_COUNT = 8
        const val SCHEMA_VERSION = 106

        val MAX_PENALTY_COEFFICIENT = 0.5
        val NO_QUESTS_PENALTY_COEFFICIENT = 0.3
        val IMPORTANT_QUEST_PENALTY_PERCENT = 5.0

        val KEY_WIDGET_AGENDA_QUEST_LIST = "widget_agenda_quest_list"
        val API_READ_TIMEOUT_SECONDS = 30
        val DEFAULT_VIEW_VERSION = "1.0"
        val SOURCE_ANDROID_CALENDAR = "android-calendar"

        val XP_BAR_MAX_VALUE = 100
        const val RC_CALENDAR_PERM = 102
        const val RC_LOCATION_PERM = 103
        val KEY_LAST_ANDROID_CALENDAR_SYNC_DATE = "LAST_ANDROID_CALENDAR_SYNC_DATE"
        val FACEBOOK_PACKAGE = "com.facebook.katana"
        val TWITTER_PACKAGE = "com.twitter.android"
        val SYNC_CALENDAR_JOB_ID = 1
        val PROFILES_FIRST_SCHEMA_VERSION = 7

        val POWER_UP_GRACE_PERIOD_DAYS = 7
        const val POWER_UPS_TRIAL_PERIOD_DAYS = 7
        const val POWER_UP_PURCHASE_DURATION_DAYS = 30

        val SKU_SUBSCRIPTION_MONTHLY = "monthly_plan_70_percent"
        val SKU_SUBSCRIPTION_QUARTERLY = "quarterly_plan_70_percent"
        val SKU_SUBSCRIPTION_YEARLY = "yearly_plan_70_percent"

        val KEY_ACHIEVEMENT_ACTION = "achievement_action"
        val KEY_ACHIEVEMENT_ACTION_CLASS = "achievement_action_class"

        const val MAX_FAVORITE_TAGS = 10

        // 0.48 = 122
        val MEDIUM_ALPHA = 122
        val NO_TRANSPARENCY_ALPHA = 255

        const val KEY_THEME = "CURRENT_THEME"

        val DEFAULT_THEME = Theme.BLUE

        const val DEFAULT_POMODORO_WORK_DURATION = 25
        const val DEFAULT_POMODORO_BREAK_DURATION = 5
        const val DEFAULT_POMODORO_LONG_BREAK_DURATION = 15

        const val REMINDERS_NOTIFICATION_CHANNEL_ID = "myPoli"
        const val REMINDERS_NOTIFICATION_CHANNEL_NAME = "myPoli"

        const val PLAN_DAY_NOTIFICATION_CHANNEL_ID = "myPoli_plan_day"
        const val PLAN_DAY_NOTIFICATION_CHANNEL_NAME = "Plan Day Reminder"

        const val USERNAME_MIN_LENGTH = 3
        const val USERNAME_MAX_LENGTH = 20

        const val MAX_FREE_TAGS = 5
        const val MAX_TAGS_PER_ITEM = 3

        const val DEFAULT_RELATIVE_REMINDER_MINUTES_FROM_START = 0L
        const val DAILY_FOCUS_HOURS_GOAL = 5

        val LEVEL_UP_REWARDS = listOf<String>(
            "https://media2.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif",
            "https://i.imgur.com/vlU5ROS.jpg",
            "https://media2.giphy.com/media/epnHmbfUFaBkk/giphy.gif",
            "https://i.imgur.com/6XQzIIE.jpg",
            "https://media0.giphy.com/media/SzLfW8ydkkjde/giphy.gif",
            "https://i.imgur.com/rH4tJzy.jpg",
            "https://media1.giphy.com/media/DGfhSy8xKKbHq/giphy.gif",
            "https://i.imgur.com/c7h9RJ3.jpg",
            "https://media1.giphy.com/media/uTCAwWNtz7U2c/giphy.gif",
            "https://i.imgur.com/Rjsuje5.jpg",
            "https://media2.giphy.com/media/xT8qBepJQzUjXpeWU8/giphy.gif",
            "https://i.imgur.com/rZIkNSB.jpg",
            "https://media2.giphy.com/media/TEFplLVRDMWBi/giphy.gif",
            "https://i.imgur.com/vQWIgJi.jpg",
            "http://www.awesomelycute.com/gallery/2014/01/awesomelycute-animals-2861.gif",
            "https://i.imgur.com/8RfjjbY.jpg",
            "http://cdn2.list25.com/wp-content/uploads/2012/04/fish1.gif",
            "https://i.imgur.com/Pt5CbEa.jpg",
            "https://media0.giphy.com/media/LLtUrlCg6qT2o/giphy.gif",
            "https://i.imgur.com/v7clQ8l.jpg"
        )
        const val DISPLAY_NAME_MAX_LENGTH = 50
        const val BIO_MAX_LENGTH = 250
    }
}

package io.ipoli.android.settings

import io.ipoli.android.Constants
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.data.Player.Preferences.TemperatureUnit.FAHRENHEIT
import io.ipoli.android.player.data.Player.Preferences.TimeFormat.TWELVE_HOURS
import io.ipoli.android.settings.SettingsViewState.StateType.*
import io.ipoli.android.store.powerup.PowerUp
import io.ipoli.android.store.powerup.middleware.ShowBuyPowerUpAction
import org.threeten.bp.DayOfWeek

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 4/2/18.
 */
sealed class SettingsAction : Action {
    data class SyncCalendarsSelected(val calendars: Set<Player.Preferences.SyncCalendar>) :
        SettingsAction()

    data class ToggleSyncCalendar(val isChecked: Boolean) : SettingsAction()
    data class PlanDayTimeChanged(val time: Time) : SettingsAction()
    data class PlanDaysChanged(val days: Set<DayOfWeek>) : SettingsAction()
    data class TimeFormatChanged(val format: Player.Preferences.TimeFormat) : SettingsAction()
    data class TemperatureUnitChanged(val unit: Player.Preferences.TemperatureUnit) :
        SettingsAction()

    data class ToggleQuickDoNotification(val isEnabled: Boolean) : SettingsAction()

    object Load : SettingsAction()
}

object SettingsReducer : BaseViewStateReducer<SettingsViewState>() {
    override fun reduce(
        state: AppState,
        subState: SettingsViewState,
        action: Action
    ) = when (action) {

        SettingsAction.Load -> {
            val selectedCalendars = state.dataState.player!!.preferences.syncCalendars.size
            createChangedState(
                state = subState,
                player = state.dataState.player,
                selectedCalendars = selectedCalendars
            )
        }

        is SettingsAction.ToggleSyncCalendar -> {
            if (action.isChecked) {
                subState.copy(
                    type = ENABLE_SYNC_CALENDARS,
                    isCalendarSyncEnabled = action.isChecked
                )
            } else subState

        }

        is DataLoadedAction.PlayerChanged -> {
            val selectedCalendars = action.player.preferences.syncCalendars.size
            createChangedState(
                player = action.player,
                selectedCalendars = selectedCalendars,
                state = subState
            )
        }

        is ShowBuyPowerUpAction -> {
            if (action.powerUp == PowerUp.Type.CALENDAR_SYNC) {
                subState.copy(
                    type = DATA_CHANGED,
                    isCalendarSyncEnabled = false
                )
            } else subState
        }

        else -> subState
    }

    private fun createChangedState(
        player: Player,
        selectedCalendars: Int,
        state: SettingsViewState
    ) =
        state.copy(
            type = DATA_CHANGED,
            playerId = player.id,
            timeFormat = player.preferences.timeFormat,
            temperatureUnit = player.preferences.temperatureUnit,
            planTime = player.preferences.planDayTime,
            planDays = player.preferences.planDays,
            selectedCalendars = selectedCalendars,
            isCalendarSyncEnabled = player.isPowerUpEnabled(PowerUp.Type.CALENDAR_SYNC) && selectedCalendars > 0,
            isQuickDoNotificationEnabled = player.preferences.isQuickDoNotificationEnabled
        )

    override fun defaultState() = SettingsViewState(
        type = LOADING,
        playerId = "",
        timeFormat = TWELVE_HOURS,
        temperatureUnit = FAHRENHEIT,
        planDays = Constants.DEFAULT_PLAN_DAYS,
        planTime = Time.of(Constants.DEFAULT_PLAN_DAY_REMINDER_START_MINUTE),
        isCalendarSyncEnabled = false,
        isQuickDoNotificationEnabled = true,
        selectedCalendars = 0
    )

    override val stateKey = key<SettingsViewState>()

}

data class SettingsViewState(
    val type: StateType,
    val playerId: String,
    val timeFormat: Player.Preferences.TimeFormat,
    val temperatureUnit: Player.Preferences.TemperatureUnit,
    val planTime: Time,
    val planDays: Set<DayOfWeek>,
    val isCalendarSyncEnabled: Boolean,
    val isQuickDoNotificationEnabled: Boolean,
    val selectedCalendars: Int
) : BaseViewState() {

    enum class StateType {
        LOADING, DATA_CHANGED, ENABLE_SYNC_CALENDARS
    }
}
package io.ipoli.android.event.calendar.picker

import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.event.Calendar
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.data.Player

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/11/2018.
 */

sealed class CalendarPickerAction : Action {
    object Load : CalendarPickerAction()
    data class SelectCalendars(val selectedCalendarPositions: List<Int>) : CalendarPickerAction()
}

object CalendarPickerReducer : BaseViewStateReducer<CalendarPickerViewState>() {

    override val stateKey = key<CalendarPickerViewState>()

    override fun reduce(
        state: AppState,
        subState: CalendarPickerViewState,
        action: Action
    ) =
        when (action) {
            is DataLoadedAction.CalendarsChanged ->
                subState.copy(
                    type = CalendarPickerViewState.StateType.CALENDARS_LOADED,
                    petAvatar = state.dataState.player!!.pet.avatar,
                    calendars = action.calendars.filter { it.isVisible }
                )

            is CalendarPickerAction.SelectCalendars -> {
                val calendars = subState.calendars
                val syncCalendars = action.selectedCalendarPositions.map {
                    Player.Preferences.SyncCalendar(
                        id = calendars[it].id,
                        name = calendars[it].name
                    )
                }.toSet()
                subState.copy(
                    type = CalendarPickerViewState.StateType.CALENDARS_SELECTED,
                    syncCalendars = syncCalendars
                )
            }

            else ->
                subState
        }

    override fun defaultState() =
        CalendarPickerViewState(
            type = CalendarPickerViewState.StateType.LOADING,
            petAvatar = PetAvatar.BEAR,
            calendars = emptyList(),
            syncCalendars = emptySet()
        )
}

data class CalendarPickerViewState(
    val type: StateType,
    val petAvatar: PetAvatar,
    val calendars: List<Calendar>,
    val syncCalendars: Set<Player.Preferences.SyncCalendar>
) : BaseViewState() {

    enum class StateType { LOADING, CALENDARS_LOADED, CALENDARS_SELECTED }
}


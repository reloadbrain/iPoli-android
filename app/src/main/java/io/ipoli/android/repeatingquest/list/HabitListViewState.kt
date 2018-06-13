package io.ipoli.android.repeatingquest.list

import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/13/18.
 */
sealed class HabitListAction : Action

object HabitListReducer : BaseViewStateReducer<HabitListViewState>() {
    override val stateKey = key<HabitListViewState>()

    override fun reduce(
        state: AppState,
        subState: HabitListViewState,
        action: Action
    ): HabitListViewState {
        return subState
    }

    override fun defaultState() = HabitListViewState(
        type = HabitListViewState.StateType.LOADING
    )
}

data class HabitListViewState(
    val type: StateType
) : BaseViewState() {
    enum class StateType {
        LOADING
    }
}
package io.ipoli.android.achievement.list

import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 06/09/2018.
 */

sealed class AchievementListAction : Action {
    object Load : AchievementListAction()
}

object AchievementListReducer : BaseViewStateReducer<AchievementListViewState>() {

    override val stateKey = key<AchievementListViewState>()

    override fun reduce(
        state: AppState,
        subState: AchievementListViewState,
        action: Action
    ) =
        when (action) {
            is DataLoadedAction.AchievementItemsChanged -> {
                subState.copy(
                    type = AchievementListViewState.StateType.ACHIEVEMENTS_LOADED,
                    achievementListItems = action.achievementListItems
                )
            }

            else -> subState
        }

    override fun defaultState() =
        AchievementListViewState(
            type = AchievementListViewState.StateType.LOADING,
            achievementListItems = emptyList()
        )
}

data class AchievementListViewState(
    val type: StateType,
    val achievementListItems: List<CreateAchievementItemsUseCase.AchievementListItem>
) : BaseViewState() {
    enum class StateType { LOADING, ACHIEVEMENTS_LOADED }
}
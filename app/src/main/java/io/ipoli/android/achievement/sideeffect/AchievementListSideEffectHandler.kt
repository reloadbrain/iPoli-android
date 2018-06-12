package io.ipoli.android.achievement.sideeffect

import io.ipoli.android.achievement.list.AchievementListAction
import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.common.AppSideEffectHandler
import io.ipoli.android.common.AppState
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.redux.Action
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 06/11/2018.
 */
object AchievementListSideEffectHandler : AppSideEffectHandler() {

    private val createAchievementItemsUseCase by required { createAchievementItemsUseCase }

    override suspend fun doExecute(action: Action, state: AppState) {
        when (action) {
            is AchievementListAction.Load -> {
                if (state.dataState.player != null) {
                    val ai = createAchievementItemsUseCase.execute(
                        CreateAchievementItemsUseCase.Params(state.dataState.player)
                    )
                    dispatch(DataLoadedAction.AchievementItemsChanged(ai))
                }
            }

            is DataLoadedAction.PlayerChanged -> {
                val ai = createAchievementItemsUseCase.execute(
                    CreateAchievementItemsUseCase.Params(action.player)
                )
                dispatch(DataLoadedAction.AchievementItemsChanged(ai))
            }

            else -> {
            }
        }
    }

    override fun canHandle(action: Action) =
        action is AchievementListAction || action is DataLoadedAction.PlayerChanged

}
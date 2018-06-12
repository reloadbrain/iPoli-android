package io.ipoli.android.player.sideeffect

import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.common.AppSideEffectHandler
import io.ipoli.android.common.AppState
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.redux.Action
import io.ipoli.android.player.ProfileAction
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.usecase.FindAverageFocusedDurationForPeriodUseCase
import io.ipoli.android.player.usecase.SaveProfileUseCase
import space.traversal.kapsule.required

object ProfileSideEffectHandler : AppSideEffectHandler() {

    private val findDailyChallengeStreakUseCase by required { findDailyChallengeStreakUseCase }
    private val findAverageProductiveDurationForPeriodUseCase by required { findAverageFocusedDurationForPeriodUseCase }
    private val saveProfileUseCase by required { saveProfileUseCase }
    private val createAchievementItemsUseCase by required { createAchievementItemsUseCase }

    override suspend fun doExecute(action: Action, state: AppState) {
        when (action) {
            is ProfileAction.Load ->
                state.dataState.player?.let {
                    updateProfileData(it)
                }

            is DataLoadedAction.PlayerChanged ->
                updateProfileData(action.player)

            is ProfileAction.Save ->
                saveProfileUseCase.execute(
                    SaveProfileUseCase.Params(
                        displayName = action.displayName,
                        bio = action.bio
                    )
                )
        }
    }

    private fun updateProfileData(player: Player) {
        val ai = createAchievementItemsUseCase.execute(
            CreateAchievementItemsUseCase.Params(player)
        )
            .filter {
                it is CreateAchievementItemsUseCase.AchievementListItem.UnlockedItem || hasUnlockedAtLeast1Level(
                    it
                )
            }
            .map {
                when (it) {
                    is CreateAchievementItemsUseCase.AchievementListItem.UnlockedItem ->
                        it.achievementItem

                    is CreateAchievementItemsUseCase.AchievementListItem.LockedItem ->
                        it.achievementItem

                    else -> throw IllegalArgumentException("Unknown achievement type $it")
                }
            }
        dispatch(
            DataLoadedAction.ProfileDataChanged(
                ai,
                findDailyChallengeStreakUseCase.execute(Unit),
                findAverageProductiveDurationForPeriodUseCase.execute(
                    FindAverageFocusedDurationForPeriodUseCase.Params(dayPeriod = 7)
                )
            )
        )
    }

    private fun hasUnlockedAtLeast1Level(it: CreateAchievementItemsUseCase.AchievementListItem) =
        it is CreateAchievementItemsUseCase.AchievementListItem.LockedItem && it.achievementItem.isMultiLevel && it.achievementItem.currentLevel > 1

    override fun canHandle(action: Action) =
        action is ProfileAction || action is DataLoadedAction.PlayerChanged

}
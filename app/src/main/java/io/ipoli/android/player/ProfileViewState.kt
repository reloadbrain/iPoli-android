package io.ipoli.android.player

import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.datetime.Duration
import io.ipoli.android.common.datetime.Minute
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.pet.Pet
import io.ipoli.android.player.ProfileViewState.StateType.*
import io.ipoli.android.player.data.Avatar
import io.ipoli.android.player.data.Player

sealed class ProfileAction : Action {
    data class Save(val displayName: String, val bio: String) : ProfileAction()

    object Load : ProfileAction()
    object StartEdit : ProfileAction()
    object StopEdit : ProfileAction()
}

object ProfileReducer : BaseViewStateReducer<ProfileViewState>() {
    override fun reduce(
        state: AppState,
        subState: ProfileViewState,
        action: Action
    ) = when (action) {

        is ProfileAction.Load -> {
            val p = state.dataState.player
            if (p != null)
                createStateFromPlayer(p, subState)
            else
                subState
        }

        is DataLoadedAction.PlayerChanged -> {
            val p = action.player
            createStateFromPlayer(p, subState)
        }

        is DataLoadedAction.ProfileDataChanged -> {

            val newState = subState.copy(
                dailyChallengeStreak = action.streak,
                last7DaysAverageProductiveDuration = action.averageProductiveDuration,
                unlockedAchievements = action.unlockedAchievements
            )
            if (hasProfileDataLoaded(newState)) {
                newState.copy(
                    type = PROFILE_DATA_LOADED
                )
            } else {
                newState
            }
        }

        is ProfileAction.StartEdit ->
            subState.copy(
                type = EDIT
            )

        ProfileAction.StopEdit ->
            subState.copy(
                type = EDIT_STOPPED
            )

        else -> subState
    }

    private fun hasProfileDataLoaded(state: ProfileViewState) =
        state.pet != null && state.last7DaysAverageProductiveDuration != null && state.dailyChallengeStreak >= 0

    private fun createStateFromPlayer(
        player: Player,
        subState: ProfileViewState
    ): ProfileViewState {
        val newState = subState.copy(
            avatar = player.avatar,
            displayName = player.displayName,
            username = player.username,
            titleIndex = player.level / 10,
            createdAgo = player.createdAt.toEpochMilli(),
            bio = player.bio,
            level = player.level,
            levelXpProgress = player.experienceProgressForLevel,
            levelXpMaxProgress = player.experienceForNextLevel,
            coins = player.coins,
            gems = player.gems,
            pet = player.pet
        )
        return if (hasProfileDataLoaded(newState)) {
            newState.copy(
                type = PROFILE_DATA_LOADED
            )
        } else {
            newState
        }
    }

    override fun defaultState() =
        ProfileViewState(
            type = LOADING,
            avatar = Avatar.AVATAR_00,
            displayName = null,
            username = null,
            bio = null,
            titleIndex = -1,
            createdAgo = -1,
            pet = null,
            level = -1,
            levelXpProgress = -1,
            levelXpMaxProgress = -1,
            coins = -1,
            gems = -1,
            dailyChallengeStreak = -1,
            last7DaysAverageProductiveDuration = null,
            unlockedAchievements = emptyList()
        )

    override val stateKey = key<ProfileViewState>()
}

data class ProfileViewState(
    val type: StateType,
    val avatar: Avatar,
    val displayName: String?,
    val username: String?,
    val titleIndex: Int,
    val createdAgo: Long,
    val bio: String?,
    val pet: Pet?,
    val level: Int,
    val levelXpProgress: Int,
    val levelXpMaxProgress: Int,
    val coins: Int,
    val gems: Int,
    val dailyChallengeStreak: Int,
    val last7DaysAverageProductiveDuration: Duration<Minute>?,
    val unlockedAchievements: List<CreateAchievementItemsUseCase.AchievementItem>
) : BaseViewState() {

    enum class StateType { LOADING, PROFILE_DATA_LOADED, EDIT, EDIT_STOPPED }
}
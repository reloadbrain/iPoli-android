package io.ipoli.android.store.avatar

import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.data.Avatar
import io.ipoli.android.store.avatar.sideeffect.BuyAvatarCompletedAction
import io.ipoli.android.store.avatar.usecase.BuyAvatarUseCase

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/24/2018.
 */

sealed class AvatarStoreAction : Action {
    object Load : AvatarStoreAction()
    data class Buy(val avatar: Avatar) : AvatarStoreAction()
    data class Change(val avatar: Avatar) : AvatarStoreAction()
}

object AvatarStoreReducer : BaseViewStateReducer<AvatarStoreViewState>() {

    override val stateKey = key<AvatarStoreViewState>()

    override fun reduce(
        state: AppState,
        subState: AvatarStoreViewState,
        action: Action
    ) =
        when (action) {

            AvatarStoreAction.Load ->
                state.dataState.player?.let {
                    subState.copy(
                        type = AvatarStoreViewState.StateType.DATA_CHANGED,
                        avatars = createAvatars(it)
                    )
                } ?: subState.copy(
                    type = AvatarStoreViewState.StateType.LOADING
                )

            is DataLoadedAction.PlayerChanged ->
                subState.copy(
                    type = AvatarStoreViewState.StateType.DATA_CHANGED,
                    avatars = createAvatars(action.player)
                )

            is BuyAvatarCompletedAction ->
                when (action.result) {
                    is BuyAvatarUseCase.Result.Bought ->
                        subState.copy(
                            type = AvatarStoreViewState.StateType.AVATAR_BOUGHT
                        )

                    is BuyAvatarUseCase.Result.TooExpensive ->
                        subState.copy(
                            type = AvatarStoreViewState.StateType.AVATAR_TOO_EXPENSIVE
                        )
                }

            else -> subState
        }

    override fun defaultState() =
        AvatarStoreViewState(
            type = AvatarStoreViewState.StateType.LOADING,
            avatars = listOf()
        )

    private fun createAvatars(player: Player) =
        Avatar.values().map {
            when {
                player.avatar == it ->
                    AvatarItem.Current(it)

                player.inventory.hasAvatar(it) ->
                    AvatarItem.Bought(it)

                else -> AvatarItem.ForSale(it)
            }
        }
}

sealed class AvatarItem(open val avatar: Avatar) {
    data class Current(override val avatar: Avatar) : AvatarItem(avatar)
    data class Bought(override val avatar: Avatar) : AvatarItem(avatar)
    data class ForSale(override val avatar: Avatar) : AvatarItem(avatar)
}

data class AvatarStoreViewState(val type: StateType, val avatars: List<AvatarItem>) :
    BaseViewState() {

    enum class StateType {
        LOADING, AVATAR_BOUGHT, AVATAR_TOO_EXPENSIVE, DATA_CHANGED
    }
}
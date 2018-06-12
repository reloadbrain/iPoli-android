package io.ipoli.android.store.theme

import io.ipoli.android.common.AppState
import io.ipoli.android.common.BaseViewStateReducer
import io.ipoli.android.common.DataLoadedAction
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.redux.Action
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.Theme
import io.ipoli.android.store.theme.sideeffect.BuyThemeCompletedAction
import io.ipoli.android.store.theme.usecase.BuyThemeUseCase

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/12/17.
 */
sealed class ThemeStoreAction : Action {
    object Load : ThemeStoreAction()
    data class Buy(val theme: Theme) : ThemeStoreAction()
    data class Change(val theme: Theme) : ThemeStoreAction()
}

object ThemeStoreReducer : BaseViewStateReducer<ThemeStoreViewState>() {
    override val stateKey = key<ThemeStoreViewState>()

    override fun reduce(
        state: AppState,
        subState: ThemeStoreViewState,
        action: Action
    ) = when (action) {
        ThemeStoreAction.Load ->
            state.dataState.player?.let {
                subState.copy(
                    type = ThemeStoreViewState.StateType.DATA_CHANGED,
                    themes = createThemes(it)
                )
            } ?: subState.copy(type = ThemeStoreViewState.StateType.LOADING)

        is DataLoadedAction.PlayerChanged ->
            subState.copy(
                type = ThemeStoreViewState.StateType.DATA_CHANGED,
                themes = createThemes(action.player)
            )

        is BuyThemeCompletedAction ->
            when (action.result) {
                is BuyThemeUseCase.Result.ThemeBought ->
                    subState.copy(type = ThemeStoreViewState.StateType.THEME_BOUGHT)

                is BuyThemeUseCase.Result.TooExpensive ->
                    subState.copy(type = ThemeStoreViewState.StateType.THEME_TOO_EXPENSIVE)
            }

        is ThemeStoreAction.Change -> {
            subState.copy(
                type = ThemeStoreViewState.StateType.THEME_CHANGED,
                theme = action.theme
            )
        }

        else -> subState
    }

    override fun defaultState() = ThemeStoreViewState(
        type = ThemeStoreViewState.StateType.LOADING,
        theme = Theme.BLUE,
        themes = emptyList()
    )

    private fun createThemes(player: Player) =
        Theme.values().map {
            when {
                player.preferences.theme == it ->
                    ThemeItem.Current(it)

                player.inventory.hasTheme(it) ->
                    ThemeItem.Bought(it)

                else -> ThemeItem.ForSale(it)
            }
        }

}

sealed class ThemeItem(open val theme: Theme) {
    data class Current(override val theme: Theme) : ThemeItem(theme)
    data class Bought(override val theme: Theme) : ThemeItem(theme)
    data class ForSale(override val theme: Theme) : ThemeItem(theme)
}

data class ThemeStoreViewState(val type: StateType, val theme: Theme, val themes: List<ThemeItem>) :
    BaseViewState() {

    enum class StateType { LOADING, THEME_BOUGHT, THEME_TOO_EXPENSIVE, THEME_CHANGED, DATA_CHANGED }
}

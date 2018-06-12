package io.ipoli.android.store.theme.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.Theme
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 12/12/17.
 */
class ChangeThemeUseCase(private val playerRepository: PlayerRepository) : UseCase<Theme, Player> {
    override fun execute(parameters: Theme): Player {
        val theme = parameters
        val player = playerRepository.find()
        requireNotNull(player)
        require(player!!.hasTheme(theme))

        val newPlayer = player.updatePreferences(
            player.preferences.copy(
                theme = theme
            )
        )

        return playerRepository.save(newPlayer)
    }
}
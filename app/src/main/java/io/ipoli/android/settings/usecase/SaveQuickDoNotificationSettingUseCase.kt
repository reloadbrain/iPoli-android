package io.ipoli.android.settings.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

class SaveQuickDoNotificationSettingUseCase(private val playerRepository: PlayerRepository) :
    UseCase<SaveQuickDoNotificationSettingUseCase.Params, Player> {

    override fun execute(parameters: Params): Player {
        val player = playerRepository.find()
        requireNotNull(player)

        return playerRepository.save(
            player!!.updatePreferences(
                player.preferences.copy(
                    isQuickDoNotificationEnabled = parameters.isEnabled
                )
            )
        )
    }

    data class Params(val isEnabled: Boolean)
}
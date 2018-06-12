package io.ipoli.android.settings.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 5/17/18.
 */
class SaveTimeFormatUseCase(
    private val playerRepository: PlayerRepository
) : UseCase<SaveTimeFormatUseCase.Params, Unit> {
    override fun execute(parameters: Params) {
        val player = playerRepository.find()
        requireNotNull(player)

        playerRepository.save(
            player!!.updatePreferences(
                player.preferences.copy(
                    timeFormat = parameters.timeFormat
                )
            )
        )
    }

    data class Params(val timeFormat: Player.Preferences.TimeFormat)
}
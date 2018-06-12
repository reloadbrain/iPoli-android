package io.ipoli.android.event.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 04/03/2018.
 */
class SaveSyncCalendarsUseCase(private val playerRepository: PlayerRepository) :
    UseCase<SaveSyncCalendarsUseCase.Params, Player> {

    override fun execute(parameters: Params): Player {
        val p = playerRepository.find()
        requireNotNull(p)
        val prefs = p!!.preferences
        return playerRepository.save(
            p.updatePreferences(
                prefs.copy(
                    syncCalendars = parameters.calendars
                )
            )
        )
    }

    data class Params(val calendars: Set<Player.Preferences.SyncCalendar>)
}
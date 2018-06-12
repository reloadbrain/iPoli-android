package io.ipoli.android.store.powerup.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.common.datetime.isAfterOrEqual
import io.ipoli.android.player.data.Membership
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.store.powerup.usecase.RemoveExpiredPowerUpsUseCase.Params
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/19/2018.
 */
class RemoveExpiredPowerUpsUseCase(private val playerRepository: PlayerRepository) :
    UseCase<Params, Player> {

    data class Params(val currentDate: LocalDate)

    override fun execute(parameters: Params): Player {
        val p = playerRepository.find()
        requireNotNull(p)
        require(p!!.membership == Membership.NONE)

        val today = parameters.currentDate

        val nonExpiredPowerUps =
            p.inventory.powerUps
                .filter { it.expirationDate.isAfterOrEqual(today) }

        return playerRepository.save(
            p.copy(
                inventory = p.inventory.setPowerUps(nonExpiredPowerUps)
            )
        )
    }

}
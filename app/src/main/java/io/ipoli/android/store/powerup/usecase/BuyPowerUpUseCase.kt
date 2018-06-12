package io.ipoli.android.store.powerup.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.store.powerup.PowerUp
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/16/2018.
 */
class BuyPowerUpUseCase(private val playerRepository: PlayerRepository) :
    UseCase<BuyPowerUpUseCase.Params, BuyPowerUpUseCase.Result> {

    override fun execute(parameters: Params): Result {
        val p = playerRepository.find()
        require(p != null)

        val powerUp = parameters.powerUp
        val powerUpPrice = powerUp.coinPrice
        if (p!!.coins < powerUpPrice) {
            return Result.TooExpensive
        }

        val inventory = p.inventory
        val newPlayer = if (inventory.isPowerUpEnabled(powerUp)) {
            val ip = inventory.getPowerUp(powerUp)!!
            val newInventory = inventory.removePowerUp(ip)
            val newPowerUp = ip.copy(
                expirationDate = ip.expirationDate.plusDays(parameters.durationDays.toLong())
            )
            p.copy(
                coins = p.coins - powerUpPrice,
                inventory = newInventory.addPowerUp(newPowerUp)
            )
        } else {
            p.copy(
                coins = p.coins - powerUpPrice,
                inventory = inventory.addPowerUp(
                    PowerUp.fromType(
                        powerUp,
                        LocalDate.now().plusDays(parameters.durationDays.toLong())
                    )
                )
            )
        }

        return Result.Bought(powerUp, playerRepository.save(newPlayer))
    }

    data class Params(
        val powerUp: PowerUp.Type,
        val durationDays: Int
    )

    sealed class Result {
        data class Bought(val powerUp: PowerUp.Type, val player: Player) : Result()
        object TooExpensive : Result()
    }
}
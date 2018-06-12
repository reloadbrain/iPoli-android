package io.ipoli.android.player.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.quest.IconPack

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 15.12.17.
 */
class BuyIconPackUseCase(private val playerRepository: PlayerRepository) :
    UseCase<BuyIconPackUseCase.Params, BuyIconPackUseCase.Result> {

    override fun execute(parameters: Params): Result {

        val iconPack = parameters.iconPack
        val player = playerRepository.find()
        requireNotNull(player)
        require(!player!!.hasIconPack(iconPack))

        if (player.gems < iconPack.gemPrice) {
            return Result.TooExpensive
        }

        val newPlayer = player.copy(
            gems = player.gems - iconPack.gemPrice,
            inventory = player.inventory.addIconPack(iconPack)
        )

        return Result.IconPackBought(playerRepository.save(newPlayer))
    }

    data class Params(val iconPack: IconPack)

    sealed class Result {
        data class IconPackBought(val player: Player) : Result()
        object TooExpensive : Result()
    }
}
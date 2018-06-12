package io.ipoli.android.store.avatar.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.data.Avatar
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/25/2018.
 */
class BuyAvatarUseCase(private val playerRepository: PlayerRepository) :
    UseCase<BuyAvatarUseCase.Params, BuyAvatarUseCase.Result> {

    override fun execute(parameters: Params): Result {
        val player = playerRepository.find()
        requireNotNull(player)
        val avatar = parameters.avatar
        require(!player!!.inventory.hasAvatar(avatar))

        if (player.gems < avatar.gemPrice) {
            return Result.TooExpensive
        }

        val newPlayer = player.copy(
            gems = player.gems - avatar.gemPrice,
            inventory = player.inventory.addAvatar(avatar)
        )

        return Result.Bought(playerRepository.save(newPlayer))
    }

    data class Params(val avatar: Avatar)

    sealed class Result {
        data class Bought(val player: Player) : Result()
        object TooExpensive : Result()
    }
}
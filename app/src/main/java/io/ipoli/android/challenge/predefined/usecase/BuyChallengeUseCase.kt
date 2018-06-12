package io.ipoli.android.challenge.usecase

import io.ipoli.android.challenge.predefined.entity.PredefinedChallenge
import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 1/2/18.
 */
class BuyChallengeUseCase(private val playerRepository: PlayerRepository) :
    UseCase<BuyChallengeUseCase.Params, BuyChallengeUseCase.Result> {

    override fun execute(parameters: Params): Result {
        val player = playerRepository.find()
        val challenge = parameters.challenge
        requireNotNull(player)
        require(!player!!.inventory.hasChallenge(challenge))

        if (player.gems < challenge.gemPrice) {
            return Result.TooExpensive
        }

        val newPlayer = player.copy(
            gems = player.gems - challenge.gemPrice,
            inventory = player.inventory.addChallenge(challenge)
        )

        return Result.ChallengeBought(playerRepository.save(newPlayer))
    }

    data class Params(val challenge: PredefinedChallenge)

    sealed class Result {
        data class ChallengeBought(val player: Player) : Result()
        object TooExpensive : Result()
    }
}
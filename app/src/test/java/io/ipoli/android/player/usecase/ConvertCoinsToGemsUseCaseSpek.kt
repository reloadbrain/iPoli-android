package io.ipoli.android.player.usecase

import io.ipoli.android.Constants
import io.ipoli.android.TestUtil
import io.ipoli.android.player.data.Player
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by vini on 12/21/17.
 */
class ConvertCoinsToGemsUseCaseSpek : Spek({

    describe("ConvertCoinsToGemsUseCaseSpek") {

        fun executeUseCase(player: Player, gems: Int) =
            ConvertCoinsToGemsUseCase(TestUtil.playerRepoMock(player)).execute(
                ConvertCoinsToGemsUseCase.Params(gems)
            )

        it("should not convert gems with insufficient coins") {
            val player = TestUtil.player().copy(
                coins = Constants.GEM_COINS_PRICE - 1,
                gems = 0
            )
            val result = executeUseCase(player, 1)
            result.`should be`(ConvertCoinsToGemsUseCase.Result.TooExpensive)
        }

        it("should convert gems") {
            val gems = 3
            val player = TestUtil.player().copy(
                coins = Constants.GEM_COINS_PRICE * gems + 10,
                gems = 0
            )
            val result = executeUseCase(player, gems)
            result.`should be instance of`(ConvertCoinsToGemsUseCase.Result.GemsConverted::class)
            val newPlayer = (result as ConvertCoinsToGemsUseCase.Result.GemsConverted).player
            newPlayer.gems.`should be equal to`(player.gems + gems)
            newPlayer.coins.`should be equal to`(player.coins - gems * Constants.GEM_COINS_PRICE)
        }

    }

})
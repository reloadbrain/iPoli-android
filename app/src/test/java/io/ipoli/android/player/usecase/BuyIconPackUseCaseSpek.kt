package io.ipoli.android.player.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.player.data.Inventory
import io.ipoli.android.player.data.Player
import io.ipoli.android.quest.IconPack
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@io.ipoli.io>
 * on 15.12.17.
 */
class BuyIconPackUseCaseSpek : Spek({

    describe("BuyIconPackUseCase") {
        fun executeUseCase(player: Player, iconPack: IconPack) =
            BuyIconPackUseCase(TestUtil.playerRepoMock(player)).execute(
                BuyIconPackUseCase.Params(
                    iconPack
                )
            )

        it("should require not bought icon pack") {

            val p = TestUtil.player().copy(
                inventory = Inventory(
                    iconPacks = setOf(IconPack.BASIC)
                )
            )

            val exec = { executeUseCase(p, IconPack.BASIC) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should not buy when not enough gems") {
            val p = TestUtil.player().copy(
                gems = IconPack.BASIC.gemPrice - 1,
                inventory = Inventory()
            )
            val result = executeUseCase(p, IconPack.BASIC)
            result.`should be`(BuyIconPackUseCase.Result.TooExpensive)
        }

        it("should buy icon pack") {
            val p = TestUtil.player().copy(
                gems = IconPack.BASIC.gemPrice,
                inventory = Inventory()
            )
            val result = executeUseCase(p, IconPack.BASIC)
            result.`should be instance of`(BuyIconPackUseCase.Result.IconPackBought::class)
            val newPlayer = (result as BuyIconPackUseCase.Result.IconPackBought).player
            newPlayer.gems.`should be equal to`(p.gems - IconPack.BASIC.gemPrice)
            newPlayer.hasIconPack(IconPack.BASIC).`should be true`()
        }
    }

})
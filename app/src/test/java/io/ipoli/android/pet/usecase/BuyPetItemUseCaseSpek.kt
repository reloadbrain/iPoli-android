package io.ipoli.android.pet.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.pet.Pet
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.pet.PetItem
import io.ipoli.android.player.data.Inventory
import io.ipoli.android.player.data.InventoryPet
import io.ipoli.android.player.data.Player
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/13/17.
 */
class BuyPetItemUseCaseSpek : Spek({

    describe("BuyPetItemUseCase") {

        fun executeUseCase(player: Player, petItem: PetItem) =
            BuyPetItemUseCase(TestUtil.playerRepoMock(player)).execute(
                BuyPetItemUseCase.Params(
                    petItem
                )
            )

        it("should require not bought pet item") {

            val player = TestUtil.player().copy(
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT
                ),
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet(
                            "Pencho",
                            PetAvatar.ELEPHANT,
                            setOf(PetItem.RED_HAT)
                        )
                    )
                )
            )
            val exec = { executeUseCase(player, PetItem.RED_HAT) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should not buy when not enough gems") {
            val player = TestUtil.player().copy(
                gems = PetItem.RED_HAT.gemPrice - 1,
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT
                ),
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet(
                            "Pencho",
                            PetAvatar.ELEPHANT
                        )
                    )
                )
            )
            val result = executeUseCase(player, PetItem.RED_HAT)
            result.`should be`(BuyPetItemUseCase.Result.TooExpensive)
        }

        it("should buy pet item") {
            val player = TestUtil.player().copy(
                gems = PetItem.RED_HAT.gemPrice,
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT
                ),
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet(
                            "Pencho",
                            PetAvatar.ELEPHANT
                        )
                    )
                )
            )
            val result = executeUseCase(player, PetItem.RED_HAT)
            result.`should be instance of`(BuyPetItemUseCase.Result.ItemBought::class)
            val newPlayer = (result as BuyPetItemUseCase.Result.ItemBought).player
            newPlayer.gems.`should be equal to`(player.gems - PetItem.RED_HAT.gemPrice)
            newPlayer.inventory.getPet(PetAvatar.ELEPHANT).hasItem(PetItem.RED_HAT)
                .`should be true`()
        }
    }

})
package io.ipoli.android.pet.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.pet.Pet
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.pet.PetEquipment
import io.ipoli.android.pet.PetItem
import io.ipoli.android.player.data.Player
import org.amshove.kluent.`should be null`
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/26/17.
 */
class TakeOffPetItemUseCaseSpek : Spek({

    describe("TakeOffPetItemUseCase") {

        fun executeUseCase(player: Player, petItem: PetItem) =
            TakeOffPetItemUseCase(TestUtil.playerRepoMock(player)).execute(
                TakeOffPetItemUseCase.Params(
                    petItem
                )
            )

        it("should require equipped item") {

            val player = TestUtil.player().copy(
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT
                )
            )
            val exec = { executeUseCase(player, PetItem.RED_HAT) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should take item off") {
            val player = TestUtil.player().copy(
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT,
                    PetEquipment(
                        hat = PetItem.RED_HAT
                    )
                )
            )
            val p = executeUseCase(player, PetItem.RED_HAT)
            p.pet.equipment.hat.`should be null`()
        }

    }
})
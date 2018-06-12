package io.ipoli.android.pet.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.data.Inventory
import io.ipoli.android.player.data.InventoryPet
import io.ipoli.android.player.data.Player
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@io.ipoli.io>
 * on 12/13/17.
 */
class RenamePetUseCaseSpek : Spek({
    describe("RenamePetUseCaseSpek") {

        val player = TestUtil.player().let {
            it.copy(
                pet = it.pet.copy(
                    name = "Flopsy",
                    avatar = PetAvatar.CHICKEN
                ),
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet("", PetAvatar.CHICKEN)
                    )
                )
            )
        }

        fun executeUseCase(player: Player, name: String) =
            RenamePetUseCase(TestUtil.playerRepoMock(player)).execute(RenamePetUseCase.Params(name))

        it("should not rename when new name is empty") {
            executeUseCase(player, "").`should be`(RenamePetUseCase.Result.EmptyName)
        }

        it("should require pet in inventory") {
            val newPlayer = player.copy(
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet("", PetAvatar.ELEPHANT)
                    )
                )
            )
            val exec = { executeUseCase(newPlayer, "Ivan") }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should set new name") {
            val newName = "Pencho"
            val result = executeUseCase(player, newName)
            result.`should be instance of`(RenamePetUseCase.Result.Renamed::class)
            val newPlayer = (result as RenamePetUseCase.Result.Renamed).player
            newPlayer.pet.name.`should be`(newName)
            newPlayer.inventory.getPet(player.pet.avatar).name.`should be`(newName)
        }
    }
})
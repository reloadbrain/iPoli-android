package io.ipoli.android.pet.usecase

import io.ipoli.android.Constants
import io.ipoli.android.TestUtil
import io.ipoli.android.pet.Pet
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.data.Player
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@io.ipoli.io>
 * on 12/5/17.
 */
class RevivePetUseCaseSpek : Spek({
    describe("RevivePetUseCase") {

        fun executeUseCase(player: Player) =
            RevivePetUseCase(TestUtil.playerRepoMock(player)).execute(Unit)

        it("should require dead pet") {
            val player = TestUtil.player().copy(
                pet = Pet(
                    "",
                    avatar = PetAvatar.ELEPHANT,
                    healthPoints = 10,
                    moodPoints = 10
                )
            )
            val exec = { executeUseCase(player) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should not revive when not enough gems") {
            val pet = TestUtil.player().pet
            val player = TestUtil.player().copy(
                gems = Constants.REVIVE_PET_GEM_PRICE - 1,
                pet = pet.copy(
                    healthPoints = 0,
                    moodPoints = 0
                )
            )
            val result = executeUseCase(player)
            result.`should be`(RevivePetUseCase.Result.TooExpensive)
        }

        it("should revive pet") {
            val pet = TestUtil.player().pet
            val player = TestUtil.player().copy(
                gems = Constants.REVIVE_PET_GEM_PRICE,
                pet = pet.copy(
                    healthPoints = 0,
                    moodPoints = 0
                )
            )
            val result = executeUseCase(player)
            result.`should be instance of`(RevivePetUseCase.Result.PetRevived::class)
            val newPlayer = (result as RevivePetUseCase.Result.PetRevived).player
            newPlayer.gems.`should be equal to`(0)
            val newPet = newPlayer.pet
            newPet.healthPoints.`should be equal to`(Constants.DEFAULT_PET_HP)
            newPet.moodPoints.`should be equal to`(Constants.DEFAULT_PET_MP)
        }

    }
})
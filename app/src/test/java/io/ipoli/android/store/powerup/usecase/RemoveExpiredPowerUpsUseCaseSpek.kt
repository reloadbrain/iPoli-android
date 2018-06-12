package io.ipoli.android.store.powerup.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.player.data.Player
import io.ipoli.android.store.powerup.PowerUp
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be true`
import org.amshove.kluent.`should equal`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/19/2018.
 */
class RemoveExpiredPowerUpsUseCaseSpek : Spek({

    describe("RemoveExpiredPowerUpsUseCaseCase") {

        fun executeUseCase(existingPowerUps: List<PowerUp> = listOf()): Player {
            val player = TestUtil.player().copy(
                inventory = TestUtil.player().inventory.setPowerUps(existingPowerUps)
            )
            return RemoveExpiredPowerUpsUseCase(TestUtil.playerRepoMock(player))
                .execute(RemoveExpiredPowerUpsUseCase.Params(currentDate = LocalDate.now()))
        }

        it("should not remove PowerUp expiring today") {
            val i = executeUseCase(
                existingPowerUps = listOf(
                    PowerUp.fromType(PowerUp.Type.TIMER, LocalDate.now())
                )
            ).inventory
            i.isPowerUpEnabled(PowerUp.Type.TIMER).`should be true`()
            i.getPowerUp(PowerUp.Type.TIMER)!!.expirationDate.`should equal`(LocalDate.now())
        }

        it("should remove expired PowerUp") {
            val i = executeUseCase(
                existingPowerUps = listOf(
                    PowerUp.fromType(PowerUp.Type.TIMER, LocalDate.now().minusDays(1))
                )
            ).inventory
            i.powerUps.`should be empty`()
        }
    }
})
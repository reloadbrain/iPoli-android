package io.ipoli.android.player.usecase

import io.ipoli.android.TestUtil
import io.ipoli.android.player.data.Inventory
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.Theme
import io.ipoli.android.store.theme.usecase.ChangeThemeUseCase
import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 12/12/17.
 */
class ChangeThemeUseCaseSpek : Spek({

    describe("ChangeThemeUseCaseSpek") {

        fun executeUseCase(player: Player, theme: Theme) =
            ChangeThemeUseCase(TestUtil.playerRepoMock(player)).execute(theme)


        it("should require theme in inventory") {
            val player = TestUtil.player().copy(
                inventory = Inventory(
                    themes = setOf(Theme.RED)
                )
            )
            val exec = { executeUseCase(player, Theme.BLUE) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should change theme") {
            val currentTheme = Theme.RED
            val newTheme = Theme.BLUE
            val p = TestUtil.player()
            val player = p.copy(
                preferences = p.preferences.copy(theme = currentTheme),
                inventory = Inventory(
                    themes = setOf(currentTheme, newTheme)
                )
            )

            val newPlayer = executeUseCase(player, Theme.BLUE)
            newPlayer.preferences.theme.`should be`(newTheme)
        }
    }
})
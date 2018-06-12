package io.ipoli.android.player.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/6/18.
 */
class SaveProfileUseCase(
    private val playerRepository: PlayerRepository
) : UseCase<SaveProfileUseCase.Params, Player> {

    override fun execute(parameters: Params): Player {
        val player = playerRepository.find()
        requireNotNull(player)
        val displayName =
            if (parameters.displayName.isNullOrBlank()) null else parameters.displayName
        val bio = if (parameters.bio.isNullOrBlank()) null else parameters.bio

        return playerRepository.save(
            player!!.copy(
                displayName = displayName,
                bio = bio
            )
        )
    }

    data class Params(val displayName: String?, val bio: String?)
}
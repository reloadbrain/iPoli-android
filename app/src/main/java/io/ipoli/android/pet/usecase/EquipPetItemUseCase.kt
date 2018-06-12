package io.ipoli.android.pet.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.pet.PetItem
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/13/17.
 */
class EquipPetItemUseCase(private val playerRepository: PlayerRepository) :
    UseCase<EquipPetItemUseCase.Params, Player> {

    override fun execute(parameters: Params): Player {
        val item = parameters.item
        val player = playerRepository.find()
        requireNotNull(player)
        val petAvatar = player!!.pet.avatar
        require(player.inventory.getPet(petAvatar).hasItem(item))

        return playerRepository.save(
            player.copy(
                pet = player.pet.equipItem(item)
            )
        )
    }

    data class Params(val item: PetItem)
}
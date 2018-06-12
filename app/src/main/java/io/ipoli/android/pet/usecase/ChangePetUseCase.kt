package io.ipoli.android.pet.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.pet.PetEquipment
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/6/17.
 */
class ChangePetUseCase(private val playerRepository: PlayerRepository) :
    UseCase<PetAvatar, Player> {
    override fun execute(parameters: PetAvatar): Player {
        val pet = parameters
        val player = playerRepository.find()
        requireNotNull(player)
        require(player!!.hasPet(pet))

        val inventoryPet = player.inventory.getPet(pet)
        val newPlayer = player.copy(
            pet = player.pet.copy(
                name = inventoryPet.name,
                avatar = inventoryPet.avatar,
                equipment = PetEquipment()
            )
        )

        return playerRepository.save(newPlayer)
    }

}
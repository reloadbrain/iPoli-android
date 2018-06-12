package io.ipoli.android.player.usecase

import io.ipoli.android.common.Reward
import io.ipoli.android.common.UseCase
import io.ipoli.android.player.LevelUpScheduler
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.quest.Quest

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 28.11.17.
 */
open class RewardPlayerUseCase(
    private val playerRepository: PlayerRepository,
    private val levelUpScheduler: LevelUpScheduler
) : UseCase<Reward, Player> {

    override fun execute(parameters: Reward): Player {
        val reward = parameters
        requireNotNull(reward.experience)
        requireNotNull(reward.coins)
        val player = playerRepository.find()
        requireNotNull(player)

        val pet = player!!.pet
        val newPet = if (pet.isDead) pet else pet.rewardFor(reward)

        val inventory = player.inventory.let {
            val bounty = reward.bounty
            if (bounty is Quest.Bounty.Food) {
                it.addFood(bounty.food)
            } else {
                it
            }
        }

        val newPlayer = player
            .addExperience(reward.experience)
            .addCoins(reward.coins)
            .copy(
                pet = newPet,
                inventory = inventory
            )
        if (newPlayer.level != player.level) {
            levelUpScheduler.schedule(newPlayer.level)
        }
        playerRepository.save(newPlayer)
        return newPlayer
    }
}
package io.ipoli.android.pet.usecase

import io.ipoli.android.common.UseCase
import io.ipoli.android.pet.Food
import io.ipoli.android.pet.Pet
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.pet.usecase.FeedPetUseCase.FoodReward.*
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/1/17.
 */

sealed class Result {
    data class PetFed(val player: Player, val wasFoodTasty: Boolean) : Result()
    object TooExpensive : Result()
}

data class Parameters(val food: Food)

class FeedPetUseCase(private val playerRepository: PlayerRepository) : UseCase<Parameters, Result> {
    override fun execute(parameters: Parameters): Result {
        val food = parameters.food
        val player = playerRepository.find()
        requireNotNull(player)

        if (player!!.gems < food.price.gems && !player.inventory.hasFood(food)) {
            return Result.TooExpensive
        }

        val (newPet, wasFoodTasty) = feedPet(player.pet, food)

        val newPlayer = playerRepository.save(
            updatePlayer(player, food, newPet)
        )

        return Result.PetFed(newPlayer, wasFoodTasty)
    }

    private fun updatePlayer(player: Player, food: Food, newPet: Pet) =
        if (player.inventory.hasFood(food)) {
            player.copy(
                pet = newPet,
                inventory = player.inventory.removeFood(food)
            )

        } else {
            player.copy(
                pet = newPet,
                gems = player.gems - food.price.gems,
                inventory = player.inventory.addFood(food, food.price.quantity - 1)
            )
        }

    private fun feedPet(pet: Pet, food: Food): FeedPetResult {
        val foodCategory = food.category
        val foodReward = foodRewardFor(foodCategory, pet.avatar.feedingCategory)
        return FeedPetResult(
            pet = pet.updateHealthAndMoodPoints(foodReward.healthPoints, foodReward.moodPoints),
            wasFoodTasty = when (foodReward) {
                is Poop -> false
                is DislikedFood -> false
                else -> true
            }
        )
    }

    private fun foodRewardFor(
        foodCategory: Food.Category,
        feedingCategory: PetAvatar.FeedingCategory
    ) =
        if (isNotHealthy(foodCategory)) {
            when (foodCategory) {
                Food.Category.JUNK -> JunkFood
                Food.Category.CANDY -> CandyFood
                Food.Category.BEER -> Beer
                else -> Poop
            }
        } else {
            if (!DISLIKED_FOOD.containsKey(feedingCategory)) {
                LikedFood
            } else {
                when {
                    DISLIKED_FOOD[feedingCategory]!!.contains(foodCategory) -> DislikedFood
                    else -> LikedFood
                }
            }
        }

    private fun isNotHealthy(foodCategory: Food.Category): Boolean {
        return (foodCategory != Food.Category.MEAT
            && foodCategory != Food.Category.VEGETABLE
            && foodCategory != Food.Category.FRUIT)
    }

    companion object {
        val DISLIKED_FOOD = mapOf(
            PetAvatar.FeedingCategory.HERBIVOROUS to listOf(Food.Category.MEAT),
            PetAvatar.FeedingCategory.CARNIVOROUS to listOf(
                Food.Category.VEGETABLE,
                Food.Category.FRUIT
            )
        )
    }

    sealed class FoodReward(val healthPoints: Int, val moodPoints: Int) {
        object LikedFood : FoodReward(10, 10)
        object DislikedFood : FoodReward(-10, -10)
        object JunkFood : FoodReward(-5, 15)
        object CandyFood : FoodReward(-10, 20)
        object Beer : FoodReward(0, 10)
        object Poop : FoodReward(-10, -10)
    }

    data class FeedPetResult(val pet: Pet, val wasFoodTasty: Boolean)

}
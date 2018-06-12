package io.ipoli.android.pet.usecase

import io.ipoli.android.TestUtil.player
import io.ipoli.android.TestUtil.playerRepoMock
import io.ipoli.android.pet.Food
import io.ipoli.android.pet.Pet
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.pet.usecase.FeedPetUseCase.FoodReward.*
import io.ipoli.android.player.data.Inventory
import io.ipoli.android.player.data.Player
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@io.ipoli.io>
 * on 12/1/17.
 */
class FeedPetUseCaseSpek : Spek({
    describe("FeedPetUseCase") {

        fun executeUseCase(player: Player, food: Food): Result {
            return FeedPetUseCase(playerRepoMock(player)).execute(Parameters(food))
        }

        describe("Player Inventory") {

            it("should not feed pet when not enough gems") {
                val player = player().copy(
                    gems = 0,
                    inventory = Inventory()
                )

                val result = executeUseCase(player, Food.BANANA)
                result.`should be instance of`(Result.TooExpensive::class)
            }

            it("should buy food and feed pet") {
                val player = player().copy(
                    gems = Food.BANANA.price.gems,
                    inventory = Inventory()
                )

                val result = executeUseCase(player, Food.BANANA)
                result.`should be instance of`(Result.PetFed::class)
                val newPlayer = (result as Result.PetFed).player
                newPlayer.gems.`should be equal to`(0)
                newPlayer.inventory.food[Food.BANANA]!!.`should be equal to`(Food.BANANA.price.quantity - 1)
            }

            it("should use food from inventory") {
                val player = player().copy(
                    gems = 10,
                    inventory = Inventory(mapOf(Food.BANANA to 1))
                )

                val result = executeUseCase(player, Food.BANANA)
                result.`should be instance of`(Result.PetFed::class)
                val newPlayer = (result as Result.PetFed).player
                newPlayer.gems.`should be equal to`(player.gems)
                newPlayer.inventory.food.`should be empty`()
            }

            it("should buy new food and not use food from inventory") {
                val player = player().copy(
                    gems = Food.APPLE.price.gems,
                    inventory = Inventory(mapOf(Food.BANANA to 1))
                )

                val result = executeUseCase(player, Food.APPLE)
                result.`should be instance of`(Result.PetFed::class)
                val newPlayer = (result as Result.PetFed).player
                newPlayer.inventory.`should equal`(
                    player.inventory.addFood(
                        Food.APPLE,
                        Food.APPLE.price.quantity - 1
                    )
                )
            }
        }

        describe("Pet Stats") {

            it("should not change stats when they are with max values") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(mapOf(Food.BANANA to 1)),
                        pet = it.pet.copy(
                            moodPoints = Pet.MAX_MP,
                            healthPoints = Pet.MAX_HP
                        )
                    )
                }

                val result = executeUseCase(player, Food.BANANA)
                val pet = (result as Result.PetFed).player.pet
                pet.moodPoints.`should be equal to`(player.pet.moodPoints)
                pet.healthPoints.`should be equal to`(player.pet.healthPoints)
            }

            it("should like fruit when it is herbivorous") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(mapOf(Food.BANANA to 1)),
                        pet = it.pet.copy(
                            avatar = PetAvatar.ELEPHANT,
                            moodPoints = Pet.GOOD_MIN_MOOD_POINTS,
                            healthPoints = (Pet.SICK_CUTOFF + 1).toInt()
                        )
                    )
                }

                val result = executeUseCase(player, Food.BANANA)
                val pet = (result as Result.PetFed).player.pet
                pet.healthPoints.`should be equal to`(player.pet.healthPoints + LikedFood.healthPoints)
                pet.moodPoints.`should be equal to`(player.pet.moodPoints + LikedFood.moodPoints)
            }

            it("should not like meat when it is herbivorous") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(mapOf(Food.STEAK to 1)),
                        pet = it.pet.copy(
                            avatar = PetAvatar.ELEPHANT,
                            moodPoints = Pet.GOOD_MIN_MOOD_POINTS,
                            healthPoints = (Pet.SICK_CUTOFF + 1 - DislikedFood.healthPoints).toInt()
                        )
                    )
                }

                val result = executeUseCase(player, Food.STEAK)
                val pet = (result as Result.PetFed).player.pet
                pet.healthPoints.`should be equal to`(player.pet.healthPoints + DislikedFood.healthPoints)
                pet.moodPoints.`should be equal to`(player.pet.moodPoints + DislikedFood.moodPoints)
            }

            it("should like meat & fruit when it is omnivorous") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(
                            mapOf(
                                Food.STEAK to 1,
                                Food.BANANA to 1
                            )
                        ),
                        pet = it.pet.copy(
                            avatar = PetAvatar.CHICKEN,
                            moodPoints = Pet.GOOD_MIN_MOOD_POINTS,
                            healthPoints = (Pet.SICK_CUTOFF + 1).toInt()
                        )
                    )
                }

                val newPlayer = (executeUseCase(player, Food.STEAK) as Result.PetFed).player
                val result = executeUseCase(newPlayer, Food.BANANA)
                val pet = (result as Result.PetFed).player.pet
                pet.healthPoints.`should be equal to`(player.pet.healthPoints + LikedFood.healthPoints * 2)
                pet.moodPoints.`should be equal to`(player.pet.moodPoints + LikedFood.moodPoints * 2)
            }

            it("should eat junk food") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(mapOf(Food.HOT_DOG to 1)),
                        pet = it.pet.copy(
                            avatar = PetAvatar.ELEPHANT,
                            moodPoints = Pet.GOOD_MIN_MOOD_POINTS,
                            healthPoints = (Pet.SICK_CUTOFF + 1 - JunkFood.healthPoints).toInt()
                        )
                    )
                }

                val result = executeUseCase(player, Food.HOT_DOG)
                val pet = (result as Result.PetFed).player.pet
                pet.healthPoints.`should be equal to`(player.pet.healthPoints + JunkFood.healthPoints)
                pet.moodPoints.`should be equal to`(player.pet.moodPoints + JunkFood.moodPoints)
            }

            it("should die") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(mapOf(Food.POOP to 1)),
                        pet = it.pet.copy(
                            avatar = PetAvatar.ELEPHANT,
                            moodPoints = 1,
                            healthPoints = 1
                        )
                    )
                }

                val result = executeUseCase(player, Food.POOP)
                val pet = (result as Result.PetFed).player.pet
                pet.isDead.`should be true`()
            }

            it("should not like food") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(mapOf(Food.POOP to 1)),
                        pet = it.pet.copy(
                            avatar = PetAvatar.ELEPHANT,
                            moodPoints = 1,
                            healthPoints = 1
                        )
                    )
                }

                val result = executeUseCase(player, Food.POOP)
                (result as Result.PetFed).wasFoodTasty.`should be false`()
            }

            it("should like food") {
                val player = player().let {
                    it.copy(
                        inventory = Inventory(mapOf(Food.BANANA to 1)),
                        pet = it.pet.copy(
                            avatar = PetAvatar.ELEPHANT,
                            moodPoints = 1,
                            healthPoints = 1
                        )
                    )
                }

                val result = executeUseCase(player, Food.BANANA)
                (result as Result.PetFed).wasFoodTasty.`should be true`()
            }
        }
    }
})
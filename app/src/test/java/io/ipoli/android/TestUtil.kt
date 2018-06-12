package io.ipoli.android

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.challenge.persistence.ChallengeRepository
import io.ipoli.android.pet.Pet
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.data.AuthProvider
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.RepeatingQuest
import io.ipoli.android.quest.data.persistence.QuestRepository
import io.ipoli.android.repeatingquest.entity.RepeatPattern
import io.ipoli.android.repeatingquest.persistence.RepeatingQuestRepository
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@io.ipoli.io>
 * on 12/1/17.
 */
object TestUtil {
    fun player() = Player(
        level = 1,
        coins = 10,
        gems = 0,
        username = "",
        displayName = "",
        bio = null,
        experience = 10,
        authProvider = AuthProvider.Guest(""),
        pet = Pet(
            "",
            avatar = PetAvatar.ELEPHANT,
            healthPoints = 30,
            moodPoints = Pet.AWESOME_MIN_MOOD_POINTS - 1
        )
    )

    fun playerRepoMock(player: Player?) = mock<PlayerRepository> {
        on { find() } doReturn player
        on { save(any<Player>()) } doAnswer { invocation ->
            invocation.getArgument(0)
        }
    }

    fun questRepoMock() = mock<QuestRepository> {
        on { save(any<Quest>()) } doAnswer { invocation ->
            invocation.getArgument(0)
        }
    }

    fun repeatingQuestRepoMock() = mock<RepeatingQuestRepository> {
        on { save(any<RepeatingQuest>()) } doAnswer { invocation ->
            invocation.getArgument(0)
        }
    }

    fun challengeRepoMock() = mock<ChallengeRepository> {
        on { save(any<Challenge>()) } doAnswer { invocation ->
            invocation.getArgument(0)
        }
    }

    val firstDateOfWeek: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)
    val lastDateOfWeek: LocalDate = LocalDate.now().with(DayOfWeek.SUNDAY)

    val quest = Quest(
        name = "Test",
        color = Color.BLUE,
        duration = 60,
        scheduledDate = LocalDate.now()
    )

    val repeatingQuest = RepeatingQuest(
        name = "Test",
        repeatPattern = RepeatPattern.Daily(startDate = firstDateOfWeek),
        color = Color.BLUE,
        duration = 60
    )

    val challenge = Challenge(
        name = "Test",
        color = Color.BLUE,
        icon = Icon.STAR,
        difficulty = Challenge.Difficulty.NORMAL,
        startDate = LocalDate.now(),
        endDate = LocalDate.now(),
        motivations = listOf()
    )
}
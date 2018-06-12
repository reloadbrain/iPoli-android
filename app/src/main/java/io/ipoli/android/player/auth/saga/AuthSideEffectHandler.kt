package io.ipoli.android.player.auth.saga

import android.annotation.SuppressLint
import com.google.firebase.auth.*
import io.ipoli.android.BuildConfig
import io.ipoli.android.Constants
import io.ipoli.android.common.AppSideEffectHandler
import io.ipoli.android.common.AppState
import io.ipoli.android.common.LoadDataAction
import io.ipoli.android.common.api.Api
import io.ipoli.android.common.redux.Action
import io.ipoli.android.pet.Pet
import io.ipoli.android.player.data.AuthProvider
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.auth.AuthAction
import io.ipoli.android.player.auth.AuthViewState
import io.ipoli.android.player.auth.UsernameValidator
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.repeatingquest.usecase.SaveRepeatingQuestUseCase
import io.ipoli.android.tag.Tag
import space.traversal.kapsule.required

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/07/2018.
 */
object AuthSideEffectHandler : AppSideEffectHandler() {

    private val eventLogger by required { eventLogger }
    private val playerRepository by required { playerRepository }
    private val tagRepository by required { tagRepository }
    private val sharedPreferences by required { sharedPreferences }
    private val petStatsChangeScheduler by required { lowerPetStatsScheduler }
    private val saveQuestsForRepeatingQuestScheduler by required { saveQuestsForRepeatingQuestScheduler }
    private val removeExpiredPowerUpsScheduler by required { removeExpiredPowerUpsScheduler }
    private val checkMembershipStatusScheduler by required { checkMembershipStatusScheduler }
    private val planDayScheduler by required { planDayScheduler }
    private val updateAchievementProgressScheduler by required { updateAchievementProgressScheduler }
    private val saveRepeatingQuestUseCase by required { saveRepeatingQuestUseCase }
    private val migrationExecutor by required { migrationExecutor }

    override fun canHandle(action: Action) = action is AuthAction

    override suspend fun doExecute(action: Action, state: AppState) {

        when (action) {
            is AuthAction.Load -> {
                val hasPlayer = playerRepository.hasPlayer()
                var isGuest = false
                var hasUsername = false
                if (hasPlayer) {
                    val player = playerRepository.find()
                    isGuest = player!!.authProvider is AuthProvider.Guest
                    hasUsername = !player.username.isNullOrEmpty()
                }
                dispatch(AuthAction.Loaded(hasPlayer, isGuest, hasUsername, action.onboardData))
            }

            is AuthAction.UserAuthenticated -> {
                val user = action.user

                val metadata = user.metadata
                val isNewUser =
                    metadata == null || metadata.creationTimestamp == metadata.lastSignInTimestamp
                val currentPlayerId = sharedPreferences.getString(Constants.KEY_PLAYER_ID, null)
                val hasDevicePlayer = currentPlayerId != null
                when {
                    !isNewUser && hasDevicePlayer -> {
                        //TODO: delete anonymous account
                        savePlayerId(user)

                        val pSchemaVersion = playerRepository.findServerSchemaVersion()!!
                        if (migrationExecutor.shouldMigrate(pSchemaVersion)) {
                            dispatch(
                                AuthAction.ExistingPlayerLoggedInFromGuest(
                                    true,
                                    pSchemaVersion
                                )
                            )
                        } else {
                            dispatch(LoadDataAction.ChangePlayer(currentPlayerId))
                            dispatch(
                                AuthAction.ExistingPlayerLoggedInFromGuest(
                                    false,
                                    pSchemaVersion
                                )
                            )
                        }
                    }
                    isNewUser && hasDevicePlayer -> {
                        updatePlayerAuthProvider(user)
                        dispatch(AuthAction.AccountsLinked)
                    }
                    isNewUser && !hasDevicePlayer -> {
                        createNewPlayer(user, state.stateFor(AuthViewState::class.java))
                    }
                    else -> loginExistingPlayer(user)
                }
            }

            is AuthAction.CompleteSetup -> {

                val username = action.username

                val usernameValidationError =
                    UsernameValidator(playerRepository).validate(username)

                if (usernameValidationError != null) {
                    dispatch(
                        AuthAction.UsernameValidationFailed(
                            usernameValidationError
                        )
                    )
                } else {

                    val player = playerRepository.find()!!
                    playerRepository.save(
                        player.copy(
                            username = action.username,
                            avatar = action.avatar
                        )
                    )
                    playerRepository.addUsername(action.username)
                    prepareAppStart()
                    dispatch(AuthAction.PlayerSetupCompleted)

                    if (!BuildConfig.DEBUG) {
                        val auth = player.authProvider
                        if (auth is AuthProvider.Facebook && auth.email != null) {
                            Api.migratePlayer(player.id, auth.email)
                        } else if (auth is AuthProvider.Google && auth.email != null) {
                            Api.migratePlayer(player.id, auth.email)
                        }
                    }
                }
            }

            is AuthAction.ValidateUsername -> {
                val username = action.username

                val usernameValidationError =
                    UsernameValidator(playerRepository).validate(username)
                if (usernameValidationError != null) {
                    dispatch(
                        AuthAction.UsernameValidationFailed(
                            usernameValidationError
                        )
                    )
                } else {
                    dispatch(AuthAction.UsernameValid)
                }
            }
        }
    }

    private suspend fun updatePlayerAuthProvider(
        user: FirebaseUser
    ) {
        val authProviders =
            user.providerData.filter { it.providerId != FirebaseAuthProvider.PROVIDER_ID }
        require(authProviders.size == 1)
        val authProvider = authProviders.first()

        val auth = when {
            authProvider.providerId == FacebookAuthProvider.PROVIDER_ID ->
                createFacebookAuthProvider(
                    authProvider,
                    user
                )
            authProvider.providerId == GoogleAuthProvider.PROVIDER_ID ->
                createGoogleAuthProvider(
                    authProvider,
                    user
                )
            else -> throw IllegalStateException("Unknown Auth provider")
        }

        val player = playerRepository.find()
        playerRepository.save(
            player!!.copy(
                authProvider = auth
            )
        )
    }

    private fun createGoogleAuthProvider(
        authProvider: UserInfo,
        user: FirebaseUser
    ) =
        AuthProvider.Google(
            userId = authProvider.uid,
            displayName = user.displayName,
            email = user.email,
            imageUrl = user.photoUrl
        )

    private fun createFacebookAuthProvider(
        authProvider: UserInfo,
        user: FirebaseUser
    ) =
        AuthProvider.Facebook(
            userId = authProvider.uid,
            displayName = user.displayName,
            email = user.email,
            imageUrl = user.photoUrl
        )

    private fun createNewPlayer(
        user: FirebaseUser,
        state: AuthViewState
    ) {

        val authProvider = if (user.providerData.size == 1) {
            user.providerData.first()
        } else {
            val authProviders =
                user.providerData.filter { it.providerId != FirebaseAuthProvider.PROVIDER_ID }
            require(authProviders.size == 1)
            authProviders.first()
        }

        val auth = when {

            authProvider.providerId == FacebookAuthProvider.PROVIDER_ID ->
                createFacebookAuthProvider(authProvider, user)

            authProvider.providerId == GoogleAuthProvider.PROVIDER_ID ->
                createGoogleAuthProvider(authProvider, user)

            authProvider.providerId == FirebaseAuthProvider.PROVIDER_ID ->
                AuthProvider.Guest(authProvider.uid)

            else -> throw IllegalStateException("Unknown Auth provider")
        }

        val petAvatar = state.petAvatar ?: Constants.DEFAULT_PET_AVATAR
        val petName =
            if (state.petName.isNullOrBlank()) Constants.DEFAULT_PET_NAME else state.petName!!

        val player = Player(
            authProvider = auth,
            username = null,
            bio = null,
            displayName = if (user.displayName != null) user.displayName!! else "",
            schemaVersion = Constants.SCHEMA_VERSION,
            pet = Pet(petName, petAvatar),
            avatar = state.playerAvatar
        )

        playerRepository.create(player, user.uid)
        savePlayerId(user)

        val tags = saveDefaultTags()

        state.repeatingQuests.forEach {
            val rq = it.first
            val ts = it.second?.let { onboardTag ->
                listOf(tags.first { it.name.toUpperCase() == onboardTag.name })
            } ?: listOf()
            saveRepeatingQuestUseCase.execute(
                SaveRepeatingQuestUseCase.Params(
                    name = rq.name,
                    subQuestNames = rq.subQuests.map { it.name },
                    color = rq.color,
                    icon = rq.icon,
                    tags = ts,
                    startTime = rq.startTime,
                    duration = rq.duration,
                    reminders = rq.reminders,
                    repeatPattern = rq.repeatPattern
                )
            )
        }

        if (auth is AuthProvider.Guest) {
            prepareAppStart()
            dispatch(AuthAction.GuestCreated)
        } else {
            dispatch(AuthAction.ShowSetUp)
        }
    }

    private fun saveDefaultTags() =
        tagRepository.save(
            listOf(
                Tag(
                    name = "Personal",
                    color = Color.ORANGE,
                    icon = Icon.DUCK,
                    isFavorite = true
                ),
                Tag(
                    name = "Work",
                    color = Color.RED,
                    icon = Icon.BRIEFCASE,
                    isFavorite = true
                ),
                Tag(
                    name = "Wellness",
                    color = Color.GREEN,
                    icon = Icon.FLOWER,
                    isFavorite = true
                )
            )
        )

    private fun loginExistingPlayer(user: FirebaseUser) {
        savePlayerId(user)

        val pSchemaVersion = playerRepository.findServerSchemaVersion()!!
        if (migrationExecutor.shouldMigrate(pSchemaVersion)) {
            dispatch(AuthAction.PlayerLoggedIn(true, pSchemaVersion))
        } else {
            dispatch(AuthAction.PlayerLoggedIn(false, pSchemaVersion))
            prepareAppStart()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun savePlayerId(user: FirebaseUser) {
        eventLogger.setPlayerId(user.uid)
        sharedPreferences.edit().putString(Constants.KEY_PLAYER_ID, user.uid).commit()
    }

    private fun prepareAppStart() {
        dispatch(LoadDataAction.All)
        petStatsChangeScheduler.schedule()
        saveQuestsForRepeatingQuestScheduler.schedule()
        removeExpiredPowerUpsScheduler.schedule()
        checkMembershipStatusScheduler.schedule()
        planDayScheduler.schedule()
        updateAchievementProgressScheduler.schedule()
    }
}
package io.ipoli.android.common.navigation

import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import io.ipoli.android.achievement.list.AchievementListViewController
import io.ipoli.android.challenge.add.AddChallengeViewController
import io.ipoli.android.challenge.edit.ChallengeMotivationsDialogController
import io.ipoli.android.challenge.edit.EditChallengeViewController
import io.ipoli.android.challenge.entity.Challenge
import io.ipoli.android.challenge.picker.ChallengePickerDialogController
import io.ipoli.android.common.InviteFriendsDialogController
import io.ipoli.android.common.home.HomeViewController
import io.ipoli.android.common.view.*
import io.ipoli.android.dailychallenge.DailyChallengeViewController
import io.ipoli.android.event.calendar.picker.CalendarPickerDialogController
import io.ipoli.android.note.NotePickerDialogController
import io.ipoli.android.onboarding.OnboardData
import io.ipoli.android.pet.PetViewController
import io.ipoli.android.pet.store.PetStoreViewController
import io.ipoli.android.planday.PlanDayViewController
import io.ipoli.android.planday.RescheduleDialogController
import io.ipoli.android.player.ProfileViewController
import io.ipoli.android.player.auth.AuthViewController
import io.ipoli.android.player.data.Player
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.quest.edit.EditQuestViewController
import io.ipoli.android.quest.reminder.picker.ReminderPickerDialogController
import io.ipoli.android.quest.reminder.picker.ReminderViewModel
import io.ipoli.android.quest.schedule.addquest.AddQuestViewController
import io.ipoli.android.quest.show.QuestDetailViewController
import io.ipoli.android.repeatingquest.edit.picker.RepeatPatternPickerDialogController
import io.ipoli.android.repeatingquest.entity.RepeatPattern
import io.ipoli.android.settings.view.DaysPickerDialogController
import io.ipoli.android.settings.view.TemperatureUnitPickerDialogController
import io.ipoli.android.settings.view.TimeFormatPickerDialogController
import io.ipoli.android.store.avatar.AvatarStoreViewController
import io.ipoli.android.store.gem.GemStoreViewController
import io.ipoli.android.store.membership.MembershipViewController
import io.ipoli.android.store.powerup.PowerUp
import io.ipoli.android.store.powerup.PowerUpStoreViewController
import io.ipoli.android.store.powerup.buy.BuyPowerUpDialogController
import io.ipoli.android.store.theme.ThemeStoreViewController
import io.ipoli.android.tag.Tag
import io.ipoli.android.tag.dialog.TagPickerDialogController
import io.ipoli.android.tag.edit.EditTagViewController
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 05/27/2018.
 */

class Navigator(private val router: Router) {

    fun setHome() {
        setController({ HomeViewController() })
    }

    fun toHome() {
        pushController({ HomeViewController() }, null)
    }

    fun toProfile() {
        pushController({ ProfileViewController() }, VerticalChangeHandler())
    }

    fun setPlanDay() {
        setController({ PlanDayViewController() })
    }

    fun toPlanDay() {
        pushController({ PlanDayViewController() }, VerticalChangeHandler())
    }

    fun setAddQuest(
        closeListener: () -> Unit,
        currentDate: LocalDate?,
        isFullscreen: Boolean = false,
        changeHandler: ControllerChangeHandler? = null
    ) {
        setController(
            { AddQuestViewController(closeListener, currentDate, isFullscreen) },
            changeHandler
        )
    }

    fun setPet(showBackButton: Boolean) {
        setController({ PetViewController(showBackButton = showBackButton) })
    }

    fun toPet(changeHandler: ControllerChangeHandler? = null) {
        pushController({ PetViewController() }, changeHandler)
    }

    fun toAchievementList(changeHandler: ControllerChangeHandler? = null) {
        pushController({ AchievementListViewController() }, changeHandler)
    }

    fun toAuth(onboardData: OnboardData?, changeHandler: ControllerChangeHandler? = null) {
        pushController({ AuthViewController(onboardData) }, changeHandler)
    }

    fun toDailyChallenge() {
        pushController({ DailyChallengeViewController() }, VerticalChangeHandler())
    }

    fun setAuth(onboardData: OnboardData? = null, changeHandler: ControllerChangeHandler? = null) {
        setController({ AuthViewController(onboardData) }, changeHandler)
    }

    fun toEditQuest(
        questId: String?,
        params: EditQuestViewController.Params? = null,
        changeHandler: ControllerChangeHandler? = null
    ) {
        pushController({ EditQuestViewController(questId, params) }, changeHandler)
    }

    fun toAddChallenge() {
        pushController({ AddChallengeViewController() }, VerticalChangeHandler())
    }

    fun toEditTag(tagId: String? = null, changeHandler: ControllerChangeHandler?) {
        pushController({ EditTagViewController(tagId) }, changeHandler)
    }

    fun toEditChallenge(challengeId: String) {
        pushController({ EditChallengeViewController(challengeId) }, FadeChangeHandler())
    }

    fun setQuest(questId: String) {
        setController({ QuestDetailViewController(questId) })
    }

    fun toQuest(questId: String) {
        pushController({ QuestDetailViewController(questId) }, VerticalChangeHandler())
    }

    fun toGemStore(changeHandler: ControllerChangeHandler? = null) {
        pushController({ GemStoreViewController() }, changeHandler)
    }

    fun toMembership(changeHandler: ControllerChangeHandler? = null) {
        pushController({ MembershipViewController() }, changeHandler)
    }

    fun toPowerUpStore(changeHandler: ControllerChangeHandler? = null) {
        pushController({ PowerUpStoreViewController() }, changeHandler)
    }

    fun toAvatarStore(changeHandler: ControllerChangeHandler? = null) {
        pushController({ AvatarStoreViewController() }, changeHandler)
    }

    fun toPetStore(changeHandler: ControllerChangeHandler? = null) {
        pushController({ PetStoreViewController() }, changeHandler)
    }

    fun toThemeStore(changeHandler: ControllerChangeHandler? = null) {
        pushController({ ThemeStoreViewController() }, changeHandler)
    }

    fun toCurrencyConverted() {
        pushDialog { CurrencyConverterDialogController() }
    }

    fun toColorPicker(listener: (Color) -> Unit = {}, selectedColor: Color? = null) {
        pushDialog { ColorPickerDialogController(listener, selectedColor) }
    }

    fun toIconPicker(listener: (Icon?) -> Unit = {}, selectedIcon: Icon? = null) {
        pushDialog { IconPickerDialogController(listener, selectedIcon) }
    }

    fun toBuyPowerUp(powerUp: PowerUp.Type, listener: (BuyPowerUpDialogController.Result) -> Unit) {
        pushDialog { BuyPowerUpDialogController(powerUp, listener) }
    }

    fun toCalendarPicker(listener: (Set<Player.Preferences.SyncCalendar>) -> Unit) {
        pushDialog { CalendarPickerDialogController(listener) }
    }

    fun toChallengeMotivations(
        motivation1: String,
        motivation2: String,
        motivation3: String,
        listener: (String, String, String) -> Unit
    ) {
        pushDialog {
            ChallengeMotivationsDialogController(
                motivation1 = motivation1,
                motivation2 = motivation2,
                motivation3 = motivation3,
                listener = listener
            )
        }
    }

    fun toChallengePicker(challenge: Challenge? = null, listener: (Challenge?) -> Unit) {
        pushDialog { ChallengePickerDialogController(challenge, listener) }
    }

    fun toDaysPicker(
        selectedDays: Set<DayOfWeek>,
        listener: (Set<DayOfWeek>) -> Unit
    ) {
        pushDialog { DaysPickerDialogController(selectedDays, listener) }
    }

    fun toDurationPicker(selectedDuration: Int? = null, listener: (Int) -> Unit) {
        pushDialog { DurationPickerDialogController(selectedDuration, listener) }
    }

    fun toFeedback(listener: FeedbackDialogController.FeedbackListener) {
        pushDialog { FeedbackDialogController(listener) }
    }

    fun toInviteFriends() {
        pushDialog { InviteFriendsDialogController() }
    }

    fun toReminderPicker(
        listener: ReminderPickerDialogController.ReminderPickedListener,
        selectedReminder: ReminderViewModel? = null
    ) {
        pushDialog { ReminderPickerDialogController(listener, selectedReminder) }
    }

    fun toRepeatPatternPicker(
        repeatPattern: RepeatPattern? = null,
        resultListener: (RepeatPattern) -> Unit,
        cancelListener: (() -> Unit)? = null
    ) {
        pushDialog {
            RepeatPatternPickerDialogController(
                repeatPattern,
                resultListener,
                cancelListener
            )
        }
    }

    fun toReschedule(
        includeToday: Boolean,
        listener: (LocalDate?) -> Unit,
        cancelListener: () -> Unit = {}
    ) {
        pushDialog { RescheduleDialogController(includeToday, listener, cancelListener) }
    }

    fun toTagPicker(selectedTags: Set<Tag> = emptySet(), listener: (Set<Tag>) -> Unit) {
        pushDialog { TagPickerDialogController(selectedTags, listener) }
    }

    fun toTemperatureUnitPicker(
        selectedTemperatureUnit: Player.Preferences.TemperatureUnit,
        listener: (Player.Preferences.TemperatureUnit) -> Unit
    ) {
        pushDialog { TemperatureUnitPickerDialogController(selectedTemperatureUnit, listener) }
    }

    fun toTextPicker(
        listener: (String) -> Unit,
        title: String,
        text: String = "",
        hint: String = ""
    ) {
        pushDialog { TextPickerDialogController(listener, title, text, hint) }
    }

    fun toTimeFormatPicker(
        selectedTimeFormat: Player.Preferences.TimeFormat,
        listener: (Player.Preferences.TimeFormat) -> Unit
    ) {
        pushDialog { TimeFormatPickerDialogController(selectedTimeFormat, listener) }
    }

    fun toNotePicker(
        note: String,
        resultListener: (String) -> Unit
    ) {
        pushDialog { NotePickerDialogController(note, resultListener) }
    }

    private inline fun <reified C : Controller> pushDialog(createDialogController: () -> C) {
        val t = tag<C>()
        val c = router.getControllerWithTag(t)
        val changeHandler = SimpleSwapChangeHandler(false)
        if (c == null || c.isBeingDestroyed || c.isDestroyed) {
            router.pushController(createTransaction(createDialogController(), changeHandler, t))
        }
    }

    private inline fun <reified C : Controller> setController(
        createController: () -> C,
        changeHandler: ControllerChangeHandler? = null
    ) {
        val tag = tag<C>()
        val c = router.getControllerWithTag(tag)
        if (c == null || c.isBeingDestroyed || c.isDestroyed) {
            router.setRoot(createTransaction(createController(), changeHandler, tag))
        } else {
            router.setRoot(createTransaction(c, changeHandler, tag))
        }
    }

    private inline fun <reified C : Controller> pushController(
        createController: () -> C,
        changeHandler: ControllerChangeHandler?
    ) {
        val tag = tag<C>()
        val c = router.getControllerWithTag(tag)
        if (c == null || c.isBeingDestroyed || c.isDestroyed) {
            router.pushController(createTransaction(createController(), changeHandler, tag))
        } else {
            router.pushController(createTransaction(c, changeHandler, tag))
        }
    }

    private fun createTransaction(
        controller: Controller,
        changeHandler: ControllerChangeHandler?,
        tag: String
    ) =
        if (changeHandler != null) {
            RouterTransaction.with(controller)
                .pushChangeHandler(changeHandler)
                .popChangeHandler(changeHandler)
                .tag(tag)
        } else {
            RouterTransaction.with(controller)
                .tag(tag)
        }

    companion object {
        inline fun <reified C : Controller> tag(): String = C::class.java.simpleName
    }

}
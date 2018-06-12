package io.ipoli.android.achievement.list

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.ipoli.android.R
import io.ipoli.android.achievement.androidAchievement
import io.ipoli.android.achievement.usecase.CreateAchievementItemsUseCase
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import kotlinx.android.synthetic.main.controller_achievement_list.view.*
import kotlinx.android.synthetic.main.item_achievement.view.*
import kotlinx.android.synthetic.main.item_achievement_list.view.*
import kotlinx.android.synthetic.main.view_default_toolbar.view.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 06/09/2018.
 */
class AchievementListViewController(args: Bundle? = null) :
    ReduxViewController<AchievementListAction, AchievementListViewState, AchievementListReducer>(
        args
    ) {

    override val reducer = AchievementListReducer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = container.inflate(R.layout.controller_achievement_list)
        setToolbar(view.toolbar)
        toolbarTitle = stringRes(R.string.controller_achievement_list_title)

        view.achievementList.layoutManager = LinearLayoutManager(container.context)
        view.achievementList.adapter = AchievementAdapter()

        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        showBackButton()
    }

    override fun onCreateLoadAction() = AchievementListAction.Load

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            router.handleBack()
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun render(state: AchievementListViewState, view: View) {
        when (state.type) {
            AchievementListViewState.StateType.LOADING -> {

            }

            AchievementListViewState.StateType.ACHIEVEMENTS_LOADED -> {
                (view.achievementList.adapter as AchievementAdapter)
                    .updateAll(state.achievementItemViewModels)
            }
        }
    }

    sealed class ItemViewModel(override val id: String) : RecyclerViewViewModel {

        data class SectionItem(val text: String) : ItemViewModel(text)

        data class LockedAchievementViewModel(
            override val id: String,
            @DrawableRes val icon: Int,
            @ColorRes val backgroundColor: Int,
            val name: String,
            val description: String,
            val showProgress: Boolean,
            val showStars: Boolean,
            @DrawableRes val star1: Int,
            @DrawableRes val star2: Int,
            @DrawableRes val star3: Int,
            val progress: Int,
            val maxProgress: Int
        ) : ItemViewModel(id)

        data class UnlockedAchievementViewModel(
            override val id: String,
            @DrawableRes val icon: Int,
            @ColorRes val backgroundColor: Int,
            val name: String,
            val description: String,
            val showStars: Boolean
        ) : ItemViewModel(id)

    }


    enum class ViewType(val value: Int) {
        SECTION(0),
        LOCKED_ACHIEVEMENT(1),
        UNLOCKED_ACHIEVEMENT(2)
    }

    inner class AchievementAdapter :
        MultiViewRecyclerViewAdapter<ItemViewModel>() {

        override fun onRegisterItemBinders() {

            registerBinder<ItemViewModel.SectionItem>(
                ViewType.SECTION.value,
                R.layout.item_list_section,
                { vm, view ->
                    (view as TextView).text = vm.text
                }
            )

            registerBinder<ItemViewModel.LockedAchievementViewModel>(
                ViewType.LOCKED_ACHIEVEMENT.value,
                R.layout.item_achievement_list,
                { vm, view ->
                    view.achievementTitle.text = vm.name
                    view.achievementDesc.text = vm.description
                    view.achievementIcon.setImageResource(vm.icon)
                    view.achievementBackground.backgroundTintList =
                        ColorStateList.valueOf(colorRes(vm.backgroundColor))

                    if (vm.showStars) {
                        view.stars.visible()
                        view.star1.setImageResource(vm.star1)
                        view.star2.setImageResource(vm.star2)
                        view.star3.setImageResource(vm.star3)
                    } else {
                        view.stars.gone()
                    }

                    if (vm.showProgress) {
                        view.achievementProgress.visible()
                        view.achievementProgressText.visible()
                        view.achievementProgress.progress = vm.progress
                        view.achievementProgress.max = vm.maxProgress
                        @SuppressLint("SetTextI18n")
                        view.achievementProgressText.text = "${vm.progress}/${vm.maxProgress}"
                    } else {
                        view.achievementProgress.gone()
                        view.achievementProgressText.gone()
                    }
                }
            )

            registerBinder<ItemViewModel.UnlockedAchievementViewModel>(
                ViewType.UNLOCKED_ACHIEVEMENT.value,
                R.layout.item_achievement_list,
                { vm, view ->
                    view.achievementTitle.text = vm.name
                    view.achievementDesc.text = vm.description
                    view.achievementIcon.setImageResource(vm.icon)
                    view.achievementBackground.backgroundTintList =
                        ColorStateList.valueOf(colorRes(vm.backgroundColor))

                    if (vm.showStars) {
                        view.stars.visible()
                        view.star1.setImageResource(R.drawable.achievement_star)
                        view.star2.setImageResource(R.drawable.achievement_star)
                        view.star3.setImageResource(R.drawable.achievement_star)
                    } else {
                        view.stars.gone()
                    }

                    view.achievementProgress.gone()
                    view.achievementProgressText.gone()
                })
        }

    }

    private val AchievementListViewState.achievementItemViewModels
        get() = achievementListItems.map {
            when (it) {
                is CreateAchievementItemsUseCase.AchievementListItem.UnlockedSection ->
                    ItemViewModel.SectionItem("Unlocked")

                is CreateAchievementItemsUseCase.AchievementListItem.LockedSection ->
                    ItemViewModel.SectionItem("In progress")

                is CreateAchievementItemsUseCase.AchievementListItem.LockedItem -> {
                    val a = it.achievementItem
                    val aa = a.androidAchievement

                    val starsToShow = if (!a.isMultiLevel) -1 else a.currentLevel

                    val star1 =
                        if (starsToShow > 0) R.drawable.achievement_star else R.drawable.achievement_star_empty
                    val star2 =
                        if (starsToShow > 1) R.drawable.achievement_star else R.drawable.achievement_star_empty
                    val star3 =
                        if (starsToShow > 1) R.drawable.achievement_star else R.drawable.achievement_star_empty

                    ItemViewModel.LockedAchievementViewModel(
                        id = stringRes(aa.title),
                        name = stringRes(aa.title),
                        icon = if (aa.isHidden) R.drawable.ic_question_mark_yellow_24dp else aa.icon,
                        backgroundColor = if (aa.isHidden) R.color.md_grey_500 else aa.color,
                        description = if (aa.isHidden) stringRes(R.string.hidden_achievement_desc) else stringRes(aa.levelDescriptions[a.currentLevel + 1]!!),
                        showStars = a.isMultiLevel,
                        star1 = star1,
                        star2 = star2,
                        star3 = star3,
                        showProgress = a.hasProgress,
                        progress = a.progress,
                        maxProgress = a.progressForNextLevel
                    )
                }

                is CreateAchievementItemsUseCase.AchievementListItem.UnlockedItem -> {
                    val a = it.achievementItem
                    val aa = a.androidAchievement

                    ItemViewModel.UnlockedAchievementViewModel(
                        id = stringRes(aa.title),
                        name = stringRes(aa.title),
                        icon = aa.icon,
                        backgroundColor = aa.color,
                        description = stringRes(aa.levelDescriptions[a.currentLevel]!!),
                        showStars = a.isMultiLevel
                    )
                }
            }
        }

}
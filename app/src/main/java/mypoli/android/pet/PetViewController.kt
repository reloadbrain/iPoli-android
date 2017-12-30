package mypoli.android.pet

import android.animation.*
import android.app.WallpaperManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import kotlinx.android.synthetic.main.controller_pet.view.*
import kotlinx.android.synthetic.main.item_pet_food.view.*
import kotlinx.android.synthetic.main.item_pet_item.view.*
import kotlinx.android.synthetic.main.view_inventory_toolbar.view.*
import mypoli.android.R
import mypoli.android.common.ViewUtils
import mypoli.android.common.mvi.MviViewController
import mypoli.android.common.view.*
import mypoli.android.pet.PetViewState.StateType.*
import mypoli.android.pet.store.PetStoreViewController
import space.traversal.kapsule.required


/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 11/24/17.
 */
class PetViewController(args: Bundle? = null) : MviViewController<PetViewState, PetViewController, PetPresenter, PetIntent>(args) {

    private val presenter by required { petPresenter }

    override fun createPresenter() = presenter

    private lateinit var inventoryToolbar: ViewGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.controller_pet, container, false)
        setHasOptionsMenu(true)

        view.fabFood.setImageDrawable(
            IconicsDrawable(view.context)
                .icon(Ionicons.Icon.ion_pizza)
                .colorRes(R.color.md_white)
                .sizeDp(24)
        )

        view.fabItems.setImageDrawable(
            IconicsDrawable(view.context)
                .icon(Ionicons.Icon.ion_tshirt)
                .colorRes(R.color.md_white)
                .sizeDp(24)
        )

        val initList: (RecyclerView) -> Unit = {
            it.layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.HORIZONTAL, false)
            it.post {
                it.x = view.width.toFloat()
            }
        }

        initList(view.itemList)
        initList(view.foodList)

        inventoryToolbar = addToolbarView(R.layout.view_inventory_toolbar) as ViewGroup
        inventoryToolbar.playerGems.setOnClickListener {
            send(PetIntent.ShowCurrencyConverter)
        }

        return view
    }

    override fun onAttach(view: View) {
        showBackButton()
        super.onAttach(view)
        send(LoadDataIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.pet_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            router.popCurrentController()
            return true
        }

        if (item.itemId == R.id.actionStore) {
            val handler = FadeChangeHandler()
            router.pushController(
                RouterTransaction.with(PetStoreViewController())
                    .pushChangeHandler(handler)
                    .popChangeHandler(handler)
            )
            return true
        }

        if (item.itemId == R.id.actionRenamePet) {
            send(RenamePetRequestIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        val PET_TOP_BORDER_PERCENT = 0.33f
        val PET_BOTTOM_BORDER_PERCENT = 0.74f
    }

    override fun render(state: PetViewState, view: View) {
        when (state.type) {
            DATA_LOADED -> {
                view.fabFood.sendOnClick(ShowFoodListIntent)
                view.fabItems.sendOnClick(PetIntent.ShowItemList)
                renderPet(state, view)

                view.post {
                    resizePet(view)
                    view.post {
                        if (!state.isDead) {
                            playEnterAnimation(view)
                        }
                    }
                }

                inventoryToolbar.playerGems.text = state.playerGems.toString()

                view.itemList.adapter = PetItemAdapter(state.itemViewModels)
                view.foodList.adapter = PetFoodAdapter(state.foodViewModels)
            }
            FOOD_LIST_SHOWN -> {
                view.fabItems.isClickable = false
                playShowListAnimation(view, view.foodList, view.fabFood, view.fabItems)

                view.fabFood.setImageResource(R.drawable.ic_close_white_24dp)
                view.fabFood.sendOnClick(HideFoodListIntent)
            }

            FOOD_LIST_HIDDEN -> {
                resetFoodList(view)
            }

            PET_FED -> {
                val responseRes = if (state.wasFoodTasty)
                    R.string.pet_tasty_food_response
                else
                    R.string.pet_not_tasty_food_response
                view.petResponse.setText(responseRes)
                val avatar = AndroidPetAvatar.valueOf(state.avatar!!.name)
                if (state.isDead) {
                    view.petState.setImageResource(avatar.deadStateImage)
                } else {
                    view.petState.setImageResource(avatar.moodImage[state.mood]!!)
                }
                playFeedPetAnimation(view, state)
            }

            FOOD_TOO_EXPENSIVE -> {
                CurrencyConverterDialogController().showDialog(router, "currency-converter")
                Toast.makeText(view.context, stringRes(R.string.food_too_expensive), Toast.LENGTH_SHORT).show()
            }

            PET_CHANGED -> {

                inventoryToolbar.playerGems.text = state.playerGems.toString()

                (view.foodList.adapter as PetFoodAdapter).updateAll(state.foodViewModels)
                (view.itemList.adapter as PetItemAdapter).updateAll(state.itemViewModels)

                renderItemComparison(state, view)
                renderPet(state, view)
            }

            RENAME_PET ->
                TextPickerDialogController({ text ->
                    send(RenamePetIntent(text))
                }, stringRes(R.string.dialog_rename_pet_title), state.petName, hint = stringRes(R.string.dialog_rename_pet_hint))
                    .showDialog(router, "text-picker-tag")

            PET_RENAMED ->
                renderPetName(state.petName)

            REVIVE_TOO_EXPENSIVE -> {
                Toast.makeText(view.context, "Revive too expensive", Toast.LENGTH_SHORT).show()
            }

            PET_REVIVED -> {
                setMenusVisibility(view, true)
            }

            SHOW_CURRENCY_CONVERTER -> {
                CurrencyConverterDialogController().showDialog(router, "currency-converter")
            }

            ITEM_LIST_SHOWN -> {
                view.itemList.adapter = PetItemAdapter(state.itemViewModels)
                view.fabFood.isClickable = false
                playShowListAnimation(view, view.itemList, view.fabItems, view.fabFood)
                playItemFabsAnimation(view)
                view.fabItems.setImageResource(R.drawable.ic_close_white_24dp)
                view.fabItems.sendOnClick(PetIntent.HideItemList)

                renderItemCategoryFabs(view, state)

                view.fabHatItems.sendOnClick(PetIntent.ShowHeadItemList)
                view.fabMaskItems.sendOnClick(PetIntent.ShowFaceItemList)
                view.fabBodyArmorItems.sendOnClick(PetIntent.ShowBodyItemList)

                playCardContainerChangeAnimation(view, view.compareItemsContainer)

                renderItemComparison(state, view)
            }

            ITEM_LIST_HIDDEN -> {
                resetItemList(view)
                playCardContainerChangeAnimation(view, view.statsContainer)
                renderEquippedPetItems(state, view)
            }

            COMPARE_ITEMS -> {
                (view.itemList.adapter as PetItemAdapter).updateAll(state.itemViewModels)
                renderItemComparison(state, view)
            }

            CHANGE_ITEM_CATEGORY -> {
                renderItemCategoryFabs(view, state)

                (view.itemList.adapter as PetItemAdapter).updateAll(state.itemViewModels)
                renderItemComparison(state, view)
            }

            ITEM_TOO_EXPENSIVE -> {
                CurrencyConverterDialogController().showDialog(router, "currency-converter")
                Toast.makeText(view.context, stringRes(R.string.pet_item_too_expensive), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setMenusVisibility(view: View, visible: Boolean) {
        listOf(
            view.fabFood,
            view.fabItems,
            view.foodList,
            view.itemList,
            view.fabHatItems,
            view.fabMaskItems,
            view.fabBodyArmorItems
        ).forEach {
            it.visible = visible
        }
    }

    private fun resetItemList(view: View) {
        view.fabFood.isClickable = true
        val heightOffset = (view.fabItems.height + ViewUtils.dpToPx(16f, view.context)) * 2
        playHideListAnimation(view, view.itemList, view.fabItems, view.fabFood, heightOffset)
        playItemFabsAnimation(view, true)
        view.fabFood.setImageDrawable(
            IconicsDrawable(view.context)
                .icon(Ionicons.Icon.ion_tshirt)
                .colorRes(R.color.md_white)
                .sizeDp(24)
        )
        view.fabItems.sendOnClick(PetIntent.ShowItemList)
    }

    private fun resetFoodList(view: View) {
        view.fabItems.isClickable = true

        val heightOffset = view.fabFood.height + ViewUtils.dpToPx(16f, view.context)

        playHideListAnimation(view, view.foodList, view.fabFood, view.fabItems, heightOffset)
        view.fabFood.setImageDrawable(
            IconicsDrawable(view.context)
                .icon(Ionicons.Icon.ion_pizza)
                .colorRes(R.color.md_white)
                .sizeDp(24)
        )
        view.fabFood.sendOnClick(ShowFoodListIntent)
    }

    private fun playCardContainerChangeAnimation(view: View, showView: View) {
        val containerHeight = view.cardContainer.height.toFloat()

        val widthSpec = View.MeasureSpec.makeMeasureSpec(showView.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(showView.height, View.MeasureSpec.EXACTLY)
        showView.measure(widthSpec, heightSpec)
        val showViewHeight = showView.measuredHeight

        val slideUpAnim = ObjectAnimator.ofFloat(view.cardContainer, "y", 0f, -containerHeight)
        slideUpAnim.duration = shortAnimTime

        val slideDownAnim = ObjectAnimator.ofFloat(view.cardContainer, "y", -showViewHeight.toFloat(), 0f)
        slideDownAnim.duration = shortAnimTime

        slideDownAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                listOf(view.reviveContainer, view.statsContainer, view.compareItemsContainer)
                    .filter { it != showView }
                    .forEach { it.visibility = View.GONE }
                showView.visibility = View.VISIBLE
            }
        })

        val anim = AnimatorSet()
        anim.playSequentially(slideUpAnim, slideDownAnim)
        anim.start()
    }

    private fun renderItemCategoryFabs(view: View, state: PetViewState) {
        view.fabHatItems.backgroundTintList = ColorStateList.valueOf(attr(R.attr.colorPrimary))
        view.fabMaskItems.backgroundTintList = ColorStateList.valueOf(attr(R.attr.colorPrimary))
        view.fabBodyArmorItems.backgroundTintList = ColorStateList.valueOf(attr(R.attr.colorPrimary))

        val itemsType = state.comparedItemsType!!

        when (itemsType) {
            PetItemType.HAT -> view.fabHatItems.backgroundTintList = ColorStateList.valueOf(attr(R.attr.colorAccent))
            PetItemType.MASK -> view.fabMaskItems.backgroundTintList = ColorStateList.valueOf(attr(R.attr.colorAccent))
            PetItemType.BODY_ARMOR -> view.fabBodyArmorItems.backgroundTintList = ColorStateList.valueOf(attr(R.attr.colorAccent))
        }
    }

    private fun renderItemComparison(state: PetViewState, view: View) {

        if (state.equippedItem != null) {
            state.equippedItem.let {
                view.curItems.visible = true
                view.noItem.visible = false
                view.curItemImage.setImageResource(it.image)
                view.curItemName.setText(it.name)

                renderItemBonus(
                    it.coinBonus,
                    it.coinBonusChange,
                    view.curItemCoinBonus
                )

                renderItemBonus(
                    it.xpBonus,
                    it.xpBonusChange,
                    view.curItemXpBonus
                )

                renderItemBonus(
                    it.bountyBonus,
                    it.bountyBonusChange,
                    view.curItemBountyBonus
                )

                view.takeOff.sendOnClick(PetIntent.TakeItemOff(it.item))
            }
        } else {

            view.curItems.visibility = View.INVISIBLE
            view.noItem.visibility = View.VISIBLE
        }

        state.newItem?.let {
            view.newItemImage.setImageResource(it.image)
            view.newItemName.setText(it.name)

            if (it.isEquipped) {
                view.equipItem.visible = false
                view.buyItem.visible = false
            } else if (it.isBought) {
                view.equipItem.visible = true
                view.buyItem.visible = false
                view.equipItem.sendOnClick(PetIntent.EquipItem(state.newItem.item))
            } else {
                view.equipItem.visible = false
                view.buyItem.visible = true
                view.buyItem.sendOnClick(PetIntent.BuyItem(state.newItem.item))
            }

            renderNewPetItems(state, view)

            renderItemBonus(
                it.coinBonus,
                it.coinBonusChange,
                view.newItemCoinBonus
            )

            renderItemBonus(
                it.xpBonus,
                it.xpBonusChange,
                view.newItemXpBonus
            )

            renderItemBonus(
                it.bountyBonus,
                it.bountyBonusChange,
                view.newItemBountyBonus
            )
        }

        state.itemComparison?.let {
            renderItemChangeResult(it.coinBonusDiff, it.coinBonusChange, view.newCoinBonusDiff)
            renderItemChangeResult(it.xpBonusDiff, it.xpBonusChange, view.newXPBonusDiff)
            renderItemChangeResult(it.bountyBonusDiff, it.bountyBonusChange, view.newBountyBonusDiff)
        }
    }

    private fun renderItemBonus(bonus: Int, changeType: ItemComparisonViewModel.Change, bonusView: TextView) {
        when (changeType) {
            ItemComparisonViewModel.Change.POSITIVE -> {
                bonusView.text = "+$bonus%"
            }
            ItemComparisonViewModel.Change.NEGATIVE -> {
                bonusView.text = "$bonus%"
            }
            ItemComparisonViewModel.Change.NO_CHANGE -> {
                bonusView.text = "$bonus%"
            }
        }
    }

    private fun renderItemChangeResult(change: Int, changeType: ItemComparisonViewModel.Change, changeView: TextView) {

        when (changeType) {
            ItemComparisonViewModel.Change.POSITIVE -> {
                changeView.text = "+$change"
                changeView.setTextColor(colorRes(R.color.md_green_700))
                changeView.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    IconicsDrawable(changeView.context)
                        .icon(Ionicons.Icon.ion_arrow_up_b)
                        .colorRes(R.color.md_green_500)
                        .sizeDp(12),
                    null)
            }
            ItemComparisonViewModel.Change.NEGATIVE -> {
                changeView.text = change.toString()
                changeView.setTextColor(colorRes(R.color.md_red_700))
                changeView.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    IconicsDrawable(changeView.context)
                        .icon(Ionicons.Icon.ion_arrow_down_b)
                        .colorRes(R.color.md_red_500)
                        .sizeDp(12),
                    null)
            }
            ItemComparisonViewModel.Change.NO_CHANGE -> {
                changeView.text = "$change="
                changeView.setTextColor(colorRes(R.color.md_dark_text_87))
                changeView.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    null,
                    null)
            }
        }
    }

    private fun createPopupAnimator(view: View, reverse: Boolean = false): Animator {
        val anim = if (reverse) R.animator.popout else R.animator.popup
        val animator = AnimatorInflater.loadAnimator(view.context, anim)
        animator.setTarget(view)
        animator.interpolator = OvershootInterpolator()
        animator.duration = shortAnimTime
        return animator
    }

    private fun playItemFabsAnimation(view: View, reverse: Boolean = false) {
        val anims = listOf<FloatingActionButton>(
            view.fabBodyArmorItems,
            view.fabMaskItems,
            view.fabHatItems)
            .map { createPopupAnimator(it, reverse) }

        val anim = AnimatorSet()
        if (reverse) anim.playSequentially(anims.reversed())
        else anim.playSequentially(anims)
        anim.start()
    }

    private fun setPetAsWallpaper(view: View) {
        val b = loadBitmapFromView(view)

        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        val desiredWidth = wallpaperManager.desiredMinimumWidth
        val desiredHeight = wallpaperManager.desiredMinimumHeight

        val originalWidth = b.width
        val originalHeight = b.height

        val hScaleFactor = originalWidth.toFloat() / desiredWidth.toFloat()
        val vScaleFactor = originalHeight.toFloat() / desiredHeight.toFloat()

        val scaleFactor = Math.max(hScaleFactor, vScaleFactor)

        val wallWidth = originalWidth / scaleFactor
        val wallHeight = originalHeight / scaleFactor

        val wallpaperBmp = Bitmap.createScaledBitmap(
            b,
            wallWidth.toInt(),
            wallHeight.toInt(),
            true
        )

        b.recycle()

        wallpaperManager.setBitmap(wallpaperBmp)

        wallpaperBmp.recycle()
    }

    private fun resizePet(view: View) {
        val petView = view.pet
        val hatView = view.hat

        val viewHeight = view.height
        val bottomY = PET_BOTTOM_BORDER_PERCENT * viewHeight
        val topY = PET_TOP_BORDER_PERCENT * viewHeight

        val newPetHeight = bottomY - topY
        val originalPetHeight = petView.height
        val scale = newPetHeight / originalPetHeight

        if (scale < 1) {
            val lp = petView.layoutParams as ConstraintLayout.LayoutParams
            lp.height = newPetHeight.toInt()
            lp.width = (petView.width * scale).toInt()
            lp.verticalBias = PET_BOTTOM_BORDER_PERCENT
            petView.layoutParams = lp

            val hatDrawable = view.context.resources.getDrawable(R.drawable.pet_3_item_head_christmas_horns, null)
            val originalHatHeight = hatDrawable.intrinsicHeight
            val newHatHeight = originalHatHeight * scale

            val hlp = hatView.layoutParams as ConstraintLayout.LayoutParams
            hlp.height = newHatHeight.toInt()
            hlp.width = (hatView.width * scale).toInt()
            hatView.layoutParams = hlp
        } else {
            val lp = petView.layoutParams as ConstraintLayout.LayoutParams
            lp.verticalBias = PET_BOTTOM_BORDER_PERCENT
            petView.layoutParams = lp
        }
    }

    private fun playEnterAnimation(view: View) {
        val anims = listOf<ImageView>(
            view.pet,
            view.petState,
            view.bodyArmor,
            view.mask,
            view.hat)
            .map {
                ObjectAnimator.ofFloat(it, "y",
                    it.y, it.y - ViewUtils.dpToPx(30f, view.context), it.y, it.y - ViewUtils.dpToPx(24f, view.context), it.y)
            }

        val set = AnimatorSet()
        set.duration = intRes(android.R.integer.config_longAnimTime).toLong() + 100
        set.playTogether(anims)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()
    }

    private fun renderPet(state: PetViewState, view: View) {
        renderPetName(state.petName)

        val avatar = AndroidPetAvatar.valueOf(state.avatar!!.name)

        view.pet.setImageResource(avatar.image)
        renderEquippedPetItems(state, view)

        if (state.isDead) {
            view.reviveContainer.visibility = View.VISIBLE
            view.compareItemsContainer.visibility = View.GONE
            view.statsContainer.visibility = View.GONE
            view.petState.setImageResource(avatar.deadStateImage)

            view.reviveHint.text = stringRes(R.string.revive_hint, state.petName)
            view.reviveCost.text = state.reviveCost.toString()

            setMenusVisibility(view, false)
            resetFoodList(view)
            resetItemList(view)

            ViewUtils.goneViews(view.hat, view.mask, view.bodyArmor)

            view.revive.sendOnClick(RevivePetIntent)
        } else if (state.comparedItemsType == null) {
            view.statsContainer.visibility = View.VISIBLE
            view.reviveContainer.visibility = View.GONE
            view.compareItemsContainer.visibility = View.GONE
            playProgressAnimation(view.healthProgress, view.healthProgress.progress, state.hp)
            view.healthPoints.text = state.hp.toString() + "/" + state.maxHP
            view.healthProgress.max = state.maxHP

            playProgressAnimation(view.moodProgress, view.moodProgress.progress, state.mp)
            view.moodPoints.text = state.mp.toString() + "/" + state.maxMP
            view.moodProgress.max = state.maxMP

            view.coinBonus.text = "+ %.2f".format(state.coinsBonus) + "%"
            view.xpBonus.text = "+ %.2f".format(state.xpBonus) + "%"
            view.unlockChanceBonus.text = "+ %.2f".format(state.unlockChanceBonus) + "%"
            view.petState.setImageResource(avatar.moodImage[state.mood]!!)
            ViewUtils.showViews(view.hat, view.mask, view.bodyArmor)
        } else {
            ViewUtils.showViews(view.hat, view.mask, view.bodyArmor)
        }

        view.stateName.text = state.stateName
    }

    private fun renderEquippedPetItems(state: PetViewState, view: View) {
        val setItem: (ImageView, EquipmentItemViewModel?) -> Unit = { iv, vm ->
            if (vm == null) iv.setImageDrawable(null)
            else iv.setImageResource(vm.image)
        }
        setItem(view.hat, state.equippedHatItem)
        setItem(view.mask, state.equippedMaskItem)
        setItem(view.bodyArmor, state.equippedBodyArmorItem)
    }

    private fun renderNewPetItems(state: PetViewState, view: View) {
        val setItem: (ImageView, Int?) -> Unit = { iv, image ->
            if (image == null) iv.setImageDrawable(null)
            else iv.setImageResource(image)
        }
        setItem(view.hat, state.newHatItemImage)
        setItem(view.mask, state.newMaskItemImage)
        setItem(view.bodyArmor, state.newBodyArmorItemImage)
    }

    private fun renderPetName(name: String) {
        inventoryToolbar.toolbarTitle.text = name
    }

    private fun playFeedPetAnimation(view: View, state: PetViewState) {
        val selectedFood = view.selectedFood
        val duration = 300L
        val slideAnim = ObjectAnimator.ofFloat(selectedFood, "x", 0f,
            (view.width / 2 - selectedFood.width).toFloat())
        val fadeAnim = ObjectAnimator.ofFloat(selectedFood, "alpha", 1f, 0f)
        fadeAnim.startDelay = duration / 2

        val anim = AnimatorSet()
        anim.playTogether(slideAnim, fadeAnim)
        anim.duration = duration
        anim.interpolator = AccelerateDecelerateInterpolator()

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                selectedFood.setImageResource(state.food!!.image)
                selectedFood.alpha = 1f
            }

            override fun onAnimationEnd(animation: Animator?) {
                if (state.isDead) {
                    return
                }
                val avatar = AndroidPetAvatar.valueOf(state.avatar!!.name)
                val stateImage = avatar.moodImage[state.mood]!!
                val responseStateImage = if (state.wasFoodTasty)
                    avatar.moodImage[PetMood.AWESOME]!!
                else
                    avatar.deadStateImage
                playFeedPetResponseAnimation(view, stateImage, responseStateImage)
            }
        })
        anim.start()
    }

    private fun playProgressAnimation(view: ProgressBar, from: Int, to: Int) {
        val animator = ObjectAnimator.ofInt(view, "progress", from, to)
        animator.duration = intRes(android.R.integer.config_shortAnimTime).toLong()
        animator.start()
    }

    private fun playFeedPetResponseAnimation(view: View, @DrawableRes currentStateImage: Int, @DrawableRes responseStateImage: Int) {
        val anim = AnimatorSet()
        anim.playSequentially(
            createShowPetResponseAnimation(view, responseStateImage),
            createHidePetResponseAnimation(view, currentStateImage),
            createShowPetResponseAnimation(view, responseStateImage),
            createHidePetResponseAnimation(view, currentStateImage)
        )
        anim.start()
    }

    private fun createHidePetResponseAnimation(view: View, @DrawableRes petStateImage: Int): Animator {
        val anim = ObjectAnimator.ofFloat(view.petState, "alpha", 0f, 1f)
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                view.petState.setImageResource(petStateImage)
                view.petResponse.alpha = 0f
            }
        })
        anim.duration = 100
        anim.startDelay = 300
        return anim
    }

    private fun createShowPetResponseAnimation(view: View, @DrawableRes petAwesomeStateImage: Int): Animator {
        val anim = ObjectAnimator.ofFloat(view.petState, "alpha", 1f, 0f)
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                view.petState.setImageResource(petAwesomeStateImage)
                view.petState.alpha = 1f
                view.petResponse.alpha = 1f
            }
        })
        anim.duration = 100
        anim.startDelay = 300
        return anim
    }

    private fun playHideListAnimation(view: View, listView: View, moveFAB: FloatingActionButton, showFAB: FloatingActionButton, heightOffset: Float) {
        val listAnim = AnimatorSet()
        listAnim.playTogether(
            ObjectAnimator.ofFloat(listView, "alpha", 1f, 0f),
            ObjectAnimator.ofFloat(listView, "x", 0f, view.width.toFloat())
        )
        val animator = AnimatorSet()

        animator.playSequentially(
            listAnim,
            ObjectAnimator.ofFloat(moveFAB, "y", moveFAB.y, view.height - heightOffset),
            ObjectAnimator.ofFloat(showFAB, "alpha", 0f, 1f)
        )
        animator.duration = intRes(android.R.integer.config_shortAnimTime).toLong()
        animator.start()
    }

    private fun playShowListAnimation(view: View, listView: View, moveView: View, hideView: View) {
        val listAnim = AnimatorSet()
        listAnim.playTogether(
            ObjectAnimator.ofFloat(listView, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(listView, "x", view.width.toFloat(), 0f)
        )
        val animator = AnimatorSet()
        animator.playSequentially(
            ObjectAnimator.ofFloat(hideView, "alpha", 1f, 0f),
            ObjectAnimator.ofFloat(moveView, "y", moveView.y, view.itemList.y - moveView.height - ViewUtils.dpToPx(8f, view.context)),
            listAnim
        )
        animator.duration = intRes(android.R.integer.config_shortAnimTime).toLong()
        animator.start()
    }

    override fun onDestroyView(view: View) {
        removeToolbarView(inventoryToolbar)
        super.onDestroyView(view)
    }

    fun loadBitmapFromView(v: View): Bitmap {
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    data class PetFoodViewModel(@DrawableRes val image: Int, val price: Food.Price, val food: Food, val quantity: Int = 0)

    inner class PetFoodAdapter(private var foodItems: List<PetFoodViewModel>) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount() = foodItems.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val vm = foodItems[position]
            holder.itemView.foodImage.setImageResource(vm.image)

            val foodPrice = holder.itemView.foodPrice
            if (vm.quantity > 0) {
                foodPrice.text = "x" + vm.quantity.toString()
                foodPrice.setCompoundDrawables(null, null, null, null)
            } else {
                foodPrice.text = "= x${vm.price.quantity}"
                foodPrice.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_gem_16dp),
                    null, null, null)
            }
            holder.itemView.setOnClickListener {
                send(FeedIntent(vm.food))
            }
        }

        fun updateAll(foodItems: List<PetFoodViewModel>) {
            this.foodItems = foodItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_pet_food, parent, false))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    data class PetItemViewModel(
        @DrawableRes val image: Int,
        val gemPrice: Int,
        val item: PetItem,
        val isSelected: Boolean = false,
        val isBought: Boolean = false,
        val isEquipped: Boolean = false
    )

    inner class PetItemAdapter(private var petItems: List<PetItemViewModel>) : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_pet_item, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val vm = petItems[position]
            holder.itemView.itemImage.setImageResource(vm.image)

            if (vm.isSelected) {
                holder.itemView.setBackgroundColor(colorRes(R.color.md_grey_200))
            } else {
                holder.itemView.background = null
            }

            val price = holder.itemView.itemPrice
            when {
                vm.isEquipped -> {
                    price.setCompoundDrawablesWithIntrinsicBounds(
                        IconicsDrawable(holder.itemView.context)
                            .icon(Ionicons.Icon.ion_android_done)
                            .color(attr(R.attr.colorAccent))
                            .sizeDp(16),
                        null, null, null
                    )
                    price.text = ""

                }
                vm.isBought -> {
                    price.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                    price.text = ""
                }
                else -> {
                    price.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(price.context, R.drawable.ic_gem_16dp), null, null, null
                    )
                    price.text = vm.gemPrice.toString()
                }
            }

            holder.itemView.sendOnClick(PetIntent.CompareItem(vm.item))
        }

        override fun getItemCount() = petItems.size

        fun updateAll(petItems: List<PetItemViewModel>) {
            this.petItems = petItems
            notifyDataSetChanged()
        }
    }

    data class EquipmentItemViewModel(
        @DrawableRes val image: Int,
        val item: PetItem
    )

    data class ItemComparisonViewModel(
        val coinBonusDiff: Int,
        val coinBonusChange: Change,
        val xpBonusDiff: Int,
        val xpBonusChange: Change,
        val bountyBonusDiff: Int,
        val bountyBonusChange: Change
    ) {
        enum class Change { POSITIVE, NEGATIVE, NO_CHANGE }
    }

    data class CompareItemViewModel(
        @DrawableRes val image: Int,
        @StringRes val name: Int,
        val item: PetItem,
        val coinBonus: Int,
        val coinBonusChange: ItemComparisonViewModel.Change,
        val xpBonus: Int,
        val xpBonusChange: ItemComparisonViewModel.Change,
        val bountyBonus: Int,
        val bountyBonusChange: ItemComparisonViewModel.Change,
        val isBought: Boolean,
        val isEquipped: Boolean
    )
}
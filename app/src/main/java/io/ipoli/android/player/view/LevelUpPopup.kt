package io.ipoli.android.player.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.common.mvi.BaseMviPresenter
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.mvi.Intent
import io.ipoli.android.common.mvi.ViewStateRenderer
import io.ipoli.android.common.view.MviPopup
import io.ipoli.android.common.view.anim.TypewriterTextAnimator
import io.ipoli.android.common.view.visible
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import kotlinx.android.synthetic.main.popup_level_up.view.*
import kotlinx.coroutines.experimental.launch
import space.traversal.kapsule.required
import kotlin.coroutines.experimental.CoroutineContext


data class LevelUpViewState(
    val level: Int? = null,
    val avatar: PetAvatar? = null
) : BaseViewState()

sealed class LevelUpIntent : Intent {
    data class LoadData(val newLevel: Int) : LevelUpIntent()
    data class ChangePlayer(val player: Player) : LevelUpIntent()
}

class LevelUpPresenter(
    private val playerRepository: PlayerRepository,
    coroutineContext: CoroutineContext
) : BaseMviPresenter<ViewStateRenderer<LevelUpViewState>, LevelUpViewState, LevelUpIntent>(
    LevelUpViewState(),
    coroutineContext
) {
    override fun reduceState(intent: LevelUpIntent, state: LevelUpViewState) =
        when (intent) {
            is LevelUpIntent.LoadData -> {
                launch {
                    sendChannel.send(LevelUpIntent.ChangePlayer(playerRepository.find()!!))
                }
                state.copy(
                    level = intent.newLevel
                )
            }

            is LevelUpIntent.ChangePlayer -> {
                val player = intent.player
                state.copy(
                    avatar = player.pet.avatar
                )
            }
        }

}

class LevelUpPopup(private val newLevel: Int) :
    MviPopup<LevelUpViewState, LevelUpPopup, LevelUpPresenter, LevelUpIntent>() {

    private val presenter by required { levelUpPresenter }

    private val eventLogger by required { eventLogger }

    override fun createPresenter() = presenter

    override fun render(state: LevelUpViewState, view: View) {
        state.avatar?.let {
            val androidAvatar = AndroidPetAvatar.valueOf(it.name)
            view.pet.setImageResource(androidAvatar.headImage)
            val params = Bundle()
            params.putInt("level", state.level!!)
            view.positive.setOnClickListener {
                params.putString("sentiment", "positive")
                eventLogger.logEvent("reward_response", params)
                hide()
            }
            view.negative.setOnClickListener {
                params.putString("sentiment", "negative")
                eventLogger.logEvent("reward_response", params)
                hide()
            }
            startTypingAnimation(view, state.level)
        }
    }

    override fun createView(inflater: LayoutInflater): View =
        inflater.inflate(R.layout.popup_level_up, null)

    override fun onViewShown(contentView: View) {
        send(LevelUpIntent.LoadData(newLevel))
    }

    private fun startTypingAnimation(contentView: View, level: Int) {
        val title = contentView.title

        val typewriterTitleAnim = TypewriterTextAnimator.of(title, "You reached new level")
        val animSet = AnimatorSet()
        animSet.playSequentially(
            typewriterTitleAnim, levelBadgeAnimation(contentView, level),
            claimRewardAnimation(contentView)
        )
        animSet.start()

        val rewardUrl = Constants.LEVEL_UP_REWARDS[(level - 2) % Constants.LEVEL_UP_REWARDS.size]
        contentView.claimReward.setOnClickListener {
            val reward = contentView.bounty as ImageView
            Glide.with(contentView.context)
                .load(rewardUrl)
                .into(reward)
            contentView.container.showNext()
        }
    }

    private fun levelBadgeAnimation(contentView: View, level: Int): Animator {
        val badge = contentView.badge
        val levelView = contentView.playerLevel
        levelView.text = level.toString()

        val fadeIn = ObjectAnimator.ofFloat(badge, "alpha", 0f, 1f)
        val scaleLevelX = ObjectAnimator.ofFloat(levelView, "scaleX", 1.5f, 1f)
        val scaleLevelY = ObjectAnimator.ofFloat(levelView, "scaleY", 1.5f, 1f)

        val scaleAnim = AnimatorSet()
        scaleAnim.playTogether(scaleLevelX, scaleLevelY)

        val anim = AnimatorSet()
        anim.interpolator = LinearOutSlowInInterpolator()
        anim.playSequentially(fadeIn, scaleAnim)
        fadeIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                badge.visible = true
            }

            override fun onAnimationEnd(animation: Animator?) {
                levelView.visible = true
            }
        })

        return anim
    }

    private fun claimRewardAnimation(contentView: View): Animator {
        val anim = ObjectAnimator.ofFloat(contentView.claimReward, "alpha", 0f, 1f)
        anim.duration = 300
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                contentView.claimReward.visible = true
            }
        })
        return anim
    }
}
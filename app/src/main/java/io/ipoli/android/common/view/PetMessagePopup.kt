package io.ipoli.android.common.view

import android.view.LayoutInflater
import android.view.View
import io.ipoli.android.R
import io.ipoli.android.common.mvi.BaseMviPresenter
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.mvi.Intent
import io.ipoli.android.common.mvi.ViewStateRenderer
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.persistence.PlayerRepository
import kotlinx.android.synthetic.main.popup_pet_message.view.*
import kotlinx.coroutines.experimental.launch
import space.traversal.kapsule.required
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

data class PetMessageViewState(
    val message: String? = null,
    val avatar: PetAvatar? = null
) : BaseViewState()

sealed class PetMessageIntent : Intent {
    data class LoadData(val message: String) : PetMessageIntent()
    data class ChangePlayer(val player: Player) : PetMessageIntent()
}

class PetMessagePresenter(
    private val playerRepository: PlayerRepository,
    coroutineContext: CoroutineContext
) : BaseMviPresenter<ViewStateRenderer<PetMessageViewState>, PetMessageViewState, PetMessageIntent>(
    PetMessageViewState(),
    coroutineContext
) {
    override fun reduceState(intent: PetMessageIntent, state: PetMessageViewState) =
        when (intent) {
            is PetMessageIntent.LoadData -> {
                launch {
                    sendChannel.send(PetMessageIntent.ChangePlayer(playerRepository.find()!!))
                }
                state.copy(
                    message = intent.message
                )
            }

            is PetMessageIntent.ChangePlayer -> {
                state.copy(
                    avatar = intent.player.pet.avatar
                )
            }
        }

}


class PetMessagePopup(
    private val message: String,
    private val actionListener: () -> Unit = {},
    private val actionText: String = ""
) : MviPopup<PetMessageViewState, PetMessagePopup, PetMessagePresenter, PetMessageIntent>(
    position = MviPopup.Position.BOTTOM,
    isAutoHide = true
) {

    private val presenter by required { petMessagePresenter }

    override fun createPresenter() = presenter

    override fun render(state: PetMessageViewState, view: View) {
        state.message?.let {
            view.petMessage.text = it
        }

        state.avatar?.let {
            val androidAvatar = AndroidPetAvatar.valueOf(it.name)
            view.petHead.setImageResource(androidAvatar.headImage)
        }

    }

    override fun createView(inflater: LayoutInflater): View {
        val v = inflater.inflate(R.layout.popup_pet_message, null)

        if (actionText.isNotBlank()) {
            v.petAction.text = actionText
        }

        v.petAction.setOnClickListener {
            actionListener()
            hide()
        }

        return v
    }


    override fun onViewShown(contentView: View) {
        send(PetMessageIntent.LoadData(message))
        autoHideAfter(TimeUnit.SECONDS.toMillis(2))
    }
}
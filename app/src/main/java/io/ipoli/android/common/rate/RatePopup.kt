package io.ipoli.android.common.rate

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.StringRes
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import io.ipoli.android.Constants
import io.ipoli.android.R
import io.ipoli.android.common.IntentUtil
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.mvi.BaseMviPresenter
import io.ipoli.android.common.mvi.BaseViewState
import io.ipoli.android.common.mvi.Intent
import io.ipoli.android.common.mvi.ViewStateRenderer
import io.ipoli.android.common.rate.RateViewState.Type.*
import io.ipoli.android.common.view.MviPopup
import kotlinx.android.synthetic.main.popup_rate.view.*
import space.traversal.kapsule.required
import kotlin.coroutines.experimental.CoroutineContext

sealed class RateIntent : Intent {
    object LoadData : RateIntent()
    object ShowFeedback : RateIntent()
    object ShowRate : RateIntent()
}


data class RateViewState(
    val type: Type
) : BaseViewState() {
    enum class Type {
        LOADING,
        DATA_CHANGED,
        SHOW_FEEDBACK,
        SHOW_RATE
    }
}

class RatePresenter(
    coroutineContext: CoroutineContext
) :
    BaseMviPresenter<ViewStateRenderer<RateViewState>, RateViewState, RateIntent>(
        RateViewState(LOADING),
        coroutineContext
    ) {
    override fun reduceState(intent: RateIntent, state: RateViewState) =
        when (intent) {
            is RateIntent.LoadData -> {
                state.copy(
                    type = DATA_CHANGED
                )
            }

            is RateIntent.ShowFeedback -> {
                state.copy(
                    type = SHOW_FEEDBACK
                )
            }

            is RateIntent.ShowRate -> {
                state.copy(
                    type = SHOW_RATE
                )
            }
        }

}

class RatePopup :
    MviPopup<RateViewState, RatePopup, RatePresenter, RateIntent>(isAutoHide = true) {

    override fun createView(inflater: LayoutInflater): View {
        val view = inflater.inflate(R.layout.popup_rate, null)
        changeTitle(view, R.string.rate_dialog_initial_title)
        view.positive.setText(R.string.dialog_yes)
        view.negative.setText(R.string.dialog_no)
        view.neutral.setText(R.string.rate_dialog_never_ask_again)
        return view
    }

    private val presenter by required { ratePresenter }

    private val eventLogger by required { eventLogger }

    override fun createPresenter() = presenter

    override fun onViewShown(contentView: View) {
        send(RateIntent.LoadData)
    }

    override fun render(state: RateViewState, view: View) {
        val neutral = view.neutral
        val negative = view.negative
        val positive = view.positive

        when (state.type) {
            DATA_CHANGED -> {
                view.rateDialogHeaderIcon.setImageResource(R.drawable.logo)
                neutral.visibility = View.VISIBLE

                positive.setOnClickListener {
                    logEvent("rate_initial", "answer", "yes")
                    send(RateIntent.ShowRate)
                }
                negative.setOnClickListener {
                    logEvent("rate_initial", "answer", "no")
                    send(RateIntent.ShowFeedback)
                }
                neutral.setOnClickListener {
                    logEvent("rate_initial", "answer", "never")
                    saveDoNotShowAgainPref(view.context)
                    hide()
                }
            }

            SHOW_FEEDBACK -> {
                neutral.visibility = View.INVISIBLE
                changeTitle(view, R.string.rate_dialog_feedback_title)
                ViewUtils.goneViews(view.rate)
                ViewUtils.showViews(view.feedbackLayout)
                positive.setText(R.string.rate_dialog_feedback_send)
                negative.setText(R.string.rate_dialog_feedback_no)

                positive.setOnClickListener {
                    val feedback = view.feedback.text.toString()
                    if (feedback.isNotEmpty()) {
                        logEvent("rate_negative", "feedback", feedback)
                        Toast.makeText(view.context, R.string.thank_you, Toast.LENGTH_SHORT).show()
                    }
                    hide()
                }
                negative.setOnClickListener {
                    logEvent("rate_negative", "answer", "no")
                    hide()
                }

                view.viewSwitcher.showNext()
            }

            SHOW_RATE -> {
                neutral.visibility = View.INVISIBLE
                changeTitle(view, R.string.rate_dialog_rate_title)
                ViewUtils.goneViews(view.feedbackLayout)
                ViewUtils.showViews(view.rate)
                positive.setText(R.string.dialog_lets_go)
                negative.setText(R.string.dialog_later)
                positive.setOnClickListener {
                    logEvent("rate_positive", "answer", "yes")
                    saveDoNotShowAgainPref(view.context)
                    view.context.startActivity(IntentUtil.startRatePage(view.context))
                    hide()
                }
                negative.setOnClickListener {
                    logEvent("rate_positive", "answer", "no")
                    hide()
                }
                view.viewSwitcher.showNext()
            }

            else -> {}
        }
    }

    private fun logEvent(name: String, paramName: String, paramValue: String) {
        eventLogger.logEvent(
            name,
            Bundle().apply { putString(paramName, paramValue) }
        )
    }

    private fun changeTitle(view: View, @StringRes title: Int) {
        view.rateDialogHeaderTitle.setText(title)
    }

    private fun saveDoNotShowAgainPref(context: Context) {
        val pm = PreferenceManager.getDefaultSharedPreferences(context)
        pm.edit().putBoolean(Constants.KEY_SHOULD_SHOW_RATE_DIALOG, false).apply()
    }
}
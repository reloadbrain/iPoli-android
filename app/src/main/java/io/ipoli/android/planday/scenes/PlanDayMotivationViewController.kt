package io.ipoli.android.planday.scenes

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.airbnb.lottie.LottieDrawable
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.weather_icons_typeface_library.WeatherIcons
import io.ipoli.android.R
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.datetime.TimeOfDay
import io.ipoli.android.common.redux.android.BaseViewController
import io.ipoli.android.common.view.*
import io.ipoli.android.planday.PlanDayAction
import io.ipoli.android.planday.PlanDayReducer
import io.ipoli.android.planday.PlanDayViewState
import io.ipoli.android.planday.data.Weather
import io.ipoli.android.player.data.Player.Preferences.TemperatureUnit.CELSIUS
import kotlinx.android.synthetic.main.controller_plan_day_motivation.view.*
import space.traversal.kapsule.required

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 5/14/18.
 */
class PlanDayMotivationViewController(args: Bundle? = null) :
    BaseViewController<PlanDayAction, PlanDayViewState>(args) {

    private lateinit var timeChangeReceiver: BroadcastReceiver

    private val imageLoader by required { imageLoader }

    override val stateKey = PlanDayReducer.stateKey

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = container.inflate(R.layout.controller_plan_day_motivation)
        view.motivationAnimation.setAnimation("plan_day_rising_sun.json")
        view.motivationAnimation.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                dispatch(PlanDayAction.RisingSunAnimationDone)
                view.motivationAnimation.setMinProgress(0.33f)
                view.motivationAnimation.repeatCount = LottieDrawable.INFINITE
                view.motivationAnimation.playAnimation()
            }
        })
        view.motivationAnimation.playAnimation()
        return view
    }

    override fun onCreateLoadAction() = PlanDayAction.LoadMotivation

    override fun onAttach(view: View) {
        super.onAttach(view)
        onTimeChanged()
        timeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Intent.ACTION_TIME_TICK == intent.action) {
                    onTimeChanged()
                }
            }
        }
        activity!!.registerReceiver(timeChangeReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        enterFullScreen()
    }

    override fun onDetach(view: View) {
        activity!!.unregisterReceiver(timeChangeReceiver)
        super.onDetach(view)
    }

    private fun onTimeChanged() {
        view!!.motivationHour.text = Time.now().toString(shouldUse24HourFormat)
    }

    override fun render(state: PlanDayViewState, view: View) {

        when (state.type) {

            PlanDayViewState.StateType.MOTIVATION_DATA_LOADED -> {

                view.motivationAnimationBackground.fadeOut(mediumAnimTime)
                view.motivationAnimation.fadeOut(animationDuration = mediumAnimTime, onComplete = {
                    view.motivationAnimationBackground.gone()
                    view.motivationAnimation.gone()
                    view.postDelayed({
                        renderMotivationData(state, view)
                    }, 2000)
                })

            }

            PlanDayViewState.StateType.IMAGE_LOADED -> {

                state.imageUrl?.let {
                    imageLoader.loadMotivationalImage(
                        imageUrl = it,
                        view = view.motivationalImage,
                        onReady = {
                            dispatch(PlanDayAction.ImageLoaded)
                        },
                        onError = {
                            view.motivationalImage.scaleType = ImageView.ScaleType.CENTER_CROP
                            dispatch(PlanDayAction.ImageLoaded)
                        }
                    )
                } ?: dispatch(PlanDayAction.ImageLoaded)
            }

            else -> {
            }
        }
    }

    private fun renderMotivationData(
        state: PlanDayViewState,
        view: View
    ) {

        view.motivationDate.text = state.dateText

        view.motivationalImageAuthor.text = stringRes(R.string.image_author, state.imageAuthor)
        view.motivationalImageAuthor.setOnClickListener {
            val authorPhotoIntent = Intent(Intent.ACTION_VIEW)
            authorPhotoIntent.data = Uri.parse(state.imageAuthorUrl)
            startActivity(authorPhotoIntent)
        }

        state.weather?.let {

            view.weatherIcon.visible()
            view.weatherTemperature.visible()
            view.weatherTemperature.text = state.temperatureText

            view.weatherIcon.setImageDrawable(
                IconicsDrawable(view.context)
                    .icon(state.weatherIcon)
                    .colorRes(R.color.md_white)
                    .sizeDp(48)
            )
        }

        view.motivationGreeting.text = state.greeting

        state.quote?.let {
            view.motivationQuote.text = it.text
            view.motivationalQuoteAuthor.text = it.author
        }
        view.backgroundLayout.fadeIn(mediumAnimTime)
        view.backgroundLayout.visible()
        view.motivationalImageAuthor.visible()

        view.startPlanDay.onDebounceClick {
            dispatch(PlanDayAction.ShowNext)
        }
    }

    private val PlanDayViewState.temperatureText
        get() = if (temperatureUnit == CELSIUS) {
            stringRes(R.string.temperature_celsius, weather!!.temperature.celsiusValue.toInt())
        } else {
            stringRes(
                R.string.temperature_fahrenheit,
                weather!!.temperature.fahrenheitValue.toInt()
            )
        }

    private val PlanDayViewState.weatherIcon: IIcon
        get() = weather!!.let {
            when {

                hasConditions(
                    Weather.Condition.CLEAR,
                    Weather.Condition.WINDY
                ) -> WeatherIcons.Icon.wic_day_windy

                hasConditions(
                    Weather.Condition.CLOUDY,
                    Weather.Condition.WINDY
                ) -> WeatherIcons.Icon.wic_day_cloudy_windy

                hasConditions(
                    Weather.Condition.RAINY,
                    Weather.Condition.WINDY
                ) -> WeatherIcons.Icon.wic_day_rain_wind

                hasConditions(
                    Weather.Condition.SNOWY,
                    Weather.Condition.WINDY
                ) -> WeatherIcons.Icon.wic_day_snow_wind

                hasConditions(
                    Weather.Condition.SNOWY,
                    Weather.Condition.STORMY
                ) -> WeatherIcons.Icon.wic_day_snow_thunderstorm

                it.conditions.contains(Weather.Condition.CLEAR) -> WeatherIcons.Icon.wic_day_sunny
                it.conditions.contains(Weather.Condition.CLOUDY) -> WeatherIcons.Icon.wic_day_cloudy
                it.conditions.contains(Weather.Condition.RAINY) -> WeatherIcons.Icon.wic_day_rain
                it.conditions.contains(Weather.Condition.SNOWY) -> WeatherIcons.Icon.wic_day_snow
                it.conditions.contains(Weather.Condition.ICY) -> WeatherIcons.Icon.wic_day_snow
                it.conditions.contains(Weather.Condition.HAZY) -> WeatherIcons.Icon.wic_day_haze
                it.conditions.contains(Weather.Condition.WINDY) -> WeatherIcons.Icon.wic_day_windy
                it.conditions.contains(Weather.Condition.FOGGY) -> WeatherIcons.Icon.wic_day_fog

                else -> WeatherIcons.Icon.wic_day_storm_showers
            }
        }

    private val PlanDayViewState.greeting: String
        get() {
            val name = playerName ?: stringsRes(R.array.plan_day_names).shuffled().first()

            return when (timeOfDay!!) {
                TimeOfDay.MORNING -> stringRes(R.string.plan_day_morning, name)
                TimeOfDay.AFTERNOON -> stringRes(R.string.plan_day_afternoon, name)
                TimeOfDay.EVENING -> stringRes(R.string.plan_day_evening, name)
                else -> stringRes(R.string.plan_day_hello, name)
            }
        }

    private fun PlanDayViewState.hasConditions(
        condition1: Weather.Condition,
        condition2: Weather.Condition
    ) =
        weather!!.conditions.contains(condition1) && weather.conditions.contains(condition2)
}
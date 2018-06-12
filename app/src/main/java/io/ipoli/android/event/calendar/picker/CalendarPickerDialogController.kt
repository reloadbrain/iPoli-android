package io.ipoli.android.event.calendar.picker

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.ipoli.android.R
import io.ipoli.android.common.view.ReduxDialogController
import io.ipoli.android.common.view.children
import io.ipoli.android.common.view.recyclerview.SimpleViewHolder
import io.ipoli.android.event.Calendar
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.player.data.Player.Preferences.SyncCalendar
import kotlinx.android.synthetic.main.dialog_calendar_picker.view.*
import kotlinx.android.synthetic.main.item_calendar_picker.view.*
import kotlinx.android.synthetic.main.view_dialog_header.view.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/11/2018.
 */
class CalendarPickerDialogController :
    ReduxDialogController<CalendarPickerAction, CalendarPickerViewState, CalendarPickerReducer> {

    override val reducer = CalendarPickerReducer

    private var pickedCalendarsListener: (Set<SyncCalendar>) -> Unit = {}

    constructor(args: Bundle? = null) : super(args)

    constructor(
        pickedCalendarsListener: (Set<SyncCalendar>) -> Unit
    ) : this() {
        this.pickedCalendarsListener = pickedCalendarsListener
    }

    override fun onCreateLoadAction() = CalendarPickerAction.Load

    override fun onCreateDialog(
        dialogBuilder: AlertDialog.Builder,
        contentView: View,
        savedViewState: Bundle?
    ): AlertDialog =
        dialogBuilder
            .setPositiveButton(R.string.sync_selected, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

    override fun onHeaderViewCreated(headerView: View) {
        headerView.dialogHeaderTitle.setText(R.string.calendar_picker_title)
    }

    override fun onCreateContentView(inflater: LayoutInflater, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_calendar_picker, null)

        view.calendarList.layoutManager = LinearLayoutManager(activity!!)
        view.calendarList.setHasFixedSize(true)
        view.calendarList.adapter = CalendarAdapter()

        return view
    }

    override fun onDialogCreated(dialog: AlertDialog, contentView: View) {
        dialog.setOnShowListener {
            setPositiveButtonListener {
                val selectedCalendars = contentView.calendarList.children.mapIndexed { index, v ->
                    if (v.calendarCheckBox.isChecked) {
                        index
                    } else {
                        null
                    }
                }.filterNotNull()
                dispatch(CalendarPickerAction.SelectCalendars(selectedCalendars))
            }
        }
    }

    override fun render(state: CalendarPickerViewState, view: View) {
        when (state.type) {
            CalendarPickerViewState.StateType.CALENDARS_LOADED -> {
                changeIcon(state.petHeadImage)
                (view.calendarList.adapter as CalendarAdapter).updateAll(state.calendars)
            }

            CalendarPickerViewState.StateType.CALENDARS_SELECTED -> {
                dismiss()
                pickedCalendarsListener(state.syncCalendars)
            }

            else -> {
            }
        }
    }

    inner class CalendarAdapter(private var viewModels: List<Calendar> = listOf()) :
        RecyclerView.Adapter<SimpleViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SimpleViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_calendar_picker,
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
            val vm = viewModels[position]
            val view = holder.itemView

            view.calendarName.text = vm.name

            view.calendarColor.backgroundTintList = ColorStateList.valueOf(vm.color)

            view.setOnClickListener {
                view.calendarCheckBox.toggle()
            }
        }

        fun updateAll(viewModels: List<Calendar>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }

        override fun getItemCount() = viewModels.size
    }

    private val CalendarPickerViewState.petHeadImage
        get() = AndroidPetAvatar.valueOf(petAvatar.name).headImage
}
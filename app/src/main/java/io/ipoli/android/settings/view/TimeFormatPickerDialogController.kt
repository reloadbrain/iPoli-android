package io.ipoli.android.settings.view

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import io.ipoli.android.R
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.view.ReduxDialogController
import io.ipoli.android.common.view.stringRes
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.pet.LoadPetDialogAction
import io.ipoli.android.pet.PetDialogReducer
import io.ipoli.android.pet.PetDialogViewState
import io.ipoli.android.player.data.Player.Preferences
import kotlinx.android.synthetic.main.view_dialog_header.view.*

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 5/17/18.
 */
class TimeFormatPickerDialogController(args: Bundle? = null) :
    ReduxDialogController<LoadPetDialogAction, PetDialogViewState, PetDialogReducer>(args) {

    override val reducer = PetDialogReducer

    private lateinit var selectedTimeFormat: Preferences.TimeFormat

    private var listener: (Preferences.TimeFormat) -> Unit = {}

    constructor(
        selectedTimeFormat: Preferences.TimeFormat,
        listener: (Preferences.TimeFormat) -> Unit
    ) : this() {
        this.selectedTimeFormat = selectedTimeFormat
        this.listener = listener
    }

    override fun onCreateContentView(inflater: LayoutInflater, savedViewState: Bundle?): View =
        inflater.inflate(R.layout.dialog_time_format, null)

    override fun onHeaderViewCreated(headerView: View) {
        headerView.dialogHeaderTitle.setText(R.string.select_time_format)
    }

    override fun onCreateDialog(
        dialogBuilder: AlertDialog.Builder,
        contentView: View,
        savedViewState: Bundle?
    ): AlertDialog {

        val formats = listOf(
            Preferences.TimeFormat.DEVICE_DEFAULT,
            Preferences.TimeFormat.TWELVE_HOURS,
            Preferences.TimeFormat.TWENTY_FOUR_HOURS
        )

        val items = listOf(
            stringRes(
                R.string.device_default_time_format,
                Time.now().toString(DateFormat.is24HourFormat(contentView.context))
            ),
            stringRes(R.string.twelve_hour_format, Time.now().toString(false)),
            stringRes(R.string.twenty_four_hour_format, Time.now().toString(true))
        )

        val checked = formats.indexOfFirst { it == selectedTimeFormat }

        return dialogBuilder
            .setSingleChoiceItems(
                items.toTypedArray(),
                checked,
                { _, which ->
                    selectedTimeFormat = formats[which]
                })
            .setPositiveButton(R.string.dialog_ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onDialogCreated(dialog: AlertDialog, contentView: View) {
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                listener(selectedTimeFormat)
                dismiss()
            }
        }
    }

    override fun onCreateLoadAction() = LoadPetDialogAction

    override fun render(state: PetDialogViewState, view: View) {
        if (state.type == PetDialogViewState.Type.PET_LOADED) {
            changeIcon(AndroidPetAvatar.valueOf(state.petAvatar!!.name).headImage)
        }
    }
}
package io.ipoli.android.settings.view

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import io.ipoli.android.R
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
class TemperatureUnitPickerDialogController(args: Bundle? = null) :
    ReduxDialogController<LoadPetDialogAction, PetDialogViewState, PetDialogReducer>(args) {

    override val reducer = PetDialogReducer

    private lateinit var selectedTemperatureUnit: Preferences.TemperatureUnit

    private lateinit var listener: (Preferences.TemperatureUnit) -> Unit

    constructor(
        selectedTemperatureUnit: Preferences.TemperatureUnit,
        listener: (Preferences.TemperatureUnit) -> Unit
    ) : this() {
        this.selectedTemperatureUnit = selectedTemperatureUnit
        this.listener = listener
    }

    override fun onCreateContentView(inflater: LayoutInflater, savedViewState: Bundle?): View =
        inflater.inflate(R.layout.dialog_temperature_unit, null)

    override fun onHeaderViewCreated(headerView: View) {
        headerView.dialogHeaderTitle.setText(R.string.select_temperature_unit)
    }

    override fun onCreateDialog(
        dialogBuilder: AlertDialog.Builder,
        contentView: View,
        savedViewState: Bundle?
    ): AlertDialog {

        val items = listOf(
            stringRes(R.string.temperature_unit_fahrenheit),
            stringRes(R.string.temperature_unit_celsius)
        )

        val checked =
            if (Preferences.TemperatureUnit.FAHRENHEIT == selectedTemperatureUnit) 0 else 1

        return dialogBuilder
            .setSingleChoiceItems(
                items.toTypedArray(),
                checked,
                { _, which ->
                    selectedTemperatureUnit =
                        if (which == 0) Preferences.TemperatureUnit.FAHRENHEIT
                        else Preferences.TemperatureUnit.CELSIUS
                })
            .setPositiveButton(R.string.dialog_ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onDialogCreated(dialog: AlertDialog, contentView: View) {
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                listener(selectedTemperatureUnit)
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
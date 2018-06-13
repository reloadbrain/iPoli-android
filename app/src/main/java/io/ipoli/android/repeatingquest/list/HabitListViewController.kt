package io.ipoli.android.repeatingquest.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.ipoli.android.R
import io.ipoli.android.common.redux.android.ReduxViewController

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/13/18.
 */
class HabitListViewController(args: Bundle? = null) :
    ReduxViewController<HabitListAction, HabitListViewState, HabitListReducer>(args) {

    override val reducer = HabitListReducer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        return inflater.inflate(R.layout.controller_habit_list, container, false)
    }

    override fun render(state: HabitListViewState, view: View) {

    }

}
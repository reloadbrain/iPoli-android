package io.ipoli.android.challenge.list.usecase

import io.ipoli.android.challenge.list.ui.ChallengeViewModel
import io.ipoli.android.challenge.persistence.ChallengeRepository
import io.ipoli.android.common.SimpleRxUseCase
import io.reactivex.Observable

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 8/23/17.
 */
class DisplayChallengeListUseCase(private val challengeRepository: ChallengeRepository) : SimpleRxUseCase<ChallengeListViewState>() {
    override fun createObservable(parameters: Unit): Observable<ChallengeListViewState> =
        challengeRepository.listenForAll()
            .map { challenges ->
                val viewModels = challenges.map { ChallengeViewModel.create(it) }
                if (viewModels.isEmpty()) {
                    ChallengeListViewState.Empty()
                } else {
                    ChallengeListViewState.DataLoaded(viewModels)
                }
            }
            .cast(ChallengeListViewState::class.java)
            .startWith(ChallengeListViewState.Loading())
            .onErrorReturn { ChallengeListViewState.Error(it) }

}
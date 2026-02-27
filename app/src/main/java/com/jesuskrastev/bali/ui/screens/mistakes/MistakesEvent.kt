package com.jesuskrastev.bali.ui.screens.mistakes

import com.jesuskrastev.bali.ui.screens.test.TestSummary

sealed class MistakesEvent {
    data object Retry : MistakesEvent()
    data class SelectOption(val optionIndex: Int) : MistakesEvent()
    data object CheckAnswer : MistakesEvent()
    data object NextQuestion : MistakesEvent()
    data class FinishReview(val onResult: (TestSummary) -> Unit) : MistakesEvent()
}

package com.jesuskrastev.bali.ui.screens.test

sealed class TestEvent {
    data object Retry : TestEvent()
    data class SelectOption(val optionIndex: Int) : TestEvent()
    data object CheckAnswer : TestEvent()
    data object NextQuestion : TestEvent()
    data object UseHint : TestEvent()
    data object UseFiftyFifty : TestEvent()
    data class FinishTest(val onResult: (TestSummary) -> Unit) : TestEvent()
}

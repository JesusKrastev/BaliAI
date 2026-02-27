package com.jesuskrastev.bali.ui.screens.exam

import com.jesuskrastev.bali.ui.screens.test.TestSummary

sealed class ExamEvent {
    data object Retry : ExamEvent()
    data class SelectOption(val optionIndex: Int) : ExamEvent()
    data class GoToQuestion(val index: Int) : ExamEvent()
    data object CheckAnswer : ExamEvent()
    data object NextQuestion : ExamEvent()
    data object PreviousQuestion : ExamEvent()
    data class FinishExam(val onResult: (TestSummary) -> Unit) : ExamEvent()
    data object ToggleQuestionReview : ExamEvent()
}

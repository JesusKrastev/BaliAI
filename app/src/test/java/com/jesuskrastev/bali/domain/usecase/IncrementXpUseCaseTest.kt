package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.User

@OptIn(ExperimentalCoroutinesApi::class)
class IncrementXpUseCaseTest {

    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var useCase: IncrementXpUseCase

    @Before
    fun setup() {
        fakeUserRepository = FakeUserRepository()
        useCase = IncrementXpUseCase(fakeUserRepository)
    }

    @Test
    fun `calculateBaseXp returns expected values for PRACTICE`() {
        assertThat(useCase.calculateBaseXp(TestMode.PRACTICE, 95)).isEqualTo(20)
        assertThat(useCase.calculateBaseXp(TestMode.PRACTICE, 90)).isEqualTo(16)
        assertThat(useCase.calculateBaseXp(TestMode.PRACTICE, 80)).isEqualTo(12)
        assertThat(useCase.calculateBaseXp(TestMode.PRACTICE, 70)).isEqualTo(8)
        assertThat(useCase.calculateBaseXp(TestMode.PRACTICE, 50)).isEqualTo(4)
    }

    @Test
    fun `calculateBaseXp returns expected values for CHALLENGE`() {
        assertThat(useCase.calculateBaseXp(TestMode.CHALLENGE, 95)).isEqualTo(22)
        assertThat(useCase.calculateBaseXp(TestMode.CHALLENGE, 70)).isEqualTo(10)
    }

    @Test
    fun `calculateBaseXp returns expected values for EXAM`() {
        assertThat(useCase.calculateBaseXp(TestMode.EXAM, 95)).isEqualTo(30)
        assertThat(useCase.calculateBaseXp(TestMode.EXAM, 70)).isEqualTo(12)
    }

    @Test
    fun `calculateSpeedBonus returns expected values`() {
        // 10s avg -> 5 XP
        assertThat(useCase.calculateSpeedBonus(100, 10, 100)).isEqualTo(5)
        // 18s avg -> 3 XP
        assertThat(useCase.calculateSpeedBonus(180, 10, 100)).isEqualTo(3)
        // 25s avg -> 1 XP
        assertThat(useCase.calculateSpeedBonus(250, 10, 100)).isEqualTo(1)
        // 35s avg -> null
        assertThat(useCase.calculateSpeedBonus(350, 10, 100)).isNull()
    }

    @Test
    fun `calculateStreakBonus returns expected values`() {
        assertThat(useCase.calculateStreakBonus(30)).isEqualTo(10)
        assertThat(useCase.calculateStreakBonus(14)).isEqualTo(5)
        assertThat(useCase.calculateStreakBonus(7)).isEqualTo(3)
        assertThat(useCase.calculateStreakBonus(5)).isEqualTo(1)
        assertThat(useCase.calculateStreakBonus(3)).isNull()
        assertThat(useCase.calculateStreakBonus(1)).isNull()
    }

    @Test
    fun `calculatePerfectionBonus returns expected values`() {
        assertThat(useCase.calculatePerfectionBonus(100, 5)).isEqualTo(5)
        assertThat(useCase.calculatePerfectionBonus(90, 5)).isNull()
        assertThat(useCase.calculatePerfectionBonus(100, 4)).isNull()
    }

    @Test
    fun `invoke with isRepeat true calculates 30 percent of base and no bonuses except streak`() = runTest {
        fakeUserRepository.insert(User(id = "user1", currentStreak = 0, xp = 0))
        
        val result = useCase(
            mode = TestMode.PRACTICE,
            correctAnswers = 10,
            totalQuestions = 10, // 100% accuracy -> 20 base
            durationSeconds = 100, // 10s avg -> would be 5 bonus if not repeat
            isRepeat = true
        )

        // 20 * 0.3 = 6
        assertThat(result.baseXp).isEqualTo(6)
        assertThat(result.bonusFast).isNull()
        assertThat(result.bonusPerfection).isNull()
        assertThat(result.xpGained).isEqualTo(6)
    }
}

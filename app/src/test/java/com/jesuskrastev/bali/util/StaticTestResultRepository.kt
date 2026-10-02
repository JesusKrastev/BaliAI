package com.jesuskrastev.bali.util

import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * [TestResultRepository] that already holds [results] when a test starts, to check what a
 * finished test makes of the results saved before it. [insert] does not change what [get] emits,
 * so a test sees only the earlier results, as the ViewModels read them before saving their own.
 *
 * @property results the earlier results every read returns
 */
class StaticTestResultRepository(private val results: List<TestResult> = emptyList()) : TestResultRepository {
    override fun getRecent(): Flow<List<TestResult>> = flowOf(results.take(10))
    override fun get(): Flow<List<TestResult>> = flowOf(results)
    override suspend fun insert(result: TestResult): String = "test_id"
    override fun count(): Flow<Int> = flowOf(results.size)
    override fun getAverageScore(): Flow<Double?> = flowOf(results.map { it.score }.average().takeIf { !it.isNaN() })
    override suspend fun clear() {}
}

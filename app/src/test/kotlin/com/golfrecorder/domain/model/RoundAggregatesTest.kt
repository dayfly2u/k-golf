package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundAggregatesTest {
    private fun round(strokes: Int, putts: Int) = RoundSummary(
        roundId = 1, courseId = 1, playedAt = 0, finishedAt = 1000,
        courseName = "course", price = null, companions = null, review = null,
        totalStrokes = strokes, totalPar = 72, totalPutts = putts,
    )

    @Test
    fun `라운드가 없으면 null`() {
        assertNull(computeRoundAggregates(emptyList()))
    }

    @Test
    fun `끝나지 않은 라운드는 집계에서 제외한다`() {
        val unfinished = round(12, 4).copy(finishedAt = null)
        val result = computeRoundAggregates(listOf(unfinished, round(90, 32)))
        assertEquals(90.0, result!!.avgTotalStrokes, 0.01)
        assertEquals(90, result.bestTotalStrokes)
        assertEquals(32.0, result.avgPutts, 0.01)
    }

    @Test
    fun `끝난 라운드가 하나도 없으면 null`() {
        assertNull(computeRoundAggregates(listOf(round(12, 4).copy(finishedAt = null))))
    }

    @Test
    fun `평균과 베스트와 평균 퍼팅을 계산한다`() {
        val result = computeRoundAggregates(listOf(round(90, 32), round(100, 36), round(88, 30)))
        assertEquals(92.666, result!!.avgTotalStrokes, 0.01)
        assertEquals(88, result.bestTotalStrokes)
        assertEquals(32.666, result.avgPutts, 0.01)
    }
}

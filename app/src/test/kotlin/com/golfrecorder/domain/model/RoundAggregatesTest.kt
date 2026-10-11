package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundAggregatesTest {
    private fun round(strokes: Int) = RoundSummary(
        roundId = 1, courseId = 1, playedAt = 0, finishedAt = 1000,
        courseName = "course", price = null, companions = null, review = null,
        totalStrokes = strokes, totalPar = 72, holesPlayed = 18,
    )

    @Test
    fun `라운드가 없으면 null`() {
        assertNull(computeRoundAggregates(emptyList()))
    }

    @Test
    fun `끝나지 않은 라운드는 집계에서 제외한다`() {
        val unfinished = round(12).copy(finishedAt = null)
        val result = computeRoundAggregates(listOf(unfinished, round(90)))
        assertEquals(90.0, result!!.avgTotalStrokes, 0.01)
        assertEquals(90, result.bestTotalStrokes)
    }

    @Test
    fun `끝난 라운드가 하나도 없으면 null`() {
        assertNull(computeRoundAggregates(listOf(round(12).copy(finishedAt = null))))
    }

    @Test
    fun `끝난 9홀 라운드는 집계에서 제외한다`() {
        val nineHoles = round(45).copy(holesPlayed = 9)
        val result = computeRoundAggregates(listOf(nineHoles, round(90), round(100)))
        assertEquals(95.0, result!!.avgTotalStrokes, 0.01)
        assertEquals(90, result.bestTotalStrokes)
    }

    @Test
    fun `18홀이 아닌 라운드뿐이면 null`() {
        val nineHoles = round(45).copy(holesPlayed = 9)
        val partial = round(60).copy(holesPlayed = 12)
        assertNull(computeRoundAggregates(listOf(nineHoles, partial)))
    }

    @Test
    fun `평균과 베스트를 계산한다`() {
        val result = computeRoundAggregates(listOf(round(90), round(100), round(88)))
        assertEquals(92.666, result!!.avgTotalStrokes, 0.01)
        assertEquals(88, result.bestTotalStrokes)
    }
}

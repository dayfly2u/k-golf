package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundSortTest {
    private fun round(id: Long, playedAt: Long, strokes: Int, finished: Boolean = true) = RoundSummary(
        roundId = id, courseId = 1, playedAt = playedAt, finishedAt = if (finished) playedAt + 1000 else null,
        courseName = "course", price = null, companions = null, review = null, totalStrokes = strokes,
    )

    private fun ids(rounds: List<RoundSummary>) = rounds.map { it.roundId }

    @Test
    fun `최신순은 playedAt 내림차순`() {
        val rounds = listOf(round(1, 100, 90), round(2, 300, 95), round(3, 200, 88))
        assertEquals(listOf(2L, 3L, 1L), ids(sortRounds(rounds, RoundSortMode.RECENT)))
    }

    @Test
    fun `점수순은 총타수 오름차순`() {
        val rounds = listOf(round(1, 100, 95), round(2, 300, 88), round(3, 200, 91))
        assertEquals(listOf(2L, 3L, 1L), ids(sortRounds(rounds, RoundSortMode.SCORE)))
    }

    @Test
    fun `점수순에서 동점이면 더 최근 라운드가 위`() {
        val rounds = listOf(round(1, 100, 90), round(2, 300, 90), round(3, 200, 90))
        assertEquals(listOf(2L, 3L, 1L), ids(sortRounds(rounds, RoundSortMode.SCORE)))
    }

    @Test
    fun `점수순에서 안 끝난 라운드와 기록 없는 라운드는 맨 뒤(최근 순)`() {
        val rounds = listOf(
            round(1, 100, 12, finished = false),
            round(2, 200, 95),
            round(3, 300, 0),
            round(4, 400, 88),
            round(5, 500, 40, finished = false),
        )
        // 끝난 라운드(4:88, 2:95) 다음에 나머지를 최근 순으로: 5, 3, 1
        assertEquals(listOf(4L, 2L, 5L, 3L, 1L), ids(sortRounds(rounds, RoundSortMode.SCORE)))
    }
}

package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HandicapTest {
    /** 파 72 코스에서 [differential]만큼 오버한(상한 적용 후) 끝난 18홀 라운드. [playedAt]이 클수록 최근. */
    private fun round(
        differential: Int,
        playedAt: Long,
        finished: Boolean = true,
        holes: Int = 18,
    ) = RoundSummary(
        roundId = playedAt, courseId = 1, playedAt = playedAt, finishedAt = if (finished) playedAt + 1000 else null,
        courseName = "course", price = null, companions = null, review = null,
        totalStrokes = 72 + differential, totalPar = 72, adjustedStrokes = 72 + differential, holesPlayed = holes,
    )

    private fun rounds(vararg differentials: Int) =
        differentials.mapIndexed { index, d -> round(d, playedAt = 1000L + index) }

    @Test
    fun `3개 미만이면 핸디캡 없음`() {
        assertNull(computeEstimatedHandicap(rounds(10, 12)))
        assertNull(computeEstimatedHandicap(emptyList()))
    }

    @Test
    fun `3개는 가장 낮은 1개에서 2 빼기`() {
        assertEquals(8.0, computeEstimatedHandicap(rounds(10, 15, 20))!!, 0.001)
    }

    @Test
    fun `4개는 가장 낮은 1개에서 1 빼기`() {
        assertEquals(9.0, computeEstimatedHandicap(rounds(10, 15, 20, 25))!!, 0.001)
    }

    @Test
    fun `5개는 가장 낮은 1개 그대로`() {
        assertEquals(10.0, computeEstimatedHandicap(rounds(10, 15, 20, 25, 30))!!, 0.001)
    }

    @Test
    fun `6개는 낮은 2개 평균에서 1 빼기`() {
        // (10 + 12) / 2 - 1 = 10.0
        assertEquals(10.0, computeEstimatedHandicap(rounds(10, 12, 20, 25, 30, 35))!!, 0.001)
    }

    @Test
    fun `7개는 낮은 2개 평균 그대로`() {
        assertEquals(11.0, computeEstimatedHandicap(rounds(10, 12, 20, 25, 30, 35, 40))!!, 0.001)
    }

    @Test
    fun `20개는 낮은 8개 평균`() {
        // 차이 1..20 → 낮은 8개 평균 = 4.5
        val list = (1..20).map { round(it, playedAt = 1000L + it) }
        assertEquals(4.5, computeEstimatedHandicap(list)!!, 0.001)
    }

    @Test
    fun `20개를 넘으면 최근 20개만 쓴다`() {
        // 가장 오래된 라운드 하나가 아주 좋아도(차이 -10) 21번째라 제외돼야 한다.
        val list = listOf(round(-10, playedAt = 1)) + (1..20).map { round(it, playedAt = 1000L + it) }
        assertEquals(4.5, computeEstimatedHandicap(list)!!, 0.001)
    }

    @Test
    fun `안 끝난 라운드와 18홀이 아닌 라운드는 제외`() {
        val list = listOf(
            round(1, playedAt = 1, finished = false),
            round(1, playedAt = 2, holes = 9),
            round(10, playedAt = 3),
            round(15, playedAt = 4),
            round(20, playedAt = 5),
        )
        // 유효 라운드는 3개(10, 15, 20) → 가장 낮은 1개 - 2.0
        assertEquals(8.0, computeEstimatedHandicap(list)!!, 0.001)
    }

    @Test
    fun `소수 첫째 자리로 반올림`() {
        // 6개: (10 + 11) / 2 - 1 = 9.5 → 그대로, 7개: (10 + 13) / 2 = 11.5
        assertEquals(9.5, computeEstimatedHandicap(rounds(10, 11, 20, 25, 30, 35))!!, 0.001)
        // 8개: (10 + 11) / 2 = 10.5, 9개: 낮은 3개 (10 + 11 + 12) / 3 = 11.0
        assertEquals(11.0, computeEstimatedHandicap(rounds(10, 11, 12, 20, 25, 30, 35, 40, 45))!!, 0.001)
        // 평균이 10.333...이면 10.3
        assertEquals(10.3, computeEstimatedHandicap(rounds(10, 10, 11, 20, 25, 30, 35, 40, 45))!!, 0.001)
    }

    @Test
    fun `표시 형식 - 플러스 핸디캡은 앞에 +`() {
        assertEquals("14.3", formatHandicap(14.3))
        assertEquals("0.0", formatHandicap(0.0))
        assertEquals("+2.1", formatHandicap(-2.1))
    }
}

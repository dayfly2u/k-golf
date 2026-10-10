package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundRankTest {
    private fun round(id: Long, playedAt: Long, strokes: Int) = RoundSummary(
        roundId = id, courseId = 1, playedAt = playedAt, finishedAt = playedAt + 1000,
        courseName = "course", price = null, companions = null, review = null, totalStrokes = strokes,
    )

    @Test
    fun `가장 최근 5개 중 타수가 가장 적으면 1위`() {
        val rounds = listOf(
            round(1, 500, 90), round(2, 400, 95), round(3, 300, 88), round(4, 200, 100), round(5, 100, 92),
        )
        assertEquals(1 to 5, computeRecentRank(rounds, targetRoundId = 3))
    }

    @Test
    fun `동률이면 더 최근 라운드가 더 높은 순위`() {
        val rounds = listOf(
            round(1, 500, 90), round(2, 400, 90), round(3, 300, 90),
        )
        // id=1이 가장 최근(playedAt=500)이면서 동률 90타 1위여야 한다.
        assertEquals(1 to 3, computeRecentRank(rounds, targetRoundId = 1))
        assertEquals(2 to 3, computeRecentRank(rounds, targetRoundId = 2))
        assertEquals(3 to 3, computeRecentRank(rounds, targetRoundId = 3))
    }

    @Test
    fun `전체 라운드가 5개 미만이면 모수가 그대로 줄어든다`() {
        val rounds = listOf(round(1, 200, 90), round(2, 100, 95))
        assertEquals(1 to 2, computeRecentRank(rounds, targetRoundId = 1))
    }

    @Test
    fun `다른 끝나지 않은 라운드는 순위 경쟁에서 제외한다`() {
        val abandoned = round(2, 400, 12).copy(finishedAt = null)
        val rounds = listOf(round(1, 500, 100), abandoned, round(3, 300, 90))
        // 12타짜리 미완료 라운드가 1위를 차지하면 안 된다 — id=3(90타)이 1위, id=1(100타)이 2위.
        assertEquals(2 to 2, computeRecentRank(rounds, targetRoundId = 1))
        assertEquals(1 to 2, computeRecentRank(rounds, targetRoundId = 3))
    }

    @Test
    fun `보고 있는 라운드 자신은 끝나지 않았어도 포함한다`() {
        val viewing = round(1, 500, 40).copy(finishedAt = null)
        val rounds = listOf(viewing, round(2, 400, 90))
        assertEquals(1 to 2, computeRecentRank(rounds, targetRoundId = 1))
    }

    @Test
    fun `최근 5개 밖의 라운드를 보면 null`() {
        val rounds = (1..6L).map { id -> round(id, playedAt = id * 100, strokes = 90) }
        // id=6이 가장 최근(playedAt=600), 최근 5개는 id=2..6 — id=1(playedAt=100)은 밖.
        assertNull(computeRecentRank(rounds, targetRoundId = 1))
    }
}

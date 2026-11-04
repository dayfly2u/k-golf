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

    private fun courseRound(id: Long, playedAt: Long, courseId: Long?, name: String, sortOrder: Int) =
        round(id, playedAt, 90).copy(courseId = courseId, courseName = name, courseSortOrder = sortOrder)

    @Test
    fun `코스순은 코스 관리에서 놓은 순서대로 같은 코스를 묶고 코스 안에서는 최근이 위`() {
        // 코스 관리 순서: 나(0) → 가(1) → 다(2). 이름 가나다순이 아니라 놓은 순서를 따라야 한다.
        val rounds = listOf(
            courseRound(1, 100, courseId = 10, name = "가", sortOrder = 1),
            courseRound(2, 400, courseId = 20, name = "나", sortOrder = 0),
            courseRound(3, 200, courseId = 30, name = "다", sortOrder = 2),
            courseRound(4, 300, courseId = 10, name = "가", sortOrder = 1),
            courseRound(5, 500, courseId = 20, name = "나", sortOrder = 0),
        )
        assertEquals(listOf(5L, 2L, 4L, 1L, 3L), ids(sortRounds(rounds, RoundSortMode.COURSE)))
    }

    @Test
    fun `코스순에서 코스가 없어진 라운드는 맨 뒤에 이름별로 묶인다`() {
        val rounds = listOf(
            courseRound(1, 100, courseId = null, name = "옛 코스 B", sortOrder = Int.MAX_VALUE),
            courseRound(2, 200, courseId = 10, name = "가", sortOrder = 0),
            courseRound(3, 300, courseId = null, name = "옛 코스 A", sortOrder = Int.MAX_VALUE),
            courseRound(4, 400, courseId = null, name = "옛 코스 B", sortOrder = Int.MAX_VALUE),
        )
        assertEquals(listOf(2L, 3L, 4L, 1L), ids(sortRounds(rounds, RoundSortMode.COURSE)))
    }

    @Test
    fun `코스순은 안 끝난 라운드도 코스 묶음에 그대로 들어간다`() {
        val rounds = listOf(
            courseRound(1, 100, courseId = 10, name = "가", sortOrder = 0),
            courseRound(2, 300, courseId = 10, name = "가", sortOrder = 0).copy(finishedAt = null),
        )
        assertEquals(listOf(2L, 1L), ids(sortRounds(rounds, RoundSortMode.COURSE)))
    }
}

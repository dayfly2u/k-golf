package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RoundYearsTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun playedAt(year: Int, month: Int = 6, day: Int = 15): Long =
        LocalDate.of(year, month, day).atTime(9, 0).atZone(seoul).toInstant().toEpochMilli()

    private fun round(
        id: Long,
        year: Int,
        strokes: Int = 95,
        finished: Boolean = true,
        holes: Int = 18,
        month: Int = 6,
    ) = RoundSummary(
        roundId = id, courseId = 1, playedAt = playedAt(year, month),
        finishedAt = if (finished) playedAt(year, month) + 1000 else null,
        courseName = "course", price = null, companions = null, review = null,
        totalStrokes = strokes, totalPar = 72, holesPlayed = holes,
    )

    @Test
    fun `연도는 기기 시간대 기준 playedAt의 연도다`() {
        // UTC로는 2025-12-31 23:30이지만 서울은 이미 2026-01-01 08:30
        val newYear = round(1, 2025).copy(playedAt = LocalDate.of(2025, 12, 31).atTime(23, 30).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli())
        assertEquals(2026, roundYear(newYear, seoul))
        assertEquals(2025, roundYear(newYear, ZoneId.of("UTC")))
    }

    @Test
    fun `기록이 있는 연도만 최신 연도부터 중복 없이`() {
        val rounds = listOf(round(1, 2024), round(2, 2026), round(3, 2024), round(4, 2025), round(5, 2026))
        assertEquals(listOf(2026, 2025, 2024), roundYears(rounds, seoul))
        assertEquals(emptyList<Int>(), roundYears(emptyList(), seoul))
    }

    @Test
    fun `연도 필터는 2개년 이상일 때부터 보인다`() {
        assertFalse(hasYearFilter(emptyList()))
        assertFalse(hasYearFilter(listOf(2026)))
        assertTrue(hasYearFilter(listOf(2026, 2025)))
    }

    @Test
    fun `선택한 연도가 유효하면 그대로, 아니면 전체로 돌아간다`() {
        val years = listOf(2026, 2025)
        assertNull(effectiveYearFilter(null, years))
        assertEquals(2025, effectiveYearFilter(2025, years))
        // 그 연도 기록이 사라진 경우(라운드 삭제)
        assertNull(effectiveYearFilter(2024, years))
        // 기록이 한 해뿐이 돼 필터가 없어진 경우
        assertNull(effectiveYearFilter(2026, listOf(2026)))
    }

    @Test
    fun `최신순은 연도를 최신부터 묶고 각 연도 안에서 날짜 내림차순`() {
        val rounds = listOf(
            round(1, 2025, month = 3), round(2, 2026, month = 4), round(3, 2025, month = 9), round(4, 2026, month = 8),
        )
        assertEquals(
            listOf(4L, 2L, 3L, 1L),
            sortRoundsByYear(rounds, RoundSortMode.RECENT, seoul).map { it.roundId },
        )
    }

    @Test
    fun `점수순도 연도 묶음은 유지하고 각 연도 안에서만 총타수 오름차순`() {
        // 2025에 80타(전체 1등)가 있어도 연도 묶음이 먼저라 2026 목록 뒤에 온다.
        val rounds = listOf(
            round(1, 2026, strokes = 95), round(2, 2025, strokes = 80),
            round(3, 2026, strokes = 90), round(4, 2025, strokes = 99),
        )
        assertEquals(
            listOf(3L, 1L, 2L, 4L),
            sortRoundsByYear(rounds, RoundSortMode.SCORE, seoul).map { it.roundId },
        )
    }

    @Test
    fun `점수순 동점은 최근 라운드가 위, 안 끝난 라운드는 각 연도의 맨 뒤`() {
        val rounds = listOf(
            round(1, 2026, strokes = 90, month = 3),
            round(2, 2026, strokes = 90, month = 8),
            round(3, 2026, strokes = 20, finished = false, month = 9),
            round(4, 2025, strokes = 70, month = 1),
        )
        assertEquals(
            listOf(2L, 1L, 3L, 4L),
            sortRoundsByYear(rounds, RoundSortMode.SCORE, seoul).map { it.roundId },
        )
    }

    @Test
    fun `점수순에서도 연도마다 구분선이 하나씩 들어간다`() {
        val rounds = listOf(
            round(1, 2026, strokes = 95), round(2, 2025, strokes = 80),
            round(3, 2026, strokes = 90), round(4, 2025, strokes = 100),
        )
        val sorted = sortRoundsByYear(rounds, RoundSortMode.SCORE, seoul)
        val entries = buildHomeListEntries(sorted, withYearHeaders = true, zone = seoul)
        assertEquals(
            listOf(
                HomeListEntry.YearHeader(2026, roundCount = 2, avgTotalStrokes = 92.5),
                HomeListEntry.Round(sorted[0]),
                HomeListEntry.Round(sorted[1]),
                HomeListEntry.YearHeader(2025, roundCount = 2, avgTotalStrokes = 90.0),
                HomeListEntry.Round(sorted[2]),
                HomeListEntry.Round(sorted[3]),
            ),
            entries,
        )
    }

    @Test
    fun `한 해뿐인 기록도 전체 보기에는 연도 구분선이 하나 들어간다`() {
        val sorted = sortRoundsByYear(listOf(round(1, 2026, strokes = 90), round(2, 2026, strokes = 100)), RoundSortMode.RECENT, seoul)
        assertFalse(hasYearFilter(roundYears(sorted, seoul)))
        val entries = buildHomeListEntries(sorted, withYearHeaders = true, zone = seoul)
        assertEquals(HomeListEntry.YearHeader(2026, roundCount = 2, avgTotalStrokes = 95.0), entries.first())
        assertEquals(3, entries.size)
    }

    @Test
    fun `연도로 거르고 null이면 전체`() {
        val rounds = listOf(round(1, 2026), round(2, 2025), round(3, 2026))
        assertEquals(listOf(1L, 3L), filterByYear(rounds, 2026, seoul).map { it.roundId })
        assertEquals(listOf(2L), filterByYear(rounds, 2025, seoul).map { it.roundId })
        assertEquals(rounds, filterByYear(rounds, null, seoul))
    }

    @Test
    fun `연도 구분선은 연도가 바뀌는 곳마다 들어가고 평균은 끝난 18홀만 센다`() {
        // 최신순: 2026 두 판(90, 100) + 2025 한 판(95)
        val sorted = listOf(
            round(1, 2026, strokes = 90, month = 9),
            round(2, 2026, strokes = 100, month = 5),
            round(3, 2025, strokes = 95, month = 10),
        )
        val entries = buildHomeListEntries(sorted, withYearHeaders = true, zone = seoul)
        assertEquals(
            listOf(
                HomeListEntry.YearHeader(2026, roundCount = 2, avgTotalStrokes = 95.0),
                HomeListEntry.Round(sorted[0]),
                HomeListEntry.Round(sorted[1]),
                HomeListEntry.YearHeader(2025, roundCount = 1, avgTotalStrokes = 95.0),
                HomeListEntry.Round(sorted[2]),
            ),
            entries,
        )
    }

    @Test
    fun `구분선의 횟수는 모든 라운드, 평균은 끝난 18홀 라운드만`() {
        val sorted = listOf(
            round(1, 2026, strokes = 90),
            round(2, 2026, strokes = 40, holes = 9),
            round(3, 2026, strokes = 20, finished = false),
        )
        val header = buildHomeListEntries(sorted, withYearHeaders = true, zone = seoul).first()
        assertEquals(HomeListEntry.YearHeader(2026, roundCount = 3, avgTotalStrokes = 90.0), header)
    }

    @Test
    fun `끝난 18홀 라운드가 없는 연도의 평균은 null`() {
        val sorted = listOf(round(1, 2026, finished = false), round(2, 2025, strokes = 80))
        val headers = buildHomeListEntries(sorted, withYearHeaders = true, zone = seoul)
            .filterIsInstance<HomeListEntry.YearHeader>()
        assertEquals(listOf(null, 80.0), headers.map { it.avgTotalStrokes })
    }

    @Test
    fun `구분선을 끄면 라운드 줄만 순서 그대로`() {
        val sorted = listOf(round(1, 2026), round(2, 2025))
        val entries = buildHomeListEntries(sorted, withYearHeaders = false, zone = seoul)
        assertEquals(sorted.map { HomeListEntry.Round(it) }, entries)
    }
}

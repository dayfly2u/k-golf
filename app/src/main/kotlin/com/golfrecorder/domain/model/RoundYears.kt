package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import java.time.Instant
import java.time.ZoneId

/** 홈 연도 필터(`전체 · 2026 · 2025 …`)는 기록이 이 개수 이상의 연도에 걸쳐 있을 때만 보인다 —
 * 한 해뿐이면 거를 게 없다. */
private const val MIN_YEARS_FOR_FILTER = 2

/** 라운드를 친 연도 — 기기 시간대 기준 `playedAt`의 연도. */
fun roundYear(round: RoundSummary, zone: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(round.playedAt).atZone(zone).year

/** 기록이 있는 연도, 최신 연도부터. */
fun roundYears(rounds: List<RoundSummary>, zone: ZoneId = ZoneId.systemDefault()): List<Int> =
    rounds.map { roundYear(it, zone) }.distinct().sortedDescending()

/** 연도 필터 칩 줄을 보일지 — [years]는 [roundYears] 결과. */
fun hasYearFilter(years: List<Int>): Boolean = years.size >= MIN_YEARS_FOR_FILTER

/**
 * 화면에 실제로 적용할 연도 — 선택한 연도([selected])가 지금도 유효할 때만 그대로 쓰고, 필터 자체가 없어졌거나
 * (기록이 한 해뿐이 된 경우) 그 연도 기록이 사라졌으면(라운드 삭제 등) null(전체)로 돌아간다.
 */
fun effectiveYearFilter(selected: Int?, years: List<Int>): Int? =
    selected?.takeIf { hasYearFilter(years) && it in years }

/**
 * 연도별로 묶어(최신 연도부터) 각 연도 안에서 [mode]로 정렬한다 — 점수순도 같은 연도 안에서만 줄 세운다. 전체 보기에서
 * 연도 구분선(`2026 ────── N회 · 평균 94.8`)이 정렬 방식과 상관없이 항상 나오려면 같은 연도 라운드가 이어져 있어야 해서다.
 * 연도 필터를 건 목록(한 해뿐)에서는 [sortRounds]와 결과가 같다.
 */
fun sortRoundsByYear(
    rounds: List<RoundSummary>,
    mode: RoundSortMode,
    zone: ZoneId = ZoneId.systemDefault(),
): List<RoundSummary> =
    rounds.groupBy { roundYear(it, zone) }
        .toSortedMap(reverseOrder())
        .values
        .flatMap { yearRounds -> sortRounds(yearRounds, mode) }

/** [year]가 null이면 전체를 그대로 돌려준다. */
fun filterByYear(rounds: List<RoundSummary>, year: Int?, zone: ZoneId = ZoneId.systemDefault()): List<RoundSummary> =
    if (year == null) rounds else rounds.filter { roundYear(it, zone) == year }

/** 홈 라운드 목록의 한 줄 — 연도 구분선 또는 라운드 카드. */
sealed interface HomeListEntry {
    /** 전체 보기에서 연도마다 앞에 들어가는 구분선 — `2026 ────── N회 · 평균 94.8`.
     * [avgTotalStrokes]는 홈 헤더와 같은 기준(끝난 18홀 라운드)이라 해당 라운드가 없으면 null. */
    data class YearHeader(val year: Int, val roundCount: Int, val avgTotalStrokes: Double?) : HomeListEntry

    data class Round(val round: RoundSummary) : HomeListEntry
}

/**
 * [sortedRounds]를 목록 줄로 바꾼다. [withYearHeaders]가 true면 연도가 바뀌는 곳마다 [HomeListEntry.YearHeader]를
 * 끼워 넣는다 — 같은 연도 라운드가 이어져 있다고 가정하므로 [sortRoundsByYear] 결과를 넘긴다. 전체 보기에서는
 * 기록이 한 해뿐이어도, 최신순/점수순 어느 쪽이든 켠다(연도를 고른 필터 목록에서는 끈다).
 */
fun buildHomeListEntries(
    sortedRounds: List<RoundSummary>,
    withYearHeaders: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): List<HomeListEntry> {
    if (!withYearHeaders) return sortedRounds.map { HomeListEntry.Round(it) }
    val roundsByYear = sortedRounds.groupBy { roundYear(it, zone) }
    val entries = mutableListOf<HomeListEntry>()
    var currentYear: Int? = null
    for (round in sortedRounds) {
        val year = roundYear(round, zone)
        if (year != currentYear) {
            val yearRounds = roundsByYear.getValue(year)
            entries += HomeListEntry.YearHeader(
                year = year,
                roundCount = yearRounds.size,
                avgTotalStrokes = computeRoundAggregates(yearRounds)?.avgTotalStrokes,
            )
            currentYear = year
        }
        entries += HomeListEntry.Round(round)
    }
    return entries
}

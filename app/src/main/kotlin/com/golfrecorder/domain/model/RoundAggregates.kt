package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary

/** 홈 화면 상단 요약 3칸(전체 평균 스코어, 베스트 스코어, 평균 퍼팅) — [rounds]가
 * 비어있으면 null(표시할 라운드가 아예 없음). */
data class RoundAggregates(val avgTotalStrokes: Double, val bestTotalStrokes: Int, val avgPutts: Double)

fun computeRoundAggregates(rounds: List<RoundSummary>): RoundAggregates? {
    if (rounds.isEmpty()) return null
    return RoundAggregates(
        avgTotalStrokes = rounds.map { it.totalStrokes }.average(),
        bestTotalStrokes = rounds.minOf { it.totalStrokes },
        avgPutts = rounds.map { it.totalPutts }.average(),
    )
}

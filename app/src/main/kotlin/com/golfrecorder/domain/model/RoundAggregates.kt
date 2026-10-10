package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary

/** 홈 화면 상단 요약 3칸(전체 평균 스코어, 베스트 스코어, 평균 퍼팅) — 끝난 라운드
 * ([RoundSummary.finishedAt] != null)가 하나도 없으면 null(표시할 게 없음). */
data class RoundAggregates(val avgTotalStrokes: Double, val bestTotalStrokes: Int, val avgPutts: Double)

/** 스펙은 "전체 라운드 기준"이지만 끝나지 않은 라운드(중간에 홈으로 나간 라운드)는 지나온
 * 홀 몫의 타수만 있어서 그대로 섞으면 "베스트 스코어"가 말도 안 되게 작게 나온다 — 그래서
 * 끝난 라운드만 센다. */
fun computeRoundAggregates(rounds: List<RoundSummary>): RoundAggregates? {
    val finished = rounds.filter { it.finishedAt != null }
    if (finished.isEmpty()) return null
    return RoundAggregates(
        avgTotalStrokes = finished.map { it.totalStrokes }.average(),
        bestTotalStrokes = finished.minOf { it.totalStrokes },
        avgPutts = finished.map { it.totalPutts }.average(),
    )
}

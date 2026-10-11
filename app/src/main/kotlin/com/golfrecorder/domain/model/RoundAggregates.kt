package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary

/** 홈 헤더의 평균 스코어/베스트스코어 — 끝난 18홀 라운드([RoundSummary.finishedAt] != null,
 * [RoundSummary.holesPlayed] == 18)가 하나도 없으면 null(표시할 게 없음). 핸디캡
 * ([computeEstimatedHandicap])과 같은 라운드 조건이다. */
data class RoundAggregates(val avgTotalStrokes: Double, val bestTotalStrokes: Int)

private const val FULL_ROUND_HOLES = 18

/** 스펙은 "전체 라운드 기준"이지만 끝나지 않은 라운드(중간에 홈으로 나간 라운드)는 지나온
 * 홀 몫의 타수만 있고, 끝난 9홀 라운드도 18홀과 타수 규모가 달라서 섞으면 평균이 낮아지고
 * "베스트스코어"가 말도 안 되게 작게 나온다 — 그래서 끝난 18홀 라운드만 센다. */
fun computeRoundAggregates(rounds: List<RoundSummary>): RoundAggregates? {
    val completed = rounds.filter { it.finishedAt != null && it.holesPlayed == FULL_ROUND_HOLES }
    if (completed.isEmpty()) return null
    return RoundAggregates(
        avgTotalStrokes = completed.map { it.totalStrokes }.average(),
        bestTotalStrokes = completed.minOf { it.totalStrokes },
    )
}

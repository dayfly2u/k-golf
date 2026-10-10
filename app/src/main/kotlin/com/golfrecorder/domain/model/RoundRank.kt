package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary

/** [targetRoundId]가 전체 라운드 중 가장 최근 [windowSize]개(코스 무관, playedAt
 * 내림차순, targetRoundId가 그 안에 들어있을 때만) 안에서 totalStrokes 기준 몇 위인지.
 * 동률이면 더 최근 라운드가 더 높은 순위(숫자가 더 작음). targetRoundId가 최근
 * [windowSize]개 안에 없으면(더 오래된 라운드를 보는 중) null. 반환값은
 * (순위, 그 구간에 실제로 들어간 라운드 수) — 전체 라운드가 [windowSize]개보다
 * 적으면 두 번째 값이 그 실제 개수로 줄어든다. */
fun computeRecentRank(rounds: List<RoundSummary>, targetRoundId: Long, windowSize: Int = 5): Pair<Int, Int>? {
    val recent = rounds.sortedByDescending { it.playedAt }.take(windowSize)
    if (recent.none { it.roundId == targetRoundId }) return null
    val ranked = recent.sortedWith(compareBy({ it.totalStrokes }, { -it.playedAt }))
    val rank = ranked.indexOfFirst { it.roundId == targetRoundId } + 1
    return rank to recent.size
}

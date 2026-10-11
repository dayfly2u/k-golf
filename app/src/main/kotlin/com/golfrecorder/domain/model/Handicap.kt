package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import kotlin.math.abs
import kotlin.math.roundToInt

/** 핸디캡 계산에 쓰는 최근 스코어 개수(WHS: 최근 20개). */
private const val RECENT_SCORES = 20

/** WHS 규칙 5.2a — 스코어가 [count]개일 때 (낮은 몇 개 평균을 쓰는지, 보정값). 20개 이상이면 낮은 8개. */
private fun usedCountAndAdjustment(count: Int): Pair<Int, Double> = when {
    count >= 20 -> 8 to 0.0
    count == 19 -> 7 to 0.0
    count >= 17 -> 6 to 0.0
    count >= 15 -> 5 to 0.0
    count >= 12 -> 4 to 0.0
    count >= 9 -> 3 to 0.0
    count >= 7 -> 2 to 0.0
    count == 6 -> 2 to -1.0
    count == 5 -> 1 to 0.0
    count == 4 -> 1 to -1.0
    else -> 1 to -2.0 // 3개
}

/**
 * **추정** 핸디캡 인덱스 — 월드 핸디캡 시스템(WHS) 방식을 그대로 따르되, 코스 레이팅·슬로프 레이팅이 앱에
 * 없어서 코스 레이팅 = 파, 슬로프 = 113(보정 없음)으로 가정한다. 그래서 라운드의 스코어 차이는
 * `조정 타수 − 파`가 되고(홀당 파 + 5 상한 적용), 공식 인덱스(KGA 하이골프 등)와는 다를 수 있다.
 *
 * 쓰는 라운드: 끝난(finishedAt != null) 18홀 라운드. 9홀/부분 라운드는 WHS의 "기대 점수" 환산이 필요해
 * 제외한다. 그중 가장 최근 20개 중 낮은 N개 평균(+보정)을 소수 첫째 자리로 반올림한다(표는
 * [usedCountAndAdjustment]). 유효 라운드가 3개 미만이면 null. 소프트/하드 캡, 이례적 스코어 보정, PCC는
 * 적용하지 않는다.
 */
fun computeEstimatedHandicap(rounds: List<RoundSummary>): Double? {
    val differentials = rounds
        .filter { it.finishedAt != null && it.holesPlayed == 18 && it.totalPar > 0 }
        .sortedByDescending { it.playedAt }
        .take(RECENT_SCORES)
        .map { (it.adjustedStrokes - it.totalPar).toDouble() }
    if (differentials.size < 3) return null
    val (used, adjustment) = usedCountAndAdjustment(differentials.size)
    val average = differentials.sorted().take(used).average()
    return ((average + adjustment) * 10).roundToInt() / 10.0
}

/** 핸디캡 표시 문자열 — 0 미만(스크래치보다 잘 치는 "플러스 핸디캡")은 앞에 +를 붙인다. */
fun formatHandicap(index: Double): String =
    if (index < 0) "+${"%.1f".format(abs(index))}" else "%.1f".format(index)

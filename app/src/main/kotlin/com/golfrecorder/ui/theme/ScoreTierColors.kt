package com.golfrecorder.ui.theme

import androidx.compose.ui.graphics.Color

/** 점수 레벨 구간(1~4)의 배경색. 구간은 domain.model의 holeScoreTier/strokeScoreTier로 구한다. */
fun scoreTierColor(tier: Int): Color = when (tier) {
    1 -> GolfTokens.ScoreTier1
    2 -> GolfTokens.ScoreTier2
    3 -> GolfTokens.ScoreTier3
    else -> GolfTokens.ScoreTier4
}

/** 구간 배경 위 글자색 — 진한 배경(1, 4구간)은 흰 글자, 연한 배경(2, 3구간)은 본문색. */
fun scoreTierTextColor(tier: Int): Color =
    if (tier == 1 || tier >= 4) GolfTokens.CardBackground else GolfTokens.TextPrimary

package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.golfrecorder.ui.theme.GolfTokens

enum class HoleProgressMode { LIVE, REVIEW }

/** 18홀 진행바 한 칸의 색. 라이브: 지나온 홀=FieldGreen, 현재 홀=Accent, 남은 홀=
 * ProgressRemaining. 리뷰(완료된 라운드): "남은 홀" 개념이 없으므로 지금 보는 홀만
 * Accent, 나머지는 전·후 구분 없이 전부 FieldGreen(시안 Hole.dc.html 그대로). */
fun holeSegmentColor(holeNumber: Int, currentHoleNumber: Int, mode: HoleProgressMode): Color = when (mode) {
    HoleProgressMode.LIVE -> when {
        holeNumber < currentHoleNumber -> GolfTokens.FieldGreen
        holeNumber == currentHoleNumber -> GolfTokens.Accent
        else -> GolfTokens.ProgressRemaining
    }
    HoleProgressMode.REVIEW -> if (holeNumber == currentHoleNumber) GolfTokens.Accent else GolfTokens.FieldGreen
}

@Composable
fun HoleProgressBar(
    totalHoles: Int,
    currentHoleNumber: Int,
    mode: HoleProgressMode,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (holeNumber in 1..totalHoles) {
            Surface(
                modifier = Modifier.weight(1f).height(6.dp),
                shape = RoundedCornerShape(3.dp),
                color = holeSegmentColor(holeNumber, currentHoleNumber, mode),
            ) {}
        }
    }
}

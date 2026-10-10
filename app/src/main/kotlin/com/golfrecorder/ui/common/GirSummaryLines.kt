package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlin.math.roundToInt

/** GIR N/M (P%) + 엄격 GIR N/M (P%, 빨강). RoundSummaryScreen(상세, 기본 두 줄
 * titleMedium/bodyMedium)과 RoundHistoryScreen(첫 화면 리스트 펼침, [singleLine]=true
 * 한 줄 bodyMedium)이 같은 계산 로직을 공유한다. */
@Composable
fun GirSummaryLines(
    girCount: Int,
    strictGirCount: Int,
    totalHoles: Int,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    singleLine: Boolean = false,
) {
    val percent: (Int) -> Int = { count -> if (totalHoles == 0) 0 else (count * 100.0 / totalHoles).roundToInt() }
    if (singleLine) {
        val text = buildAnnotatedString {
            // 표시명과 코드명이 반대다 — girCount(isGreenInRegulation 기준)는 화면에
            // "L-GIR"로, strictGirCount(isStrictGreenInRegulation 기준)는 화면에
            // "GIR"로 보여준다(redesign-spec.md 4.1, 의미를 바꾸는 게 아니라 표시만 교체).
            withStyle(SpanStyle(color = Color.Black)) {
                append("L-GIR $girCount/$totalHoles (${percent(girCount)}%)")
            }
            append(" / ")
            // 숏어프로치가 전혀 없었던 엄격 기준(화면 표시명 "GIR")을 눈에 띄게
            // 빨간색으로 같이 보여준다.
            withStyle(SpanStyle(color = Color.Red)) {
                append("GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)")
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        // 표시명과 코드명이 반대다 — 위 singleLine 분기와 동일(redesign-spec.md 4.1).
        Text(
            "L-GIR $girCount/$totalHoles (${percent(girCount)}%)",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Red,
        )
    }
}

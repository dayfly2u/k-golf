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
            withStyle(SpanStyle(color = Color.Black)) {
                append("GIR $girCount/$totalHoles (${percent(girCount)}%)")
            }
            append(" / ")
            // 일반 GIR은 strokesToGreen만으로 판정해 숏어프로치가 있었어도 성공으로
            // 잡힐 수 있다 — 숏어프로치가 전혀 없었던 엄격 기준도 눈에 띄게 빨간색으로
            // 같이 보여준다.
            withStyle(SpanStyle(color = Color.Red)) {
                append("엄격 GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)")
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            "GIR $girCount/$totalHoles (${percent(girCount)}%)",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "엄격 GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Red,
        )
    }
}

package com.golfrecorder.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.golfrecorder.domain.model.DriverDistanceStats
import kotlin.math.roundToInt

// 드라이버/퍼팅 통계 한 줄 — 눈에 띄게 짙은 파랑, 평균 수치는 굵게.
private val ROUND_STATS_COLOR = Color(0xFF0D47A1)

/** "드라이버 평균 Nm (최대 Nm) / 퍼팅 평균 N.N (총 N)" 한 줄. RoundSummaryScreen과
 * RoundHistoryScreen(첫 화면 리스트 펼침)이 같은 스타일을 공유한다. */
@Composable
fun RoundStatsLine(driverStats: DriverDistanceStats, avgPutts: Double, totalPutts: Int, modifier: Modifier = Modifier) {
    val driverAvg = driverStats.avgMeters?.let { "${it.roundToInt()}m" } ?: "-"
    val driverMax = driverStats.maxMeters?.let { "${it.roundToInt()}m" } ?: "-"
    val avgPuttsText = "%.1f".format(avgPutts)
    val text = buildAnnotatedString {
        append("드라이버 평균 ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(driverAvg) }
        append(" (최대 $driverMax) / 퍼팅 평균 ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(avgPuttsText) }
        append(" (총 $totalPutts)")
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = ROUND_STATS_COLOR, modifier = modifier)
}

package com.golfrecorder.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** 대기 화면 로고의 깃발 — SVG `M7 21V3l10 4-10 4` + `M4 21h12` (24×24, 선 두께 2). */
@Composable
fun WatchFlagIcon(iconSize: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(iconSize)) {
        val u = this.size.minDimension / 24f
        val flag = Path().apply {
            moveTo(7f * u, 21f * u)
            lineTo(7f * u, 3f * u)
            lineTo(17f * u, 7f * u)
            lineTo(7f * u, 11f * u)
        }
        drawPath(flag, color, style = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(4f * u, 21f * u), Offset(16f * u, 21f * u), strokeWidth = 2f * u, cap = StrokeCap.Round)
    }
}

/** 이전/다음 홀 버튼의 쉐브론 — SVG `M15 18l-6-6 6-6`(왼쪽) / `M9 18l6-6-6-6`(오른쪽), 선 두께 2.6. */
@Composable
fun WatchChevron(pointsRight: Boolean, color: Color, iconSize: Dp) {
    Canvas(Modifier.size(iconSize)) {
        val u = this.size.minDimension / 24f
        val path = Path().apply {
            if (pointsRight) {
                moveTo(9f * u, 18f * u)
                lineTo(15f * u, 12f * u)
                lineTo(9f * u, 6f * u)
            } else {
                moveTo(15f * u, 18f * u)
                lineTo(9f * u, 12f * u)
                lineTo(15f * u, 6f * u)
            }
        }
        drawPath(path, color, style = Stroke(width = 2.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * "폰 연결 끊김" 알약의 Wi-Fi 끊김 아이콘 — 시안 SVG(24×24, 선 두께 2.2)의 호 세 개는 모두
 * 중심 (12, 20)이라 같은 중심의 drawArc로 옮겼다: 반지름 5(225°→315°), 반지름 10 왼쪽 조각
 * (225.4°부터 34.3°), 반지름 10 오른쪽 조각(298.4°부터 16.2°), 점, 대각선 슬래시.
 */
@Composable
fun WatchWifiOffIcon(iconSize: Dp, color: Color) {
    Canvas(Modifier.size(iconSize)) {
        val u = this.size.minDimension / 24f
        val strokeWidth = 2.2f * u
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        fun arc(radius: Float, startDegrees: Float, sweepDegrees: Float) = drawArc(
            color = color,
            startAngle = startDegrees,
            sweepAngle = sweepDegrees,
            useCenter = false,
            topLeft = Offset((12f - radius) * u, (20f - radius) * u),
            size = Size(radius * 2f * u, radius * 2f * u),
            style = stroke,
        )
        arc(radius = 5f, startDegrees = 225f, sweepDegrees = 90f)
        arc(radius = 10f, startDegrees = 225.4f, sweepDegrees = 34.3f)
        arc(radius = 10f, startDegrees = 298.4f, sweepDegrees = 16.2f)
        drawLine(color, Offset(2f * u, 2f * u), Offset(22f * u, 22f * u), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawCircle(color, radius = 0.8f * u, center = Offset(12f * u, 20f * u))
    }
}

package com.golfrecorder.wear

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// 시안(WatchPlay.dc.html)의 SVG 링: 360 기준 반지름 170, 두께 7, 칸 사이 틈 4(원둘레 방향 길이).
private const val RING_RADIUS = 170f
private const val RING_STROKE = 7f
private const val RING_GAP = 4f

/** 화면 가장자리를 따라 홀 수만큼 칸을 그린다. 12시 방향에서 시계 방향으로 시작. */
@Composable
fun WatchRing(segments: List<RingSegment>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (segments.isEmpty()) return@Canvas
        val unit = size.minDimension / WatchScale.DESIGN_SIZE
        val radius = RING_RADIUS * unit
        val center = Offset(size.width / 2f, size.height / 2f)
        val step = 360f / segments.size
        val gapDegrees = (RING_GAP / (2f * Math.PI.toFloat() * RING_RADIUS)) * 360f
        segments.forEachIndexed { index, segment ->
            drawArc(
                color = when (segment) {
                    RingSegment.DONE -> WatchColors.Accent
                    RingSegment.CURRENT -> WatchColors.Yellow
                    RingSegment.REMAINING -> WatchColors.RingRemaining
                },
                startAngle = -90f + index * step,
                sweepAngle = step - gapDegrees,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = RING_STROKE * unit, cap = StrokeCap.Butt),
            )
        }
    }
}

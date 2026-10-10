package com.golfrecorder.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.golfrecorder.ui.theme.GolfTokens

/** [value] 중 [index](1부터)번째 칸이 채워지는 비율(0~1) — 별/막대 공통. 4.5점이면 5번째 칸이 0.5. */
internal fun fillFraction(value: Float, index: Int): Float = (value - (index - 1)).coerceIn(0f, 1f)

// 시안 별 모양(24x24 좌표계)
// 안드로이드 Path는 JVM 단위 테스트에서 만들 수 없어 파일 로드 시점이 아니라 처음 그릴 때 만든다(lazy).
private val STAR_PATH: Path by lazy {
    Path().apply {
        moveTo(12f, 3f)
        relativeLineTo(2.7f, 5.6f)
        relativeLineTo(6.1f, 0.9f)
        relativeLineTo(-4.4f, 4.3f)
        relativeLineTo(1f, 6.1f)
        lineTo(12f, 17f)
        relativeLineTo(-5.4f, 2.9f)
        relativeLineTo(1f, -6.1f)
        lineTo(3.2f, 9.5f)
        relativeLineTo(6.1f, -0.9f)
        close()
    }
}

@Composable
private fun StarCell(size: Dp, fill: Float) {
    Canvas(modifier = Modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        scale(scale = unit, pivot = Offset.Zero) {
            val outline = Stroke(width = 1.5f, join = StrokeJoin.Round)
            drawPath(STAR_PATH, GolfTokens.CardBackground)
            drawPath(STAR_PATH, GolfTokens.StarStroke, style = outline)
            if (fill > 0f) {
                clipRect(left = 0f, top = 0f, right = 24f * fill, bottom = 24f) {
                    drawPath(STAR_PATH, GolfTokens.Accent)
                    drawPath(STAR_PATH, GolfTokens.StarStroke, style = outline)
                }
            }
        }
    }
}

/** 평점 별 5개(0.5점 단위로 반 개 채움) — 시안 코스 수정 화면. 폭이 모자라면 별 크기를 줄인다(최대 24dp). */
@Composable
fun RatingStars(rating: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val gap = 2.dp
        val starSize = minOf(24.dp, (maxWidth - gap * 4) / 5)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (index in 1..5) StarCell(starSize, fillFraction(rating, index))
        }
    }
}

/** 난이도 막대 5칸(칸마다 [level]만큼 채움) — 시안 코스 수정 화면. 난이도는 지금 정수 1단위라 칸 단위로 채워진다. */
@Composable
fun DifficultyBars(level: Float, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (index in 1..5) {
            val track = RoundedCornerShape(4.dp)
            Box(
                modifier = Modifier.weight(1f)
                    .height(12.dp)
                    .clip(track)
                    .background(GolfTokens.BadgePlain),
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight()
                        .fillMaxWidth(fillFraction(level, index))
                        .background(GolfTokens.ObBorder),
                )
            }
        }
    }
}

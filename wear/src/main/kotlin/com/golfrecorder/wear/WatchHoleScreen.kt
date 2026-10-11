package com.golfrecorder.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.wear.compose.material.Text

/**
 * 홀이 바뀐 직후 잠깐 뜨는 화면 — 새 홀 번호를 크게 보여주고 이전 홀 결과와 합계를 요약한다.
 * 버튼으로 홀을 계속 넘길 수 있고, 다른 곳을 누르면 [onDismiss].
 */
@Composable
fun WatchHoleScreen(
    state: RoundUiState,
    direction: HoleMoveDirection,
    onMove: (HoleMoveDirection) -> Unit,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(WatchColors.Background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        val scale = WatchScale.forScreen(minOf(maxWidth, maxHeight))

        WatchRing(ringSegments(state.holeCount, state.holeNumber), Modifier.fillMaxSize())

        Text(
            holeMoveCaption(direction),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = scale.dp(52f).coerceAtLeast(26.dp)),
            color = WatchColors.Label,
            fontSize = scale.sp(12f),
            maxLines = 1,
        )

        Column(
            modifier = Modifier.align(Alignment.Center).offset(y = -scale.dp(12f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scale.dp(4f).coerceAtLeast(2.dp)),
        ) {
            Text(
                "HOLE",
                color = WatchColors.Label,
                fontFamily = WatchFonts.Number,
                fontWeight = FontWeight.SemiBold,
                fontSize = scale.sp(22f, minSp = 12f),
                lineHeight = scale.sp(22f, minSp = 12f),
                maxLines = 1,
            )
            Text(
                "${state.holeNumber}",
                color = WatchColors.White,
                fontFamily = WatchFonts.Number,
                fontWeight = FontWeight.Bold,
                fontSize = scale.sp(96f, minSp = 52f),
                lineHeight = scale.sp(96f, minSp = 52f) * 0.9f,
                maxLines = 1,
            )
            WatchParChip(state.par, designFontPx = 14f, scale = scale)
        }

        WatchCircleButton(
            onClick = { onMove(HoleMoveDirection.PREVIOUS) },
            size = scale.touch(56f, minDp = 40f),
            background = WatchColors.ButtonDark,
            description = "이전 홀",
            modifier = Modifier.align(Alignment.CenterStart).padding(start = scale.dp(26f).coerceAtLeast(12.dp)),
            enabled = state.holeNumber > 1,
        ) { WatchChevron(pointsRight = false, color = WatchColors.White, iconSize = scale.dp(22f).coerceAtLeast(16.dp)) }
        WatchCircleButton(
            onClick = { onMove(HoleMoveDirection.NEXT) },
            size = scale.touch(56f, minDp = 40f),
            background = WatchColors.Accent,
            description = "다음 홀",
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = scale.dp(26f).coerceAtLeast(12.dp)),
            enabled = state.holeNumber < state.holeCount,
        ) { WatchChevron(pointsRight = true, color = WatchColors.White, iconSize = scale.dp(22f).coerceAtLeast(16.dp)) }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = scale.dp(44f).coerceAtLeast(22.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scale.dp(3f).coerceAtLeast(1.dp)),
        ) {
            state.previousHole?.let { previous ->
                val summary = holeSummary(previous.strokes, previous.par)
                Text(
                    buildAnnotatedString {
                        append("${state.holeNumber - 1}번 홀 ")
                        withStyle(SpanStyle(color = WatchColors.White, fontWeight = FontWeight.SemiBold)) {
                            append("${summary.strokes}타 · ${summary.gradeName}")
                        }
                    },
                    color = WatchColors.Label,
                    fontSize = scale.sp(12f),
                    maxLines = 1,
                )
            }
            WatchTotalLine(
                total = state.scoreBefore,
                labelDesignPx = 12f,
                valueDesignPx = 16f,
                valueMinSp = 12f,
                scale = scale,
            )
        }
    }
}

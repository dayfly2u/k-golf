package com.golfrecorder.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.wear.compose.material.Text
import com.golfrecorder.wearsync.WearSync

/** 라운드 진행 중 기본 화면: 진행 링 + 홀 이동 + 두 스테퍼 + 이 홀 결과/합계. */
@Composable
fun WatchPlayScreen(
    state: RoundUiState,
    badge: StatusBadge,
    onAction: (String) -> Unit,
    onMove: (HoleMoveDirection) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(WatchColors.Background)) {
        val scale = WatchScale.forScreen(minOf(maxWidth, maxHeight))
        val holeStrokes = state.strokesToGreen + state.strokesPutt

        WatchRing(ringSegments(state.holeCount, state.holeNumber), Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 맨 위 줄이 둥근 가장자리·링에 잘리지 않게 최소 16dp.
                .padding(top = scale.dp(24f).coerceAtLeast(16.dp), bottom = scale.dp(26f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WatchStatusLine(badge, scale)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(scale.dp(8f).coerceAtLeast(3.dp)),
            ) {
                WatchCircleButton(
                    onClick = { onMove(HoleMoveDirection.PREVIOUS) },
                    size = scale.touch(44f, minDp = 34f),
                    background = WatchColors.ButtonDark,
                    description = "이전 홀",
                    enabled = state.holeNumber > 1,
                ) { WatchChevron(pointsRight = false, color = WatchColors.White, iconSize = scale.dp(18f).coerceAtLeast(14.dp)) }
                Column(
                    modifier = Modifier.padding(horizontal = scale.dp(2f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "HOLE ${state.holeNumber}",
                        color = WatchColors.White,
                        fontFamily = WatchFonts.Number,
                        fontWeight = FontWeight.Bold,
                        fontSize = scale.sp(30f, minSp = 18f),
                        lineHeight = scale.sp(30f, minSp = 18f),
                        maxLines = 1,
                    )
                    WatchParChip(state.par, designFontPx = 12f, scale = scale)
                }
                WatchCircleButton(
                    onClick = { onMove(HoleMoveDirection.NEXT) },
                    size = scale.touch(44f, minDp = 34f),
                    background = WatchColors.ButtonDark,
                    description = "다음 홀",
                    enabled = state.holeNumber < state.holeCount,
                ) { WatchChevron(pointsRight = true, color = WatchColors.White, iconSize = scale.dp(18f).coerceAtLeast(14.dp)) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(scale.dp(6f).coerceAtLeast(3.dp))) {
                StepperRow(
                    label = "그린까지",
                    value = state.strokesToGreen,
                    canDecrement = state.strokesToGreen > 0,
                    onDecrement = { onAction(WearSync.ACTION_DECREMENT_TO_GREEN) },
                    onIncrement = { onAction(WearSync.ACTION_INCREMENT_TO_GREEN) },
                    scale = scale,
                )
                StepperRow(
                    label = "숏 · 퍼팅",
                    value = state.strokesPutt,
                    canDecrement = state.strokesPutt > 0,
                    onDecrement = { onAction(WearSync.ACTION_DECREMENT_PUTT) },
                    onIncrement = { onAction(WearSync.ACTION_INCREMENT_PUTT) },
                    scale = scale,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val summary = holeSummary(holeStrokes, state.par)
                Text(
                    buildAnnotatedString {
                        append("이 홀 ")
                        withStyle(
                            SpanStyle(
                                fontFamily = WatchFonts.Number,
                                fontSize = scale.sp(17f, minSp = 13f),
                            ),
                        ) { append("${summary.strokes}") }
                        append("타 · ${summary.gradeName}")
                    },
                    color = WatchColors.White,
                    fontSize = scale.sp(13f, minSp = 11f),
                    // 더 키가 큰 Barlow 숫자(17f/13f) 기준 — 기본 20sp 줄 높이가 남지 않게.
                    lineHeight = scale.lineSp(17f, minSp = 13f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                WatchTotalLine(
                    total = runningTotal(state.scoreBefore, holeStrokes, state.par),
                    labelDesignPx = 11f,
                    valueDesignPx = 14f,
                    valueMinSp = 12f,
                    scale = scale,
                )
            }
        }
    }
}

/** `[−] 라벨 + 값 [+]` 한 줄. 좌우 여백은 시안의 48px. */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    canDecrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    scale: WatchScale,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = scale.dp(48f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.dp(8f).coerceAtLeast(4.dp)),
    ) {
        WatchCircleButton(
            onClick = onDecrement,
            size = scale.touch(52f),
            background = WatchColors.ButtonDark,
            description = "$label 빼기",
            enabled = canDecrement,
        ) {
            Text(
                "−",
                color = WatchColors.White,
                fontSize = scale.sp(26f, minSp = 18f),
                lineHeight = scale.lineSp(26f, minSp = 18f),
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = WatchColors.Label,
                fontSize = scale.sp(11f),
                lineHeight = scale.sp(11f) * 1.2f,
                maxLines = 1,
            )
            Text(
                "$value",
                color = WatchColors.White,
                fontFamily = WatchFonts.Number,
                fontWeight = FontWeight.Bold,
                fontSize = scale.sp(46f, minSp = 26f),
                lineHeight = scale.sp(46f, minSp = 26f),
                maxLines = 1,
            )
        }
        WatchCircleButton(
            onClick = onIncrement,
            size = scale.touch(52f),
            background = WatchColors.Accent,
            description = "$label 더하기",
        ) {
            Text(
                "+",
                color = WatchColors.White,
                fontSize = scale.sp(26f, minSp = 18f),
                lineHeight = scale.lineSp(26f, minSp = 18f),
            )
        }
    }
}

package com.golfrecorder.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.wear.compose.material.Text
import com.golfrecorder.wearsync.WearSync

/** 라운드 진행 중 기본 화면: 진행 링 + 홀 이동 + 세 스테퍼(그린까지·숏게임·퍼팅) + 이 홀 결과/합계 한 줄. */
@Composable
fun WatchPlayScreen(
    state: RoundUiState,
    badge: StatusBadge,
    battery: BatteryInfo?,
    onAction: (String) -> Unit,
    onMove: (HoleMoveDirection) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(WatchColors.Background)) {
        val scale = WatchScale.forScreen(minOf(maxWidth, maxHeight))
        val holeStrokes = holeStrokes(state)

        WatchRing(ringSegments(state.holeCount, state.holeNumber), Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 맨 위 줄이 둥근 가장자리·링에 잘리지 않게 최소 16dp. 아래 한 줄 요약은 둥근 아래 가장자리에 닿지
                // 않게 시안의 38px만큼 띄운다.
                .padding(top = scale.dp(24f).coerceAtLeast(16.dp), bottom = scale.dp(38f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WatchStatusLine(badge, battery, scale)

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

            Column(verticalArrangement = Arrangement.spacedBy(scale.dp(6f).coerceAtLeast(2.dp))) {
                StepperRow(
                    label = "그린까지",
                    value = state.strokesToGreen,
                    canDecrement = state.strokesToGreen > 0,
                    onDecrement = { onAction(WearSync.ACTION_DECREMENT_TO_GREEN) },
                    onIncrement = { onAction(WearSync.ACTION_INCREMENT_TO_GREEN) },
                    scale = scale,
                )
                // 구버전 폰은 숏게임을 따로 모르므로(퍼팅 값이 숏게임 합산) 이 줄은 0으로 두고 누르지 못하게 한다.
                StepperRow(
                    label = "숏게임",
                    value = state.strokesShortGame,
                    canDecrement = state.strokesShortGame > 0,
                    canIncrement = state.shortGameSupported,
                    onDecrement = { onAction(WearSync.ACTION_DECREMENT_SHORT_GAME) },
                    onIncrement = { onAction(WearSync.ACTION_INCREMENT_SHORT_GAME) },
                    scale = scale,
                )
                StepperRow(
                    label = "퍼팅",
                    value = state.strokesPutt,
                    canDecrement = state.strokesPutt > 0,
                    onDecrement = { onAction(WearSync.ACTION_DECREMENT_PUTT) },
                    onIncrement = { onAction(WearSync.ACTION_INCREMENT_PUTT) },
                    scale = scale,
                )
            }

            val summary = holeSummary(holeStrokes, state.par)
            val total = runningTotal(state.scoreBefore, holeStrokes, state.par)
            val numberStyle = SpanStyle(fontFamily = WatchFonts.Number, fontSize = scale.sp(16f, minSp = 13f))
            Text(
                buildAnnotatedString {
                    append("이 홀 ")
                    withStyle(numberStyle) { append("${summary.strokes}") }
                    append("타 · ${summary.gradeName} ")
                    withStyle(SpanStyle(color = WatchColors.Label, fontWeight = FontWeight.Normal)) { append("· 합계 ") }
                    withStyle(numberStyle) { append(formatTotal(total)) }
                },
                color = WatchColors.White,
                fontSize = scale.sp(12f),
                // 더 키가 큰 Barlow 숫자(16f) 기준 — 기본 20sp 줄 높이가 남지 않게.
                lineHeight = scale.lineSp(16f, minSp = 13f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/**
 * `라벨  [−] 값 [+]` 한 줄. 라벨은 왼쪽(남는 폭), −/+와 값은 오른쪽에 붙는다. 좌우 여백은 시안의 36px.
 * [canIncrement]가 false면 −/+를 모두 눌리지 않게 흐리게 한다(구버전 폰의 숏게임 줄).
 */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    canDecrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    scale: WatchScale,
    canIncrement: Boolean = true,
) {
    // 세 줄이 198dp(40mm) 화면에 들어가야 해서 버튼 하한을 지금(38dp)보다 낮춘 28dp로 둔다.
    val buttonSize = scale.touch(46f, minDp = 28f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = scale.dp(36f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.dp(6f).coerceAtLeast(3.dp)),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f).padding(start = scale.dp(6f)),
            color = WatchColors.White,
            fontSize = scale.sp(17f, minSp = 12f),
            lineHeight = scale.lineSp(17f, minSp = 12f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        WatchCircleButton(
            onClick = onDecrement,
            size = buttonSize,
            background = WatchColors.ButtonDark,
            description = "$label 빼기",
            enabled = canIncrement && canDecrement,
        ) {
            Text(
                "−",
                color = WatchColors.White,
                fontSize = scale.sp(24f, minSp = 16f),
                lineHeight = scale.lineSp(24f, minSp = 16f),
            )
        }
        Text(
            "$value",
            modifier = Modifier.widthIn(min = scale.dp(34f)),
            color = WatchColors.White,
            fontFamily = WatchFonts.Number,
            fontWeight = FontWeight.Bold,
            fontSize = scale.sp(40f, minSp = 22f),
            lineHeight = scale.sp(40f, minSp = 22f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        WatchCircleButton(
            onClick = onIncrement,
            size = buttonSize,
            background = WatchColors.Accent,
            description = "$label 더하기",
            enabled = canIncrement,
        ) {
            Text(
                "+",
                color = WatchColors.White,
                fontSize = scale.sp(24f, minSp = 16f),
                lineHeight = scale.lineSp(24f, minSp = 16f),
            )
        }
    }
}

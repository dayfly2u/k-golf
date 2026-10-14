package com.golfrecorder.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.golfrecorder.wearsync.WearSync

/**
 * 워치 배터리가 10% 미만/5% 미만으로 떨어질 때 잠깐 뜨는 경고 화면 — 퍼센트를 크게, 단계별 안내를 아래에 보인다
 * (10% 미만은 호박색, 5% 미만은 붉은색). 아무 데나 누르면 닫힌다. [warning]은 [WearSync].BATTERY_WARNING_*.
 */
@Composable
fun WatchBatteryAlertScreen(percent: Int, warning: Int, onDismiss: () -> Unit) {
    val critical = warning == WearSync.BATTERY_WARNING_CRITICAL
    val accent = if (critical) WatchColors.CriticalText else WatchColors.Yellow
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

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scale.dp(4f).coerceAtLeast(2.dp)),
        ) {
            Text(
                "와치 배터리",
                color = WatchColors.Label,
                fontSize = scale.sp(12f),
                lineHeight = scale.lineSp(12f),
                maxLines = 1,
            )
            Text(
                "$percent%",
                color = accent,
                fontFamily = WatchFonts.Number,
                fontWeight = FontWeight.Bold,
                fontSize = scale.sp(96f, minSp = 52f),
                lineHeight = scale.sp(96f, minSp = 52f) * 0.9f,
                maxLines = 1,
            )
            Text(
                if (critical) "곧 꺼질 수 있어요" else "충전이 필요해요",
                color = WatchColors.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = scale.sp(14f, minSp = 11f),
                lineHeight = scale.lineSp(14f, minSp = 11f),
                maxLines = 1,
            )
            if (critical) {
                Text(
                    "꺼지면 폰으로 입력하세요",
                    color = WatchColors.Label,
                    fontSize = scale.sp(12f),
                    lineHeight = scale.lineSp(12f),
                    maxLines = 1,
                )
            }
        }

        Text(
            "화면을 누르면 닫혀요",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = scale.dp(48f).coerceAtLeast(22.dp)),
            color = WatchColors.IdleHint,
            fontSize = scale.sp(11f),
            lineHeight = scale.lineSp(11f),
            maxLines = 1,
        )
    }
}

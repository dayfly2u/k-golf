package com.golfrecorder.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.wear.compose.material.Text
import com.golfrecorder.wearsync.WearSync

/**
 * 홀 기록 화면 맨 위 한 줄 — 연결됨(점 + 글자) / 호박색 알약(끊김, GPS 실패). 워치 배터리가 경고 구간이면
 * ([battery]의 경고 단계가 있을 때) 연결 상태 대신 "배터리 N%" 알약을 보인다: 10% 미만은 호박색, 5% 미만은 붉은색.
 * 연결이 끊겨 있으면 알약의 Wi-Fi 끊김 아이콘이 그 사실을 알려준다. 잠깐 뜨는 GPS 실패 안내가 가장 우선이다.
 */
@Composable
fun WatchStatusLine(badge: StatusBadge, battery: BatteryInfo?, scale: WatchScale) {
    val warning = battery?.warning ?: WearSync.BATTERY_WARNING_NONE
    if (battery != null && warning != WearSync.BATTERY_WARNING_NONE && badge != StatusBadge.GPS_FAILURE) {
        WatchAmberPill(
            text = batteryPillText(battery.percent),
            showWifiOff = badge == StatusBadge.DISCONNECTED,
            scale = scale,
            critical = warning == WearSync.BATTERY_WARNING_CRITICAL,
        )
        return
    }
    when (badge) {
        StatusBadge.CONNECTED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(scale.dp(5f).coerceAtLeast(3.dp)),
        ) {
            Box(
                Modifier.size(scale.dp(6f).coerceAtLeast(4.dp)).clip(CircleShape)
                    .background(WatchColors.ConnectedDot),
            )
            Text(
                "폰 연결됨",
                color = WatchColors.Label,
                fontSize = scale.sp(11f),
                lineHeight = scale.lineSp(11f),
                maxLines = 1,
            )
        }
        StatusBadge.DISCONNECTED -> WatchAmberPill("폰 연결 끊김", showWifiOff = true, scale = scale)
        StatusBadge.GPS_FAILURE -> WatchAmberPill("GPS 신호 없음", showWifiOff = false, scale = scale)
    }
}

/** 호박색 알약 — 시안 WatchIdle의 "폰 연결 끊김"(배경 #3A2A0E, 글자 #F2C14E, 600). [critical]이면 같은 모양의 붉은 알약. */
@Composable
fun WatchAmberPill(text: String, showWifiOff: Boolean, scale: WatchScale, critical: Boolean = false) {
    val contentColor = if (critical) WatchColors.CriticalText else WatchColors.Yellow
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (critical) WatchColors.CriticalPillBackground else WatchColors.AmberPillBackground)
            .padding(
                horizontal = scale.dp(12f).coerceAtLeast(8.dp),
                vertical = scale.dp(5f).coerceAtLeast(3.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.dp(6f).coerceAtLeast(3.dp)),
    ) {
        if (showWifiOff) {
            WatchWifiOffIcon(iconSize = scale.dp(14f).coerceAtLeast(10.dp), color = contentColor)
        }
        Text(
            text,
            color = contentColor,
            fontSize = scale.sp(12f),
            lineHeight = scale.lineSp(12f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

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

/** 홀 기록 화면 맨 위 한 줄 — 연결됨(점 + 글자) / 호박색 알약(끊김, GPS 실패). */
@Composable
fun WatchStatusLine(badge: StatusBadge, scale: WatchScale) {
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

/** 호박색 알약 — 시안 WatchIdle의 "폰 연결 끊김"(배경 #3A2A0E, 글자 #F2C14E, 600). */
@Composable
fun WatchAmberPill(text: String, showWifiOff: Boolean, scale: WatchScale) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(WatchColors.AmberPillBackground)
            .padding(
                horizontal = scale.dp(12f).coerceAtLeast(8.dp),
                vertical = scale.dp(5f).coerceAtLeast(3.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.dp(6f).coerceAtLeast(3.dp)),
    ) {
        if (showWifiOff) {
            WatchWifiOffIcon(iconSize = scale.dp(14f).coerceAtLeast(10.dp), color = WatchColors.Yellow)
        }
        Text(
            text,
            color = WatchColors.Yellow,
            fontSize = scale.sp(12f),
            lineHeight = scale.lineSp(12f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

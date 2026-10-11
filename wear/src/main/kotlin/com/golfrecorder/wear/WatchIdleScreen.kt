package com.golfrecorder.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.wear.compose.material.Text

/** 라운드가 없을 때(폰에서 시작 전) 보이는 화면. 연결이 끊겨 있으면 위쪽에 호박색 알약. */
@Composable
fun WatchIdleScreen(isPhoneConnected: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize().background(WatchColors.Background)) {
        val scale = WatchScale.forScreen(minOf(maxWidth, maxHeight))

        if (!isPhoneConnected) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = scale.dp(46f).coerceAtLeast(22.dp))) {
                WatchAmberPill("폰 연결 끊김", showWifiOff = true, scale = scale)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scale.dp(14f).coerceAtLeast(6.dp)),
        ) {
            Box(
                modifier = Modifier
                    .size(scale.dp(64f).coerceAtLeast(40.dp))
                    .clip(RoundedCornerShape(scale.dp(20f).coerceAtLeast(12.dp)))
                    .background(WatchColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                WatchFlagIcon(iconSize = scale.dp(34f).coerceAtLeast(22.dp), color = WatchColors.White)
            }
            Text(
                "K-Golf",
                color = WatchColors.White,
                fontSize = scale.sp(20f, minSp = 15f),
                lineHeight = scale.lineSp(20f, minSp = 15f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                "폰에서 라운드를 시작하면\n여기에 홀이 표시돼요",
                color = WatchColors.IdleBody,
                fontSize = scale.sp(14f, minSp = 11f),
                lineHeight = scale.sp(14f, minSp = 11f) * 1.5f,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            "연결되면 자동으로 따라와요",
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = scale.dp(48f).coerceAtLeast(22.dp)),
            color = WatchColors.IdleHint,
            fontSize = scale.sp(11f),
            lineHeight = scale.lineSp(11f),
            maxLines = 1,
        )
    }
}

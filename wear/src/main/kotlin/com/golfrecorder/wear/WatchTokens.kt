package com.golfrecorder.wear

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** 워치 시안(docs/design/2026-10-redesign/Watch*.dc.html)의 색. */
object WatchColors {
    val Background = Color(0xFF000000)
    val ButtonDark = Color(0xFF1E2A22)
    val RingRemaining = Color(0xFF26332B)
    val Accent = Color(0xFF2E7D4F)
    val Yellow = Color(0xFFF2C14E)
    val Label = Color(0xFF9AA59C)
    val ConnectedDot = Color(0xFF4CAF50)
    val AmberPillBackground = Color(0xFF3A2A0E)
    // 배터리 5% 미만(CRITICAL) 경고 — 시안에 없는 색이라 호박색 알약과 같은 톤으로 붉게 맞췄다.
    val CriticalPillBackground = Color(0xFF3D1616)
    val CriticalText = Color(0xFFFF8A80)
    val IdleBody = Color(0xFFC9D1CB)
    val IdleHint = Color(0xFF7E8A82)
    val White = Color(0xFFFFFFFF)
}

object WatchFonts {
    /** 숫자(타수·홀 번호·합계)에만 쓴다 — 한글은 시스템 글꼴. */
    val Number = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )
}

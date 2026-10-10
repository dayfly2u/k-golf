package com.golfrecorder.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class GolfTokensTest {
    @Test
    fun `핵심 색상이 스펙 팔레트와 정확히 일치한다`() {
        assertEquals(Color(0xFF1F5C3F), GolfTokens.FieldGreen)
        assertEquals(Color(0xFF123A28), GolfTokens.FieldGreenDark)
        assertEquals(Color(0xFFF2C14E), GolfTokens.Accent)
        assertEquals(Color(0xFFF4F5EF), GolfTokens.Background)
        assertEquals(Color(0xFFFFFFFF), GolfTokens.CardBackground)
        assertEquals(Color(0xFF18241D), GolfTokens.TextPrimary)
        assertEquals(Color(0xFF56635A), GolfTokens.TextSecondary)
        assertEquals(Color(0xFFECEFE7), GolfTokens.Divider)
        assertEquals(Color(0xFFCDD3C8), GolfTokens.Border)
    }

    @Test
    fun `벌타 색상이 스펙과 정확히 일치한다`() {
        assertEquals(Color(0xFFFFF6E8), GolfTokens.ObBackground)
        assertEquals(Color(0xFFE3A34A), GolfTokens.ObBorder)
        assertEquals(Color(0xFF7A4A0C), GolfTokens.ObText)
        assertEquals(Color(0xFFEEF3F9), GolfTokens.HazardBackground)
        assertEquals(Color(0xFF6B8FB8), GolfTokens.HazardBorder)
        assertEquals(Color(0xFF24476E), GolfTokens.HazardText)
    }

    @Test
    fun `스코어 셀 색상이 스펙과 정확히 일치한다`() {
        assertEquals(GolfTokens.Border, GolfTokens.ScoreUnderParBorder)
        assertEquals(Color(0xFFDCE6F1), GolfTokens.ScoreBogeyBackground)
        assertEquals(Color(0xFF6B8FB8), GolfTokens.ScoreDoubleOrWorseBackground)
        assertEquals(Color(0xFFDADFD4), GolfTokens.ProgressRemaining)
    }

    @Test
    fun `모서리와 터치영역이 스펙 범위 중앙값과 일치한다`() {
        assertEquals(20.dp, GolfTokens.CardCorner)
        assertEquals(16.dp, GolfTokens.ButtonCorner)
        assertEquals(999.dp, GolfTokens.ChipCorner)
        assertEquals(44.dp, GolfTokens.MinTouchTarget)
        assertEquals(58.dp, GolfTokens.PrimaryButtonHeight)
    }
}

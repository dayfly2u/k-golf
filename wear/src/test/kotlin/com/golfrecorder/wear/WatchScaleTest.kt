package com.golfrecorder.wear

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchScaleTest {
    // 갤럭시 워치 SM-R900: 396px / 320dpi = 198dp → 360 기준의 0.55배
    private val scale = WatchScale.forScreen(198.dp)

    @Test
    fun `dp scales proportionally to the 360 design`() {
        assertEquals(55f, scale.dp(100f).value, 0.01f)
    }

    @Test
    fun `sp is proportional above its floor`() {
        assertEquals(52.8f, scale.sp(96f, minSp = 40f).value, 0.01f)
    }

    @Test
    fun `sp never goes below its floor`() {
        assertEquals(10f, scale.sp(11f).value, 0.001f)
        assertEquals(26f, scale.sp(46f, minSp = 26f).value, 0.001f)
    }

    @Test
    fun `lineSp is the font size times 1_2 so the text does not keep a 20sp default line box`() {
        // 11px → 6.05sp는 하한 10sp로 올라가고, 줄 높이는 그 10sp의 1.2배.
        assertEquals(12f, scale.lineSp(11f).value, 0.01f)
        // 96px → 52.8sp (하한 40sp 위) → 63.36sp.
        assertEquals(63.36f, scale.lineSp(96f, minSp = 40f).value, 0.01f)
    }

    @Test
    fun `touch targets never go below their floor`() {
        assertEquals(38f, scale.touch(52f).value, 0.001f)
        assertEquals(34f, scale.touch(44f, minDp = 34f).value, 0.001f)
        assertEquals(55f, scale.touch(100f).value, 0.01f)
    }
}

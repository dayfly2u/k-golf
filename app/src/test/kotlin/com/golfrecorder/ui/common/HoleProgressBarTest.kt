package com.golfrecorder.ui.common

import com.golfrecorder.ui.theme.GolfTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class HoleProgressBarTest {
    @Test
    fun `라이브 모드 - 지나온 홀은 FieldGreen`() {
        assertEquals(GolfTokens.FieldGreen, holeSegmentColor(holeNumber = 3, currentHoleNumber = 7, mode = HoleProgressMode.LIVE))
    }

    @Test
    fun `라이브 모드 - 현재 홀은 Accent`() {
        assertEquals(GolfTokens.Accent, holeSegmentColor(holeNumber = 7, currentHoleNumber = 7, mode = HoleProgressMode.LIVE))
    }

    @Test
    fun `라이브 모드 - 남은 홀은 ProgressRemaining`() {
        assertEquals(GolfTokens.ProgressRemaining, holeSegmentColor(holeNumber = 12, currentHoleNumber = 7, mode = HoleProgressMode.LIVE))
    }

    @Test
    fun `리뷰 모드 - 보고 있는 홀만 Accent`() {
        assertEquals(GolfTokens.Accent, holeSegmentColor(holeNumber = 4, currentHoleNumber = 4, mode = HoleProgressMode.REVIEW))
    }

    @Test
    fun `리뷰 모드 - 나머지는 전부 FieldGreen(이전 이후 구분 없음)`() {
        assertEquals(GolfTokens.FieldGreen, holeSegmentColor(holeNumber = 2, currentHoleNumber = 4, mode = HoleProgressMode.REVIEW))
        assertEquals(GolfTokens.FieldGreen, holeSegmentColor(holeNumber = 15, currentHoleNumber = 4, mode = HoleProgressMode.REVIEW))
    }
}

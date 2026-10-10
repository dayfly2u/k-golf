package com.golfrecorder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreFormattingTest {
    @Test
    fun `formatToPar는 0을 E로 표시한다`() {
        assertEquals("E", formatToPar(0))
    }

    @Test
    fun `formatToPar는 양수에 플러스를 붙인다`() {
        assertEquals("+3", formatToPar(3))
    }

    @Test
    fun `formatToPar는 음수를 그대로 표시한다`() {
        assertEquals("-2", formatToPar(-2))
    }

    @Test
    fun `scoreGradeLabel 버디 이하는 이글도 포함한다`() {
        assertEquals("버디 이하", scoreGradeLabel(-2))
        assertEquals("버디 이하", scoreGradeLabel(-1))
    }

    @Test
    fun `scoreGradeLabel 파는 0`() {
        assertEquals("파", scoreGradeLabel(0))
    }

    @Test
    fun `scoreGradeLabel 보기는 1`() {
        assertEquals("보기", scoreGradeLabel(1))
    }

    @Test
    fun `scoreGradeLabel 더블은 2`() {
        assertEquals("더블", scoreGradeLabel(2))
    }

    @Test
    fun `scoreGradeLabel 트리플 이상은 3 이상 전부`() {
        assertEquals("트리플+", scoreGradeLabel(3))
        assertEquals("트리플+", scoreGradeLabel(5))
    }
}

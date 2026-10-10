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
    fun `holeScoreTier 버디 이하는 0구간(별도 색)`() {
        assertEquals(0, holeScoreTier(-2))
        assertEquals(0, holeScoreTier(-1))
    }

    @Test
    fun `holeScoreTier 파는 1구간`() {
        assertEquals(1, holeScoreTier(0))
    }

    @Test
    fun `holeScoreTier 보기는 2구간, 더블은 3구간`() {
        assertEquals(2, holeScoreTier(1))
        assertEquals(3, holeScoreTier(2))
    }

    @Test
    fun `holeScoreTier 트리플 이상은 4구간`() {
        assertEquals(4, holeScoreTier(3))
        assertEquals(4, holeScoreTier(7))
    }

    @Test
    fun `scoreTierLabel은 모든 파 대비 점수에서 scoreGradeLabel과 같다`() {
        for (scoreToPar in -4..8) {
            assertEquals(scoreGradeLabel(scoreToPar), scoreTierLabel(holeScoreTier(scoreToPar)))
        }
    }

    @Test
    fun `strokeScoreTier 총타수 구간 경계`() {
        assertEquals(null, strokeScoreTier(0))
        assertEquals(1, strokeScoreTier(60))
        assertEquals(1, strokeScoreTier(79))
        assertEquals(1, strokeScoreTier(80))
        assertEquals(1, strokeScoreTier(89))
        assertEquals(2, strokeScoreTier(90))
        assertEquals(2, strokeScoreTier(94))
        assertEquals(3, strokeScoreTier(95))
        assertEquals(3, strokeScoreTier(99))
        assertEquals(4, strokeScoreTier(100))
        assertEquals(4, strokeScoreTier(130))
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

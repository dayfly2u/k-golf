package com.golfrecorder.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingVisualsTest {
    @Test
    fun `4점 5점 반이면 앞 네 칸은 가득, 다섯 번째는 반`() {
        assertEquals(1f, fillFraction(4.5f, 1), 0.001f)
        assertEquals(1f, fillFraction(4.5f, 4), 0.001f)
        assertEquals(0.5f, fillFraction(4.5f, 5), 0.001f)
    }

    @Test
    fun `0점이면 모든 칸이 빔`() {
        for (index in 1..5) assertEquals(0f, fillFraction(0f, index), 0.001f)
    }

    @Test
    fun `5점이면 모든 칸이 가득`() {
        for (index in 1..5) assertEquals(1f, fillFraction(5f, index), 0.001f)
    }

    @Test
    fun `정수 난이도 3이면 세 칸까지 채움`() {
        assertEquals(1f, fillFraction(3f, 3), 0.001f)
        assertEquals(0f, fillFraction(3f, 4), 0.001f)
    }
}

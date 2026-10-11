package com.golfrecorder.wear

import org.junit.Assert.assertEquals
import org.junit.Test

class RoundDisplayTest {

    @Test
    fun `grade names from -3 to 3 match the design table`() {
        assertEquals("앨버트로스", holeGradeName(-3))
        assertEquals("이글", holeGradeName(-2))
        assertEquals("버디", holeGradeName(-1))
        assertEquals("파", holeGradeName(0))
        assertEquals("보기", holeGradeName(1))
        assertEquals("더블", holeGradeName(2))
        assertEquals("트리플", holeGradeName(3))
    }

    @Test
    fun `grades outside the table fall back to a signed number`() {
        assertEquals("+4", holeGradeName(4))
        assertEquals("+7", holeGradeName(7))
        assertEquals("-4", holeGradeName(-4))
    }

    @Test
    fun `formatTotal shows a plus sign for positive, bare zero, and minus for negative`() {
        assertEquals("+5", formatTotal(5))
        assertEquals("0", formatTotal(0))
        assertEquals("-2", formatTotal(-2))
    }

    @Test
    fun `holeSummary grades by strokes minus par`() {
        assertEquals(HoleSummary(4, "파"), holeSummary(4, 4))
        assertEquals(HoleSummary(5, "보기"), holeSummary(5, 4))
        assertEquals(HoleSummary(2, "버디"), holeSummary(2, 3))
    }

    @Test
    fun `holeSummary with no strokes has a dash instead of a grade`() {
        assertEquals(HoleSummary(0, "-"), holeSummary(0, 4))
    }

    @Test
    fun `runningTotal adds the current hole only once it has strokes`() {
        // 시안 예: 이전까지 +5, 파 4
        assertEquals(5, runningTotal(scoreBefore = 5, holeStrokes = 0, par = 4))
        assertEquals(5, runningTotal(scoreBefore = 5, holeStrokes = 4, par = 4))
        assertEquals(4, runningTotal(scoreBefore = 5, holeStrokes = 3, par = 4))
        assertEquals(7, runningTotal(scoreBefore = 5, holeStrokes = 6, par = 4))
    }

    @Test
    fun `ring marks earlier holes done, the current hole, and the rest remaining`() {
        val segments = ringSegments(holeCount = 18, currentHole = 7)
        assertEquals(18, segments.size)
        assertEquals(List(6) { RingSegment.DONE }, segments.subList(0, 6))
        assertEquals(RingSegment.CURRENT, segments[6])
        assertEquals(List(11) { RingSegment.REMAINING }, segments.subList(7, 18))
    }

    @Test
    fun `ring on the first hole has only the current segment lit`() {
        val segments = ringSegments(holeCount = 9, currentHole = 1)
        assertEquals(9, segments.size)
        assertEquals(RingSegment.CURRENT, segments[0])
        assertEquals(List(8) { RingSegment.REMAINING }, segments.subList(1, 9))
    }

    @Test
    fun `ring on the last hole has every earlier segment done`() {
        val segments = ringSegments(holeCount = 9, currentHole = 9)
        assertEquals(List(8) { RingSegment.DONE }, segments.subList(0, 8))
        assertEquals(RingSegment.CURRENT, segments[8])
    }

    @Test
    fun `ring with a non-positive hole count is empty`() {
        assertEquals(emptyList<RingSegment>(), ringSegments(holeCount = 0, currentHole = 1))
    }

    @Test
    fun `gps failure wins over disconnected, which wins over connected`() {
        assertEquals(StatusBadge.GPS_FAILURE, statusBadge(isPhoneConnected = true, showGpsFailure = true))
        assertEquals(StatusBadge.GPS_FAILURE, statusBadge(isPhoneConnected = false, showGpsFailure = true))
        assertEquals(StatusBadge.DISCONNECTED, statusBadge(isPhoneConnected = false, showGpsFailure = false))
        assertEquals(StatusBadge.CONNECTED, statusBadge(isPhoneConnected = true, showGpsFailure = false))
    }

    @Test
    fun `hole move caption follows the direction`() {
        assertEquals("다음 홀로 이동", holeMoveCaption(HoleMoveDirection.NEXT))
        assertEquals("이전 홀로 이동", holeMoveCaption(HoleMoveDirection.PREVIOUS))
    }
}

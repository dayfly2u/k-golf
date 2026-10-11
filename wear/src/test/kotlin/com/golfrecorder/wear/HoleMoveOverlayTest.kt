package com.golfrecorder.wear

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HoleMoveOverlayTest {

    @Test
    fun `hole change without a request shows nothing`() = runTest {
        val overlay = HoleMoveOverlayState()
        overlay.onHoleChanged()
        assertNull(overlay.shownDirection)
    }

    @Test
    fun `request then hole change shows the screen for 2_5 seconds`() = runTest {
        val overlay = HoleMoveOverlayState()
        overlay.request(HoleMoveDirection.NEXT)
        launch { overlay.onHoleChanged() }
        runCurrent()
        assertEquals(HoleMoveDirection.NEXT, overlay.shownDirection)
        assertNull(overlay.pending)

        advanceTimeBy(2_499)
        runCurrent()
        assertEquals(HoleMoveDirection.NEXT, overlay.shownDirection)

        advanceTimeBy(2)
        runCurrent()
        assertNull(overlay.shownDirection)
    }

    @Test
    fun `another hole change while shown restarts the timer`() = runTest {
        val overlay = HoleMoveOverlayState()
        overlay.request(HoleMoveDirection.NEXT)
        val first = launch { overlay.onHoleChanged() }
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        // LaunchedEffect가 홀 번호 키 변경으로 취소되고 다시 시작되는 상황.
        first.cancel()
        val second = launch { overlay.onHoleChanged() }
        runCurrent()

        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(HoleMoveDirection.NEXT, overlay.shownDirection)

        advanceTimeBy(600)
        runCurrent()
        assertNull(overlay.shownDirection)
        second.cancel()
    }

    @Test
    fun `a new request while shown switches direction`() = runTest {
        val overlay = HoleMoveOverlayState()
        overlay.request(HoleMoveDirection.NEXT)
        val first = launch { overlay.onHoleChanged() }
        runCurrent()

        overlay.request(HoleMoveDirection.PREVIOUS)
        first.cancel()
        launch { overlay.onHoleChanged() }
        runCurrent()
        assertEquals(HoleMoveDirection.PREVIOUS, overlay.shownDirection)
    }

    @Test
    fun `an unanswered request expires after 5 seconds`() = runTest {
        val overlay = HoleMoveOverlayState()
        overlay.request(HoleMoveDirection.NEXT)
        launch { overlay.expireRequestAfterTimeout() }
        runCurrent()

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(HoleMoveDirection.NEXT, overlay.pending)

        advanceTimeBy(2)
        runCurrent()
        assertNull(overlay.pending)

        // 만료된 뒤 폰에서 홀이 바뀌어도 이동 화면은 뜨지 않는다.
        overlay.onHoleChanged()
        assertNull(overlay.shownDirection)
    }

    @Test
    fun `dismiss hides the screen immediately`() = runTest {
        val overlay = HoleMoveOverlayState()
        overlay.request(HoleMoveDirection.NEXT)
        launch { overlay.onHoleChanged() }
        runCurrent()
        overlay.dismiss()
        assertNull(overlay.shownDirection)
    }
}

package com.golfrecorder.wear

import com.golfrecorder.wearsync.WearSync
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchBatteryTest {
    private val none = WearSync.BATTERY_WARNING_NONE
    private val low = WearSync.BATTERY_WARNING_LOW
    private val critical = WearSync.BATTERY_WARNING_CRITICAL

    private fun info(percent: Int, charging: Boolean = false) = BatteryInfo(percent, charging)

    @Test
    fun `10퍼센트 미만이면 LOW, 5퍼센트 미만이면 CRITICAL`() {
        assertEquals(none, batteryWarning(100, charging = false))
        assertEquals(none, batteryWarning(10, charging = false))
        assertEquals(low, batteryWarning(9, charging = false))
        assertEquals(low, batteryWarning(5, charging = false))
        assertEquals(critical, batteryWarning(4, charging = false))
        assertEquals(critical, batteryWarning(0, charging = false))
    }

    @Test
    fun `충전 중이면 퍼센트가 낮아도 경고하지 않는다`() {
        assertEquals(none, batteryWarning(3, charging = true))
        assertEquals(none, info(8, charging = true).warning)
    }

    @Test
    fun `경고 단계가 올라갈 때만 rose`() {
        assertTrue(batteryWarningRose(info(11), info(9)))
        assertTrue(batteryWarningRose(info(9), info(4)))
        // 같은 단계에서 퍼센트만 내려가는 건 올라간 게 아니다
        assertFalse(batteryWarningRose(info(9), info(8)))
        // 충전으로 해제되거나 이미 경고 없음
        assertFalse(batteryWarningRose(info(4), info(4, charging = true)))
        assertFalse(batteryWarningRose(info(50), info(49)))
    }

    @Test
    fun `처음 읽은 값이 경고 구간이면 올라온 것으로 본다`() {
        assertTrue(batteryWarningRose(null, info(8)))
        assertFalse(batteryWarningRose(null, info(80)))
    }

    @Test
    fun `폰에는 경고 단계가 바뀔 때와 경고 중 퍼센트가 바뀔 때만 보낸다`() {
        // 단계 변화: 시작, 격상, 해제(충전)
        assertTrue(shouldReportBattery(info(11), info(9)))
        assertTrue(shouldReportBattery(info(9), info(4)))
        assertTrue(shouldReportBattery(info(4), info(4, charging = true)))
        // 경고 중 퍼센트 변화
        assertTrue(shouldReportBattery(info(9), info(8)))
        // 경고 없음 구간의 퍼센트 변화는 보내지 않는다
        assertFalse(shouldReportBattery(info(50), info(49)))
        // 같은 값
        assertFalse(shouldReportBattery(info(9), info(9)))
    }

    @Test
    fun `첫 값은 경고 구간일 때만 폰에 보낸다`() {
        assertTrue(shouldReportBattery(null, info(8)))
        assertFalse(shouldReportBattery(null, info(80)))
    }

    @Test
    fun `알약 문구는 퍼센트만`() {
        assertEquals("배터리 9%", batteryPillText(9))
    }

    @Test
    fun `진동은 LOW 한 번 CRITICAL 세 번 경고 없음은 없음`() {
        assertArrayEquals(longArrayOf(0, 500), batteryVibration(low))
        assertArrayEquals(longArrayOf(0, 400, 150, 400, 150, 400), batteryVibration(critical))
        assertNull(batteryVibration(none))
    }
}

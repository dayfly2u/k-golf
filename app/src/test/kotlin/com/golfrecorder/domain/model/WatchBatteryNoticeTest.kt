package com.golfrecorder.domain.model

import com.golfrecorder.wearsync.WearSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WatchBatteryNoticeTest {
    private val none = WearSync.BATTERY_WARNING_NONE
    private val low = WearSync.BATTERY_WARNING_LOW
    private val critical = WearSync.BATTERY_WARNING_CRITICAL

    @Test
    fun `경고 단계가 올라가면 알린다`() {
        assertEquals(WatchBatteryNoticeAction.ALERT, watchBatteryNoticeAction(none, low))
        assertEquals(WatchBatteryNoticeAction.ALERT, watchBatteryNoticeAction(low, critical))
        // 처음부터 5% 미만으로 보고가 온 경우
        assertEquals(WatchBatteryNoticeAction.ALERT, watchBatteryNoticeAction(none, critical))
    }

    @Test
    fun `같은 단계에서 퍼센트만 바뀌면 조용히 갱신한다`() {
        assertEquals(WatchBatteryNoticeAction.UPDATE, watchBatteryNoticeAction(low, low))
        assertEquals(WatchBatteryNoticeAction.UPDATE, watchBatteryNoticeAction(critical, critical))
    }

    @Test
    fun `경고가 풀리면 알림을 지운다`() {
        assertEquals(WatchBatteryNoticeAction.CANCEL, watchBatteryNoticeAction(low, none))
        assertEquals(WatchBatteryNoticeAction.CANCEL, watchBatteryNoticeAction(critical, none))
        assertEquals(WatchBatteryNoticeAction.CANCEL, watchBatteryNoticeAction(none, none))
    }

    @Test
    fun `알림 제목은 퍼센트 문구는 단계별`() {
        assertEquals("와치 배터리 9%", watchBatteryNoticeTitle(9))
        assertEquals("배터리가 부족해요. 충전이 필요해요.", watchBatteryNoticeBody(low))
        assertEquals("곧 꺼질 수 있어요. 꺼지면 폰 화면에서 타수를 입력하세요.", watchBatteryNoticeBody(critical))
        assertNotEquals(watchBatteryNoticeBody(low), watchBatteryNoticeBody(critical))
    }
}

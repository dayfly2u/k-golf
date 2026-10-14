package com.golfrecorder.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchActionGuardTest {

    @Test
    fun `누른 홀과 지금 홀이 같으면 낡지 않았다`() {
        assertFalse(isStaleWatchAction(queuedHole = 5, currentHole = 5))
    }

    @Test
    fun `누른 홀과 지금 홀이 다르면 낡은 입력이다`() {
        // 워치가 꺼져 있다가 한참 뒤 재연결: 5번 홀에서 누른 입력이 지금 7번 홀일 때 도착
        assertTrue(isStaleWatchAction(queuedHole = 5, currentHole = 7))
        // 앞 홀로 돌아간 경우도 마찬가지
        assertTrue(isStaleWatchAction(queuedHole = 7, currentHole = 5))
    }

    @Test
    fun `홀 번호를 안 보낸 구버전 워치의 입력은 그대로 처리한다`() {
        assertFalse(isStaleWatchAction(queuedHole = null, currentHole = 7))
    }
}

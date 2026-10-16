package com.golfrecorder.wear

import com.golfrecorder.wearsync.WearSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimisticRoundTest {
    // 5번 홀(파 4)에서 그린까지 3 + 퍼팅 2 = 5타(+1), 4번 홀은 5타(파 4, +1), 그 앞까지 합계 +3.
    private val pars = listOf(4, 5, 3, 4, 4, 5, 3, 4, 4, 4, 5, 3, 4, 4, 5, 3, 4, 4)
    private val server = RoundUiState(
        roundActive = true,
        holeNumber = 5,
        par = 4,
        strokesToGreen = 3,
        strokesPutt = 2,
        holeCount = 18,
        scoreBefore = 3,
        previousHole = PreviousHole(strokes = 5, par = 4),
        holePars = pars,
    )

    private fun act(action: String, hole: Int) = PendingAction(action, hole)

    @Test
    fun `대기 입력이 없으면 서버 상태 그대로`() {
        assertSame(server, displayRound(server, emptyList()))
    }

    @Test
    fun `같은 홀의 타수 입력은 서버 값에 순서대로 얹힌다`() {
        val result = displayRound(
            server,
            listOf(
                act(WearSync.ACTION_INCREMENT_TO_GREEN, 5),
                act(WearSync.ACTION_INCREMENT_TO_GREEN, 5),
                act(WearSync.ACTION_DECREMENT_PUTT, 5),
            ),
        )
        assertEquals(5, result.strokesToGreen)
        assertEquals(1, result.strokesPutt)
        assertEquals(5, result.holeNumber)
    }

    @Test
    fun `감소는 단계마다 0에서 막힌다`() {
        val fresh = server.copy(strokesToGreen = 0, strokesPutt = 0)
        val result = displayRound(
            fresh,
            listOf(act(WearSync.ACTION_DECREMENT_PUTT, 5), act(WearSync.ACTION_INCREMENT_PUTT, 5)),
        )
        // 0에서 빼는 건 폰이 무시하므로 1이 맞다(합산 후 막으면 0이 된다)
        assertEquals(1, result.strokesPutt)
    }

    @Test
    fun `▶를 누르면 연결이 끊겨 있어도 바로 다음 홀로 보인다`() {
        val result = displayRound(server, listOf(act(WearSync.ACTION_NEXT_HOLE, 5)))
        assertEquals(6, result.holeNumber)
        assertEquals(0, result.strokesToGreen)
        assertEquals(0, result.strokesPutt)
        // 6번 홀의 파(목록 6번째 = 5), 합계 기준 = 3 + (5 - 4) = 4, 직전 홀 = 방금 친 5번 홀 결과
        assertEquals(5, result.par)
        assertEquals(4, result.scoreBefore)
        assertEquals(PreviousHole(strokes = 5, par = 4), result.previousHole)
    }

    @Test
    fun `다음 홀로 넘어간 뒤 누른 입력은 그 홀 숫자에 쌓인다`() {
        val result = displayRound(
            server,
            listOf(
                act(WearSync.ACTION_NEXT_HOLE, 5),
                act(WearSync.ACTION_INCREMENT_TO_GREEN, 6),
                act(WearSync.ACTION_INCREMENT_TO_GREEN, 6),
            ),
        )
        assertEquals(6, result.holeNumber)
        assertEquals(2, result.strokesToGreen)
        assertEquals(0, result.strokesPutt)
    }

    @Test
    fun `이전 홀에서 누른 입력은 새 홀에 안 보이고 다시 돌아오면 보인다`() {
        val moved = displayRound(
            server,
            listOf(act(WearSync.ACTION_INCREMENT_TO_GREEN, 5), act(WearSync.ACTION_NEXT_HOLE, 5)),
        )
        assertEquals(6, moved.holeNumber)
        assertEquals(0, moved.strokesToGreen)

        val back = displayRound(
            server,
            listOf(
                act(WearSync.ACTION_INCREMENT_TO_GREEN, 5),
                act(WearSync.ACTION_NEXT_HOLE, 5),
                act(WearSync.ACTION_PREV_HOLE, 6),
            ),
        )
        assertEquals(5, back.holeNumber)
        assertEquals(4, back.strokesToGreen)
        assertEquals(2, back.strokesPutt)
    }

    @Test
    fun `마지막 홀의 ▶와 1번 홀의 ◀는 폰처럼 무시한다`() {
        val last = server.copy(holeNumber = 18, holeCount = 18)
        assertEquals(18, displayRound(last, listOf(act(WearSync.ACTION_NEXT_HOLE, 18))).holeNumber)
        val first = server.copy(holeNumber = 1, previousHole = null)
        assertEquals(1, displayRound(first, listOf(act(WearSync.ACTION_PREV_HOLE, 1))).holeNumber)
    }

    @Test
    fun `9홀 라운드는 9번 홀이 끝`() {
        val nine = server.copy(holeNumber = 9, holeCount = 9)
        assertEquals(9, displayRound(nine, listOf(act(WearSync.ACTION_NEXT_HOLE, 9))).holeNumber)
    }

    @Test
    fun `◀로 앞 홀로 가면 합계 기준에서 그 홀 몫을 뺀다`() {
        val result = displayRound(server, listOf(act(WearSync.ACTION_PREV_HOLE, 5)))
        assertEquals(4, result.holeNumber)
        // 4번 홀 결과(5타, 파 4 → +1)를 합계 기준 3에서 뺀다
        assertEquals(2, result.scoreBefore)
        assertEquals(4, result.par)
        // 서버는 4번 홀의 기존 타수를 알려줬지만 3번 홀은 모른다 → 직전 홀 줄은 비운다
        assertNull(result.previousHole)
    }

    @Test
    fun `서버가 홀별 파를 안 주면 서버 홀의 파를 그대로 쓴다`() {
        val oldPhone = server.copy(holePars = emptyList())
        val result = displayRound(oldPhone, listOf(act(WearSync.ACTION_NEXT_HOLE, 5)))
        assertEquals(4, result.par)
    }

    @Test
    fun `타수 0인 홀은 합계에 안 더한다`() {
        val untouched = server.copy(strokesToGreen = 0, strokesPutt = 0)
        val result = displayRound(untouched, listOf(act(WearSync.ACTION_NEXT_HOLE, 5)))
        assertEquals(3, result.scoreBefore)
        assertNull(result.previousHole)
    }

    @Test
    fun `두 홀 넘어가면 사이 홀에서 누른 입력도 합계에 들어간다`() {
        // 5번 홀 5타(+1) → 6번 홀(파 5)에서 7타(+2) → 7번 홀로: 합계 기준 = 3 + 1 + 2
        val actions = listOf(act(WearSync.ACTION_NEXT_HOLE, 5)) +
            List(7) { act(WearSync.ACTION_INCREMENT_TO_GREEN, 6) } +
            act(WearSync.ACTION_NEXT_HOLE, 6)
        val result = displayRound(server, actions)
        assertEquals(7, result.holeNumber)
        assertEquals(6, result.scoreBefore)
        assertEquals(PreviousHole(strokes = 7, par = 5), result.previousHole)
    }

    @Test
    fun `숏게임 입력도 서버 값에 얹히고 0에서 막힌다`() {
        val withShort = server.copy(strokesShortGame = 1)
        val result = displayRound(
            withShort,
            listOf(
                act(WearSync.ACTION_INCREMENT_SHORT_GAME, 5),
                act(WearSync.ACTION_INCREMENT_SHORT_GAME, 5),
                act(WearSync.ACTION_DECREMENT_SHORT_GAME, 5),
            ),
        )
        assertEquals(2, result.strokesShortGame)
        assertEquals(3, result.strokesToGreen)
        assertEquals(2, result.strokesPutt)

        val fresh = server.copy(strokesShortGame = 0)
        val floored = displayRound(
            fresh,
            listOf(act(WearSync.ACTION_DECREMENT_SHORT_GAME, 5), act(WearSync.ACTION_INCREMENT_SHORT_GAME, 5)),
        )
        assertEquals(1, floored.strokesShortGame)
    }

    @Test
    fun `다음 홀로 넘어간 뒤 누른 숏게임은 그 홀에 쌓이고 이전 홀 줄은 0에서 시작한다`() {
        val result = displayRound(
            server.copy(strokesShortGame = 2),
            listOf(
                act(WearSync.ACTION_NEXT_HOLE, 5),
                act(WearSync.ACTION_INCREMENT_SHORT_GAME, 6),
            ),
        )
        assertEquals(6, result.holeNumber)
        assertEquals(1, result.strokesShortGame)
        assertEquals(0, result.strokesToGreen)
        assertEquals(0, result.strokesPutt)
    }

    @Test
    fun `숏게임 타수도 직전 홀 결과와 합계 기준에 들어간다`() {
        // 5번 홀: 3 + 숏게임 1 + 퍼팅 2 = 6타(파 4 → +2). 6번 홀로 가면 합계 기준 = 3 + 2
        val withShort = server.copy(strokesShortGame = 1)
        val result = displayRound(withShort, listOf(act(WearSync.ACTION_NEXT_HOLE, 5)))
        assertEquals(5, result.scoreBefore)
        assertEquals(PreviousHole(strokes = 6, par = 4), result.previousHole)

        // 6번 홀(파 5)에서 숏게임만 3번 누르고 7번 홀로 → 3타(−2): 합계 기준 = 3 + 2 + (3 − 5)
        val twoHoles = displayRound(
            withShort,
            listOf(act(WearSync.ACTION_NEXT_HOLE, 5)) +
                List(3) { act(WearSync.ACTION_INCREMENT_SHORT_GAME, 6) } +
                act(WearSync.ACTION_NEXT_HOLE, 6),
        )
        assertEquals(3, twoHoles.scoreBefore)
        assertEquals(PreviousHole(strokes = 3, par = 5), twoHoles.previousHole)
    }

    @Test
    fun `대기 목록은 시퀀스 이하만 확인으로 빼고 순서를 지킨다`() {
        val pending = PendingActions()
        pending.add(300, act(WearSync.ACTION_INCREMENT_PUTT, 5))
        pending.add(100, act(WearSync.ACTION_NEXT_HOLE, 5))
        pending.add(200, act(WearSync.ACTION_INCREMENT_TO_GREEN, 6))
        assertEquals(
            listOf(WearSync.ACTION_NEXT_HOLE, WearSync.ACTION_INCREMENT_TO_GREEN, WearSync.ACTION_INCREMENT_PUTT),
            pending.toList().map { it.action },
        )

        assertTrue(pending.confirmThrough(200))
        assertEquals(listOf(WearSync.ACTION_INCREMENT_PUTT), pending.toList().map { it.action })
        // 이미 뺀 범위는 다시 확인해도 바뀐 게 없다
        assertFalse(pending.confirmThrough(200))
        assertFalse(pending.confirmThrough(0))
    }

    @Test
    fun `삭제 이벤트로 하나만 빼고 라운드가 끝나면 모두 비운다`() {
        val pending = PendingActions()
        pending.add(100, act(WearSync.ACTION_NEXT_HOLE, 5))
        pending.add(200, act(WearSync.ACTION_INCREMENT_PUTT, 6))
        assertTrue(pending.remove(100))
        assertFalse(pending.remove(100))
        assertEquals(1, pending.toList().size)
        pending.clear()
        assertEquals(0, pending.toList().size)
    }
}

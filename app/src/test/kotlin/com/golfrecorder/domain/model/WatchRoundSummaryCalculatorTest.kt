package com.golfrecorder.domain.model

import com.golfrecorder.data.local.entity.PenaltyEntity
import com.golfrecorder.data.local.entity.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchRoundSummaryCalculatorTest {

    private fun shot(hole: Int, phase: ShotPhase, shotIndex: Int) = ShotEntity(
        roundId = 1, holeNumber = hole, phase = phase.name, shotIndex = shotIndex,
        lat = 0.0, lng = 0.0, capturedAt = 0L,
    )

    private fun penalty(hole: Int, phase: ShotPhase, penaltyIndex: Int) = PenaltyEntity(
        roundId = 1, holeNumber = hole, phase = phase.name, type = PenaltyType.OB.name,
        penaltyIndex = penaltyIndex, strokeCount = 1, lat = 0.0, lng = 0.0, capturedAt = 0L,
    )

    /** 한 홀의 샷 기록 — phase별 타수만큼 shotIndex 1..n을 만든다. */
    private fun holeShots(hole: Int, toGreen: Int = 0, shortGame: Int = 0, putt: Int = 0): List<ShotEntity> =
        (1..toGreen).map { shot(hole, ShotPhase.TO_GREEN, it) } +
            (1..shortGame).map { shot(hole, ShotPhase.SHORT_GAME, it) } +
            (1..putt).map { shot(hole, ShotPhase.PUTT, it) }

    private val pars = mapOf(1 to 4, 2 to 5, 3 to 3, 4 to 4)

    private fun calculate(
        currentHole: Int,
        shots: List<ShotEntity>,
        penalties: List<PenaltyEntity> = emptyList(),
        parByHole: Map<Int, Int> = pars,
    ) = WatchRoundSummaryCalculator.calculate(currentHole, parByHole, shots, penalties)

    @Test
    fun `first hole has no earlier holes`() {
        val result = calculate(1, holeShots(1, toGreen = 2))
        assertEquals(0, result.scoreBefore)
        assertNull(result.previousHole)
    }

    @Test
    fun `sums score to par of earlier holes and reports the previous hole`() {
        // 1번 홀: 2+2 = 4타(파 4 → 0), 2번 홀: 3+1+2 = 6타(파 5 → +1)
        val shots = holeShots(1, toGreen = 2, putt = 2) + holeShots(2, toGreen = 3, shortGame = 1, putt = 2)
        val result = calculate(3, shots)
        assertEquals(1, result.scoreBefore)
        assertEquals(PreviousHoleResult(strokes = 6, par = 5), result.previousHole)
    }

    @Test
    fun `penalties count toward the hole total`() {
        // 1번 홀: 그린까지 샷 1개 + OB 벌타(penaltyIndex 3 → 그린까지 3타) + 퍼팅 2 = 5타, 파 4 → +1
        val shots = holeShots(1, toGreen = 1, putt = 2)
        val penalties = listOf(penalty(1, ShotPhase.TO_GREEN, penaltyIndex = 3))
        val result = calculate(2, shots, penalties)
        assertEquals(1, result.scoreBefore)
        assertEquals(PreviousHoleResult(strokes = 5, par = 4), result.previousHole)
    }

    @Test
    fun `earlier hole with zero strokes is left out of the sum`() {
        // 1번 홀은 기록 없음(건너뜀), 2번 홀 4+2 = 6타, 파 5 → +1
        val result = calculate(3, holeShots(2, toGreen = 4, putt = 2))
        assertEquals(1, result.scoreBefore)
        assertEquals(PreviousHoleResult(strokes = 6, par = 5), result.previousHole)
    }

    @Test
    fun `previous hole without strokes gives no previous result but earlier holes still count`() {
        // 1번 홀 3+2 = 5타(파 4 → +1), 2번 홀 기록 없음
        val result = calculate(3, holeShots(1, toGreen = 3, putt = 2))
        assertEquals(1, result.scoreBefore)
        assertNull(result.previousHole)
    }

    @Test
    fun `hole missing from the course defaults to par 4`() {
        val result = calculate(2, holeShots(1, toGreen = 3, putt = 2), parByHole = emptyMap())
        assertEquals(1, result.scoreBefore)
        assertEquals(PreviousHoleResult(strokes = 5, par = 4), result.previousHole)
    }

    @Test
    fun `current and later holes are ignored`() {
        val shots = holeShots(1, toGreen = 2, putt = 2) + holeShots(2, toGreen = 9) + holeShots(3, toGreen = 9)
        val result = calculate(2, shots)
        assertEquals(0, result.scoreBefore)
        assertEquals(PreviousHoleResult(strokes = 4, par = 4), result.previousHole)
    }
}

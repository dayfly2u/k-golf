package com.golfrecorder.domain.model

import com.golfrecorder.data.local.entity.PenaltyEntity
import com.golfrecorder.data.local.entity.ShotEntity

/** 워치 "홀 이동" 화면의 "{n}번 홀 {타수}타 · {등급}" 줄에 쓰는 이전 홀 결과. */
data class PreviousHoleResult(val strokes: Int, val par: Int)

/** 폰이 워치로 보내는 합계 기준값과 이전 홀 결과. */
data class WatchRoundSummary(
    /** 현재 홀보다 번호가 작은 홀 중 타수가 1 이상인 홀의 (타수 − 파) 합. 해당 홀이 없으면 0. */
    val scoreBefore: Int,
    /** 바로 이전 홀(현재 번호 − 1)의 결과. 1번 홀이거나 그 홀 타수가 0이면 null. */
    val previousHole: PreviousHoleResult?,
)

object WatchRoundSummaryCalculator {
    private const val DEFAULT_PAR = 4
    private val PHASES = listOf(ShotPhase.TO_GREEN, ShotPhase.SHORT_GAME, ShotPhase.PUTT)

    /**
     * 홀별 타수는 hole_records가 아니라 샷·벌타 기록에서 구한다 — hole_records는 폰 라운드
     * 화면이 열려 있을 때만 저장되는데, 워치로만 기록하는 동안에는 RoundRecordingService만
     * 살아 있기 때문이다. 워치 화면에 이미 보이는 타수(그린까지 + 숏/퍼팅, 벌타 포함)와
     * 같은 기준이다.
     */
    fun calculate(
        currentHoleNumber: Int,
        parByHole: Map<Int, Int>,
        shots: List<ShotEntity>,
        penalties: List<PenaltyEntity>,
    ): WatchRoundSummary {
        val shotsByHole = shots.groupBy { it.holeNumber }
        val penaltiesByHole = penalties.groupBy { it.holeNumber }

        fun strokesOf(hole: Int): Int {
            val holeShots = shotsByHole[hole].orEmpty()
            val holePenalties = penaltiesByHole[hole].orEmpty()
            return PHASES.sumOf { phase -> StrokeCalculator.currentTotal(holeShots, holePenalties, phase) }
        }

        fun parOf(hole: Int): Int = parByHole[hole] ?: DEFAULT_PAR

        var scoreBefore = 0
        for (hole in 1 until currentHoleNumber) {
            val strokes = strokesOf(hole)
            if (strokes > 0) scoreBefore += strokes - parOf(hole)
        }

        val previousHole = (currentHoleNumber - 1).takeIf { it >= 1 }?.let { hole ->
            val strokes = strokesOf(hole)
            if (strokes > 0) PreviousHoleResult(strokes, parOf(hole)) else null
        }
        return WatchRoundSummary(scoreBefore, previousHole)
    }
}

package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import com.golfrecorder.data.local.entity.HoleRecordEntity
import com.golfrecorder.data.local.entity.ShotEntity

/** 홈 "최근 5경기 추이"에 넣는 라운드 수. */
const val TREND_ROUND_COUNT = 5

/** 추이 그래프의 한 점(라운드 하나). 값을 구할 수 없는 항목은 null — 그래프에서 그 자리를 비운다.
 * 계산은 홈에서 카드를 펼쳤을 때의 통계 타일(GIR/홀당 퍼팅/드라이버)과 같다. */
data class RoundTrendPoint(
    val roundId: Long,
    val playedAt: Long,
    val courseName: String,
    val totalStrokes: Int,
    /** 파4/5 티샷 평균 거리(m) — 샷 위치를 안 찍었거나 전부 "드라이버 통계에서 제외"면 null. */
    val driverAvgMeters: Double?,
    /** 총 퍼팅 / 기록된 홀 수 — 퍼팅을 하나도 안 적은 라운드는 null. */
    val puttsPerHole: Double?,
    /** 엄격 GIR(숏어프로치 없이 규정 타수 안에 그린) 비율, 0~100 — 퍼팅을 하나도 안 적은 라운드는
     * 그린 위 타수가 전부 숏어프로치로 잡혀 0%가 되므로 null. */
    val girPercent: Double?,
)

/** 끝난 18홀 라운드([isFinishedFullRound]) 중 최근 [count]개를 오래된 것부터 돌려준다 — 그래프의 왼쪽→오른쪽 순서.
 * 연도 필터와는 무관하다(핸디캡처럼 "최근 기록" 기준). */
fun recentFullRounds(rounds: List<RoundSummary>, count: Int = TREND_ROUND_COUNT): List<RoundSummary> =
    rounds.filter { it.isFinishedFullRound() }
        .sortedByDescending { it.playedAt }
        .take(count)
        .reversed()

/** [holeRecords]는 그 라운드의 홀 기록 전부, [shots]는 그 라운드의 샷 전부(ShotDao.getShotsForRound 순서). */
fun buildRoundTrendPoint(
    round: RoundSummary,
    holeRecords: List<HoleRecordEntity>,
    shots: List<ShotEntity>,
): RoundTrendPoint {
    val holes = holeRecords.size
    val totalPutts = holeRecords.sumOf { it.strokesPutt }
    val hasPutts = holes > 0 && totalPutts > 0
    val driver = calculateDriverDistanceStats(
        shots,
        holeRecords.associate { it.holeNumber to it.par },
        holeRecords.filter { it.excludeFromDriverStats }.map { it.holeNumber }.toSet(),
    )
    val strictGirHoles = holeRecords.count { record ->
        HoleResult(
            record.holeNumber, record.par, record.strokesToGreen, record.strokesGreenToHoleOut,
            strokesPutt = record.strokesPutt,
        ).isStrictGreenInRegulation
    }
    return RoundTrendPoint(
        roundId = round.roundId,
        playedAt = round.playedAt,
        courseName = round.courseName,
        totalStrokes = round.totalStrokes,
        driverAvgMeters = driver.avgMeters,
        puttsPerHole = if (hasPutts) totalPutts.toDouble() / holes else null,
        girPercent = if (hasPutts) strictGirHoles * 100.0 / holes else null,
    )
}

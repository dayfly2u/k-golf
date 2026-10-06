package com.golfrecorder.domain.model

import com.golfrecorder.data.local.entity.ShotEntity
import com.golfrecorder.util.haversineMeters

/** par 4/5 홀의 티샷(드라이버) 비거리 통계. 데이터가 없으면(기록된 샷이 없거나
 * 파4/5 홀이 없음) null. */
data class DriverDistanceStats(val avgMeters: Double?, val maxMeters: Double?)

/** [shots]는 holeNumber, 그 다음 phase 순서(TO_GREEN→SHORT_GAME→PUTT), 그 다음
 * shotIndex 순으로 정렬돼 있어야 한다(ShotDao.getShots/getShotsForRound가 이 순서를
 * 보장한다). 샷 위치는 "그 샷을 치기 전 서 있던 지점"을 기록하므로(사용자 확인),
 * 파4/5 홀의 1번째 "그린까지" 샷 위치에서 그 다음 기록된 샷 위치까지의 거리가
 * 곧 티샷이 날아간 거리다. */
fun calculateDriverDistanceStats(shots: List<ShotEntity>, parByHole: Map<Int, Int>): DriverDistanceStats {
    val distances = shots.groupBy { it.holeNumber }.mapNotNull { (holeNumber, holeShots) ->
        val par = parByHole[holeNumber] ?: return@mapNotNull null
        if (par != 4 && par != 5) return@mapNotNull null
        if (holeShots.size < 2) return@mapNotNull null
        val first = holeShots[0]
        if (first.phase != ShotPhase.TO_GREEN.name || first.shotIndex != 1) return@mapNotNull null
        val second = holeShots[1]
        haversineMeters(first.lat, first.lng, second.lat, second.lng)
    }
    return DriverDistanceStats(
        avgMeters = distances.takeIf { it.isNotEmpty() }?.average(),
        maxMeters = distances.maxOrNull(),
    )
}

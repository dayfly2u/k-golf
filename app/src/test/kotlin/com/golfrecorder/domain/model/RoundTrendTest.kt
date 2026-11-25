package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import com.golfrecorder.data.local.entity.HoleRecordEntity
import com.golfrecorder.data.local.entity.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundTrendTest {
    private fun round(id: Long, playedAt: Long, holes: Int = 18, finished: Boolean = true, strokes: Int = 90) =
        RoundSummary(
            roundId = id, courseId = 1, playedAt = playedAt, finishedAt = if (finished) playedAt + 1000 else null,
            courseName = "course", price = null, companions = null, review = null, totalStrokes = strokes,
            holesPlayed = holes,
        )

    private fun hole(
        number: Int,
        par: Int,
        toGreen: Int,
        greenToHoleOut: Int,
        putts: Int,
        excludeDriver: Boolean = false,
    ) = HoleRecordEntity(
        roundId = 1, holeNumber = number, par = par, strokesToGreen = toGreen,
        strokesGreenToHoleOut = greenToHoleOut, strokesPutt = putts, excludeFromDriverStats = excludeDriver,
    )

    private fun shot(hole: Int, index: Int, lat: Double) = ShotEntity(
        roundId = 1, holeNumber = hole, phase = ShotPhase.TO_GREEN.name, shotIndex = index,
        lat = lat, lng = 127.0, capturedAt = 0,
    )

    @Test
    fun `끝난 18홀 라운드만 최근 5개를 오래된 것부터`() {
        val rounds = listOf(
            round(1, 100), round(2, 200), round(3, 300, holes = 9), round(4, 400, finished = false),
            round(5, 500), round(6, 600), round(7, 700), round(8, 800), round(9, 900),
        )
        // 9홀(3)과 안 끝난 라운드(4)는 빠지고, 남은 1·2·5·6·7·8·9 중 최근 5개
        assertEquals(listOf(5L, 6L, 7L, 8L, 9L), recentFullRounds(rounds).map { it.roundId })
    }

    @Test
    fun `5개보다 적으면 있는 만큼`() {
        val rounds = listOf(round(2, 200), round(1, 100), round(3, 300, holes = 9))
        assertEquals(listOf(1L, 2L), recentFullRounds(rounds).map { it.roundId })
    }

    @Test
    fun `홀당 퍼팅과 GIR은 홈 카드 타일과 같은 계산`() {
        val records = listOf(
            hole(1, par = 4, toGreen = 2, greenToHoleOut = 2, putts = 2), // GIR
            hole(2, par = 4, toGreen = 2, greenToHoleOut = 3, putts = 2), // 숏어프로치 1 → 엄격 GIR 아님
            hole(3, par = 3, toGreen = 2, greenToHoleOut = 1, putts = 1), // 규정 타수 넘김
            hole(4, par = 5, toGreen = 3, greenToHoleOut = 1, putts = 1), // GIR
        )
        val point = buildRoundTrendPoint(round(1, 100, strokes = 18), records, emptyList())
        assertEquals(6.0 / 4, point.puttsPerHole!!, 1e-9)
        assertEquals(50.0, point.girPercent!!, 1e-9)
        assertEquals(18, point.totalStrokes)
    }

    @Test
    fun `퍼팅을 하나도 안 적은 라운드는 홀당 퍼팅과 GIR이 비어 있다`() {
        val records = listOf(
            hole(1, par = 4, toGreen = 2, greenToHoleOut = 2, putts = 0),
            hole(2, par = 4, toGreen = 3, greenToHoleOut = 2, putts = 0),
        )
        val point = buildRoundTrendPoint(round(1, 100), records, emptyList())
        assertNull(point.puttsPerHole)
        assertNull(point.girPercent)
    }

    @Test
    fun `드라이버 거리는 파4·5 티샷 평균이고 제외한 홀은 빠진다`() {
        val records = listOf(
            hole(1, par = 4, toGreen = 2, greenToHoleOut = 2, putts = 2),
            hole(2, par = 4, toGreen = 2, greenToHoleOut = 2, putts = 2, excludeDriver = true),
        )
        // 위도 0.002도 ≈ 222m, 2번 홀은 제외
        val shots = listOf(shot(1, 1, 37.0), shot(1, 2, 37.002), shot(2, 1, 37.0), shot(2, 2, 37.0001))
        val point = buildRoundTrendPoint(round(1, 100), records, shots)
        assertEquals(222.0, point.driverAvgMeters!!, 2.0)
    }

    @Test
    fun `샷 위치가 없으면 드라이버 거리가 비어 있다`() {
        val records = listOf(hole(1, par = 4, toGreen = 2, greenToHoleOut = 2, putts = 2))
        assertNull(buildRoundTrendPoint(round(1, 100), records, emptyList()).driverAvgMeters)
    }
}

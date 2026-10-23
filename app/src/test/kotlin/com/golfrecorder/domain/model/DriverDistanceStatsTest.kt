package com.golfrecorder.domain.model

import com.golfrecorder.data.local.entity.ShotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DriverDistanceStatsTest {
    // 위도 0.0001도 ≈ 11.1m — 경도는 적도 근처라 같은 비율. 거리 자체보다 "어느 홀이 들어갔나"를 본다.
    private fun shot(hole: Int, phase: ShotPhase, index: Int, lat: Double, lng: Double = 127.0) =
        ShotEntity(
            roundId = 1, holeNumber = hole, phase = phase.name, shotIndex = index,
            lat = lat, lng = lng, capturedAt = 0,
        )

    // 티샷 위치(index 1)와 둘째 샷 위치(index 2)의 위도 차이로 거리를 정한다.
    private fun teeAndSecond(hole: Int, teeLat: Double, secondLat: Double) = listOf(
        shot(hole, ShotPhase.TO_GREEN, 1, teeLat),
        shot(hole, ShotPhase.TO_GREEN, 2, secondLat),
    )

    private val pars = mapOf(1 to 4, 2 to 5, 3 to 3)

    @Test
    fun `파3 홀과 샷이 모자란 홀은 빠지고 파4와 파5의 티샷 거리만 센다`() {
        val shots = teeAndSecond(1, 37.0, 37.002) + teeAndSecond(2, 37.0, 37.003) + teeAndSecond(3, 37.0, 37.001)
        val stats = calculateDriverDistanceStats(shots, pars)
        assertNotNull(stats.avgMeters)
        // 파3(3번 홀)은 제외 — 1번 홀 ≈ 222m, 2번 홀 ≈ 333m, 평균 ≈ 278m, 최장 ≈ 333m
        assertEquals(278.0, stats.avgMeters!!, 2.0)
        assertEquals(333.0, stats.maxMeters!!, 2.0)
    }

    @Test
    fun `제외한 홀은 평균과 최장에서 빠진다`() {
        // 2번 홀의 티샷을 엉뚱한 곳에서 눌러 거리가 비정상적으로 짧게(≈11m) 잡힌 경우
        val shots = teeAndSecond(1, 37.0, 37.002) + teeAndSecond(2, 37.0, 37.0001)
        val withAll = calculateDriverDistanceStats(shots, pars)
        assertEquals(117.0, withAll.avgMeters!!, 2.0)

        val excluded = calculateDriverDistanceStats(shots, pars, excludedHoles = setOf(2))
        assertEquals(222.0, excluded.avgMeters!!, 2.0)
        assertEquals(222.0, excluded.maxMeters!!, 2.0)
    }

    @Test
    fun `최장 거리도 제외한 홀에서는 안 잡힌다`() {
        // 1번 홀이 비정상적으로 길게(≈1.1km) 잡힌 경우 — 제외하면 최장이 2번 홀로 바뀐다
        val shots = teeAndSecond(1, 37.0, 37.01) + teeAndSecond(2, 37.0, 37.002)
        assertEquals(1111.0, calculateDriverDistanceStats(shots, pars).maxMeters!!, 3.0)
        assertEquals(222.0, calculateDriverDistanceStats(shots, pars, excludedHoles = setOf(1)).maxMeters!!, 2.0)
    }

    @Test
    fun `모든 홀을 제외하면 평균과 최장이 없다`() {
        val shots = teeAndSecond(1, 37.0, 37.002) + teeAndSecond(2, 37.0, 37.003)
        val stats = calculateDriverDistanceStats(shots, pars, excludedHoles = setOf(1, 2))
        assertNull(stats.avgMeters)
        assertNull(stats.maxMeters)
    }

    @Test
    fun `제외 목록이 비어 있으면 예전 계산과 같다`() {
        val shots = teeAndSecond(1, 37.0, 37.002)
        assertEquals(
            calculateDriverDistanceStats(shots, pars),
            calculateDriverDistanceStats(shots, pars, excludedHoles = emptySet()),
        )
    }
}

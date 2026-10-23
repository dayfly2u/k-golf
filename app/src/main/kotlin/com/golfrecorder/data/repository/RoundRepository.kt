package com.golfrecorder.data.repository

import com.golfrecorder.data.local.dao.RoundDao
import com.golfrecorder.data.local.dto.RoundSummary
import com.golfrecorder.data.local.entity.HoleRecordEntity
import com.golfrecorder.data.local.entity.RoundEntity
import com.golfrecorder.data.local.relation.RoundWithHoleRecords
import kotlinx.coroutines.flow.Flow

class RoundRepository(private val roundDao: RoundDao) {

    suspend fun startRound(
        courseId: Long,
        courseName: String,
        playedAt: Long,
        locationSource: String,
        mapProvider: String,
    ): Long =
        roundDao.insertRound(
            RoundEntity(
                courseId = courseId,
                courseName = courseName,
                playedAt = playedAt,
                locationSource = locationSource,
                mapProvider = mapProvider,
            )
        )

    suspend fun finishRound(roundId: Long, finishedAt: Long) =
        roundDao.updateFinishedAt(roundId, finishedAt)

    suspend fun updateRoundReview(roundId: Long, price: Int?, companions: String?, review: String?) =
        roundDao.updateRoundReview(roundId, price, companions, review)

    suspend fun saveHoleRecord(
        roundId: Long,
        holeNumber: Int,
        par: Int,
        strokesToGreen: Int,
        strokesGreenToHoleOut: Int,
        strokesPutt: Int
    ) {
        // 이미 있는 홀은 타수 컬럼만 갱신해서 excludeFromDriverStats 같은 다른 컬럼을 보존한다.
        // 없으면 새로 넣고, 그 사이 다른 쪽이 먼저 넣어서 무시됐으면(-1) 한 번 더 갱신한다.
        val updated = roundDao.updateHoleRecordStrokes(
            roundId, holeNumber, par, strokesToGreen, strokesGreenToHoleOut, strokesPutt,
        )
        if (updated == 0) {
            val inserted = roundDao.insertHoleRecordIfAbsent(
                HoleRecordEntity(
                    roundId = roundId,
                    holeNumber = holeNumber,
                    par = par,
                    strokesToGreen = strokesToGreen,
                    strokesGreenToHoleOut = strokesGreenToHoleOut,
                    strokesPutt = strokesPutt
                )
            )
            if (inserted == -1L) {
                roundDao.updateHoleRecordStrokes(
                    roundId, holeNumber, par, strokesToGreen, strokesGreenToHoleOut, strokesPutt,
                )
            }
        }
    }

    suspend fun setExcludeFromDriverStats(roundId: Long, holeNumber: Int, exclude: Boolean) =
        roundDao.updateExcludeFromDriverStats(roundId, holeNumber, exclude)

    fun getRoundWithHoleRecords(roundId: Long): Flow<RoundWithHoleRecords?> =
        roundDao.getRoundWithHoleRecords(roundId)

    fun getRoundSummaries(): Flow<List<RoundSummary>> = roundDao.getRoundSummaries()

    suspend fun deleteRound(roundId: Long) = roundDao.deleteRound(roundId)

    suspend fun countRoundsForCourse(courseId: Long): Int = roundDao.countRoundsForCourse(courseId)

    suspend fun getCurrentHoleNumber(roundId: Long): Int? = roundDao.getCurrentHoleNumber(roundId)

    suspend fun updateCurrentHoleNumber(roundId: Long, holeNumber: Int) =
        roundDao.updateCurrentHoleNumber(roundId, holeNumber)

    suspend fun getLocationSource(roundId: Long): String? = roundDao.getLocationSource(roundId)

    suspend fun getMapProvider(roundId: Long): String? = roundDao.getMapProvider(roundId)
}

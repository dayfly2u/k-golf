package com.golfrecorder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.golfrecorder.data.local.dto.RoundSummary
import com.golfrecorder.data.local.entity.HoleRecordEntity
import com.golfrecorder.data.local.entity.RoundEntity
import com.golfrecorder.data.local.relation.RoundWithHoleRecords
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {

    @Insert
    suspend fun insertRound(round: RoundEntity): Long

    @Query("UPDATE rounds SET finishedAt = :finishedAt WHERE id = :roundId")
    suspend fun updateFinishedAt(roundId: Long, finishedAt: Long)

    @Query(
        """
        UPDATE rounds SET
            price = :price,
            companions = :companions,
            review = :review
        WHERE id = :roundId
        """
    )
    suspend fun updateRoundReview(roundId: Long, price: Int?, companions: String?, review: String?)

    // roundId+holeNumber에 unique index가 걸려 있다. 같은 홀을 다시 저장할 때 REPLACE로 행을
    // 통째로 갈아끼우면 사용자가 켜 둔 excludeFromDriverStats처럼 타수와 무관한 컬럼이 기본값으로
    // 되돌아가므로, 이미 있는 행은 타수 컬럼만 UPDATE하고(바뀐 행 수를 돌려준다) 없을 때만
    // INSERT한다(RoundRepository.saveHoleRecord).
    @Query(
        """
        UPDATE hole_records SET
            par = :par,
            strokesToGreen = :strokesToGreen,
            strokesGreenToHoleOut = :strokesGreenToHoleOut,
            strokesPutt = :strokesPutt
        WHERE roundId = :roundId AND holeNumber = :holeNumber
        """
    )
    suspend fun updateHoleRecordStrokes(
        roundId: Long,
        holeNumber: Int,
        par: Int,
        strokesToGreen: Int,
        strokesGreenToHoleOut: Int,
        strokesPutt: Int,
    ): Int

    /** 이미 같은 홀의 행이 있으면 아무것도 하지 않고 -1을 돌려준다. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHoleRecordIfAbsent(holeRecord: HoleRecordEntity): Long

    @Query(
        "UPDATE hole_records SET excludeFromDriverStats = :exclude WHERE roundId = :roundId AND holeNumber = :holeNumber"
    )
    suspend fun updateExcludeFromDriverStats(roundId: Long, holeNumber: Int, exclude: Boolean)

    @Transaction
    @Query("SELECT * FROM rounds WHERE id = :roundId")
    fun getRoundWithHoleRecords(roundId: Long): Flow<RoundWithHoleRecords?>

    @Query(
        """
        SELECT r.id AS roundId, r.courseId AS courseId, r.playedAt AS playedAt,
               r.finishedAt AS finishedAt,
               COALESCE(c.name, r.courseName) AS courseName,
               r.price AS price, r.companions AS companions, r.review AS review,
               SUM(hr.strokesToGreen + hr.strokesGreenToHoleOut) AS totalStrokes,
               SUM(hr.par) AS totalPar,
               SUM(MIN(hr.strokesToGreen + hr.strokesGreenToHoleOut, hr.par + 5)) AS adjustedStrokes,
               COUNT(hr.holeNumber) AS holesPlayed
        FROM rounds r
        LEFT JOIN courses c ON c.id = r.courseId
        LEFT JOIN hole_records hr ON hr.roundId = r.id
        GROUP BY r.id
        ORDER BY r.playedAt DESC
        """
    )
    fun getRoundSummaries(): Flow<List<RoundSummary>>

    @Query("DELETE FROM rounds WHERE id = :roundId")
    suspend fun deleteRound(roundId: Long)

    @Query("SELECT COUNT(*) FROM rounds WHERE courseId = :courseId")
    suspend fun countRoundsForCourse(courseId: Long): Int

    @Query("SELECT currentHoleNumber FROM rounds WHERE id = :roundId")
    suspend fun getCurrentHoleNumber(roundId: Long): Int?

    @Query("UPDATE rounds SET currentHoleNumber = :holeNumber WHERE id = :roundId")
    suspend fun updateCurrentHoleNumber(roundId: Long, holeNumber: Int)

    @Query("SELECT locationSource FROM rounds WHERE id = :roundId")
    suspend fun getLocationSource(roundId: Long): String?

    @Query("SELECT mapProvider FROM rounds WHERE id = :roundId")
    suspend fun getMapProvider(roundId: Long): String?
}

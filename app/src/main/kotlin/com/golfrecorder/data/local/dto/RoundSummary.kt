package com.golfrecorder.data.local.dto

data class RoundSummary(
    val roundId: Long,
    val courseId: Long?,
    val playedAt: Long,
    val finishedAt: Long?,
    val courseName: String,
    /** 코스 관리에서 놓은 순서([com.golfrecorder.data.local.entity.CourseEntity.sortOrder]) — 코스순 정렬용.
     * 코스가 없어진 라운드는 쿼리가 [Int.MAX_VALUE]를 채워 맨 뒤로 간다. */
    val courseSortOrder: Int = Int.MAX_VALUE,
    /** 코스 별점([com.golfrecorder.data.local.entity.CourseEntity.rating]) — 코스순을 별점순으로 볼 때 쓴다.
     * 별점을 안 매겼거나 코스가 없어진 라운드는 null. */
    val courseRating: Double? = null,
    val price: Int?,
    val companions: String?,
    val review: String?,
    val totalStrokes: Int,
    val totalPar: Int = 0,
    /** 홀마다 파 + 5를 넘는 타수는 잘라낸 총타수(추정 핸디캡용 — WHS의 최대 홀 스코어와 같은 취지). */
    val adjustedStrokes: Int = 0,
    /** 기록된 홀 수 — 18홀 라운드만 핸디캡에 쓴다. */
    val holesPlayed: Int = 0,
)

package com.golfrecorder.data.local.dto

data class RoundSummary(
    val roundId: Long,
    val courseId: Long?,
    val playedAt: Long,
    val finishedAt: Long?,
    val courseName: String,
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

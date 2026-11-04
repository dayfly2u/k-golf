package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary

/** 홈 라운드 목록 정렬: 최신순(기본) / 점수순(총타수 낮은 순) / 코스순(코스 관리에서 놓은 순서). */
enum class RoundSortMode { RECENT, SCORE, COURSE }

/**
 * [mode]에 맞춰 라운드를 정렬한다. 점수순은 단순 총타수 오름차순이고(코스별 파 차이는 보지 않는다),
 * 동점이면 더 최근 라운드가 위다. 아직 안 끝났거나 기록이 없는 라운드는 지나온 홀 몫의 타수뿐이라 점수로
 * 줄 세울 수 없으므로 맨 뒤에 최근 순으로 둔다.
 *
 * 코스순은 코스 관리 화면에서 사용자가 놓은 코스 순서([RoundSummary.courseSortOrder])대로 같은 코스 라운드를
 * 묶고, 한 코스 안에서는 최근 라운드가 위다. 코스가 지워져 순서가 없는 라운드는 맨 뒤에 코스 이름별로 묶는다.
 */
fun sortRounds(rounds: List<RoundSummary>, mode: RoundSortMode): List<RoundSummary> = when (mode) {
    RoundSortMode.RECENT -> rounds.sortedByDescending { it.playedAt }
    RoundSortMode.SCORE -> {
        val (scorable, rest) = rounds.partition { it.finishedAt != null && it.totalStrokes > 0 }
        scorable.sortedWith(compareBy({ it.totalStrokes }, { -it.playedAt })) +
            rest.sortedByDescending { it.playedAt }
    }
    RoundSortMode.COURSE -> rounds.sortedWith(
        compareBy<RoundSummary> { it.courseSortOrder }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.courseName }
            .thenBy { it.courseId ?: Long.MAX_VALUE }
            .thenByDescending { it.playedAt },
    )
}

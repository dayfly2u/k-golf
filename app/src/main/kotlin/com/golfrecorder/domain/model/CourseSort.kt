package com.golfrecorder.domain.model

import com.golfrecorder.data.local.entity.CourseEntity

/**
 * 코스를 늘어놓는 방식 — 코스 관리 목록, 코스 선택 목록, 홈 라운드 목록의 코스순이 모두 이 하나를 따른다.
 * [MANUAL]은 코스 관리에서 사용자가 끌어서 정한 순서([CourseEntity.sortOrder]), [RATING]은 별점이 높은 순.
 * 별점순이어도 사용자가 정한 순서는 지워지지 않고 별점이 같은 코스끼리의 순서로 쓰인다.
 *
 * [storageKey]는 SharedPreferences에 저장되는 값이라 상수 이름을 바꿔도 안 변하게 따로 둔다.
 */
enum class CourseSortMode(val storageKey: String) {
    MANUAL("manual"),
    RATING("rating");

    companion object {
        fun fromStorageKey(key: String?): CourseSortMode = entries.find { it.storageKey == key } ?: MANUAL
    }
}

/** 별점이 높은 순, 별점을 안 매긴(null) 코스는 맨 뒤. 코스 목록과 라운드 목록의 코스순이 같이 쓴다. */
internal val ratingDescNullsLast: Comparator<Double?> = Comparator { a, b ->
    when {
        a == null && b == null -> 0
        a == null -> 1
        b == null -> -1
        else -> b.compareTo(a)
    }
}

/** [courses]를 [mode] 방식으로 정렬한다. 같은 순위끼리는 사용자가 정한 순서, 그다음 id 순. */
fun sortCourses(courses: List<CourseEntity>, mode: CourseSortMode): List<CourseEntity> = when (mode) {
    CourseSortMode.MANUAL -> courses.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    CourseSortMode.RATING -> courses.sortedWith(
        Comparator<CourseEntity> { a, b -> ratingDescNullsLast.compare(a.rating, b.rating) }
            .thenBy { it.sortOrder }
            .thenBy { it.id },
    )
}

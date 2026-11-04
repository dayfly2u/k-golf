package com.golfrecorder.domain.model

import com.golfrecorder.data.local.entity.CourseEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseSortTest {
    private fun course(id: Long, sortOrder: Int, rating: Double?) =
        CourseEntity(id = id, name = "코스$id", sortOrder = sortOrder, rating = rating)

    private fun ids(courses: List<CourseEntity>) = courses.map { it.id }

    @Test
    fun `내가 정한 순서는 sortOrder 오름차순이고 별점은 보지 않는다`() {
        val courses = listOf(course(1, 2, 5.0), course(2, 0, 1.0), course(3, 1, null))
        assertEquals(listOf(2L, 3L, 1L), ids(sortCourses(courses, CourseSortMode.MANUAL)))
    }

    @Test
    fun `별점순은 별점 높은 순`() {
        val courses = listOf(course(1, 0, 3.0), course(2, 1, 4.5), course(3, 2, 4.0))
        assertEquals(listOf(2L, 3L, 1L), ids(sortCourses(courses, CourseSortMode.RATING)))
    }

    @Test
    fun `별점순에서 별점이 같으면 내가 정한 순서`() {
        val courses = listOf(course(1, 2, 4.0), course(2, 0, 4.0), course(3, 1, 4.0), course(4, 3, 5.0))
        assertEquals(listOf(4L, 2L, 3L, 1L), ids(sortCourses(courses, CourseSortMode.RATING)))
    }

    @Test
    fun `별점순에서 별점 없는 코스는 맨 뒤이고 그 안에서는 내가 정한 순서`() {
        val courses = listOf(course(1, 0, null), course(2, 1, 0.5), course(3, 2, null), course(4, 3, 3.0))
        assertEquals(listOf(4L, 2L, 1L, 3L), ids(sortCourses(courses, CourseSortMode.RATING)))
    }

    @Test
    fun `정렬 방식은 저장값으로 복원되고 모르는 값은 내가 정한 순서`() {
        assertEquals(CourseSortMode.RATING, CourseSortMode.fromStorageKey(CourseSortMode.RATING.storageKey))
        assertEquals(CourseSortMode.MANUAL, CourseSortMode.fromStorageKey(CourseSortMode.MANUAL.storageKey))
        assertEquals(CourseSortMode.MANUAL, CourseSortMode.fromStorageKey(null))
        assertEquals(CourseSortMode.MANUAL, CourseSortMode.fromStorageKey("없는 값"))
    }
}

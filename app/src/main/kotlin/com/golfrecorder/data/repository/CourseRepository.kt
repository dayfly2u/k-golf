package com.golfrecorder.data.repository

import com.golfrecorder.data.local.dao.CourseDao
import com.golfrecorder.data.local.entity.CourseEntity
import com.golfrecorder.data.local.relation.CourseWithHoles
import com.golfrecorder.domain.model.CourseSortMode
import com.golfrecorder.domain.model.sortCourses
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

class CourseRepository(
    private val courseDao: CourseDao,
    private val sortPreference: CourseSortPreference,
) {

    /** 지금 고른 코스 정렬 방식 — 홈의 코스순도 이걸 따라간다. */
    val sortMode: StateFlow<CourseSortMode> = sortPreference.mode

    fun setSortMode(mode: CourseSortMode) = sortPreference.set(mode)

    /** 코스 목록을 [sortMode] 방식으로 정렬해 흘려보낸다 — 코스 관리와 코스 선택이 같은 순서를 본다. */
    fun getCourses(): Flow<List<CourseEntity>> =
        combine(courseDao.getCourses(), sortPreference.mode) { courses, mode -> sortCourses(courses, mode) }

    fun getCourseWithHoles(courseId: Long): Flow<CourseWithHoles?> =
        courseDao.getCourseWithHoles(courseId)

    suspend fun createCourse(name: String, holePars: List<Int>): Long {
        val nextSortOrder = courseDao.getMaxSortOrder() + 1
        val course = CourseEntity(name = name, holeCount = holePars.size, sortOrder = nextSortOrder)
        return courseDao.insertCourseWithHoles(course, holePars)
    }

    /** [orderedCourses]가 화면에 보여줄 새 순서 — 각 항목의 sortOrder를 그 위치(index)로 다시 매겨서 저장한다. */
    suspend fun reorder(orderedCourses: List<CourseEntity>) =
        courseDao.updateAll(orderedCourses.mapIndexed { index, course -> course.copy(sortOrder = index) })

    suspend fun updateHolePar(holeId: Long, par: Int) =
        courseDao.updateHolePar(holeId, par)

    suspend fun updateCourseName(courseId: Long, name: String) =
        courseDao.updateCourseName(courseId, name)

    suspend fun updateCourseReview(
        courseId: Long,
        rating: Double?,
        difficulty: Int?,
        region: String?,
        distance: String?,
        travelTime: String?,
        oneLineReview: String?,
        transportInfo: String?,
        clubhouseInfo: String?,
        courseInfo: String?,
    ) = courseDao.updateCourseReview(
        courseId, rating, difficulty, region, distance, travelTime, oneLineReview, transportInfo, clubhouseInfo, courseInfo,
    )

    suspend fun deleteCourse(courseId: Long) =
        courseDao.deleteCourse(courseId)
}

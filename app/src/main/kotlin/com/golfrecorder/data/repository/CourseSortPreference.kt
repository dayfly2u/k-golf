package com.golfrecorder.data.repository

import android.content.Context
import androidx.core.content.edit
import com.golfrecorder.domain.model.CourseSortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 코스 정렬 방식(내가 정한 순서 / 별점순)을 기억한다. 마지막으로 고른 값이 앱을 다시 켜도 남고,
 * [mode]를 보는 화면(코스 관리·코스 선택·홈의 코스순)은 값이 바뀌는 즉시 같이 바뀐다.
 * DB가 아니라 SharedPreferences라 스키마와 백업/복원에는 영향이 없다(복원하면 기기의 설정이 그대로 남는다).
 */
class CourseSortPreference(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(CourseSortMode.fromStorageKey(prefs.getString(KEY_MODE, null)))
    val mode: StateFlow<CourseSortMode> = _mode.asStateFlow()

    fun set(mode: CourseSortMode) {
        prefs.edit { putString(KEY_MODE, mode.storageKey) }
        _mode.value = mode
    }

    private companion object {
        const val PREFS_NAME = "course_sort"
        const val KEY_MODE = "mode"
    }
}

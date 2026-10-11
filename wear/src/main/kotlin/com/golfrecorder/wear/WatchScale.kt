package com.golfrecorder.wear

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 시안은 360×360 기준이다. 실제 워치(예: 198dp)에서는 배율이 0.55 안팎이라 시안의 글자·
 * 버튼을 그대로 곱하면 11px 라벨이 6sp, 52px 버튼이 29dp가 되어 읽거나 누를 수 없다.
 * 그래서 도형·간격·위치는 비례로 두고, 글자와 눌러야 하는 버튼에는 하한을 둔다.
 */
@Immutable
class WatchScale private constructor(private val factor: Float) {

    /** 도형·간격·위치 — 시안 px을 비례로만 변환. */
    fun dp(designPx: Float): Dp = (designPx * factor).dp

    /** 글자 — 비례하되 [minSp] 아래로는 내려가지 않는다. */
    fun sp(designPx: Float, minSp: Float = MIN_TEXT_SP): TextUnit = maxOf(designPx * factor, minSp).sp

    /**
     * 글자의 줄 높이 — [sp]와 같은 크기의 [LINE_HEIGHT_RATIO]배. wear `MaterialTheme`의 기본
     * 본문 스타일은 줄 높이가 20sp라서, 줄 높이를 직접 주지 않으면 10~13sp 글자도 20sp짜리
     * 줄 상자를 차지해 198dp 화면에서 세로로 넘친다.
     */
    fun lineSp(designPx: Float, minSp: Float = MIN_TEXT_SP): TextUnit = sp(designPx, minSp) * LINE_HEIGHT_RATIO

    /** 눌러야 하는 원형 버튼 지름 — 비례하되 [minDp] 아래로는 내려가지 않는다. */
    fun touch(designPx: Float, minDp: Float = MIN_TOUCH_DP): Dp = maxOf(designPx * factor, minDp).dp

    companion object {
        const val DESIGN_SIZE = 360f
        const val MIN_TEXT_SP = 10f
        const val MIN_TOUCH_DP = 38f
        const val LINE_HEIGHT_RATIO = 1.2f

        fun forScreen(minSide: Dp): WatchScale = WatchScale(minSide.value / DESIGN_SIZE)
    }
}

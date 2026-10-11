package com.golfrecorder.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val HOLE_SCREEN_MS = 2500L
private const val MOVE_REQUEST_TIMEOUT_MS = 5000L

/**
 * "홀 이동" 화면을 언제 띄울지 — 워치에서 ◀/▶를 눌러 홀이 바뀐 경우에만 뜬다(폰에서 바뀐
 * 경우는 조용히 홀 기록 화면만 갱신). 누르면 [request]로 "이동 요청"이 서고, 폰이 처리해서
 * 홀 번호가 실제로 바뀌면([onHoleChanged]) 화면을 2.5초 보여준다. 폰과 연결이 끊겨 홀이 안
 * 바뀌는 경우를 위해 요청은 5초 뒤 만료된다([expireRequestAfterTimeout]).
 */
@Stable
class HoleMoveOverlayState {
    var pending by mutableStateOf<HoleMoveDirection?>(null)
        private set
    var shownDirection by mutableStateOf<HoleMoveDirection?>(null)
        private set

    fun request(direction: HoleMoveDirection) {
        pending = direction
    }

    fun dismiss() {
        shownDirection = null
    }

    suspend fun expireRequestAfterTimeout() {
        if (pending != null) {
            delay(MOVE_REQUEST_TIMEOUT_MS)
            pending = null
        }
    }

    /**
     * 홀 번호가 바뀔 때마다 호출한다. 이동 요청이 있으면 그 방향으로 화면을 띄우고, 이미
     * 떠 있는 중(연속 이동 포함)이면 방향은 그대로 두고 타이머만 처음부터 다시 센다.
     * 호출한 코루틴이 홀 번호 키 변경으로 취소돼도 다음 호출이 타이머를 이어받으므로
     * 화면이 끝없이 떠 있지 않는다.
     */
    suspend fun onHoleChanged() {
        pending?.let { requested ->
            shownDirection = requested
            pending = null
        }
        if (shownDirection != null) {
            delay(HOLE_SCREEN_MS)
            shownDirection = null
        }
    }
}

@Composable
fun rememberHoleMoveOverlay(holeNumber: Int): HoleMoveOverlayState {
    val overlay = remember { HoleMoveOverlayState() }
    LaunchedEffect(overlay.pending) { overlay.expireRequestAfterTimeout() }
    LaunchedEffect(holeNumber) { overlay.onHoleChanged() }
    return overlay
}

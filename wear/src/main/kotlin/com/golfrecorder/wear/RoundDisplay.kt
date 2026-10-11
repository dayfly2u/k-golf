package com.golfrecorder.wear

/** 이 홀 결과 이름 — 시안(WatchPlay.dc.html)의 names 표와 같다. 표에 없는 값은 부호 숫자. */
fun holeGradeName(scoreToPar: Int): String = when (scoreToPar) {
    -3 -> "앨버트로스"
    -2 -> "이글"
    -1 -> "버디"
    0 -> "파"
    1 -> "보기"
    2 -> "더블"
    3 -> "트리플"
    else -> formatTotal(scoreToPar)
}

/** 합계 표기: 양수 `+5`, 0은 `0`, 음수 `-2` (폰의 "E"와 다름 — 시안 로직). */
fun formatTotal(scoreToPar: Int): String = if (scoreToPar > 0) "+$scoreToPar" else "$scoreToPar"

/** "이 홀 {strokes}타 · {gradeName}" 줄. 타수가 0이면 등급은 "-". */
data class HoleSummary(val strokes: Int, val gradeName: String)

fun holeSummary(strokes: Int, par: Int): HoleSummary =
    HoleSummary(strokes, if (strokes > 0) holeGradeName(strokes - par) else "-")

/** 합계 = 현재 홀 이전까지의 파 대비 합 + (이 홀을 치기 시작했으면 이 홀의 파 대비). */
fun runningTotal(scoreBefore: Int, holeStrokes: Int, par: Int): Int =
    scoreBefore + if (holeStrokes > 0) holeStrokes - par else 0

enum class RingSegment { DONE, CURRENT, REMAINING }

/** 진행 링의 칸 상태 — 홀 번호 1..[holeCount] 순서. */
fun ringSegments(holeCount: Int, currentHole: Int): List<RingSegment> =
    List(holeCount.coerceAtLeast(0)) { index ->
        val hole = index + 1
        when {
            hole < currentHole -> RingSegment.DONE
            hole == currentHole -> RingSegment.CURRENT
            else -> RingSegment.REMAINING
        }
    }

enum class StatusBadge { CONNECTED, DISCONNECTED, GPS_FAILURE }

/** 맨 위 상태 줄에 무엇을 보일지 — 잠깐 뜨는 GPS 실패가 가장 우선, 그다음 연결 끊김. */
fun statusBadge(isPhoneConnected: Boolean, showGpsFailure: Boolean): StatusBadge = when {
    showGpsFailure -> StatusBadge.GPS_FAILURE
    !isPhoneConnected -> StatusBadge.DISCONNECTED
    else -> StatusBadge.CONNECTED
}

enum class HoleMoveDirection { NEXT, PREVIOUS }

fun holeMoveCaption(direction: HoleMoveDirection): String = when (direction) {
    HoleMoveDirection.NEXT -> "다음 홀로 이동"
    HoleMoveDirection.PREVIOUS -> "이전 홀로 이동"
}

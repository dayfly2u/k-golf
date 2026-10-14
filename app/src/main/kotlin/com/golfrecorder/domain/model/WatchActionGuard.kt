package com.golfrecorder.domain.model

/**
 * 워치에서 온 큐 액션이 낡았는지 — 워치가 누른 순간 보던 홀([queuedHole])이 라운드의 현재 홀([currentHole])과
 * 다르면 낡은 입력이다(워치가 꺼져 있다가 늦게 재연결된 경우 등). 낡은 액션은 버린다: 이전 홀에서 만든 워치
 * GPS 좌표를 단 채 다른 홀에 타수가 더해지거나, 홀이 한 번 더 넘어가는 걸 막는다.
 * [queuedHole]이 null이면(홀 번호를 안 보내는 구버전 워치) 판단할 수 없으니 낡지 않은 것으로 본다.
 */
fun isStaleWatchAction(queuedHole: Int?, currentHole: Int): Boolean =
    queuedHole != null && queuedHole != currentHole

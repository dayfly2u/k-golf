package com.golfrecorder.wear

import com.golfrecorder.wearsync.WearSync
import java.util.TreeMap

/**
 * 워치에서 눌렀지만 폰이 아직 처리했다고 확인해 주지 않은 액션. [hole]은 누른 순간 화면에 보이던 홀 번호
 * (아직 폰이 확인 안 한, 낙관적으로 먼저 넘어간 홀 포함)이고, 폰에 보내는 [WearSync.KEY_QUEUED_HOLE]과 같은 값이다.
 */
data class PendingAction(val action: String, val hole: Int)

/**
 * 폰이 아직 처리하지 않은 워치 입력 목록(누른 순서 = 큐 시퀀스 순서). 폰의 확인은 두 가지로 온다 — 상태에 실린
 * "여기까지 처리했다"([confirmThrough], 상태와 같이 도착하므로 이중으로 보이지 않는다)와, 큐 아이템 삭제 이벤트
 * ([remove], 구버전 폰이나 상태가 안 실린 경우의 보조).
 */
class PendingActions {
    private val items = TreeMap<Long, PendingAction>()

    fun add(seq: Long, action: PendingAction) {
        items[seq] = action
    }

    /** [seq] 하나를 뺀다. 실제로 있었으면 true. */
    fun remove(seq: Long): Boolean = items.remove(seq) != null

    /** 시퀀스가 [lastProcessedSeq] 이하인 것을 모두 뺀다. 뺀 게 있으면 true. */
    fun confirmThrough(lastProcessedSeq: Long): Boolean {
        val confirmed = items.headMap(lastProcessedSeq, true)
        if (confirmed.isEmpty()) return false
        confirmed.clear()
        return true
    }

    fun clear() = items.clear()

    fun toList(): List<PendingAction> = items.values.toList()
}

/**
 * 폰이 확정해 준 [server] 상태 위에 아직 확인 못 받은 [pending] 입력을 누른 순서대로 얹어 화면에 보일 상태를
 * 만든다. 블루투스가 끊긴 동안에도 타수는 물론 **홀 이동도 바로 화면에 반영**돼서, 다음 홀로 넘어간 뒤 누른
 * 입력이 그 홀 숫자에 쌓이는 게 보이고, 재연결되면 같은 순서로 폰에 반영된다.
 *
 * - 홀 번호: 서버 홀에서 시작해 ▶/◀를 순서대로 적용(1..holeCount 범위 밖은 폰과 같이 무시).
 * - 타수: 화면에 보이는 홀에서 눌린 입력만 더한다(감소는 폰처럼 0 아래로 안 내려가게 단계마다 0으로 막는다).
 *   서버가 아는 현재 홀이 아닌 홀은 기존 타수를 모르므로 0에서 시작한다(재연결되면 서버 값으로 바로잡힌다).
 * - 파: 서버 홀/직전 홀은 서버가 준 값, 그 밖은 홀별 파 목록(`holePars`, 없으면 서버 홀의 파).
 * - 합계 기준값/직전 홀 결과: 홀을 넘어간 만큼 그 홀들의 (타수 − 파)를 더하거나 빼서 맞춘다(타수 0인 홀은 제외 — 폰과
 *   같은 규칙). 서버가 모르는 홀(현재·직전 홀 밖)의 기존 타수는 0으로 본다.
 */
fun displayRound(server: RoundUiState, pending: List<PendingAction>): RoundUiState {
    if (pending.isEmpty()) return server

    var hole = server.holeNumber
    for (p in pending) {
        when (p.action) {
            WearSync.ACTION_NEXT_HOLE -> if (hole < server.holeCount) hole++
            WearSync.ACTION_PREV_HOLE -> if (hole > 1) hole--
        }
    }

    val (toGreen, putt) = counters(server, pending, hole)

    var scoreBefore = server.scoreBefore
    for (k in server.holeNumber until hole) scoreBefore += contribution(server, pending, k)
    for (k in hole until server.holeNumber) scoreBefore -= contribution(server, pending, k)

    val previousHole = if (hole >= 2) {
        val total = totalAt(server, pending, hole - 1)
        if (total > 0) PreviousHole(total, parAt(server, hole - 1)) else null
    } else {
        null
    }

    return server.copy(
        holeNumber = hole,
        par = parAt(server, hole),
        strokesToGreen = toGreen,
        strokesPutt = putt,
        scoreBefore = scoreBefore,
        previousHole = previousHole,
    )
}

/** [hole]의 (그린까지, 숏·퍼팅) 타수 — 서버 값(서버 홀일 때만)에 그 홀에서 누른 입력을 순서대로 얹는다. */
private fun counters(server: RoundUiState, pending: List<PendingAction>, hole: Int): Pair<Int, Int> {
    var toGreen = if (hole == server.holeNumber) server.strokesToGreen else 0
    var putt = if (hole == server.holeNumber) server.strokesPutt else 0
    for (p in pending) {
        if (p.hole != hole) continue
        when (p.action) {
            WearSync.ACTION_INCREMENT_TO_GREEN -> toGreen++
            WearSync.ACTION_DECREMENT_TO_GREEN -> toGreen = (toGreen - 1).coerceAtLeast(0)
            WearSync.ACTION_INCREMENT_PUTT -> putt++
            WearSync.ACTION_DECREMENT_PUTT -> putt = (putt - 1).coerceAtLeast(0)
        }
    }
    return toGreen to putt
}

/** [hole]의 총타수. 서버가 아는 건 현재 홀과 바로 이전 홀뿐이라 그 밖의 홀은 입력으로 쌓인 것만 안다. */
private fun totalAt(server: RoundUiState, pending: List<PendingAction>, hole: Int): Int {
    if (hole == server.holeNumber) {
        val (toGreen, putt) = counters(server, pending, hole)
        return toGreen + putt
    }
    var total = if (hole == server.holeNumber - 1) server.previousHole?.strokes ?: 0 else 0
    for (p in pending) {
        if (p.hole != hole) continue
        when (p.action) {
            WearSync.ACTION_INCREMENT_TO_GREEN, WearSync.ACTION_INCREMENT_PUTT -> total++
            WearSync.ACTION_DECREMENT_TO_GREEN, WearSync.ACTION_DECREMENT_PUTT -> total = (total - 1).coerceAtLeast(0)
        }
    }
    return total
}

private fun parAt(server: RoundUiState, hole: Int): Int = when {
    hole == server.holeNumber -> server.par
    hole == server.holeNumber - 1 && server.previousHole != null -> server.previousHole.par
    else -> server.holePars.getOrNull(hole - 1) ?: server.par
}

/** [hole]이 합계에 더하는 값 — 타수가 1 이상일 때만 (타수 − 파), 0타 홀은 제외(폰과 같은 규칙). */
private fun contribution(server: RoundUiState, pending: List<PendingAction>, hole: Int): Int {
    val total = totalAt(server, pending, hole)
    return if (total > 0) total - parAt(server, hole) else 0
}

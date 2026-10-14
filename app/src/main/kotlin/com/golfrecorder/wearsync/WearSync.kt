package com.golfrecorder.wearsync

/**
 * 폰 앱(:app)과 워치 앱(:wear)이 Data Layer로 주고받는 경로/키 상수.
 * 두 모듈은 별도 Gradle 모듈이라 소스를 공유하지 않으므로, 이 파일은
 * `wear/src/main/kotlin/com/golfrecorder/wearsync/WearSync.kt`에 내용을
 * 그대로 복제해서 둔다 — 한쪽만 고치면 통신이 깨지니 항상 같이 수정할 것.
 */
object WearSync {
    /** 워치 → 폰: 버튼 입력 명령 큐. DataClient DataItem이며 경로는
     * "ACTION_QUEUE_PATH_PREFIX/{시퀀스}" 형태로 액션마다 고유하게 만든다.
     * MessageClient(옛 방식)는 그 순간 연결돼 있어야만 전달되고, 끊겨 있으면
     * 조용히 실패했다 — 골프장에서는 폰-워치 블루투스 거리가 종종 끊길 수 있어서
     * (특히 "워치 GPS 켜기"를 쓸 만큼 폰을 멀리 둔 상황일수록 더), DataItem 큐로
     * 바꿔서 끊긴 동안 눌러도 재연결되면 순서대로(시퀀스 오름차순) 전달되게 한다.
     * DataMap 키는 KEY_QUEUED_ACTION(문자열), 위치가 필요한 액션(INCREMENT_*,
     * 이 라운드가 워치 GPS 기준일 때만)은 KEY_QUEUED_LAT/KEY_QUEUED_LNG도 같이
     * 싣는다. 폰이 처리한 뒤에는 그 DataItem을 지운다(재처리/무한 누적 방지). */
    const val ACTION_QUEUE_PATH_PREFIX = "/stroke/action-queue"
    const val KEY_QUEUED_ACTION = "action"
    const val KEY_QUEUED_LAT = "lat"
    const val KEY_QUEUED_LNG = "lng"
    /** 워치가 이 액션을 누른 순간 화면에 보이던 홀 번호. 폰은 처리할 때 이 값이 라운드의 현재 홀과
     * 다르면 그 액션을 버린다 — 워치가 꺼져 있다가 한참 뒤 재연결돼 오래된 입력이 늦게 도착해도
     * 엉뚱한 홀에 타수가 더해지거나 홀이 또 넘어가지 않게 하려는 것이다. 이 키가 없으면(구버전 워치)
     * 검사 없이 그대로 처리한다. */
    const val KEY_QUEUED_HOLE = "hole"

    /** 폰 → 워치: 최신 라운드 상태. DataClient DataMap. */
    const val STATE_PATH = "/round-state"

    const val ACTION_INCREMENT_TO_GREEN = "INCREMENT_TO_GREEN"
    const val ACTION_DECREMENT_TO_GREEN = "DECREMENT_TO_GREEN"
    // 워치는 숏어프로치(칩)는 다루지 않고 퍼팅만 올린다 — 폰에서는 숏어프로치/퍼팅을
    // 따로 입력할 수 있지만, 워치 화면은 작아서 퍼팅 하나만 남겼다.
    const val ACTION_INCREMENT_PUTT = "INCREMENT_PUTT"
    const val ACTION_DECREMENT_PUTT = "DECREMENT_PUTT"
    const val ACTION_NEXT_HOLE = "NEXT_HOLE"
    const val ACTION_PREV_HOLE = "PREV_HOLE"

    const val KEY_ROUND_ACTIVE = "roundActive"
    const val KEY_HOLE_NUMBER = "holeNumber"
    const val KEY_PAR = "par"
    const val KEY_STROKES_TO_GREEN = "strokesToGreen"
    const val KEY_STROKES_PUTT = "strokesPutt"
    const val KEY_HOLE_COUNT = "holeCount"
    /** DataItem은 내용이 바뀌지 않으면 재전송해도 워치의 onDataChanged가 다시 호출되지
     * 않는다(내용 해시 기준 중복 제거) — 값이 우연히 이전과 같아도 매번 갱신 이벤트가
     * 뜨도록 보내는 시점의 타임스탬프를 함께 싣는다. 워치 쪽은 이 값을 읽지 않는다. */
    const val KEY_UPDATED_AT = "updatedAt"
    /** 워치에서 누른 버튼이 GPS 위치를 못 잡아 실제로는 기록되지 않았을 때, 그 실패
     * 시각(epoch ms)을 싣는다. 워치는 이 값이 이전에 본 값보다 커지면(=새로운 실패)
     * 진동으로 알려준다 — 폰 화면의 "위치를 가져오지 못해 기록되지 않았습니다" Toast와
     * 같은 의도지만, 워치는 화면이 작아 텍스트 대신 햅틱으로 대신한다. */
    const val KEY_LAST_FAILED_AT = "lastFailedAt"
    /** 이 라운드가 샷 GPS를 워치 기준으로 기록하는지(true) 폰 기준인지(false) —
     * 코스 선택 화면에서 라운드 시작 시 정한 값. 워치는 이 값이 true일 때만 자체
     * GPS를 계속 추적하고, INCREMENT_* 액션에 좌표를 실어 보낸다. */
    const val KEY_USE_WATCH_LOCATION = "useWatchLocation"
    /** 현재 홀보다 번호가 작은 홀 중 타수가 1 이상인 홀의 (타수 − 파) 합 — 워치 "합계"의
     * 기준값(워치는 여기에 현재 홀의 파 대비를 더한다). 구버전 폰이 보내지 않으면 0으로 본다. */
    const val KEY_SCORE_BEFORE = "scoreBefore"
    /** 바로 이전 홀의 총타수와 파 — 둘은 항상 같이 있거나 같이 없다(이전 홀이 없거나 그 홀
     * 타수가 0이면 둘 다 보내지 않는다). 워치 "홀 이동" 화면의 "{n}번 홀 {타수}타 · {등급}"용. */
    const val KEY_PREV_HOLE_STROKES = "prevHoleStrokes"
    const val KEY_PREV_HOLE_PAR = "prevHolePar"
    /** 폰 → 워치: 코스의 홀별 파 목록(1번 홀부터 [KEY_HOLE_COUNT]개, IntegerArrayList). 워치가 홀 이동을 폰
     * 확인 전에 화면에 먼저 반영할 때 그 홀의 파를 알기 위해 쓴다. 없으면(구버전 폰) 현재 홀의 파
     * ([KEY_PAR])를 그대로 쓴다. */
    const val KEY_HOLE_PARS = "holePars"
    /** 폰이 지금까지 처리한 워치 액션 중 가장 큰 큐 시퀀스(= 워치가 그 액션을 만든 시각, ms). 이 상태가
     * 도착하면 워치는 그 시퀀스 이하의 대기 액션이 이미 이 상태에 반영됐다고 보고 낙관적 표시에서 뺀다 —
     * 상태 도착과 큐 아이템 삭제 이벤트가 따로 와서 한순간 홀/타수가 이중으로 보이는 걸 막는다. 없으면 0. */
    const val KEY_LAST_PROCESSED_SEQ = "lastProcessedSeq"
}

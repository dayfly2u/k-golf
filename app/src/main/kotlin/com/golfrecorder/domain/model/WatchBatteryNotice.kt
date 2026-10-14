package com.golfrecorder.domain.model

import com.golfrecorder.wearsync.WearSync

/** 폰이 워치 배터리 보고를 받았을 때 알림을 어떻게 다룰지. */
enum class WatchBatteryNoticeAction {
    /** 경고가 풀렸다(충전 시작 등) — 알림을 지운다. */
    CANCEL,

    /** 경고 단계가 올라갔다(10% 미만 진입, 5% 미만 진입) — 소리·진동·헤드업으로 알린다. */
    ALERT,

    /** 같은 단계에서 퍼센트만 바뀌었다 — 알림 내용만 조용히 갱신한다. */
    UPDATE,
}

/** [previousWarning]은 직전에 받은 경고 단계, [warning]은 방금 받은 단계([WearSync].BATTERY_WARNING_*). */
fun watchBatteryNoticeAction(previousWarning: Int, warning: Int): WatchBatteryNoticeAction = when {
    warning == WearSync.BATTERY_WARNING_NONE -> WatchBatteryNoticeAction.CANCEL
    warning > previousWarning -> WatchBatteryNoticeAction.ALERT
    else -> WatchBatteryNoticeAction.UPDATE
}

fun watchBatteryNoticeTitle(level: Int): String = "와치 배터리 ${level}%"

/** 5% 미만이면 곧 꺼질 수 있다는 것과 꺼진 뒤 할 일을, 10% 미만이면 충전을 안내한다. */
fun watchBatteryNoticeBody(warning: Int): String =
    if (warning >= WearSync.BATTERY_WARNING_CRITICAL) {
        "곧 꺼질 수 있어요. 꺼지면 폰 화면에서 타수를 입력하세요."
    } else {
        "배터리가 부족해요. 충전이 필요해요."
    }

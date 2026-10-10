package com.golfrecorder.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val periodDateFormat = SimpleDateFormat("MM.dd (E) HH:mm", Locale.KOREA)
private val periodTimeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)

/** "09.27 (토) 07:10~11:52 (4시간 42분)" — 아직 안 끝난 라운드는 "09.27 (토) 07:10~".
 * 홈 카드와 결과 화면 헤더가 같은 형식을 쓴다(시안 그대로). */
fun formatRoundPeriod(playedAt: Long, finishedAt: Long?): String {
    val start = periodDateFormat.format(Date(playedAt))
    if (finishedAt == null) return "$start~"
    val totalMinutes = (finishedAt - playedAt) / 60_000
    return "$start~${periodTimeFormat.format(Date(finishedAt))} (${totalMinutes / 60}시간 ${totalMinutes % 60}분)"
}

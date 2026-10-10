package com.golfrecorder.domain.model

/** 파 대비 점수를 "+3"/"E"/"-2" 형식으로. 결과 화면 스코어카드와 홀 기록 화면의
 * "현재 스코어"가 같은 포맷을 쓴다. */
fun formatToPar(scoreToPar: Int): String = when {
    scoreToPar == 0 -> "E"
    scoreToPar > 0 -> "+$scoreToPar"
    else -> "$scoreToPar"
}

/** 스코어 대 파 결과를 다섯 구간 이름으로 — 결과 화면 홀 결과 비율 막대(4.2)와
 * 홀 기록 화면의 "다음 홀" 버튼 서브텍스트(4.5)가 같은 구간 이름을 쓴다. */
fun scoreGradeLabel(scoreToPar: Int): String = when {
    scoreToPar <= -1 -> "버디 이하"
    scoreToPar == 0 -> "파"
    scoreToPar == 1 -> "보기"
    scoreToPar == 2 -> "더블"
    else -> "트리플+"
}

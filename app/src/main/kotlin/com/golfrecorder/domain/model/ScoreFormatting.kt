package com.golfrecorder.domain.model

/** 파 대비 점수를 "+3"/"E"/"-2" 형식으로. 결과 화면 스코어카드와 홀 기록 화면의
 * "현재 스코어"가 같은 포맷을 쓴다. */
fun formatToPar(scoreToPar: Int): String = when {
    scoreToPar == 0 -> "E"
    scoreToPar > 0 -> "+$scoreToPar"
    else -> "$scoreToPar"
}

/** 홀 하나의 파 대비 점수를 점수 레벨 구간으로: 0=버디 이하(별도 금색), 1=파, 2=보기, 3=더블,
 * 4=트리플 이상. 1~4구간은 홈의 총타수 배지(strokeScoreTier)와 같은 4색이고, 결과 화면
 * 스코어카드/비율 막대가 공유하는 기준이다. */
fun holeScoreTier(scoreToPar: Int): Int = when {
    scoreToPar <= -1 -> 0
    scoreToPar == 0 -> 1
    scoreToPar == 1 -> 2
    scoreToPar == 2 -> 3
    else -> 4
}

/** 라운드 총타수를 점수 레벨 4구간으로(홈 배지): 80대=1, 90~94=2, 95~99=3, 100 이상=4,
 * 80 미만은 null(배지 색 없음). */
fun strokeScoreTier(strokes: Int): Int? = when {
    strokes in 80..89 -> 1
    strokes in 90..94 -> 2
    strokes in 95..99 -> 3
    strokes >= 100 -> 4
    else -> null
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

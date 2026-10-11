package com.golfrecorder.domain.model

/**
 * 상세 결과 화면의 "설정 노트"에 쓰는 표시 이름 — 코스 선택 화면의 스위치 문구
 * ("와치 GPS 켜기"/"꺼져있으면 핸드폰 GPS", "해외 코스 (구글맵 사용)"/"꺼져있으면 카카오맵")와
 * 같은 표현이다. 라운드에는 [LocationSource]/[MapProvider]의 name이 문자열로 저장되며,
 * 알 수 없는 값은 라운드 기본값(PHONE/KAKAO, RoundEntity 참고)으로 본다.
 */
fun locationSourceLabel(locationSource: String): String =
    if (locationSource == LocationSource.WATCH.name) "와치 GPS" else "핸드폰 GPS"

fun mapProviderLabel(mapProvider: String): String =
    if (mapProvider == MapProvider.GOOGLE.name) "구글맵" else "카카오맵"

/** 스코어카드 헤더 우측에 보이는 한 줄 — 예: "와치 GPS · 카카오맵". */
fun roundSetupNote(locationSource: String, mapProvider: String): String =
    "${locationSourceLabel(locationSource)} · ${mapProviderLabel(mapProvider)}"

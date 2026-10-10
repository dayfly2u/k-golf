package com.golfrecorder.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 리디자인 확정 시안(docs/design/2026-10-redesign/redesign-brief.md)의 색상·모서리·
 * 터치영역 값. 화면마다 흩어진 private Color 상수를 여기 하나로 모아서, 서로 다른
 * 화면의 색이 조용히 어긋나는 문제(예: 경고 배지 색이 행 배경과 겹쳐 안 보이던 버그)를
 * 원천적으로 막는다. */
object GolfTokens {
    // 기본
    val FieldGreen = Color(0xFF1F5C3F)
    val FieldGreenDark = Color(0xFF123A28)
    val FieldGreenTint = Color(0xFFDCEBE1) // 선택된 행 등 연한 녹색 배경(GolfTheme의 primaryContainer)
    val Accent = Color(0xFFF2C14E) // CTA, 현재 홀
    val Background = Color(0xFFF4F5EF)
    val CardBackground = Color(0xFFFFFFFF)

    // 텍스트
    val TextPrimary = Color(0xFF18241D)
    val TextSecondary = Color(0xFF56635A)
    val Divider = Color(0xFFECEFE7)
    val Border = Color(0xFFCDD3C8)

    // 벌타
    val ObBackground = Color(0xFFFFF6E8)
    val ObBorder = Color(0xFFE3A34A)
    val ObText = Color(0xFF7A4A0C)
    val HazardBackground = Color(0xFFEEF3F9)
    val HazardBorder = Color(0xFF6B8FB8)
    val HazardText = Color(0xFF24476E)

    // 스코어 셀(파 이하/보기/더블 이상) — 스코어카드 그리드·홈 배지 공용
    val ScoreUnderParBorder: Color = Border // 흰 바탕 + 테두리
    val ScoreBogeyBackground = Color(0xFFDCE6F1)
    val ScoreDoubleOrWorseBackground = Color(0xFF6B8FB8) // 흰 글자

    // 점수 레벨 색 — 홈 총타수 배지와 결과 화면 스코어카드·홀 결과 비율 막대가 같이 쓴다
    // (1=파 진녹색 → 4=가장 나쁨 진빨강). 어느 구간인지는 holeScoreTier/strokeScoreTier.
    // 0구간(버디 이하)은 홀 점수에만 있는 금색(시안 Summary.dc.html의 버디 이하 색).
    val ScoreTier0 = Accent
    val ScoreTier1 = FieldGreen
    val ScoreTier2 = Color(0xFFC8E6C9)
    val ScoreTier3 = Color(0xFFFFCDD2)
    val ScoreTier4 = Color(0xFFC62828)

    // 홀 상세(리뷰)에서 실수한 항목(3퍼팅 이상, 숏어프로치, GIR 실패, OB/해저드) 행 하이라이트 — 연한 빨강.
    val MistakeHighlight = ScoreTier3

    // 18홀 진행바 — 아직 안 지나온 홀
    val ProgressRemaining = Color(0xFFDADFD4)

    // 모서리
    val CardCorner: Dp = 20.dp
    val ButtonCorner: Dp = 16.dp
    val ChipCorner: Dp = 999.dp

    // 터치 영역
    val MinTouchTarget: Dp = 44.dp
    val PrimaryButtonHeight: Dp = 58.dp
}

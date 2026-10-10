package com.golfrecorder.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.golfrecorder.R

/** 리디자인 확정 시안의 본문/숫자 글꼴 — 둘 다 Google Fonts(OFL), res/font에 번들.
 * 숫자 글꼴(Barlow Condensed)은 스코어·타수·거리·통계 숫자에만 쓴다(브리프 1절). */
object GolfFonts {
    val BodyFontFamily = FontFamily(
        Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_kr_medium, FontWeight.Medium),
        Font(R.font.ibm_plex_sans_kr_semibold, FontWeight.SemiBold),
        Font(R.font.ibm_plex_sans_kr_bold, FontWeight.Bold),
    )

    val NumberFontFamily = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )
}

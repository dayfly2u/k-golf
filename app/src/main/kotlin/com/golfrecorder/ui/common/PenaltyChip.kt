package com.golfrecorder.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfTokens

enum class PenaltyColor { OB, HAZARD }

private data class PenaltyPalette(val background: Color, val border: Color, val text: Color)

private fun PenaltyColor.palette(): PenaltyPalette = when (this) {
    PenaltyColor.OB -> PenaltyPalette(GolfTokens.ObBackground, GolfTokens.ObBorder, GolfTokens.ObText)
    PenaltyColor.HAZARD -> PenaltyPalette(GolfTokens.HazardBackground, GolfTokens.HazardBorder, GolfTokens.HazardText)
}

/** OB +1 / OB +2 / 해저드 +1 입력 버튼. */
@Composable
fun PenaltyButton(text: String, color: PenaltyColor, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = color.palette()
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        // ButtonDefaults(OutlinedButtonDefaults가 아님)가 buttonColors()/outlinedButtonColors()/
        // textButtonColors() 등을 전부 제공하는 단일 객체다 — Task 7에서 같은 실수로 한 번
        // 고친 적 있음(OutlinedButtonDefaults는 실제로 존재하지 않는 클래스).
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.background,
            contentColor = palette.text,
        ),
        border = BorderStroke(1.5.dp, palette.border),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** "OB 0" / "해저드 0" 읽기전용 칩(홀 상세/리뷰). */
@Composable
fun PenaltyDisplayChip(text: String, color: PenaltyColor, modifier: Modifier = Modifier) {
    val palette = color.palette()
    Text(
        text,
        color = palette.text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = modifier
            .background(palette.background, RoundedCornerShape(GolfTokens.ChipCorner))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

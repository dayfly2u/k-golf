package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfTokens

/** 화면 하단에 고정으로 두는 큰 CTA 버튼(새 라운딩 시작, 새 코스 추가). */
@Composable
fun PrimaryCtaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(GolfTokens.PrimaryButtonHeight),
        shape = RoundedCornerShape(GolfTokens.ButtonCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = GolfTokens.Accent,
            contentColor = GolfTokens.TextPrimary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

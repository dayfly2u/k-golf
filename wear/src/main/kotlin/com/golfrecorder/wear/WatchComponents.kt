package com.golfrecorder.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.wear.compose.material.Text

/** 원형 버튼. 비활성이면 흐리게 하고 눌리지 않는다. */
@Composable
fun WatchCircleButton(
    onClick: () -> Unit,
    size: Dp,
    background: Color,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 노랑 바탕 검정 글자의 "파 N" 칩. [designFontPx]는 시안 글자 크기(홀 기록 12, 홀 이동 14). */
@Composable
fun WatchParChip(par: Int, designFontPx: Float, scale: WatchScale) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(WatchColors.Yellow)
            .padding(horizontal = scale.dp(10f).coerceAtLeast(6.dp), vertical = 1.dp),
    ) {
        Text(
            "파 $par",
            color = Color.Black,
            fontSize = scale.sp(designFontPx),
            lineHeight = scale.lineSp(designFontPx),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** "합계 +5" 줄 — 라벨은 회색 시스템 글꼴, 값은 흰색 Barlow. */
@Composable
fun WatchTotalLine(total: Int, labelDesignPx: Float, valueDesignPx: Float, valueMinSp: Float, scale: WatchScale) {
    val text = buildAnnotatedString {
        append("합계 ")
        withStyle(
            SpanStyle(
                fontFamily = WatchFonts.Number,
                fontWeight = FontWeight.Bold,
                fontSize = scale.sp(valueDesignPx, minSp = valueMinSp),
                color = WatchColors.White,
            ),
        ) { append(formatTotal(total)) }
    }
    Text(
        text,
        color = WatchColors.Label,
        fontSize = scale.sp(labelDesignPx),
        // 값(Barlow) 글자가 더 크므로 그 크기 기준으로 줄 높이를 정한다.
        lineHeight = scale.lineSp(valueDesignPx, minSp = valueMinSp),
        maxLines = 1,
    )
}

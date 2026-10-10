package com.golfrecorder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens

private const val STEPPER_BUTTON_SIZE_DP = 48

/** 그린까지/숏게임/퍼팅처럼 "라벨 + 힌트 + 값"을 한 행으로 보여주는 공용 틀. [trailing]에
 * 편집용 −/+ 버튼 또는 읽기전용 숫자를 끼워 넣는다 — [StepperRow]/[ReadOnlyStepperRow]가
 * 각각 이 틀을 감싼다. */
@Composable
private fun StepperRowScaffold(
    label: String,
    hint: String,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    highlight: Boolean = false,
    trailing: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            // 읽기 전용(홀 상세 리뷰) 행은 하이라이트 여부와 상관없이 같은 안쪽 여백을 줘서, 빨간 행과
            // 아닌 행의 숫자 위치가 어긋나지 않게 한다.
            modifier = Modifier.fillMaxWidth()
                .then(if (highlight) Modifier.background(GolfTokens.MistakeHighlight, RoundedCornerShape(12.dp)) else Modifier)
                .then(if (readOnly) Modifier.padding(horizontal = 12.dp) else Modifier)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GolfTokens.TextPrimary)
                Text(hint, fontSize = 12.sp, color = GolfTokens.TextSecondary)
            }
            trailing()
        }
        if (showDivider) HorizontalDivider(color = GolfTokens.Divider)
    }
}

@Composable
fun StepperRow(
    label: String,
    hint: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
) {
    StepperRowScaffold(label, hint, showDivider, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDecrement,
                modifier = Modifier.size(STEPPER_BUTTON_SIZE_DP.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GolfTokens.TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GolfTokens.Border),
            ) { Text("−", fontSize = 20.sp) }
            Box(modifier = Modifier.size(STEPPER_BUTTON_SIZE_DP.dp), contentAlignment = Alignment.Center) {
                Text(
                    "$value",
                    fontFamily = GolfFonts.NumberFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = GolfTokens.TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Button(
                onClick = onIncrement,
                modifier = Modifier.size(STEPPER_BUTTON_SIZE_DP.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GolfTokens.Accent, contentColor = GolfTokens.TextPrimary),
            ) { Text("+", fontSize = 20.sp) }
        }
    }
}

@Composable
fun ReadOnlyStepperRow(
    label: String,
    hint: String,
    value: Int,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
    /** true면 행 배경을 연한 빨강으로 칠해 실수한 항목임을 표시한다(홀 상세 리뷰용). */
    highlight: Boolean = false,
) {
    StepperRowScaffold(label, hint, showDivider, modifier, readOnly = true, highlight = highlight) {
        Text(
            "$value",
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = GolfTokens.TextPrimary,
        )
    }
}

package com.golfrecorder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.golfrecorder.ui.theme.GolfTokens

/** 코스의 평점/난이도 표시 — 코스 관리와 코스 선택 화면이 같은 모양을 쓴다. 평점은 노란 별 + 숫자,
 * 난이도는 주황 칩 "난이도 N"(예전엔 둘 다 ★라 헷갈렸다). 값이 없으면 그 항목은 그리지 않는다. */
@Composable
fun CourseRatingRow(rating: Double?, difficulty: Int?, modifier: Modifier = Modifier) {
    if (rating == null && difficulty == null) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (rating != null) {
            // 노란 Accent는 흰 배경 위에서 글자색으로 쓰기엔 대비가 너무 낮아(~1.7:1) 별만
            // 노랗게, 숫자는 본문색으로 둔다.
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = GolfTokens.Accent)) { append("★") }
                    append(" ${"%.1f".format(rating)}")
                },
                color = GolfTokens.TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (difficulty != null) {
            Text(
                "난이도 $difficulty",
                color = GolfTokens.ObText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .background(GolfTokens.ObBackground, RoundedCornerShape(GolfTokens.ChipCorner))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

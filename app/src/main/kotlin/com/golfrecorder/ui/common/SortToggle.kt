package com.golfrecorder.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfTokens

/** 목록 정렬 토글 `최신순 | 점수순 | 코스순` 같은 알약 묶음 — 회색 알약 트랙 안에서 선택된 쪽만 진녹색 알약으로
 * 채운다. 홈 라운드 목록과 코스 관리 목록이 같은 모양을 쓴다. [options]는 (값, 화면에 보일 이름) 순서쌍. */
@Composable
fun <T> SortToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(GolfTokens.ChipCorner)
    Row(
        modifier = modifier.clip(pill).background(GolfTokens.BadgePlain).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier.height(36.dp)
                    .clip(pill)
                    .background(if (on) GolfTokens.FieldGreen else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) GolfTokens.CardBackground else GolfTokens.TextSecondary,
                )
            }
        }
    }
}

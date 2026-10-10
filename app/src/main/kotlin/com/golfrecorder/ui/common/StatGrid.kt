package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens

/** 통계 그리드 한 칸 — "22%" / "GIR" 처럼 큰 숫자(Barlow Condensed) + 작은 라벨.
 * [sub]를 주면 숫자 옆에 작은 보조 텍스트(예: "m", "4/18")를 붙인다. */
data class StatItem(val label: String, val value: String, val sub: String? = null)

@Composable
private fun StatCell(item: StatItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(6.dp)) {
        Text(item.label, fontSize = 11.sp, color = GolfTokens.TextSecondary)
        Row {
            Text(
                item.value,
                fontFamily = GolfFonts.NumberFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
            if (item.sub != null) {
                Text(
                    " " + item.sub,
                    fontSize = 11.sp,
                    color = GolfTokens.TextSecondary,
                )
            }
        }
    }
}

/** [columns]칸 고정 그리드 — 홈 펼침은 4개 항목에 columns=4(1행), 결과 화면은 6개
 * 항목에 columns=3(2행). 항목이 4~6개뿐이라
 * `LazyVerticalGrid`를 쓰지 않고 직접 행으로 나눈다 — `LazyVerticalGrid`/`LazyColumn`은
 * 세로 스크롤 부모 안에 중첩되면 "무한 높이" 제약으로 크래시하는 문제가 있어(이 코드베이스
 * 다른 곳에서 겪은 적 있는 패턴), 항목 수가 고정이고 적은 이 용도엔 가장 단순하고
 * 안전한 방식이다. */
@Composable
fun StatGrid(items: List<StatItem>, columns: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { item -> StatCell(item, modifier = Modifier.weight(1f)) }
                // 마지막 행이 columns보다 적게 채워지면 남은 칸만큼 빈 공간으로 메워
                // 셀 너비가 줄어들지 않게 한다.
                repeat(columns - rowItems.size) { Column(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}

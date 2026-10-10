package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.golfrecorder.ui.theme.GolfTokens

/** 리디자인 공용 리스트 카드 — 흰 배경, 둥근 모서리, 옅은 그림자. [onClick]을 주면
 * 카드 전체가 탭 타깃이 된다(홈 라운드 카드의 "탭하면 펼침" 패턴) — `Card(onClick = ...)`
 * 오버로드를 써서 리플이 카드의 둥근 모서리 안에서만 뜬다. */
@Composable
fun GolfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(GolfTokens.CardCorner)
    val colors = CardDefaults.cardColors(containerColor = GolfTokens.CardBackground)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

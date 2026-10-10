package com.golfrecorder.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.data.local.dto.RoundSummary
import com.golfrecorder.domain.model.DriverDistanceStats
import com.golfrecorder.domain.model.HoleResult
import com.golfrecorder.domain.model.RoundAggregates
import com.golfrecorder.domain.model.formatToPar
import com.golfrecorder.domain.model.strokeScoreTier
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfIcons
import com.golfrecorder.ui.theme.GolfTokens
import com.golfrecorder.ui.theme.scoreTierColor
import com.golfrecorder.ui.theme.scoreTierTextColor
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val priceFormat = NumberFormat.getNumberInstance(Locale.KOREA)

@Composable
private fun RowScope.HeaderStat(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f)
            .background(GolfTokens.CardBackground.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, fontSize = 12.sp, color = GolfTokens.CardBackground.copy(alpha = 0.85f))
        Text(
            value,
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 30.sp,
            color = GolfTokens.CardBackground,
        )
    }
}

/** 시안 홈 헤더 — 로고(깃발 아이콘) + "K-Golf", 테두리 pill "코스 관리"(핀 아이콘), 그 아래 반투명 타일 3칸
 * (평균 스코어/베스트/평균 퍼팅). 타일은 끝난 라운드가 있을 때만([aggregates]가 null이면 생략). */
@Composable
internal fun HomeHeader(aggregates: RoundAggregates?, onManageCourses: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(GolfTokens.FieldGreen)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .background(GolfTokens.CardBackground, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(GolfIcons.Flag, contentDescription = null, modifier = Modifier.size(18.dp), tint = GolfTokens.FieldGreen)
                }
                Text("K-Golf", color = GolfTokens.CardBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            val pill = RoundedCornerShape(22.dp)
            Row(
                modifier = Modifier.height(44.dp)
                    .clip(pill)
                    .border(1.dp, GolfTokens.CardBackground.copy(alpha = 0.35f), pill)
                    .clickable(onClick = onManageCourses)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GolfTokens.CardBackground,
                )
                Text("코스 관리", color = GolfTokens.CardBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (aggregates != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderStat("평균 스코어", "%.1f".format(aggregates.avgTotalStrokes))
                HeaderStat("베스트", "${aggregates.bestTotalStrokes}")
                HeaderStat("평균 퍼팅", "%.1f".format(aggregates.avgPutts))
            }
        }
    }
}

/** 카드 오른쪽 64dp 정사각 점수 배지 — 총타수 + 파 대비. 끝난 라운드는 점수 레벨 색, 아직 안 끝난
 * 라운드는 회색(부분 타수라 레벨을 매기지 않는다). */
@Composable
internal fun RoundScoreBadge(round: RoundSummary) {
    val tier = if (round.finishedAt != null) strokeScoreTier(round.totalStrokes) else null
    val background = tier?.let { scoreTierColor(it) } ?: GolfTokens.BadgePlain
    val content = tier?.let { scoreTierTextColor(it) } ?: GolfTokens.TextPrimary
    Column(
        modifier = Modifier.size(64.dp).background(background, RoundedCornerShape(16.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${round.totalStrokes}",
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 30.sp,
            color = content,
        )
        if (round.totalPar > 0) {
            Text(
                formatToPar(round.totalStrokes - round.totalPar),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
        }
    }
}

@Composable
private fun RowScope.StatTile(value: String, label: String) {
    Column(
        modifier = Modifier.weight(1f)
            .background(GolfTokens.Background, RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            color = GolfTokens.TextPrimary,
        )
        Text(label, fontSize = 11.sp, color = GolfTokens.TextSecondary)
    }
}

/** 카드를 펼쳤을 때의 내용(시안 순서): 구분선 → 그린피·동반자 → 리뷰 인용 박스(없으면 "리뷰 남기기" 링크) →
 * 4칸 통계 타일(GIR/퍼팅/드라이버/벌타) → "상세 결과 보기 ›". GIR 표시명은 코드명과 반대라 strict가 "GIR". */
@Composable
internal fun RoundCardDetails(
    round: RoundSummary,
    holeResults: List<HoleResult>,
    driverStats: DriverDistanceStats,
    penaltyCount: Int,
    onOpenDetail: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(color = GolfTokens.Divider)
        val infoLine = listOfNotNull(
            round.price?.let { "${priceFormat.format(it)}원" },
            round.companions?.takeIf { it.isNotBlank() },
        ).joinToString(" | ")
        if (infoLine.isNotBlank()) {
            Text(infoLine, fontSize = 12.sp, color = GolfTokens.TextSecondary)
        }
        val review = round.review?.trim()?.takeIf { it.isNotEmpty() }
        if (review != null) {
            Text(
                review,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = GolfTokens.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
                    .background(GolfTokens.QuoteBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        } else {
            Row(
                modifier = Modifier.clickable(onClick = onOpenDetail).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = GolfTokens.FieldGreen)
                Text("리뷰 남기기", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GolfTokens.FieldGreen)
            }
        }
        if (holeResults.isNotEmpty()) {
            val total = holeResults.size
            val girPercent = (holeResults.count { it.isStrictGreenInRegulation } * 100.0 / total).roundToInt()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatTile("$girPercent%", "GIR")
                StatTile("${holeResults.sumOf { it.strokesPutt }}", "퍼팅")
                StatTile(driverStats.avgMeters?.let { "${it.roundToInt()}m" } ?: "-", "드라이버")
                StatTile("$penaltyCount", "벌타")
            }
        }
        Text(
            "상세 결과 보기 ›",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = GolfTokens.FieldGreen,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail),
        )
    }
}

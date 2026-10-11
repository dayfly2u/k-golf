package com.golfrecorder.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.domain.model.HoleResult
import com.golfrecorder.domain.model.holeScoreTier
import com.golfrecorder.domain.model.scoreTierLabel
import com.golfrecorder.ui.common.GolfCard
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens
import com.golfrecorder.ui.theme.scoreTierColor
import com.golfrecorder.ui.theme.scoreTierTextColor
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private fun percent(count: Int, total: Int): Int = if (total == 0) 0 else (count * 100.0 / total).roundToInt()

/** 시안 "홀 결과 비율" 카드 — 한 줄 100% 막대(구간마다 퍼센트 표시) + 색 칩이 붙은 범례(홀 수).
 * 구간은 [holeScoreTier]로 나누고 순서는 항상 0~4구간. 홀 수가 0인 구간은 막대에서
 * 뺀다(Compose weight()는 0을 허용하지 않는다). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HoleResultRatioCard(holeResults: List<HoleResult>, modifier: Modifier = Modifier) {
    if (holeResults.isEmpty()) return
    val total = holeResults.size
    val tierCounts = (0..4).associateWith { tier -> holeResults.count { holeScoreTier(it.scoreToPar) == tier } }
    GolfCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("홀 결과 비율", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
            Text("${total}홀 기준", fontSize = 12.sp, color = GolfTokens.TextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            (0..4).filter { tier -> (tierCounts[tier] ?: 0) > 0 }.forEach { tier ->
                val count = tierCounts[tier] ?: 0
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .background(scoreTierColor(tier))
                        .clipToBounds(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${percent(count, total)}%",
                        fontFamily = GolfFonts.NumberFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = scoreTierTextColor(tier),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (0..4).forEach { tier ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(9.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(scoreTierColor(tier)),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${scoreTierLabel(tier)} ${tierCounts[tier] ?: 0}",
                        fontSize = 12.sp,
                        color = GolfTokens.TextPrimary,
                    )
                }
            }
        }
    }
}

private val CELL_SHAPE = RoundedCornerShape(7.dp)
private const val SLOTS_PER_HALF = 9

/** 라벨 칸(34dp) + 9개 홀 칸 + 합계 칸(34dp) 한 줄. [cell]은 0..8번째 칸을 그린다. */
@Composable
private fun CardGridRow(
    label: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    cell: @Composable (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) { label() }
        repeat(SLOTS_PER_HALF) { index ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { cell(index) }
        }
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) { trailing() }
    }
}

@Composable
private fun SmallGridText(text: String, size: Int = 12, color: Color = GolfTokens.TextSecondary) {
    Text(text, fontSize = size.sp, color = color)
}

/** 타수 칸 — 파 대비 점수 레벨 색: 버디 이하 금색, 파 진녹색, 보기 연초록, 더블 연빨강, 트리플
 * 이상 진빨강(파~트리플은 홈 총타수 배지와 같은 색). 폭은 칸에 맞춰 줄어들고 최대 28dp. */
@Composable
private fun StrokeCell(hole: HoleResult, enabled: Boolean, onClick: () -> Unit) {
    val tier = holeScoreTier(hole.scoreToPar)
    Box(
        modifier = Modifier
            .widthIn(max = 28.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CELL_SHAPE)
            .background(scoreTierColor(tier))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${hole.totalStrokes}",
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = scoreTierTextColor(tier),
        )
    }
}

@Composable
private fun ScoreCardHalf(
    name: String,
    holes: List<HoleResult>,
    enabled: Boolean,
    onHoleClick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(vertical = 3.dp)) {
        CardGridRow(
            label = { SmallGridText(name, 11) },
            trailing = { SmallGridText("합", 11) },
            cell = { index -> holes.getOrNull(index)?.let { SmallGridText("${it.holeNumber}", 11) } },
        )
        CardGridRow(
            label = { SmallGridText("파") },
            trailing = { SmallGridText("${holes.sumOf { it.par }}") },
            cell = { index -> holes.getOrNull(index)?.let { SmallGridText("${it.par}") } },
        )
        CardGridRow(
            label = {
                Text("타수", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GolfTokens.TextPrimary)
            },
            trailing = {
                Text(
                    "${holes.sumOf { it.totalStrokes }}",
                    fontFamily = GolfFonts.NumberFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = GolfTokens.TextPrimary,
                )
            },
            cell = { index ->
                holes.getOrNull(index)?.let { hole ->
                    StrokeCell(hole, enabled = enabled, onClick = { onHoleClick(hole.holeNumber) })
                }
            },
        )
    }
}

/** 시안 "스코어카드" 카드 — OUT(1~9)/IN(10~18) 격자(홀 번호 / 파 / 타수 + 합계). 타수 칸을
 * 누르면 그 홀 상세로 간다([enabled]는 코스가 남아있을 때만 true이고, 이때만 점수 아래에
 * "* 각 홀을 누르면 …" 각주를 보인다). 헤더 우측에는 이 라운드의 위치·지도 설정
 * ([setupNote], 예: "와치 GPS · 카카오맵")을 한 줄로 둔다. */
@Composable
internal fun ScoreCardGrid(
    holeResults: List<HoleResult>,
    enabled: Boolean,
    onHoleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    setupNote: String? = null,
) {
    if (holeResults.isEmpty()) return
    GolfCard(modifier = modifier) {
        // 제목(14sp)이 우측 설정 줄(11sp)보다 높아서 Bottom 정렬이어도 제목은 좌측 최상단에 붙는다.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("스코어카드", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
            setupNote?.let { note ->
                Text(note, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GolfTokens.TextSecondary)
            }
        }
        Spacer(Modifier.height(4.dp))
        listOf("OUT" to holeResults.take(SLOTS_PER_HALF), "IN" to holeResults.drop(SLOTS_PER_HALF))
            .filter { (_, holes) -> holes.isNotEmpty() }
            .forEach { (name, holes) -> ScoreCardHalf(name, holes, enabled, onHoleClick) }
        if (enabled) {
            Spacer(Modifier.height(6.dp))
            Text(
                "* 각 홀을 누르면 홀 상세정보와 각 샷의 거리가 나옵니다",
                fontSize = 11.sp,
                color = GolfTokens.TextSecondary,
            )
        }
    }
}

private val reviewPriceFormat = NumberFormat.getNumberInstance(Locale.KOREA)

@Composable
private fun ReviewInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label, fontSize = 12.sp, color = GolfTokens.TextSecondary, modifier = Modifier.width(72.dp))
        Text(value, fontSize = 14.sp, color = GolfTokens.TextPrimary, modifier = Modifier.weight(1f))
    }
}

/** "리뷰 쓰기" 버튼 위에 보여주는 작성된 리뷰 카드 — 가격/동반자/리뷰 내용. 아무것도 안 적었으면
 * 안내 문구만 보여준다. 값은 [RoundSummaryViewModel]의 price/companions/review 그대로(저장하면
 * 바로 갱신된다). */
@Composable
internal fun ReviewCard(price: String, companions: String, review: String, modifier: Modifier = Modifier) {
    val priceText = price.trim().toIntOrNull()?.let { "${reviewPriceFormat.format(it)}원" }
    val companionsText = companions.trim().takeIf { it.isNotEmpty() }
    val reviewText = review.trim().takeIf { it.isNotEmpty() }
    GolfCard(modifier = modifier) {
        Text("라운딩 리뷰", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
        Spacer(Modifier.height(6.dp))
        if (priceText == null && companionsText == null && reviewText == null) {
            Text(
                "작성된 리뷰가 없습니다. 아래 \"리뷰 쓰기\"에서 남길 수 있어요.",
                fontSize = 12.sp,
                color = GolfTokens.TextSecondary,
            )
            return@GolfCard
        }
        priceText?.let { ReviewInfoRow("라운딩 가격", it) }
        companionsText?.let { ReviewInfoRow("동반자", it) }
        if (reviewText != null) {
            if (priceText != null || companionsText != null) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = GolfTokens.Divider)
                Spacer(Modifier.height(8.dp))
            }
            Text(reviewText, fontSize = 14.sp, lineHeight = 21.sp, color = GolfTokens.TextPrimary)
        }
    }
}

/** 시안 하단의 "리뷰 쓰기" 버튼이 여는 입력창 — 가격/동반자/리뷰를 한 번에 고치고 저장한다. */
@Composable
internal fun ReviewDialog(
    initialPrice: String,
    initialCompanions: String,
    initialReview: String,
    onSave: (price: String, companions: String, review: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var price by remember { mutableStateOf(initialPrice) }
    var companions by remember { mutableStateOf(initialCompanions) }
    var review by remember { mutableStateOf(initialReview) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("라운딩 리뷰 (선택 입력)") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("라운딩 가격 (원)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = companions,
                    onValueChange = { companions = it },
                    label = { Text("동반자") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("라운딩 리뷰") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(price, companions, review) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

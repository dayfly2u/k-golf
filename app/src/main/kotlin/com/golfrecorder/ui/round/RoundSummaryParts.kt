package com.golfrecorder.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.golfrecorder.domain.model.scoreGradeLabel
import com.golfrecorder.ui.common.GolfCard
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val periodDateFormat = SimpleDateFormat("MM.dd (E) HH:mm", Locale.KOREA)
private val periodTimeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)

/** "09.27 (토) 07:10~11:52 (4시간 42분)" — 아직 안 끝난 라운드는 "09.27 (토) 07:10~". */
internal fun formatRoundPeriod(playedAt: Long, finishedAt: Long?): String {
    val start = periodDateFormat.format(Date(playedAt))
    if (finishedAt == null) return "$start~"
    val totalMinutes = (finishedAt - playedAt) / 60_000
    return "$start~${periodTimeFormat.format(Date(finishedAt))} (${totalMinutes / 60}시간 ${totalMinutes % 60}분)"
}

private val GRADE_ORDER = listOf("버디 이하", "파", "보기", "더블", "트리플+")

private val GRADE_COLOR = mapOf(
    "버디 이하" to GolfTokens.Accent,
    "파" to GolfTokens.FieldGreen,
    "보기" to GolfTokens.RatioBogey,
    "더블" to GolfTokens.ScoreDoubleOrWorseBackground,
    "트리플+" to GolfTokens.RatioTriple,
)

// 밝은 구간(버디 이하 노랑, 보기 연블루)은 어두운 글자, 나머지는 흰 글자(시안 그대로).
private val LIGHT_GRADES = setOf("버디 이하", "보기")

private fun percent(count: Int, total: Int): Int = if (total == 0) 0 else (count * 100.0 / total).roundToInt()

/** 시안 "홀 결과 비율" 카드 — 한 줄 100% 막대(구간마다 퍼센트 표시) + 색 칩이 붙은 범례(홀 수).
 * 구간은 [scoreGradeLabel]로 나누고 순서는 항상 [GRADE_ORDER]. 홀 수가 0인 구간은 막대에서
 * 뺀다(Compose weight()는 0을 허용하지 않는다). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HoleResultRatioCard(holeResults: List<HoleResult>, modifier: Modifier = Modifier) {
    if (holeResults.isEmpty()) return
    val total = holeResults.size
    val counts = GRADE_ORDER.associateWith { grade -> holeResults.count { scoreGradeLabel(it.scoreToPar) == grade } }
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
            GRADE_ORDER.filter { grade -> (counts[grade] ?: 0) > 0 }.forEach { grade ->
                val count = counts[grade] ?: 0
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .background(GRADE_COLOR.getValue(grade))
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
                        color = if (grade in LIGHT_GRADES) GolfTokens.TextPrimary else GolfTokens.CardBackground,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GRADE_ORDER.forEach { grade ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(9.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(GRADE_COLOR.getValue(grade)),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("$grade ${counts[grade] ?: 0}", fontSize = 12.sp, color = GolfTokens.TextPrimary)
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

/** 타수 칸 — 파 대비로 색을 정한다(시안 그대로): 파 이하는 흰 바탕 + 테두리, 보기는 연한
 * 블루, 더블 이상은 진한 블루 + 흰 글자. 폭은 칸에 맞춰 줄어들고 최대 28dp. */
@Composable
private fun StrokeCell(hole: HoleResult, enabled: Boolean, onClick: () -> Unit) {
    val diff = hole.scoreToPar
    val fill = when {
        diff <= 0 -> GolfTokens.CardBackground
        diff == 1 -> GolfTokens.ScoreBogeyBackground
        else -> GolfTokens.ScoreDoubleOrWorseBackground
    }
    val textColor = if (diff >= 2) GolfTokens.CardBackground else GolfTokens.TextPrimary
    Box(
        modifier = Modifier
            .widthIn(max = 28.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CELL_SHAPE)
            .background(fill)
            .then(if (diff <= 0) Modifier.border(1.5.dp, GolfTokens.ScoreUnderParBorder, CELL_SHAPE) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${hole.totalStrokes}",
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = textColor,
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
 * 누르면 그 홀 상세로 간다([enabled]는 코스가 남아있을 때만 true). */
@Composable
internal fun ScoreCardGrid(
    holeResults: List<HoleResult>,
    enabled: Boolean,
    onHoleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (holeResults.isEmpty()) return
    GolfCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("스코어카드", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
            if (enabled) {
                Text("홀을 누르면 샷 경로와 거리", fontSize = 11.sp, color = GolfTokens.TextSecondary)
            }
        }
        Spacer(Modifier.height(4.dp))
        listOf("OUT" to holeResults.take(SLOTS_PER_HALF), "IN" to holeResults.drop(SLOTS_PER_HALF))
            .filter { (_, holes) -> holes.isNotEmpty() }
            .forEach { (name, holes) -> ScoreCardHalf(name, holes, enabled, onHoleClick) }
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

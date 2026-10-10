package com.golfrecorder.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.golfrecorder.data.local.relation.CourseWithHoles
import com.golfrecorder.service.RoundRecordingService
import com.golfrecorder.data.repository.CourseRepository
import com.golfrecorder.data.repository.PenaltyRepository
import com.golfrecorder.data.repository.RoundRepository
import com.golfrecorder.data.repository.ShotRepository
import com.golfrecorder.domain.model.DriverDistanceStats
import com.golfrecorder.domain.model.HoleResult
import com.golfrecorder.domain.model.PenaltyType
import com.golfrecorder.domain.model.calculateDriverDistanceStats
import com.golfrecorder.domain.model.formatToPar
import com.golfrecorder.domain.model.scoreGradeLabel
import com.golfrecorder.ui.common.StatGrid
import com.golfrecorder.ui.common.StatItem
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class RoundSummaryViewModel(
    private val roundRepository: RoundRepository,
    courseRepository: CourseRepository,
    shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
    val roundId: Long,
    /** null이면 이 라운드를 기록한 코스가 이미 삭제된 것 — 홀 수정은 코스의 그린 위치
     * 등 홀 정보가 필요해서 코스가 남아있을 때만 가능하다. */
    val courseId: Long?,
) : ViewModel() {
    private val roundWithRecords = roundRepository.getRoundWithHoleRecords(roundId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** "완료" 버튼을 눌러 라운드가 끝났는지 — 홀을 눌러 들어갈 때 라이브 플레이로
     * 취급할지 리뷰 전용으로 취급할지 이 값으로 결정한다. [roundWithRecords]의 현재
     * 값을 그대로 읽는다 — 별도 StateFlow로 파생시키면 아무도 구독하지 않는 한
     * WhileSubscribed 정책 때문에 초기값(false)에서 절대 갱신되지 않는 문제가 있었다. */
    val isFinished: Boolean
        get() = roundWithRecords.value?.round?.finishedAt != null

    var price by mutableStateOf("")
    var companions by mutableStateOf("")
    var review by mutableStateOf("")

    init {
        viewModelScope.launch {
            roundRepository.getRoundWithHoleRecords(roundId).first()?.let { round ->
                price = round.round.price?.toString().orEmpty()
                companions = round.round.companions.orEmpty()
                review = round.round.review.orEmpty()
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            roundRepository.deleteRound(roundId)
            onDeleted()
        }
    }

    fun saveReview() {
        viewModelScope.launch {
            roundRepository.updateRoundReview(
                roundId = roundId,
                price = price.trim().toIntOrNull(),
                companions = companions.trim().ifBlank { null },
                review = review.trim().ifBlank { null },
            )
        }
    }

    // 코스가 남아있는 동안은 파/이름 둘 다 코스의 최신 값을 우선 쓴다(사용자 확정) —
    // 코스에서 파를 고치면 이미 기록된 라운드의 이븐/보기 표시도, 코스 이름을
    // 바꾸면 라운드 목록/제목도 최신 값으로 보여야 한다는 요청. 코스가 삭제됐거나
    // (courseId == null) 그 홀이 이제 코스에 없으면(홀 수를 줄인 경우 등) 라운드
    // 시작 시점에 저장해 둔 스냅샷으로 돌아간다.
    private val liveCourse: Flow<CourseWithHoles?> = if (courseId != null) {
        courseRepository.getCourseWithHoles(courseId)
    } else {
        flowOf(null)
    }

    val holeResults: StateFlow<List<HoleResult>> = combine(roundWithRecords, liveCourse) { round, course ->
        val liveParByHole = course?.holes.orEmpty().associate { hole -> hole.holeNumber to hole.par }
        round?.holeRecords.orEmpty()
            .sortedBy { record -> record.holeNumber }
            .map { record ->
                val par = liveParByHole[record.holeNumber] ?: record.par
                HoleResult(
                    record.holeNumber, par, record.strokesToGreen, record.strokesGreenToHoleOut,
                    strokesPutt = record.strokesPutt,
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driverDistanceStats: StateFlow<DriverDistanceStats> =
        combine(shotRepository.getShotsForRound(roundId), holeResults) { shots, holes ->
            calculateDriverDistanceStats(shots, holes.associate { it.holeNumber to it.par })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriverDistanceStats(null, null))

    val penaltyCounts: StateFlow<Pair<Int, Int>> = penaltyRepository.getPenaltiesForRound(roundId)
        .map { list ->
            list.count { it.type == PenaltyType.OB.name } to list.count { it.type == PenaltyType.HAZARD.name }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    val courseName: StateFlow<String> = combine(roundWithRecords, liveCourse) { round, course ->
        course?.course?.name ?: round?.round?.courseName.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
}

class RoundSummaryViewModelFactory(
    private val roundRepository: RoundRepository,
    private val courseRepository: CourseRepository,
    private val shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
    private val roundId: Long,
    private val courseId: Long?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoundSummaryViewModel(roundRepository, courseRepository, shotRepository, penaltyRepository, roundId, courseId) as T
}

// GIR(그린 적중) 실패("그린 N"), 숏어프로치 발생("숏 N", 50m 이하 숏은 곧 그린을 놓쳤다는
// 뜻), 3퍼팅 이상("퍼팅 N") 공통 경고 스타일. 전부 꽉 찬 빨강 배경 + 흰 글씨였던 적도,
// 테두리만 있는 앰버 칩이었던 적도 있었다 — 빨강 계열은 더블보기+2타 이상 홀의 진한
// 빨강 행 배경과 겹치면(배지든 테두리든) 거의 안 보였고, 빨강을 쓰는 한 행 배경이 뭐든
// 피할 수 없는 문제였다. 행 배경과 겹치지 않는 앰버로, 그리고 테두리가 아니라 꽉 채워서
// 행 배경과 무관하게 항상 대비가 보장되게 했다.
private val WARNING_COLOR = Color(0xFFE65100) // 진한 앰버
private val WARNING_TEXT_COLOR = Color.White

@Composable
private fun WarningBadge(text: String) {
    Text(
        text,
        color = WARNING_TEXT_COLOR,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(WARNING_COLOR, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

// GolfTokens의 3단계(파 이하/보기/더블 이상)로 다시 묶는다(redesign-spec.md 3.7) —
// "파 이하"(버디 이하 + 파)는 배경 없음, "보기"(+1)는 처음으로 배경을 받고,
// "더블 이상"(+2부터 전부)은 기존에 나뉘어 있던 연한/진한 빨강을 하나로 합친다.
private fun scoreRowColor(scoreToPar: Int): Color = when {
    scoreToPar == 1 -> GolfTokens.ScoreBogeyBackground
    scoreToPar >= 2 -> GolfTokens.ScoreDoubleOrWorseBackground
    else -> Color.Transparent
}

private fun scoreRowTextColor(scoreToPar: Int): Color =
    if (scoreToPar >= 2) GolfTokens.CardBackground else GolfTokens.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSummaryScreen(
    viewModel: RoundSummaryViewModel,
    onEditHole: (holeNumber: Int) -> Unit,
    onHome: () -> Unit,
) {
    val holeResults by viewModel.holeResults.collectAsStateWithLifecycle()
    val courseName by viewModel.courseName.collectAsStateWithLifecycle()
    val driverStats by viewModel.driverDistanceStats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val totalStrokes = holeResults.sumOf { it.totalStrokes }
    val totalScoreToPar = holeResults.sumOf { it.scoreToPar }
    val girCount = holeResults.count { it.isGreenInRegulation }
    val strictGirCount = holeResults.count { it.isStrictGreenInRegulation }
    val totalPutts = holeResults.sumOf { it.strokesPutt }
    val avgPutts = if (holeResults.isEmpty()) 0.0 else totalPutts.toDouble() / holeResults.size
    val frontNineStrokes = holeResults.take(9).sumOf { it.totalStrokes }
    val frontNineScoreToPar = holeResults.take(9).sumOf { it.scoreToPar }
    val backNineStrokes = holeResults.drop(9).sumOf { it.totalStrokes }
    val backNineScoreToPar = holeResults.drop(9).sumOf { it.scoreToPar }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("라운드를 삭제할까요?") },
            text = { Text("삭제하면 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    // 지금 보고 있는 라운드가 워치 연동 서비스가 추적 중인 라운드일 수
                    // 있으므로(가장 최근에 연 라운드 화면 기준) 삭제 시 함께 멈춘다 —
                    // 안 그러면 삭제된 라운드의 낡은 정보가 워치에 계속 남아있게 된다.
                    RoundRecordingService.stop(context)
                    viewModel.delete(onHome)
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            },
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .background(GolfTokens.FieldGreen)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 8.dp, bottom = 14.dp, start = 4.dp, end = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "홈으로", tint = GolfTokens.CardBackground)
                    }
                    Column {
                        Row {
                            Text(
                                courseName.ifBlank { "라운드 결과" },
                                color = GolfTokens.CardBackground,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (viewModel.courseId == null && courseName.isNotBlank()) {
                                Text(
                                    " (삭제됨)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GolfTokens.CardBackground.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("삭제", color = GolfTokens.CardBackground)
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(GolfTokens.Background)) {
            Text(
                "총 ${totalStrokes}타 (${formatToPar(totalScoreToPar)})",
                fontFamily = GolfFonts.NumberFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                color = GolfTokens.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val penaltyCounts by viewModel.penaltyCounts.collectAsStateWithLifecycle()
            val (obCount, hazardCount) = penaltyCounts
            StatGrid(
                items = listOf(
                    StatItem(label = "GIR", value = "$strictGirCount", sub = "/${holeResults.size}"),
                    StatItem(label = "L-GIR", value = "$girCount", sub = "/${holeResults.size}"),
                    StatItem(label = "퍼팅", value = "$totalPutts", sub = "평균 ${"%.1f".format(avgPutts)}"),
                    StatItem(
                        label = "드라이버 평균",
                        value = driverStats.avgMeters?.roundToInt()?.toString() ?: "-",
                        sub = "m",
                    ),
                    StatItem(
                        label = "드라이버 최장",
                        value = driverStats.maxMeters?.roundToInt()?.toString() ?: "-",
                        sub = "m",
                    ),
                    StatItem(label = "벌타", value = "OB $obCount", sub = "해저드 $hazardCount"),
                ),
                columns = 3,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            HoleResultRatioBar(
                holeResults = holeResults,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = GolfTokens.Divider)
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(holeResults, key = { _, hole -> hole.holeNumber }) { index, hole ->
                    if (index == 9) {
                        // 전반 마지막 홀과 "후반" 구분줄이 붙어서 눈에 잘 안 띄길래
                        // 얇은 흰 칸으로 한 번 끊어준다.
                        Spacer(
                            modifier = Modifier.fillMaxWidth()
                                .height(18.dp)
                                .background(GolfTokens.CardBackground),
                        )
                    }
                    if (index == 0 || index == 9) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(GolfTokens.Background)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                if (index == 0) "전반" else "후반",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.FieldGreen,
                            )
                            Text(
                                if (index == 0) {
                                    "${frontNineStrokes}타 (${formatToPar(frontNineScoreToPar)})"
                                } else {
                                    "${backNineStrokes}타 (${formatToPar(backNineScoreToPar)})"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.FieldGreen,
                            )
                        }
                    }
                    val textColor = scoreRowTextColor(hole.scoreToPar)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(scoreRowColor(hole.scoreToPar))
                            .clickable(enabled = viewModel.courseId != null) { onEditHole(hole.holeNumber) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${hole.holeNumber}홀 (파${hole.par})", fontWeight = FontWeight.Bold, color = textColor)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hole.isGreenInRegulation) {
                                Text("그린 ${hole.strokesToGreen}", color = textColor)
                            } else {
                                WarningBadge("그린 ${hole.strokesToGreen}")
                            }
                            // 숏어프로치를 실제로 입력한 홀만 나눠서 보여준다 — 대부분의
                            // 홀은 전부 퍼팅이라, 매번 "숏 0 · 퍼팅 N"으로 보이면 오히려
                            // 안 나눈 것보다 읽기 불편하다.
                            if (hole.strokesShortGame > 0) {
                                Text(" · ", color = textColor)
                                WarningBadge("숏 ${hole.strokesShortGame}")
                            }
                            Text(" · ", color = textColor)
                            // 3퍼팅 이상도 숏어프로치와 마찬가지로 실수로 보고 경고 표기.
                            if (hole.strokesPutt >= 3) {
                                WarningBadge("퍼팅 ${hole.strokesPutt}")
                            } else {
                                Text("퍼팅 ${hole.strokesPutt}", color = textColor)
                            }
                        }
                        Text("${hole.totalStrokes}타 (${formatToPar(hole.scoreToPar)})", color = textColor)
                    }
                    HorizontalDivider(color = GolfTokens.Divider)
                }
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("라운딩 리뷰 (선택 입력)", fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
                            TextButton(onClick = { viewModel.saveReview() }) { Text("저장") }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.price,
                            onValueChange = { viewModel.price = it },
                            label = { Text("라운딩 가격 (원)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.companions,
                            onValueChange = { viewModel.companions = it },
                            label = { Text("동반자") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = viewModel.review,
                            onValueChange = { viewModel.review = it },
                            label = { Text("라운딩 리뷰") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

private val GRADE_ORDER = listOf("버디 이하", "파", "보기", "더블", "트리플+")

private val GRADE_COLOR = mapOf(
    "버디 이하" to GolfTokens.Accent,
    "파" to GolfTokens.FieldGreen,
    "보기" to GolfTokens.ScoreBogeyBackground,
    "더블" to GolfTokens.ScoreDoubleOrWorseBackground,
    "트리플+" to GolfTokens.TextSecondary,
)

/** 18홀 스코어를 다섯 구간(버디 이하/파/보기/더블/트리플+)으로 나눠 한 줄 비율 막대 +
 * 아래 범례(홀 수)로 보여준다. [scoreGradeLabel]로 구간을 나누고, 구간 순서는 항상
 * [GRADE_ORDER] 그대로 — 홀 수가 0인 구간은 막대에서 생략한다(Compose weight()는
 * 0을 허용하지 않는다). */
@Composable
private fun HoleResultRatioBar(holeResults: List<HoleResult>, modifier: Modifier = Modifier) {
    if (holeResults.isEmpty()) return
    val counts = GRADE_ORDER.associateWith { grade -> holeResults.count { scoreGradeLabel(it.scoreToPar) == grade } }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(GolfTokens.ChipCorner)),
        ) {
            GRADE_ORDER.filter { grade -> (counts[grade] ?: 0) > 0 }.forEach { grade ->
                Spacer(
                    modifier = Modifier
                        .weight((counts[grade] ?: 0).toFloat())
                        .fillMaxHeight()
                        .background(GRADE_COLOR.getValue(grade)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GRADE_ORDER.forEach { grade ->
                Text(
                    "$grade ${counts[grade] ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GolfTokens.TextSecondary,
                )
            }
        }
    }
}

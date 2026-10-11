package com.golfrecorder.ui.round

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.golfrecorder.domain.model.computeRecentRank
import com.golfrecorder.domain.model.formatToPar
import com.golfrecorder.domain.model.roundSetupNote
import com.golfrecorder.ui.common.StatGrid
import com.golfrecorder.ui.common.formatRoundPeriod
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

    val recentRank: StateFlow<Pair<Int, Int>?> = roundRepository.getRoundSummaries()
        .map { all -> computeRecentRank(all, roundId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** (시작 시각, 끝난 시각 또는 null) — 헤더의 "09.27 (토) 07:10~11:52 (4시간 42분)"용. */
    val roundTimes: StateFlow<Pair<Long, Long?>?> = roundWithRecords
        .map { rwr -> rwr?.round?.let { it.playedAt to it.finishedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courseName: StateFlow<String> = combine(roundWithRecords, liveCourse) { round, course ->
        course?.course?.name ?: round?.round?.courseName.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** "와치 GPS · 카카오맵" 같은 위치·지도 설정 한 줄 — 라운드를 시작할 때 코스 선택 화면에서
     * 정한 값이라 라운드 내내 고정이다. 스코어카드 헤더 우측에 쓴다. */
    val setupNote: StateFlow<String?> = roundWithRecords
        .map { rwr -> rwr?.round?.let { roundSetupNote(it.locationSource, it.mapProvider) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
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
    val penaltyCounts by viewModel.penaltyCounts.collectAsStateWithLifecycle()
    val roundTimes by viewModel.roundTimes.collectAsStateWithLifecycle()
    val recentRank by viewModel.recentRank.collectAsStateWithLifecycle()
    val setupNote by viewModel.setupNote.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    val totalHoles = holeResults.size
    val totalStrokes = holeResults.sumOf { it.totalStrokes }
    val totalScoreToPar = holeResults.sumOf { it.scoreToPar }
    val girCount = holeResults.count { it.isGreenInRegulation }
    val strictGirCount = holeResults.count { it.isStrictGreenInRegulation }
    val totalPutts = holeResults.sumOf { it.strokesPutt }
    val avgPutts = if (holeResults.isEmpty()) 0.0 else totalPutts.toDouble() / holeResults.size
    val percentOfHoles: (Int) -> Int = { count -> if (totalHoles == 0) 0 else (count * 100.0 / totalHoles).roundToInt() }

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

    if (showReviewDialog) {
        ReviewDialog(
            initialPrice = viewModel.price,
            initialCompanions = viewModel.companions,
            initialReview = viewModel.review,
            onSave = { price, companions, review ->
                viewModel.price = price
                viewModel.companions = companions
                viewModel.review = review
                viewModel.saveReview()
                showReviewDialog = false
            },
            onDismiss = { showReviewDialog = false },
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .background(GolfTokens.FieldGreen)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 6.dp, bottom = 18.dp, start = 4.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "홈으로", tint = GolfTokens.CardBackground)
                    }
                    // 제목이 남는 폭을 전부 차지하고(weight) 삭제 버튼은 먼저 측정돼 항상 제 크기를
                    // 지킨다 — 코스 이름이 길면 제목이 줄바꿈될 뿐 버튼이 밀려나지 않는다.
                    Column(modifier = Modifier.weight(1f)) {
                        Row {
                            Text(
                                courseName.ifBlank { "라운드 결과" },
                                color = GolfTokens.CardBackground,
                                fontSize = 16.sp,
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
                        roundTimes?.let { (playedAt, finishedAt) ->
                            Text(
                                formatRoundPeriod(playedAt, finishedAt),
                                fontSize = 12.sp,
                                color = GolfTokens.CardBackground.copy(alpha = 0.85f),
                            )
                        }
                    }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("삭제", color = GolfTokens.CardBackground)
                    }
                }
                Row(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "$totalStrokes",
                        fontFamily = GolfFonts.NumberFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 76.sp,
                        lineHeight = 68.sp,
                        color = GolfTokens.CardBackground,
                    )
                    Column(
                        modifier = Modifier.padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            formatToPar(totalScoreToPar),
                            fontFamily = GolfFonts.NumberFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            color = GolfTokens.CardBackground,
                        )
                        recentRank?.let { (rank, pool) ->
                            Text(
                                "최근 ${pool}회 중 ${rank}위",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.TextPrimary,
                                modifier = Modifier
                                    .background(GolfTokens.Accent, RoundedCornerShape(GolfTokens.ChipCorner))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(GolfTokens.Background)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            ) {
                OutlinedButton(
                    onClick = { showReviewDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(GolfTokens.ButtonCorner),
                    border = BorderStroke(1.5.dp, GolfTokens.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GolfTokens.TextPrimary),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("리뷰 쓰기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(GolfTokens.Background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 표시명과 코드명이 반대다 — "GIR"은 strictGirCount(isStrictGreenInRegulation),
            // "L-GIR"은 girCount(isGreenInRegulation)(redesign-spec.md 4.1, 표시만 교체).
            StatGrid(
                items = listOf(
                    StatItem(label = "GIR", value = "${percentOfHoles(strictGirCount)}%", sub = "$strictGirCount/$totalHoles"),
                    StatItem(label = "L-GIR", value = "${percentOfHoles(girCount)}%", sub = "$girCount/$totalHoles"),
                    // 홀당 평균이 주 값이고 전체 개수는 작은 보조 값 — 라운드 길이(9/18홀)와 무관하게
                    // 비교할 수 있는 쪽을 크게 보여준다.
                    StatItem(label = "홀당 퍼팅", value = "%.1f".format(avgPutts), sub = "총 $totalPutts"),
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
                    StatItem(
                        label = "벌타",
                        value = "${penaltyCounts.first + penaltyCounts.second}",
                        sub = "OB ${penaltyCounts.first} · 해저드 ${penaltyCounts.second}",
                    ),
                ),
                columns = 3,
                cardCells = true,
            )
            HoleResultRatioCard(holeResults = holeResults)
            ScoreCardGrid(
                holeResults = holeResults,
                enabled = viewModel.courseId != null,
                onHoleClick = onEditHole,
                setupNote = setupNote,
            )
            ReviewCard(
                price = viewModel.price,
                companions = viewModel.companions,
                review = viewModel.review,
            )
        }
    }
}

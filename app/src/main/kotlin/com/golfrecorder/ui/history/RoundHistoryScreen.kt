package com.golfrecorder.ui.history

import android.content.Intent
import android.net.Uri
import android.os.Process
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.golfrecorder.backup.BackupManager
import com.golfrecorder.data.local.dto.RoundSummary
import com.golfrecorder.data.repository.PenaltyRepository
import com.golfrecorder.data.repository.RoundRepository
import com.golfrecorder.data.repository.ShotRepository
import com.golfrecorder.domain.model.DriverDistanceStats
import com.golfrecorder.domain.model.HoleResult
import com.golfrecorder.domain.model.HomeListEntry
import com.golfrecorder.domain.model.RoundSortMode
import com.golfrecorder.domain.model.buildHomeListEntries
import com.golfrecorder.domain.model.calculateDriverDistanceStats
import com.golfrecorder.domain.model.computeEstimatedHandicap
import com.golfrecorder.domain.model.computeRoundAggregates
import com.golfrecorder.domain.model.effectiveYearFilter
import com.golfrecorder.domain.model.filterByYear
import com.golfrecorder.domain.model.roundYears
import com.golfrecorder.domain.model.sortRoundsByYear
import com.golfrecorder.ui.common.GolfCard
import com.golfrecorder.ui.common.MoreBelowIndicator
import com.golfrecorder.ui.common.PrimaryCtaButton
import com.golfrecorder.ui.common.formatRoundPeriod
import com.golfrecorder.ui.theme.GolfIcons
import com.golfrecorder.ui.theme.GolfTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoundHistoryViewModel(
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
) : ViewModel() {
    val rounds: StateFlow<List<RoundSummary>> = roundRepository.getRoundSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 리스트 행을 펼쳤을 때만(remember(round.roundId) 스코프) 구독되는 홀별/샷별
    // 상세 데이터 — 리스트 전체를 미리 불러오지 않고 펼친 라운드만 조회한다.
    fun getHoleResults(roundId: Long): Flow<List<HoleResult>> =
        roundRepository.getRoundWithHoleRecords(roundId).map { round ->
            round?.holeRecords.orEmpty()
                .sortedBy { it.holeNumber }
                .map { record ->
                    HoleResult(
                        record.holeNumber, record.par, record.strokesToGreen, record.strokesGreenToHoleOut,
                        strokesPutt = record.strokesPutt,
                    )
                }
        }

    fun getDriverDistanceStats(roundId: Long): Flow<DriverDistanceStats> =
        combine(shotRepository.getShotsForRound(roundId), roundRepository.getRoundWithHoleRecords(roundId)) { shots, round ->
            val records = round?.holeRecords.orEmpty()
            calculateDriverDistanceStats(
                shots,
                records.associate { it.holeNumber to it.par },
                records.filter { it.excludeFromDriverStats }.map { it.holeNumber }.toSet(),
            )
        }

    /** 펼친 카드의 "벌타" 타일 — OB/해저드 건수 합. */
    fun getPenaltyCount(roundId: Long): Flow<Int> =
        penaltyRepository.getPenaltiesForRound(roundId).map { it.size }
}

class RoundHistoryViewModelFactory(
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoundHistoryViewModel(roundRepository, shotRepository, penaltyRepository) as T
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundHistoryScreen(
    viewModel: RoundHistoryViewModel,
    onStartRound: () -> Unit,
    onManageCourses: () -> Unit,
    onRoundClick: (RoundSummary) -> Unit,
) {
    val rounds by viewModel.rounds.collectAsStateWithLifecycle()
    var expandedRoundIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // 화면을 돌려도 정렬이 유지되도록 rememberSaveable. 점수순은 단순 총타수 오름차순(sortRounds 참고).
    var sortMode by rememberSaveable { mutableStateOf(RoundSortMode.RECENT) }
    // 연도 필터 — 기본은 전체(null)이고, 기록이 2개년 이상일 때만 칩이 보인다. 선택은 화면 회전/뒤로 갔다 오기에서
    // 유지하되(rememberSaveable) 앱을 새로 열면 다시 전체. 고른 연도 기록이 사라졌으면 전체로 돌아간다.
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    val years = remember(rounds) { roundYears(rounds) }
    val yearFilter = effectiveYearFilter(selectedYear, years)
    val visibleRounds = remember(rounds, yearFilter) { filterByYear(rounds, yearFilter) }
    // 연도별로 묶고(최신 연도부터) 그 안에서 최신순/점수순으로 정렬한다 — 점수순도 같은 연도 안에서만 줄 세운다.
    val sortedRounds = remember(visibleRounds, sortMode) { sortRoundsByYear(visibleRounds, sortMode) }
    // 전체 보기에서는 정렬 방식과 상관없이, 기록이 한 해뿐이어도 연도 구분선을 넣는다. 연도를 고르면 한 해만 보이니 없다.
    val listEntries = remember(sortedRounds, yearFilter) {
        buildHomeListEntries(sortedRounds, withYearHeaders = yearFilter == null)
    }
    val overallAggregates = remember(rounds) { computeRoundAggregates(rounds) }
    val yearAggregates = remember(visibleRounds) { computeRoundAggregates(visibleRounds) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { BackupManager.backup(context, uri) }
                    .onSuccess { Toast.makeText(context, "백업 완료", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, "백업 실패: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    var restorePendingUri by remember { mutableStateOf<Uri?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) restorePendingUri = uri }

    restorePendingUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restorePendingUri = null },
            title = { Text("백업을 복원할까요?") },
            text = {
                Text("복원하면 지금 기기에 저장된 모든 코스·라운드 기록이 이 백업 파일 내용으로 대체됩니다. 되돌릴 수 없습니다.")
            },
            confirmButton = {
                TextButton(onClick = {
                    restorePendingUri = null
                    scope.launch {
                        runCatching { BackupManager.restore(context, uri) }
                            .onSuccess {
                                // 기존 리포지토리/DAO가 닫힌 연결을 들고 있으므로 새로
                                // 뜨는 프로세스에서 깨끗하게 다시 열리도록 앱을 재시작한다.
                                val intent = context.packageManager
                                    .getLaunchIntentForPackage(context.packageName)
                                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
                                if (intent != null) context.startActivity(intent)
                                Process.killProcess(Process.myPid())
                            }
                            .onFailure { Toast.makeText(context, "복원 실패: ${it.message}", Toast.LENGTH_LONG).show() }
                    }
                }) { Text("복원") }
            },
            dismissButton = {
                TextButton(onClick = { restorePendingUri = null }) { Text("취소") }
            },
        )
    }

    Scaffold(
        topBar = {
            HomeHeader(
                aggregates = yearAggregates,
                // 핸디캡은 최근 기록 기준이라 연도 필터와 무관하게 전체 라운드로 계산한다.
                handicap = remember(rounds) { computeEstimatedHandicap(rounds) },
                showSummary = overallAggregates != null,
                years = years,
                selectedYear = yearFilter,
                onSelectYear = { selectedYear = it },
                onManageCourses = onManageCourses,
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = { backupLauncher.launch(BackupManager.backupFileName()) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                ) { Text("백업", style = MaterialTheme.typography.bodySmall) }
                TextButton(
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                ) { Text("복원", style = MaterialTheme.typography.bodySmall) }
            }
        },
    ) { padding ->
        if (rounds.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Text(
                    "아직 기록된 라운드가 없습니다. 아래 버튼을 눌러 시작하세요.",
                    color = GolfTokens.TextPrimary,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryCtaButton(
                    text = "새 라운딩 시작!",
                    onClick = onStartRound,
                    icon = Icons.Filled.Add,
                    modifier = Modifier.height(GolfTokens.PrimaryButtonHeight),
                )
            }
            return@Scaffold
        }
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier.padding(padding).fillMaxSize().background(GolfTokens.Background),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = GolfTokens.PrimaryButtonHeight + 32.dp,
                ),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                if (yearFilter == null) "총 라운드" else "${yearFilter}년 라운드",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.TextPrimary,
                            )
                            Text("${visibleRounds.size}회", fontSize = 12.sp, color = GolfTokens.TextSecondary)
                        }
                        RoundSortToggle(selected = sortMode, onSelect = { sortMode = it })
                    }
                }
                items(
                    listEntries,
                    key = { entry ->
                        when (entry) {
                            is HomeListEntry.YearHeader -> "year-${entry.year}"
                            is HomeListEntry.Round -> "round-${entry.round.roundId}"
                        }
                    },
                ) { entry ->
                    val round = when (entry) {
                        is HomeListEntry.YearHeader -> {
                            YearDividerRow(entry.year, entry.roundCount, entry.avgTotalStrokes)
                            return@items
                        }
                        is HomeListEntry.Round -> entry.round
                    }
                    val expanded = round.roundId in expandedRoundIds
                    GolfCard(
                        modifier = Modifier.padding(bottom = 10.dp),
                        onClick = {
                            expandedRoundIds = if (expanded) {
                                expandedRoundIds - round.roundId
                            } else {
                                expandedRoundIds + round.roundId
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row {
                                    Text(
                                        round.courseName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GolfTokens.TextPrimary,
                                    )
                                    if (round.courseId == null) {
                                        Text(
                                            " (삭제됨)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GolfTokens.TextSecondary,
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        GolfIcons.Clock,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = GolfTokens.TextSecondary,
                                    )
                                    Text(
                                        formatRoundPeriod(round.playedAt, round.finishedAt),
                                        fontSize = 13.sp,
                                        color = GolfTokens.TextSecondary,
                                    )
                                }
                            }
                            RoundScoreBadge(round)
                        }
                        if (expanded) {
                            val holeResults by remember(round.roundId) { viewModel.getHoleResults(round.roundId) }
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                            val driverStats by remember(round.roundId) { viewModel.getDriverDistanceStats(round.roundId) }
                                .collectAsStateWithLifecycle(initialValue = DriverDistanceStats(null, null))
                            val penaltyCount by remember(round.roundId) { viewModel.getPenaltyCount(round.roundId) }
                                .collectAsStateWithLifecycle(initialValue = 0)
                            RoundCardDetails(
                                round = round,
                                holeResults = holeResults,
                                driverStats = driverStats,
                                penaltyCount = penaltyCount,
                                onOpenDetail = { onRoundClick(round) },
                            )
                        }
                    }
                }
            }
            MoreBelowIndicator(
                listState = listState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            )
            PrimaryCtaButton(
                text = "새 라운딩 시작!",
                onClick = onStartRound,
                icon = Icons.Filled.Add,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(GolfTokens.PrimaryButtonHeight),
            )
        }
    }
}

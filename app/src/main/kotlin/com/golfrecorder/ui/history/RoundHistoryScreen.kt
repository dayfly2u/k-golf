package com.golfrecorder.ui.history

import android.content.Intent
import android.net.Uri
import android.os.Process
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.golfrecorder.backup.BackupManager
import com.golfrecorder.data.local.dto.RoundSummary
import com.golfrecorder.data.repository.RoundRepository
import com.golfrecorder.data.repository.ShotRepository
import com.golfrecorder.domain.model.DriverDistanceStats
import com.golfrecorder.domain.model.HoleResult
import com.golfrecorder.domain.model.calculateDriverDistanceStats
import com.golfrecorder.ui.common.GirSummaryLines
import com.golfrecorder.ui.common.GolfCard
import com.golfrecorder.ui.common.MoreBelowIndicator
import com.golfrecorder.ui.common.PrimaryCtaButton
import com.golfrecorder.ui.common.RoundStatsLine
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoundHistoryViewModel(
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository,
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
        combine(shotRepository.getShotsForRound(roundId), getHoleResults(roundId)) { shots, holes ->
            calculateDriverDistanceStats(shots, holes.associate { it.holeNumber to it.par })
        }
}

class RoundHistoryViewModelFactory(
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoundHistoryViewModel(roundRepository, shotRepository) as T
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
private val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
private val priceFormat = NumberFormat.getNumberInstance(Locale.KOREA)

private fun formatDuration(playedAt: Long, finishedAt: Long): String {
    val totalMinutes = (finishedAt - playedAt) / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}hr ${minutes}m"
}

private fun formatRoundPeriod(playedAt: Long, finishedAt: Long?): String {
    val date = dateFormat.format(Date(playedAt))
    val start = timeFormat.format(Date(playedAt))
    if (finishedAt == null) return "$date $start~"
    val end = timeFormat.format(Date(finishedAt))
    return "$date $start~$end (${formatDuration(playedAt, finishedAt)})"
}

private val STROKE_LIGHT_BLUE = Color(0xFFBBDEFB)
private val STROKE_LIGHT_GREEN = Color(0xFFC8E6C9)
private val STROKE_LIGHT_RED = Color(0xFFFFCDD2) // 연한 빨강 — RoundSummaryScreen의 DOUBLE_BOGEY_COLOR와 동일 톤
private val STROKE_DARK_RED = Color(0xFFC62828) // 진한 빨강 — RoundSummaryScreen의 GIR_MISS_BG_COLOR와 동일 톤

private fun strokeScoreColor(strokes: Int): Color? = when {
    strokes in 80..89 -> STROKE_LIGHT_BLUE
    strokes in 90..94 -> STROKE_LIGHT_GREEN
    strokes in 95..99 -> STROKE_LIGHT_RED
    strokes >= 100 -> STROKE_DARK_RED
    else -> null
}

// 진한 빨강 배경은 검정 텍스트로는 대비가 약해서 흰 글자로 바꾼다 — RoundSummaryScreen의
// scoreRowTextColor와 동일한 패턴.
private fun strokeScoreTextColor(strokes: Int): Color =
    if (strokes >= 100) Color.White else Color.Unspecified

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
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .background(GolfTokens.FieldGreen)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "K-Golf",
                    color = GolfTokens.CardBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                )
                TextButton(onClick = onManageCourses) {
                    Text("코스 관리", color = GolfTokens.CardBackground)
                }
            }
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                items(rounds, key = { it.roundId }) { round ->
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Text(round.courseName, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
                                    if (round.courseId == null) {
                                        Text(
                                            " (삭제됨)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GolfTokens.TextSecondary,
                                        )
                                    }
                                }
                                Text(
                                    formatRoundPeriod(round.playedAt, round.finishedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GolfTokens.TextSecondary,
                                )
                                val infoLine = listOfNotNull(
                                    round.price?.let { "${priceFormat.format(it)}원" },
                                    round.companions?.takeIf { it.isNotBlank() },
                                ).joinToString(" | ")
                                if (infoLine.isNotBlank()) {
                                    Text(infoLine, style = MaterialTheme.typography.bodySmall, color = GolfTokens.TextSecondary)
                                }
                            }
                            val backgroundColor = strokeScoreColor(round.totalStrokes)
                            Text(
                                "${round.totalStrokes}",
                                fontFamily = GolfFonts.NumberFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                color = strokeScoreTextColor(round.totalStrokes),
                                modifier = (
                                    if (backgroundColor != null) {
                                        Modifier.background(backgroundColor, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                ).padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        if (expanded) {
                            val holeResults by remember(round.roundId) { viewModel.getHoleResults(round.roundId) }
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                            val driverStats by remember(round.roundId) { viewModel.getDriverDistanceStats(round.roundId) }
                                .collectAsStateWithLifecycle(initialValue = DriverDistanceStats(null, null))
                            val expandedGirCount = holeResults.count { it.isGreenInRegulation }
                            val expandedStrictGirCount = holeResults.count { it.isStrictGreenInRegulation }
                            val expandedTotalPutts = holeResults.sumOf { it.strokesPutt }
                            val expandedAvgPutts = if (holeResults.isEmpty()) {
                                0.0
                            } else {
                                expandedTotalPutts.toDouble() / holeResults.size
                            }
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                if (holeResults.isNotEmpty()) {
                                    GirSummaryLines(
                                        girCount = expandedGirCount,
                                        strictGirCount = expandedStrictGirCount,
                                        totalHoles = holeResults.size,
                                        singleLine = true,
                                    )
                                    RoundStatsLine(
                                        driverStats = driverStats,
                                        avgPutts = expandedAvgPutts,
                                        totalPutts = expandedTotalPutts,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                val reviewText = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)) {
                                        append("리뷰: ")
                                    }
                                    val reviewContent = round.review?.takeIf { it.isNotBlank() }
                                    if (reviewContent != null) {
                                        withStyle(SpanStyle(color = GolfTokens.TextPrimary)) { append(reviewContent) }
                                    } else {
                                        withStyle(SpanStyle(color = GolfTokens.TextSecondary)) {
                                            append("작성된 라운딩 리뷰가 없습니다. \"상세 결과 보기\"에서 추가할 수 있습니다.")
                                        }
                                    }
                                }
                                Text(reviewText, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "상세 결과 보기 ›",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GolfTokens.FieldGreen,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clickable { onRoundClick(round) },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                )
                            }
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(GolfTokens.PrimaryButtonHeight),
            )
        }
    }
}

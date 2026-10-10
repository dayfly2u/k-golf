# K-Golf 리디자인 스테이지 2 — 기존 화면 디자인 교체 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 스테이지 1에서 만든 디자인 토큰(`GolfTokens`)·글꼴(`GolfFonts`)·공용 컴포넌트(`GolfCard`/`PrimaryCtaButton`/`StatGrid`/`HoleProgressBar`/`StepperRow`/`PenaltyChip`)를 5개 기존 화면에 실제로 적용해 확정 시안대로 다시 그린다. **기능은 하나도 바꾸지 않는다** — ViewModel 로직, 상태, 콜백 시그니처는 전부 그대로 두고 Composable UI 레이어만 교체한다.

**Architecture:** 각 화면 파일(`RoundHistoryScreen.kt`, `RoundPlayScreen.kt`, `RoundSummaryScreen.kt`, `CourseManageScreen.kt`, `CourseEditScreen.kt`)을 한 Task씩 맡아 그 파일의 Composable 부분만 다시 쓴다. ViewModel/Factory 클래스는 건드리지 않는다(수정 대상 라인 범위에서 제외).

**Tech Stack:** Jetpack Compose, Material3(토큰 교체), 스테이지 1의 `com.golfrecorder.ui.theme`/`com.golfrecorder.ui.common`.

## Global Constraints

- **기능 변화 없음.** 모든 Task는 ViewModel 클래스·Factory 클래스·상태 변수·콜백 파라미터 시그니처를 한 글자도 바꾸지 않는다. `@Composable fun XScreen(...)`의 파라미터 목록도 그대로 유지한다. 바뀌는 건 그 함수 **내부**의 UI 코드뿐이다. 각 Task의 마지막 자체 점검 단계에서 "로직(분기 조건, 호출 순서, 전달되는 값)이 하나도 안 바뀌었는지" 원본과 diff를 다시 확인한다.
- **헤더 패턴**: 시안 중 `Main.dc.html`(홈)과 `Summary.dc.html`(라운드 결과)만 초록 배경 + 아래쪽 둥근 모서리(26dp) + 내용을 품은 커스텀 헤더를 쓴다. 나머지(`RoundPlay.dc.html`/`Hole.dc.html`/`CourseManage.dc.html`/`CourseEdit.dc.html`)는 흰 배경에 아이콘 전용 뒤로가기 + 타이틀인 평범한 헤더다. **둘 다 `Scaffold(topBar = { ... })` 슬롯 자체는 그대로 쓴다** — ux-conventions-commercial의 "TopAppBar 사용" 규칙과 어긋나지 않으면서 시안의 시각을 그대로 내려면, 초록 헤더 2곳은 `topBar` 슬롯 안에 `TopAppBar`를 호출하는 대신 직접 만든 초록색 Composable(`Modifier.clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))`)을 넣고, 나머지 4곳은 지금처럼 `TopAppBar`를 그대로 쓰되 색상만 토큰으로 바꾼다.
- **뒤로가기 아이콘**: `C:\github\.claude\skills\ux-conventions-commercial\SKILL.md`의 패턴대로 전 화면 공통으로 `IconButton(onClick = ...) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }`를 쓴다(지금의 `TextButton { Text("< 뒤로") }` 대체). `import androidx.compose.material.icons.Icons`, `import androidx.compose.material.icons.automirrored.filled.ArrowBack`, `import androidx.compose.material3.Icon`, `import androidx.compose.material3.IconButton` 필요.
- **GolfCard 리플 모양 수정 포함**: `app/src/main/kotlin/com/golfrecorder/ui/common/GolfCard.kt`는 지금 `Modifier.clickable(onClick = onClick)`을 써서 리플이 카드의 둥근 모서리를 무시하고 네모로 뜬다(스테이지 1 최종 리뷰에서 발견, 실제 화면에 처음 쓰이는 지금 고친다). Task 1에서 이 파일을 함께 고친다(아래 Task 1 Step 1).
- **코스 난이도는 아직 정수 단위.** `CourseEntity.difficulty`는 아직 `Int?`다(0.5 단위 전환은 스테이지 4의 DB 마이그레이션 이후). Task 5(코스 추가·수정)에서 시안처럼 −/+ 버튼 UI로 바꾸되, 난이도 쪽 스텝은 **1 단위**로 유지한다(평점은 이미 `Double?`라 0.5 단위로 바꿔도 안전).
- **OB/해저드 되돌리기 기능 보존**: 시안(`RoundPlay.dc.html`)은 "OB +1"/"OB +2"/"해저드 +1" 버튼만 보여주고 되돌리기(decrement) UI가 안 보이지만, 지금 앱은 되돌릴 수 있다(`onRemovePenalty`). 기능을 없앨 수 없으므로, 버튼들 아래에 현재 건수를 보여주는 `PenaltyDisplayChip`을 두고 그 칩을 탭하면 되돌리게 한다(아래 Task 2에서 구체적으로).
- 앱 코드가 바뀌는 커밋마다 `app/build.gradle.kts`의 `versionName` PATCH를 1 올리고 커밋 제목 끝에 `(vX.Y.Z)`를 붙인다(계획 작성 시점 버전은 `0.8.14` — 실행 시점에 실제 파일을 열어 현재 값을 확인하고 거기서 +1 할 것, 이 숫자를 맹신하지 말 것). `versionCode`는 건드리지 않는다.
- 이 스테이지의 모든 Task는 **컴파일 확인만** 검증 수단으로 쓴다(`./gradlew.bat :app:compileDebugKotlin`) — 기존 로직을 그대로 옮기는 순수 리스타일링이라 새 단위테스트 대상이 없다(스테이지 1에서 정한 컨벤션: 순수 로직만 JUnit, 레이아웃은 실기기 확인).
- 각 Task 끝날 때마다 실기기 설치 확인은 **사용자 책임**이다(워크스페이스 CLAUDE.md 안드로이드 앱 검증 그라운드룰) — Claude는 빌드·설치·`logcat`까지만 하고 화면을 직접 넘겨가며 캡처하지 않는다.

---

## File Structure

| 파일 | 역할 | 비고 |
|---|---|---|
| `app/src/main/kotlin/com/golfrecorder/ui/common/GolfCard.kt` | (수정) 리플 모양 수정 | Task 1에서 같이 |
| `app/src/main/kotlin/com/golfrecorder/ui/history/RoundHistoryScreen.kt` | 홈 — 전면 교체 | Task 1 |
| `app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt` | 홀 기록(라이브) + 홀 상세(리뷰) — 전면 교체 | Task 2 |
| `app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt` | 라운드 결과 — 전면 교체 | Task 3 |
| `app/src/main/kotlin/com/golfrecorder/ui/course/CourseManageScreen.kt` | 코스 관리 — 전면 교체 | Task 4 |
| `app/src/main/kotlin/com/golfrecorder/ui/course/CourseEditScreen.kt` | 코스 추가·수정 — 전면 교체 | Task 5 |

---

### Task 1: 홈 (`RoundHistoryScreen`)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/common/GolfCard.kt` (리플 모양 수정)
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/history/RoundHistoryScreen.kt` (Composable 전체 교체, ViewModel/Factory 그대로)

**Interfaces:**
- Consumes: `GolfTokens.*`, `GolfFonts.NumberFontFamily`, `GolfCard`, `PrimaryCtaButton`, `MoreBelowIndicator`(기존), `GirSummaryLines`/`RoundStatsLine`(기존)
- Produces: 변화 없음(이 화면을 호출하는 쪽 시그니처 그대로)

- [ ] **Step 1: GolfCard 리플 모양 수정**

`GolfCard.kt`의 `Card(...)` 호출을 아래로 바꾼다 — `onClick`이 있을 때 `Card(onClick = ...)` 오버로드(리플이 모양에 맞게 클립됨)를 쓰고, 없을 때는 지금처럼 평범한 `Card(...)`를 쓴다:

```kotlin
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
```

- [ ] **Step 2: RoundHistoryScreen.kt 전체 교체**

ViewModel/Factory(기존 `class RoundHistoryViewModel`, `class RoundHistoryViewModelFactory`, `formatDuration`/`formatRoundPeriod` 헬퍼, `strokeScoreColor`/`strokeScoreTextColor`)는 **그대로 둔다** — 아래는 그 아래의 `RoundHistoryScreen` Composable과 import만 교체한 전체 파일이다:

```kotlin
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
private val STROKE_LIGHT_RED = Color(0xFFFFCDD2)
private val STROKE_DARK_RED = Color(0xFFC62828)

private fun strokeScoreColor(strokes: Int): Color? = when {
    strokes in 80..89 -> STROKE_LIGHT_BLUE
    strokes in 90..94 -> STROKE_LIGHT_GREEN
    strokes in 95..99 -> STROKE_LIGHT_RED
    strokes >= 100 -> STROKE_DARK_RED
    else -> null
}

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
            TopAppBar(
                title = {
                    Text(
                        "K-Golf",
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    )
                },
                actions = {
                    TextButton(onClick = onManageCourses) {
                        Text("코스 관리", color = GolfTokens.CardBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GolfTokens.FieldGreen,
                    titleContentColor = GolfTokens.CardBackground,
                ),
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
```

**로직 보존 체크리스트** (자체 점검 때 원본과 비교):
- `expandedRoundIds` 토글 조건 동일
- **인터랙션 변경(의도된 것, 버그 아님)**: `onRoundClick(round)`를 호출하는 🔍 아이콘(접힌 상태에서도 항상 노출)은 완전히 제거됐다 — redesign-spec.md 3.1절이 명시한 의도된 변경이다. 대신 펼쳤을 때만 보이는 "상세 결과 보기 ›" 텍스트(카드 맨 아래, 우측 정렬)가 `onRoundClick(round)`를 호출한다. 접은 상태에서 상세로 바로 가는 경로는 이제 없다(먼저 펼쳐야 함) — 이것도 의도된 것.
- `GirSummaryLines`/`RoundStatsLine` 호출 인자 동일
- 백업/복원 다이얼로그 로직 100% 동일(문구, `runCatching`, 프로세스 재시작 포함)
- "새 라운딩 시작!"이 이제 하단 고정 `PrimaryCtaButton`(기존엔 TopAppBar actions) — `onStartRound` 콜백 자체는 동일하게 전달

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/common/GolfCard.kt \
        app/src/main/kotlin/com/golfrecorder/ui/history/RoundHistoryScreen.kt \
        app/build.gradle.kts
git commit -m "feat: 홈 화면 리디자인 적용 (vX.Y.Z)"
```

---

### Task 2: 홀 기록(라이브) + 홀 상세(리뷰) (`RoundPlayScreen`)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt` (Composable 전체 교체, ViewModel/Factory/`PENALTY_OB_COLOR`/`PENALTY_HAZARD_COLOR` 그대로)

**Interfaces:**
- Consumes: `GolfTokens.*`, `StepperRow`/`ReadOnlyStepperRow`, `PenaltyButton`/`PenaltyDisplayChip`/`PenaltyColor`(스테이지 1)
- Produces: 변화 없음

이 Task가 가장 크다 — 라이브(`!isReview`)와 리뷰(`isReview`) 두 모드를 한 파일에서 전부 새 컴포넌트로 바꾼다. OB/해저드는 "기존 PenaltyStepper/StrokeStepper(둘 다 구 컴포넌트, 이 파일 밖 `ui/common`에 있음 — 건드리지 않음, 이 화면에서만 참조를 끊는다)" 대신 `PenaltyButton`(추가) + `PenaltyDisplayChip`(표시, 탭하면 되돌리기)으로 바꾼다.

- [ ] **Step 1: RoundPlayScreen.kt의 Composable 부분 교체**

`class RoundPlayViewModel`, `class RoundPlayViewModelFactory`, 맨 위 `PENALTY_OB_COLOR`/`PENALTY_HAZARD_COLOR` 상수는 **그대로 둔다**(이 두 상수는 지도 마커 색과 맞추는 용도라 `CourseMapView`와 공유 — 이 Task 범위 밖). 아래는 `@OptIn(ExperimentalMaterial3Api::class) @Composable fun RoundPlayScreen(...)` 함수 **전체**와 필요한 import 변경이다 — 함수 시그니처(`viewModel`, `mapSlotState`, `onFinished`, `onShowSummary` 4개 파라미터)는 그대로.

```kotlin
// 교체 대상 import 블록(파일 맨 위, package 선언 바로 아래) — 기존 import 중
// android.widget.Toast ~ kotlinx.coroutines.sync.withLock 까지는 전부 그대로 두고
// 아래 줄만 추가한다. 원본 파일엔 background/clickable/RoundedCornerShape가 전혀
// import돼 있지 않았다(필요한 적이 없었음) — 새 코드가 셋 다 쓰므로 빠뜨리면
// 컴파일 에러가 난다:
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import com.golfrecorder.ui.common.PenaltyButton
import com.golfrecorder.ui.common.PenaltyColor
import com.golfrecorder.ui.common.PenaltyDisplayChip
import com.golfrecorder.ui.common.ReadOnlyStepperRow
import com.golfrecorder.ui.common.StepperRow
import com.golfrecorder.ui.theme.GolfTokens
```

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundPlayScreen(
    viewModel: RoundPlayViewModel,
    mapSlotState: MapSlotState,
    onFinished: () -> Unit,
    onShowSummary: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shotMutex = remember { Mutex() }

    LaunchedEffect(Unit) {
        if (viewModel.isReview) {
            RoundRecordingService.stop(context)
        } else {
            RoundRecordingService.start(context, viewModel.roundId, viewModel.courseId)
        }
    }

    BackHandler { onShowSummary() }

    val holes by viewModel.holes.collectAsStateWithLifecycle()
    val shots by viewModel.shots.collectAsStateWithLifecycle()
    val penalties by viewModel.penalties.collectAsStateWithLifecycle()
    val holeCount = holes.size
    val currentHole = holes.find { it.holeNumber == viewModel.currentHoleNumber }
    val par = currentHole?.par ?: 4
    val greenLat = currentHole?.greenLat
    val greenLng = currentHole?.greenLng
    val greenLocation = if (greenLat != null && greenLng != null) AppLatLng(greenLat, greenLng) else null

    var hasLocationPermission by remember { mutableStateOf(LocationCapture.hasPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 결과와 무관하게 앱은 계속 동작한다. */ }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    val online = remember(viewModel.currentHoleNumber) { isOnline(context) }

    var currentLocation by remember { mutableStateOf<AppLatLng?>(null) }
    LaunchedEffect(hasLocationPermission, viewModel.currentHoleNumber, online) {
        if (hasLocationPermission && online) {
            currentLocation = LocationCapture.getCurrentLocation(context)
        }
    }

    var showFinishConfirm by remember { mutableStateOf(false) }
    var recenterSignal by remember { mutableStateOf(0) }
    fun recenterOnCurrentLocation() {
        scope.launch {
            val loc = LocationCapture.getCurrentLocation(context)
            if (loc != null) {
                currentLocation = loc
                recenterSignal += 1
            }
        }
    }

    fun onStepperChange(phase: ShotPhase, oldValue: Int, newValue: Int) {
        val holeNumber = viewModel.currentHoleNumber
        scope.launch {
            shotMutex.withLock {
                if (newValue > oldValue) {
                    val loc = if (hasLocationPermission) LocationTracker.latestLocation(context) else null
                    if (loc != null) {
                        viewModel.recordShot(holeNumber, phase, newValue, loc.lat, loc.lng)
                    } else {
                        Toast.makeText(context, "위치를 가져오지 못해 타수가 기록되지 않았습니다", Toast.LENGTH_SHORT).show()
                    }
                } else if (newValue < oldValue) {
                    viewModel.removeShot(holeNumber, phase, oldValue)
                }
            }
            RoundRecordingService.refreshState(context)
        }
    }

    fun onAddPenalty(phase: ShotPhase, type: PenaltyType, strokeCount: Int) {
        val holeNumber = viewModel.currentHoleNumber
        val lastShotLocation = shots.filter { it.phase == phase.name }
            .maxByOrNull { it.shotIndex }
            ?.let { AppLatLng(it.lat, it.lng) }
        scope.launch {
            shotMutex.withLock {
                val loc = lastShotLocation
                    ?: if (hasLocationPermission) LocationTracker.latestLocation(context) else null
                if (loc == null) {
                    Toast.makeText(context, "위치를 가져오지 못해 벌타가 기록되지 않았습니다", Toast.LENGTH_SHORT).show()
                }
                viewModel.addPenalty(holeNumber, phase, type, strokeCount, loc?.lat, loc?.lng) {
                    RoundRecordingService.refreshState(context)
                }
            }
        }
    }

    fun onRemovePenalty(phase: ShotPhase, type: PenaltyType) {
        viewModel.removeLastPenalty(viewModel.currentHoleNumber, phase, type) {
            RoundRecordingService.refreshState(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${viewModel.currentHoleNumber}홀",
                            fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "파 $par",
                            color = GolfTokens.CardBackground,
                            modifier = Modifier
                                .background(GolfTokens.FieldGreen, RoundedCornerShape(GolfTokens.ChipCorner))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onShowSummary() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val fixedLocation = currentLocation
            val showRecenterButton = !viewModel.isReview && hasLocationPermission && online
            if (showRecenterButton) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { recenterOnCurrentLocation() }) { Text("위치 조정") }
                }
                Spacer(Modifier.height(8.dp))
            }
            val provider = viewModel.mapProvider
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(GolfTokens.FieldGreenDark, RoundedCornerShape(GolfTokens.CardCorner)),
            ) {
                when {
                    !hasLocationPermission -> Text(
                        "위치 권한이 필요합니다.",
                        color = GolfTokens.CardBackground,
                        modifier = Modifier.padding(16.dp),
                    )
                    online && provider != null -> CourseMapSlot(
                        state = mapSlotState,
                        cameraKey = "round-${viewModel.roundId}-hole-${viewModel.currentHoleNumber}",
                        provider = provider,
                        greenLocation = greenLocation,
                        currentLocation = fixedLocation,
                        shots = shots.map { ShotPoint(ShotPhase.valueOf(it.phase), it.lat, it.lng) },
                        penalties = penalties.map { PenaltyPoint(PenaltyType.valueOf(it.type), it.lat, it.lng) },
                        recenterSignal = recenterSignal,
                        preferCurrentLocation = !viewModel.isReview,
                    )
                    online -> {}
                    greenLocation != null && fixedLocation != null -> {
                        val distance = haversineMeters(
                            fixedLocation.lat, fixedLocation.lng, greenLocation.lat, greenLocation.lng,
                        )
                        Text(
                            "오프라인 - 그린까지 약 ${distance.toInt()}m",
                            color = GolfTokens.CardBackground,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    else -> Text(
                        "오프라인 상태입니다.",
                        color = GolfTokens.CardBackground,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            val obToGreenCount = penalties.count {
                it.phase == ShotPhase.TO_GREEN.name && it.type == PenaltyType.OB.name
            }
            val hazardToGreenCount = penalties.count {
                it.phase == ShotPhase.TO_GREEN.name && it.type == PenaltyType.HAZARD.name
            }

            Spacer(Modifier.height(16.dp))
            if (viewModel.isReview) {
                ReadOnlyStepperRow(label = "그린까지", hint = "티샷부터 그린 도착", value = viewModel.strokesToGreen)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PenaltyDisplayChip("OB ${obToGreenCount}", PenaltyColor.OB)
                    PenaltyDisplayChip("해저드 ${hazardToGreenCount}", PenaltyColor.HAZARD)
                }
                ReadOnlyStepperRow(label = "숏어프로치", hint = "어프로치 · 칩샷", value = viewModel.strokesShortGame)
                ReadOnlyStepperRow(label = "퍼팅", hint = "그린 위", value = viewModel.strokesPutt, showDivider = false)
            } else {
                StepperRow(
                    label = "그린까지",
                    hint = "티샷부터 그린 도착",
                    value = viewModel.strokesToGreen,
                    onIncrement = { onStepperChange(ShotPhase.TO_GREEN, viewModel.strokesToGreen, viewModel.strokesToGreen + 1) },
                    onDecrement = { onStepperChange(ShotPhase.TO_GREEN, viewModel.strokesToGreen, viewModel.strokesToGreen - 1) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PenaltyButton(
                        text = "OB +1",
                        color = PenaltyColor.OB,
                        onClick = { onAddPenalty(ShotPhase.TO_GREEN, PenaltyType.OB, 1) },
                        modifier = Modifier.weight(1f),
                    )
                    PenaltyButton(
                        text = "OB +2",
                        color = PenaltyColor.OB,
                        onClick = { onAddPenalty(ShotPhase.TO_GREEN, PenaltyType.OB, 2) },
                        modifier = Modifier.weight(1f),
                    )
                    PenaltyButton(
                        text = "해저드 +1",
                        color = PenaltyColor.HAZARD,
                        onClick = { onAddPenalty(ShotPhase.TO_GREEN, PenaltyType.HAZARD, 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // 시안엔 OB/해저드를 되돌리는 UI가 없지만(+버튼만 보임), 기존에 되돌리기
                // 기능이 있었으므로(onRemovePenalty) 없앨 수 없다 — 건수 칩을 탭하면
                // 되돌리게 해서 기능은 유지하면서 화면은 깔끔하게 둔다. 0건이면 탭해도
                // onRemovePenalty가 조용히 아무것도 안 한다(ViewModel이 이미 처리).
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PenaltyDisplayChip(
                        "OB ${obToGreenCount} · 탭해서 취소",
                        PenaltyColor.OB,
                        modifier = Modifier.clickable { onRemovePenalty(ShotPhase.TO_GREEN, PenaltyType.OB) },
                    )
                    PenaltyDisplayChip(
                        "해저드 ${hazardToGreenCount} · 탭해서 취소",
                        PenaltyColor.HAZARD,
                        modifier = Modifier.clickable { onRemovePenalty(ShotPhase.TO_GREEN, PenaltyType.HAZARD) },
                    )
                }
                StepperRow(
                    label = "숏어프로치",
                    hint = "어프로치 · 칩샷",
                    value = viewModel.strokesShortGame,
                    onIncrement = { onStepperChange(ShotPhase.SHORT_GAME, viewModel.strokesShortGame, viewModel.strokesShortGame + 1) },
                    onDecrement = { onStepperChange(ShotPhase.SHORT_GAME, viewModel.strokesShortGame, viewModel.strokesShortGame - 1) },
                )
                StepperRow(
                    label = "퍼팅",
                    hint = "그린 위",
                    value = viewModel.strokesPutt,
                    onIncrement = { onStepperChange(ShotPhase.PUTT, viewModel.strokesPutt, viewModel.strokesPutt + 1) },
                    onDecrement = { onStepperChange(ShotPhase.PUTT, viewModel.strokesPutt, viewModel.strokesPutt - 1) },
                    showDivider = false,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        viewModel.goToHole(viewModel.currentHoleNumber - 1) {
                            RoundRecordingService.refreshState(context)
                        }
                    },
                    enabled = viewModel.currentHoleNumber > 1,
                    shape = RoundedCornerShape(GolfTokens.ButtonCorner),
                ) { Text("이전 홀") }
                if (holeCount == 0 || viewModel.currentHoleNumber < holeCount) {
                    Button(
                        onClick = {
                            viewModel.goToHole(viewModel.currentHoleNumber + 1) {
                                RoundRecordingService.refreshState(context)
                            }
                        },
                        shape = RoundedCornerShape(GolfTokens.ButtonCorner),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = GolfTokens.TextPrimary,
                            contentColor = GolfTokens.CardBackground,
                        ),
                    ) { Text("다음 홀") }
                } else if (!viewModel.isReview) {
                    Button(
                        onClick = { showFinishConfirm = true },
                        shape = RoundedCornerShape(GolfTokens.ButtonCorner),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = GolfTokens.TextPrimary,
                            contentColor = GolfTokens.CardBackground,
                        ),
                    ) { Text("완료") }
                }
            }
            if (showFinishConfirm) {
                AlertDialog(
                    onDismissRequest = { showFinishConfirm = false },
                    title = { Text("라운드를 완료할까요?") },
                    text = { Text("완료하면 홀 정보를 더 이상 수정할 수 없습니다.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showFinishConfirm = false
                            viewModel.finishRound(onFinished)
                        }) { Text("완료") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFinishConfirm = false }) { Text("취소") }
                    },
                )
            }
        }
    }
}
```

**주의**: 위 코드에서 `OutlinedButton`/`ButtonDefaults`를 `androidx.compose.material3.OutlinedButton`/`androidx.compose.material3.ButtonDefaults`처럼 완전한 경로로 쓴 자리가 있다 — import를 깔끔히 하고 싶으면 `import androidx.compose.material3.OutlinedButton`과 `import androidx.compose.material3.ButtonDefaults`를 추가하고 짧게 써도 된다(기능 동일, 구현자 재량).

**OB/해저드 입력 UI가 통째로 바뀌는 것(기존 PenaltyStepper/StrokeStepper → PenaltyButton+PenaltyDisplayChip)은 시각적 교체이지 기능 변화가 아니다** — `onAddPenalty`/`onRemovePenalty` 호출 지점과 인자는 원본과 동일하게 유지했는지 자체 점검에서 반드시 확인.

**로직 보존 체크리스트**:
- `onStepperChange`/`onAddPenalty`/`onRemovePenalty`/`recenterOnCurrentLocation` 함수 본문 100% 동일
- `LaunchedEffect`들의 키·조건·내용 100% 동일(특히 `RoundRecordingService.start/stop` 분기)
- `obToGreenCount`/`hazardToGreenCount` 계산식 동일
- 지도 `when` 분기 4가지(권한 없음/온라인+provider/온라인만/오프라인) 순서와 조건 동일
- 이전 홀 `enabled` 조건, 다음 홀 vs 완료 버튼 분기(`holeCount == 0 || currentHoleNumber < holeCount`) 동일
- 완료 확인 다이얼로그 문구·버튼 동일

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt app/build.gradle.kts
git commit -m "feat: 홀 기록/홀 상세 화면 리디자인 적용 (vX.Y.Z)"
```

---

### Task 3: 라운드 결과 (`RoundSummaryScreen`)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt` (Composable 전체 교체, ViewModel/Factory/`formatToPar`/색상 상수/`WarningBadge`/`scoreRowColor`/`scoreRowTextColor` 그대로)

**Interfaces:**
- Consumes: `GolfTokens.*`, `GolfFonts.NumberFontFamily`
- Produces: 변화 없음

**주의**: 이 Task는 스테이지 3(신규-UI)의 "6칸 통계"·"홀 결과 비율 막대"·"최근 5회 중 N위"를 넣지 않는다 — 지금 있는 그대로("총 N타", `GirSummaryLines`, `RoundStatsLine`, 18홀 스코어카드, 리뷰 입력)를 시안의 색/모서리/헤더 스타일로만 바꾼다. 헤더는 Global Constraints의 "초록 커스텀 헤더" 패턴을 쓴다(Main.dc.html과 같은 종류).

- [ ] **Step 1: RoundSummaryScreen.kt의 Composable 부분 교체**

`class RoundSummaryViewModel`, `class RoundSummaryViewModelFactory`, `formatToPar`, `PAR_COLOR`/`BIRDIE_OR_BETTER_COLOR`/`DOUBLE_BOGEY_COLOR`/`WORSE_THAN_DOUBLE_BOGEY_COLOR`/`WARNING_COLOR`/`WARNING_TEXT_COLOR`, `WarningBadge`, `scoreRowColor`, `scoreRowTextColor`는 **그대로 둔다**. `@Composable fun RoundSummaryScreen(...)` 전체를 아래로 교체(시그니처는 `viewModel`, `onEditHole`, `onHome` 그대로):

```kotlin
// 추가 import (기존 블록 끝에 추가). background/clickable는 원본에 이미 있음 —
// clip은 foundation 패키지가 아니라 ui.draw 패키지라 주의(흔한 실수):
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens
```

```kotlin
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
                    .background(GolfTokens.FieldGreen)
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "총 ${totalStrokes}타 (${formatToPar(totalScoreToPar)})",
                    fontFamily = GolfFonts.NumberFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    color = GolfTokens.TextPrimary,
                )
                GirSummaryLines(
                    girCount = girCount,
                    strictGirCount = strictGirCount,
                    totalHoles = holeResults.size,
                    horizontalAlignment = Alignment.End,
                )
            }
            RoundStatsLine(
                driverStats = driverStats,
                avgPutts = avgPutts,
                totalPutts = totalPutts,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            HorizontalDivider(color = GolfTokens.Divider)
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(holeResults, key = { _, hole -> hole.holeNumber }) { index, hole ->
                    if (index == 9) {
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
                            if (hole.strokesShortGame > 0) {
                                Text(" · ", color = textColor)
                                WarningBadge("숏 ${hole.strokesShortGame}")
                            }
                            Text(" · ", color = textColor)
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
```

**로직 보존 체크리스트**:
- 삭제 다이얼로그(문구, `RoundRecordingService.stop` 호출, `viewModel.delete(onHome)`) 동일
- `totalStrokes`/`totalScoreToPar`/`girCount`/`strictGirCount`/`totalPutts`/`avgPutts`/`frontNine*`/`backNine*` 계산식 전부 동일
- 홀 행의 `onEditHole(hole.holeNumber)` 클릭 조건(`enabled = viewModel.courseId != null`) 동일
- GIR/숏/퍼팅 경고 배지 분기 조건(`isGreenInRegulation`, `strokesShortGame > 0`, `strokesPutt >= 3`) 동일
- 리뷰 입력 3개 필드 + 저장 버튼 동일

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt app/build.gradle.kts
git commit -m "feat: 라운드 결과 화면 리디자인 적용 (vX.Y.Z)"
```

---

### Task 4: 코스 관리 (`CourseManageScreen`)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/course/CourseManageScreen.kt` (Composable 전체 교체, ViewModel/Factory/`ReviewSection` 그대로)

**Interfaces:**
- Consumes: `GolfTokens.*`, `GolfCard`, `PrimaryCtaButton`
- Produces: 변화 없음

**드래그 정렬 컴포넌트(`dragElevation`/`dragHandle`/`rememberDragDropListState`)는 그대로 재사용** — 이 Task는 그 로직을 건드리지 않고 시각 스타일만 바꾼다.

- [ ] **Step 1: CourseManageScreen.kt의 Composable 부분 교체**

`class CourseManageViewModel`, `class CourseManageViewModelFactory`, `@Composable private fun ReviewSection(...)`은 **그대로 둔다**. `@Composable fun CourseManageScreen(...)` 전체를 아래로 교체(시그니처는 `viewModel`, `onAddCourse`, `onEditCourse`, `onBack` 그대로):

```kotlin
// 추가 import. 원본엔 clickable만 있고 background/RoundedCornerShape는 없다 —
// 새 코드(배경색, 난이도 칩)가 둘 다 쓰므로 빠뜨리면 컴파일 에러:
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.golfrecorder.ui.common.GolfCard
import com.golfrecorder.ui.common.PrimaryCtaButton
import com.golfrecorder.ui.theme.GolfTokens
```

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManageScreen(
    viewModel: CourseManageViewModel,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var expandedCourseIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val context = LocalContext.current

    var displayCourses by remember { mutableStateOf(courses) }
    val listState = rememberLazyListState()
    val dragState = rememberDragDropListState(
        listState = listState,
        itemCount = { displayCourses.size },
        onMove = { from, to ->
            displayCourses = displayCourses.toMutableList().apply { add(to, removeAt(from)) }
        },
        onReorderFinished = { viewModel.reorder(displayCourses) },
    )
    LaunchedEffect(courses) {
        if (dragState.draggingItemIndex == null) displayCourses = courses
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("코스 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    Text(
                        "${displayCourses.size}개 코스",
                        style = MaterialTheme.typography.bodySmall,
                        color = GolfTokens.TextSecondary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        if (displayCourses.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Text("등록된 코스가 없습니다. 아래 버튼을 눌러 추가하세요.", color = GolfTokens.TextPrimary)
                Spacer(Modifier.height(16.dp))
                PrimaryCtaButton(
                    text = "새 코스 추가",
                    onClick = onAddCourse,
                    modifier = Modifier.height(GolfTokens.PrimaryButtonHeight),
                )
            }
            return@Scaffold
        }
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(GolfTokens.Background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "코스를 누르면 상세 리뷰가 펼쳐져요. ≡를 길게 눌러 순서를 바꿀 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GolfTokens.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        itemsIndexed(displayCourses, key = { _, course -> course.id }) { index, course ->
                            val expanded = course.id in expandedCourseIds
                            GolfCard(
                                modifier = Modifier.dragElevation(index, dragState).padding(bottom = 10.dp),
                                onClick = {
                                    expandedCourseIds = if (expanded) {
                                        expandedCourseIds - course.id
                                    } else {
                                        expandedCourseIds + course.id
                                    }
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "≡",
                                            modifier = Modifier.dragHandle(index, dragState).padding(end = 12.dp),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = GolfTokens.TextSecondary,
                                        )
                                        Column {
                                            Text(course.name, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (course.rating != null) {
                                                    Text(
                                                        "★ ${"%.1f".format(course.rating)}  ",
                                                        color = GolfTokens.FieldGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                                if (course.difficulty != null) {
                                                    Text(
                                                        "난이도 ${course.difficulty}",
                                                        color = GolfTokens.ObText,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier
                                                            .background(GolfTokens.ObBackground, RoundedCornerShape(GolfTokens.ChipCorner))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                            val infoLine = listOfNotNull(
                                                course.region?.takeIf { it.isNotBlank() },
                                                course.distance?.takeIf { it.isNotBlank() },
                                                course.travelTime?.takeIf { it.isNotBlank() },
                                            ).joinToString(" | ")
                                            if (infoLine.isNotBlank()) {
                                                Text(infoLine, style = MaterialTheme.typography.bodySmall, color = GolfTokens.TextSecondary)
                                            }
                                            if (!course.oneLineReview.isNullOrBlank()) {
                                                Text(
                                                    course.oneLineReview.chunked(30).joinToString("\n"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GolfTokens.TextSecondary,
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "✏️",
                                        modifier = Modifier
                                            .clickable { onEditCourse(course.id) }
                                            .padding(4.dp),
                                    )
                                }
                                if (expanded) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        val hasDetail = !course.transportInfo.isNullOrBlank() ||
                                            !course.clubhouseInfo.isNullOrBlank() ||
                                            !course.courseInfo.isNullOrBlank()
                                        if (!hasDetail) {
                                            Text(
                                                "입력된 리뷰 상세 정보가 없습니다. \"✏️\"에서 추가할 수 있습니다.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GolfTokens.TextSecondary,
                                            )
                                        } else {
                                            course.transportInfo?.takeIf { it.isNotBlank() }?.let {
                                                ReviewSection("교통", it)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                            course.clubhouseInfo?.takeIf { it.isNotBlank() }?.let {
                                                ReviewSection("클럽하우스", it)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                            course.courseInfo?.takeIf { it.isNotBlank() }?.let {
                                                ReviewSection("코스", it)
                                            }
                                        }

                                        val youtubeLinksFlow = remember(course.id) { viewModel.getYoutubeLinks(course.id) }
                                        val youtubeLinks by youtubeLinksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                                        if (youtubeLinks.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "유튜브 링크",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = GolfTokens.FieldGreen,
                                            )
                                            youtubeLinks.forEach { link ->
                                                Text(
                                                    "▶ ${link.title ?: link.url}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GolfTokens.FieldGreen,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                        .clickable {
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                                        }
                                                        .padding(vertical = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MoreBelowIndicator(
                        listState = listState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                    )
                }
            }
            PrimaryCtaButton(
                text = "새 코스 추가",
                onClick = onAddCourse,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(GolfTokens.PrimaryButtonHeight),
            )
        }
    }
}
```

**로직 보존 체크리스트**:
- `displayCourses`/`dragState`/`LaunchedEffect(courses)` 드래그 정렬 로직 100% 동일
- `expandedCourseIds` 토글 조건 동일
- `onEditCourse(course.id)` 호출, 펼친 상태의 리뷰 섹션 3개 `takeIf`/`hasDetail` 조건, 유튜브 링크 목록 동일
- "새 코스 추가" 콜백(`onAddCourse`)은 하단 고정 버튼으로 위치만 이동, 콜백 자체는 동일

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/course/CourseManageScreen.kt app/build.gradle.kts
git commit -m "feat: 코스 관리 화면 리디자인 적용 (vX.Y.Z)"
```

---

### Task 5: 코스 추가·수정 (`CourseEditScreen`)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/course/CourseEditScreen.kt` (Composable 2개 전체 교체 — `CourseEditScreen`, `AddYoutubeLinkDialog`는 그대로. ViewModel/Factory/`difficultyLabel`/상수 그대로)

**Interfaces:**
- Consumes: `GolfTokens.*`
- Produces: 변화 없음

**난이도는 1단위 유지(Global Constraints 참고) — 이 Task는 `Slider` 2개를 시안 스타일의 −/+ 버튼+막대 UI로 바꾸되, `viewModel.rating`/`viewModel.difficulty` 변경 폭은 각각 0.5f/1f로 둔다(지금 코드의 `step` 크기와 동일, 그냥 UI만 Slider→버튼으로 바뀜).** 홀별 파는 시안대로 전반/후반 2열 + 홀마다 파3/4/5 3버튼 세그먼트로 바꾼다(지금의 세로 1열 `FilterChip` 목록 대체) — 이 3버튼 세그먼트는 이 화면에만 쓰이는 전용 UI라 `ui/common`에 새 컴포넌트를 만들지 않고 이 파일 안에 private Composable로 둔다.

- [ ] **Step 1: CourseEditScreen.kt의 Composable 부분 교체**

`class CourseEditViewModel`, `class CourseEditViewModelFactory`, `difficultyLabel`, `DEFAULT_HOLE_COUNT`/`DEFAULT_PAR`/`PAR_OPTIONS`는 **그대로 둔다**. `@Composable fun CourseEditScreen(...)`를 아래로 교체(시그니처는 `viewModel`, `isNew`, `onBack` 그대로). `AddYoutubeLinkDialog`는 **그대로 둔다**(시안에 유튜브 검색 다이얼로그 디테일이 없어 이번 교체 대상에서 제외 — 호출부만 아래 코드에 그대로 남아있음).

```kotlin
// 추가 import. SectionHeader/kotlin.math.roundToInt는 원본에 이미 있으니 중복
// 추가하지 말 것. RoundedCornerShape/size/width는 원본에 없던 것 — 새 코드
// (HoleParSegment, 평점/난이도 −/+ 버튼, 카드 배경)가 전부 쓰므로 필요:
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.golfrecorder.ui.theme.GolfTokens
```

```kotlin
/** 홀 하나의 파3/4/5 선택 — 이 화면 전용(다른 화면에서 안 씀)이라 공용 컴포넌트로
 * 안 뽑고 여기 private으로 둔다. */
@Composable
private fun HoleParSegment(holeNumber: Int, selectedPar: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "${holeNumber}",
            fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            color = GolfTokens.TextSecondary,
            modifier = Modifier.width(20.dp),
        )
        Row(
            modifier = Modifier.weight(1f)
                .background(GolfTokens.Background, RoundedCornerShape(10.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PAR_OPTIONS.forEach { par ->
                val selected = selectedPar == par
                Text(
                    "$par",
                    fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) GolfTokens.CardBackground else GolfTokens.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .clickable { onSelect(par) }
                        .background(
                            if (selected) GolfTokens.FieldGreen else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    viewModel: CourseEditViewModel,
    isNew: Boolean,
    onBack: () -> Unit,
) {
    val youtubeLinks by viewModel.youtubeLinks.collectAsStateWithLifecycle()
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var pendingDeleteLink by remember { mutableStateOf<CourseYoutubeLinkEntity?>(null) }
    var pendingDeleteCourse by remember { mutableStateOf(false) }
    var courseBlockedFromDelete by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    if (pendingDeleteCourse) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCourse = false },
            title = { Text("코스를 삭제할까요?") },
            text = { Text("\"${viewModel.name}\"을(를) 삭제하면 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteCourse = false
                    viewModel.delete(onBack)
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCourse = false }) { Text("취소") }
            },
        )
    }

    courseBlockedFromDelete?.let { roundCount ->
        AlertDialog(
            onDismissRequest = { courseBlockedFromDelete = null },
            title = { Text("삭제할 수 없습니다") },
            text = {
                Text(
                    "\"${viewModel.name}\"으로 기록된 라운드가 ${roundCount}개 있어 삭제할 수 없습니다. " +
                        "홈 화면에서 그 라운드들을 먼저 삭제해주세요."
                )
            },
            confirmButton = {
                TextButton(onClick = { courseBlockedFromDelete = null }) { Text("확인") }
            },
        )
    }

    pendingDeleteLink?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLink = null },
            title = { Text("링크를 삭제할까요?") },
            text = { Text("\"${link.title ?: link.url}\"를 삭제하면 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteYoutubeLink(link.id); pendingDeleteLink = null }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteLink = null }) { Text("취소") } },
        )
    }

    if (showAddLinkDialog) {
        val searchResults by viewModel.youtubeSearchResults.collectAsStateWithLifecycle()
        val isSearching by viewModel.isSearchingYoutube.collectAsStateWithLifecycle()
        AddYoutubeLinkDialog(
            initialQuery = viewModel.name,
            searchResults = searchResults,
            isSearching = isSearching,
            onSearch = viewModel::searchYoutube,
            onDismiss = {
                showAddLinkDialog = false
                viewModel.clearYoutubeSearch()
            },
            onConfirm = { url, category, selectedResult ->
                viewModel.addYoutubeLink(url, category, selectedResult)
                showAddLinkDialog = false
                viewModel.clearYoutubeSearch()
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "코스 추가" else "코스 수정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onBack) },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = GolfTokens.CardBackground),
                        modifier = Modifier
                            .background(GolfTokens.FieldGreen, RoundedCornerShape(GolfTokens.ChipCorner))
                            .padding(horizontal = 4.dp),
                    ) { Text("저장") }
                    if (!isNew) {
                        TextButton(onClick = {
                            viewModel.checkDeletable { roundCount ->
                                if (roundCount > 0) {
                                    courseBlockedFromDelete = roundCount
                                } else {
                                    pendingDeleteCourse = true
                                }
                            }
                        }) {
                            Text("삭제", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).background(GolfTokens.Background)) {
            item {
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("코스 이름") },
                    isError = viewModel.nameError && viewModel.name.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (viewModel.nameError && viewModel.name.isBlank()) {
                    Text(
                        "코스 이름을 입력하세요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(GolfTokens.CardBackground, RoundedCornerShape(GolfTokens.CardCorner))
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("홀별 파", fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
                        Text(
                            "총 파 ${viewModel.pars.sum()}",
                            color = GolfTokens.CardBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .background(GolfTokens.FieldGreen, RoundedCornerShape(GolfTokens.ChipCorner))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "OUT 1~9 · 파 ${viewModel.pars.take(9).sum()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.FieldGreen,
                            )
                            Spacer(Modifier.height(4.dp))
                            (0 until 9).forEach { i ->
                                HoleParSegment(i + 1, viewModel.pars[i]) { par -> viewModel.setPar(i, par) }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "IN 10~18 · 파 ${viewModel.pars.drop(9).sum()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.FieldGreen,
                            )
                            Spacer(Modifier.height(4.dp))
                            (9 until 18).forEach { i ->
                                HoleParSegment(i + 1, viewModel.pars[i]) { par -> viewModel.setPar(i, par) }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(GolfTokens.CardBackground, RoundedCornerShape(GolfTokens.CardCorner))
                        .padding(14.dp),
                ) {
                    SectionHeader("리뷰 (선택 입력)")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (viewModel.rating <= 0f) "평점: 없음" else "평점: ${"%.1f".format(viewModel.rating)}",
                        color = GolfTokens.TextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { viewModel.rating = (viewModel.rating - 0.5f).coerceIn(0f, 5f) },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) { Text("−") }
                        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "%.1f".format(viewModel.rating),
                                fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.TextPrimary,
                            )
                        }
                        Button(
                            onClick = { viewModel.rating = (viewModel.rating + 0.5f).coerceIn(0f, 5f) },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GolfTokens.FieldGreen),
                        ) { Text("+") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (viewModel.difficulty <= 0f) {
                            "난이도: 없음"
                        } else {
                            val level = viewModel.difficulty.roundToInt()
                            "난이도: ${difficultyLabel(level)} ($level)"
                        },
                        color = GolfTokens.TextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 난이도는 아직 Int? 컬럼이라 1단위로만 바꾼다(0.5단위는 스테이지 4 마이그레이션 이후).
                        androidx.compose.material3.OutlinedButton(
                            onClick = { viewModel.difficulty = (viewModel.difficulty - 1f).coerceIn(0f, 5f) },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) { Text("−") }
                        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "${viewModel.difficulty.roundToInt()}",
                                fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = GolfTokens.TextPrimary,
                            )
                        }
                        Button(
                            onClick = { viewModel.difficulty = (viewModel.difficulty + 1f).coerceIn(0f, 5f) },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GolfTokens.ObBorder),
                        ) { Text("+") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.region,
                            onValueChange = { viewModel.region = it },
                            label = { Text("지역") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = viewModel.distance,
                            onValueChange = { viewModel.distance = it },
                            label = { Text("거리") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = viewModel.travelTime,
                            onValueChange = { viewModel.travelTime = it },
                            label = { Text("시간") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.oneLineReview,
                        onValueChange = { viewModel.oneLineReview = it },
                        label = { Text("한줄평") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.transportInfo,
                        onValueChange = { viewModel.transportInfo = it },
                        label = { Text("교통 (거리, 경로 등)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.clubhouseInfo,
                        onValueChange = { viewModel.clubhouseInfo = it },
                        label = { Text("클럽하우스 (외관/내관, 소품 등)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.courseInfo,
                        onValueChange = { viewModel.courseInfo = it },
                        label = { Text("코스 (티샷, 코스, 그린 등)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = GolfTokens.Divider)
                    Spacer(Modifier.height(16.dp))
                    if (viewModel.existingCourseId == null) {
                        SectionHeader("유튜브 링크")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "코스를 저장한 후에 유튜브 링크를 추가할 수 있어요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GolfTokens.TextSecondary,
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionHeader("유튜브 링크")
                            TextButton(onClick = { showAddLinkDialog = true }) { Text("링크 추가") }
                        }
                        if (youtubeLinks.isEmpty()) {
                            Text(
                                "아직 추가한 링크가 없어요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GolfTokens.TextSecondary,
                            )
                        }
                        YoutubeCategory.entries.forEach { category ->
                            val links = youtubeLinks.filter { it.category == category.name }
                            if (links.isNotEmpty()) {
                                Text(category.displayLabel, style = MaterialTheme.typography.labelMedium, color = GolfTokens.TextPrimary)
                                links.forEach { link ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                            }
                                            .padding(vertical = 6.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(end = 56.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            VideoThumbnail(link.thumbnailUrl)
                                            Column {
                                                Text(
                                                    link.title ?: link.url,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GolfTokens.TextPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                link.channelTitle?.let {
                                                    Text(
                                                        it,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = GolfTokens.TextSecondary,
                                                    )
                                                }
                                            }
                                        }
                                        TextButton(
                                            onClick = { pendingDeleteLink = link },
                                            modifier = Modifier.align(Alignment.CenterEnd),
                                        ) { Text("삭제") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**로직 보존 체크리스트**:
- 3개 다이얼로그(삭제 확인/삭제 불가 안내/링크 삭제 확인) 문구·분기 100% 동일
- `viewModel.setPar(i, par)` 호출(홀 인덱스, 파 값) 동일 — UI만 FilterChip 1열→3버튼 세그먼트 2열로 바뀜
- `rating`/`difficulty` 증감 폭이 기존 Slider의 `steps`(rating: 0.5단위, difficulty: 1단위)와 **정확히** 같은지 확인 — 다르면 저장되는 값의 반올림 결과가 미묘하게 달라질 수 있다
- 유튜브 링크 섹션(`existingCourseId == null` 분기, 카테고리별 목록, 삭제 버튼) 동일
- `AddYoutubeLinkDialog` 호출 인자 동일(그 다이얼로그 자체는 안 바뀜)

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/course/CourseEditScreen.kt app/build.gradle.kts
git commit -m "feat: 코스 추가·수정 화면 리디자인 적용 (vX.Y.Z)"
```

---

## 완료 기준

- [ ] Task 1~5 전부 컴파일 성공, `./gradlew.bat :app:assembleDebug` 전체 빌드 성공
- [ ] 실기기 설치 후 6개 화면(홈/홀 기록/홀 상세/라운드 결과/코스 관리/코스 추가·수정) 전부 시안 스타일로 보이고, 전과 똑같이 동작하는지 사용자가 직접 확인
- [ ] 스테이지 3(신규-UI) 계획은 이 스테이지가 실기기에서 확인된 뒤에 작성한다 — GIR/L-GIR 이름 교체, 6칸 통계, 18홀 진행바 등은 지금 만든 실제 화면 구조를 그대로 확장해야 하므로, 구조가 틀어질 수 있는 상태에서 미리 쓰지 않는다.

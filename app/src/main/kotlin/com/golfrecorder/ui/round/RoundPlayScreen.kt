package com.golfrecorder.ui.round

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.golfrecorder.data.local.entity.HoleEntity
import com.golfrecorder.data.local.entity.PenaltyEntity
import com.golfrecorder.data.local.entity.ShotEntity
import com.golfrecorder.data.repository.CourseRepository
import com.golfrecorder.data.repository.PenaltyRepository
import com.golfrecorder.data.repository.RoundRepository
import com.golfrecorder.data.repository.ShotRepository
import com.golfrecorder.domain.model.HoleResult
import com.golfrecorder.domain.model.MapProvider
import com.golfrecorder.domain.model.PenaltyType
import com.golfrecorder.domain.model.ShotPhase
import com.golfrecorder.domain.model.StrokeCalculator
import com.golfrecorder.domain.model.formatToPar
import com.golfrecorder.domain.model.scoreGradeLabel
import com.golfrecorder.location.LatLng as AppLatLng
import com.golfrecorder.location.LocationCapture
import com.golfrecorder.location.LocationTracker
import com.golfrecorder.service.RoundRecordingService
import com.golfrecorder.ui.common.HoleProgressBar
import com.golfrecorder.ui.common.HoleProgressMode
import com.golfrecorder.ui.common.PenaltyButton
import com.golfrecorder.ui.common.PenaltyColor
import com.golfrecorder.ui.common.PenaltyDisplayChip
import com.golfrecorder.ui.common.ReadOnlyStepperRow
import com.golfrecorder.ui.common.StepperRow
import com.golfrecorder.ui.map.CourseMapSlot
import com.golfrecorder.ui.map.GreenDistanceBox
import com.golfrecorder.ui.map.MapSlotState
import com.golfrecorder.ui.map.PenaltyPoint
import com.golfrecorder.ui.map.ShotPoint
import com.golfrecorder.ui.theme.GolfTokens
import com.golfrecorder.util.haversineMeters
import com.golfrecorder.util.isOnline
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 라이브 플레이 중 "그린까지" 거리를 갱신하려고 위치 추적기의 최신 위치를 읽는 주기(추적기 자체 갱신 주기와 같다). */
private const val LIVE_LOCATION_POLL_MILLIS = 3_000L

class RoundPlayViewModel(
    private val roundRepository: RoundRepository,
    private val courseRepository: CourseRepository,
    private val shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
    val roundId: Long,
    val courseId: Long,
    initialHoleNumber: Int,
    /** 라운드가 이미 "완료"된 뒤 결과 화면에서 홀을 리뷰하러 들어온 것인지 — true면
     * 지금 서 있는 곳이 그 홀과 무관하므로(라이브 플레이가 아님) 위치 조정/핀 재지정과
     * 타수·OB·해저드·숏게임 입력 UI를 모두 끄고 읽기 전용으로 보여준다. 아직 완료 전인
     * 라운드는 결과 화면에서 홀을 눌러도 이어서 플레이하는 것이므로 false로 들어온다
     * (호출자가 그 라운드의 finishedAt 여부를 보고 결정해서 넘긴다). */
    val isReview: Boolean,
) : ViewModel() {
    val holes: StateFlow<List<HoleEntity>> = courseRepository.getCourseWithHoles(courseId)
        .map { it?.holes.orEmpty().sortedBy { hole -> hole.holeNumber } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 결과(요약) 화면과 같은 패턴으로 이 라운드의 hole_records를 HoleResult로
     * 매핑한다 — 18홀 진행바의 "현재 스코어"(진행 홀까지, 지금 치는 홀 제외)를
     * 계산하는 데 쓴다. */
    val holeResults: StateFlow<List<HoleResult>> = roundRepository.getRoundWithHoleRecords(roundId)
        .map { round ->
            round?.holeRecords.orEmpty().sortedBy { it.holeNumber }.map { record ->
                HoleResult(
                    record.holeNumber, record.par, record.strokesToGreen, record.strokesGreenToHoleOut,
                    strokesPutt = record.strokesPutt,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentHoleNumber by mutableStateOf(initialHoleNumber)
        private set
    var strokesToGreen by mutableStateOf(0)
        private set
    var strokesShortGame by mutableStateOf(0)
        private set
    var strokesPutt by mutableStateOf(0)
        private set
    // DB 읽기(getMapProvider)가 끝나기 전까지는 아직 실제 프로바이더를 모른다는 뜻으로
    // null을 쓴다 — KAKAO로 기본값을 잡아두면 GOOGLE 라운드에서도 잠깐이지만 카카오
    // 지도가 먼저 그려졌다 구글로 갈아타는 "깜빡임"이 생긴다(아래 init, 그리고
    // RoundPlayScreen의 CourseMapSlot 호출부 참고).
    var mapProvider by mutableStateOf<MapProvider?>(null)
        private set

    private val shotsFlow = MutableStateFlow<List<ShotEntity>>(emptyList())
    val shots: StateFlow<List<ShotEntity>> = shotsFlow
    private var shotsCollectJob: Job? = null

    private val penaltiesFlow = MutableStateFlow<List<PenaltyEntity>>(emptyList())
    val penalties: StateFlow<List<PenaltyEntity>> = penaltiesFlow
    private var penaltiesCollectJob: Job? = null
    private var strokeTotalsJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = roundRepository.getMapProvider(roundId)
            mapProvider = runCatching { MapProvider.valueOf(stored ?: "KAKAO") }.getOrDefault(MapProvider.KAKAO)
        }
        loadHole(initialHoleNumber)
        if (!isReview) {
            viewModelScope.launch { roundRepository.updateCurrentHoleNumber(roundId, initialHoleNumber) }
            // 워치에서 홀을 넘기면(RoundRecordingService가 rounds.currentHoleNumber를
            // 직접 갱신) 이미 열려있는 폰 화면도 재진입 없이 따라가게 한다. 폰 자신의
            // goToHole()이 쓴 값은 loadHole()이 currentHoleNumber를 동기적으로 먼저
            // 갱신해두므로, 이 Flow가 뒤늦게 같은 값을 들고 도착해도 아래 비교에서
            // 걸러져 중복 loadHole 호출이 일어나지 않는다.
            viewModelScope.launch {
                roundRepository.getRoundWithHoleRecords(roundId).collect { rwr ->
                    val holeNumber = rwr?.round?.currentHoleNumber ?: return@collect
                    if (holeNumber != currentHoleNumber) {
                        loadHole(holeNumber)
                    }
                }
            }
        }
    }

    private fun loadHole(holeNumber: Int) {
        currentHoleNumber = holeNumber
        shotsCollectJob?.cancel()
        shotsCollectJob = viewModelScope.launch {
            shotRepository.getShots(roundId, holeNumber).collect { shotsFlow.value = it }
        }
        penaltiesCollectJob?.cancel()
        penaltiesCollectJob = viewModelScope.launch {
            penaltyRepository.getPenalties(roundId, holeNumber).collect { penaltiesFlow.value = it }
        }
        // strokesToGreen/strokesShortGame/strokesPutt을 shots/penalties Flow로부터
        // 계속 다시 계산한다(1회성 시드가 아님) — 워치가 이 홀의 shots/penalties를
        // 바꿔도 이미 열려있는 폰 화면이 재진입 없이 곧바로 반영되게 하기 위해서다.
        strokeTotalsJob?.cancel()
        strokeTotalsJob = viewModelScope.launch {
            combine(shotsFlow, penaltiesFlow) { shots, penalties -> shots to penalties }
                .collect { (shots, penalties) ->
                    strokesToGreen = StrokeCalculator.currentTotal(shots, penalties, ShotPhase.TO_GREEN)
                    strokesShortGame = StrokeCalculator.currentTotal(shots, penalties, ShotPhase.SHORT_GAME)
                    strokesPutt = StrokeCalculator.currentTotal(shots, penalties, ShotPhase.PUTT)
                    // 홀에 들어오는 즉시, 그리고 이후 값이 바뀔 때마다 hole_records에
                    // 반영해둔다 — 한 타도 안 치고 바로 뒤로 나가도 라운드 결과(요약)
                    // 화면의 홀 목록에 그 홀이 바로 보여야 "코스를 고른 순간 라운드가
                    // 시작됐다"는 의미와 맞는다.
                    // 라운드가 이미 삭제됐으면 절대 쓰지 않는다 — 삭제가 shots/penalties를
                    // 캐스케이드로 비우면 이 collect가 한 번 더 깨어나는데, 그때 없는 라운드의
                    // hole_records를 쓰려다 FK 제약 위반으로 크래시가 났었다.
                    if (!isReview && roundRepository.getCurrentHoleNumber(roundId) != null) {
                        val par = courseRepository.getCourseWithHoles(courseId).first()
                            ?.holes?.find { it.holeNumber == holeNumber }?.par ?: 4
                        roundRepository.saveHoleRecord(
                            roundId, holeNumber, par, strokesToGreen, strokesShortGame + strokesPutt, strokesPutt,
                        )
                    }
                }
        }
    }

    fun goToHole(holeNumber: Int, onDone: () -> Unit = {}) {
        saveCurrentHole {
            viewModelScope.launch {
                roundRepository.updateCurrentHoleNumber(roundId, holeNumber)
                onDone()
            }
            loadHole(holeNumber)
        }
    }

    fun finishRound(onFinished: () -> Unit) {
        saveCurrentHole {
            viewModelScope.launch {
                roundRepository.finishRound(roundId, System.currentTimeMillis())
                onFinished()
            }
        }
    }

    private fun saveCurrentHole(after: () -> Unit) {
        val par = holes.value.find { it.holeNumber == currentHoleNumber }?.par ?: 4
        viewModelScope.launch {
            roundRepository.saveHoleRecord(
                roundId, currentHoleNumber, par, strokesToGreen, strokesShortGame + strokesPutt, strokesPutt,
            )
            after()
        }
    }

    /**
     * 호출자(Composable)가 GPS fix를 잡아온 뒤 순서를 보장해 호출하는 suspend 함수.
     * [holeNumber]는 버튼을 누른 시점의 홀 번호를 호출자가 캡처해서 넘긴다 — GPS fix를
     * 기다리는 동안 사용자가 다음 홀로 넘어가면 그 사이 [currentHoleNumber]가 바뀌어서,
     * 여기서 그 값을 다시 읽으면 이 샷이 엉뚱한(새) 홀에 기록되어 지도에 이전 홀
     * 마지막 샷과 새 홀 첫 샷을 잇는 있어선 안 될 선이 그려지는 버그가 있었다.
     */
    suspend fun recordShot(holeNumber: Int, phase: ShotPhase, shotIndex: Int, lat: Double, lng: Double) {
        shotRepository.recordShot(roundId, holeNumber, phase, shotIndex, lat, lng)
    }

    suspend fun removeShot(holeNumber: Int, phase: ShotPhase, shotIndex: Int) {
        shotRepository.removeShot(roundId, holeNumber, phase, shotIndex)
    }

    /**
     * OB/해저드는 규칙상 벌타일 뿐 실제 위치를 갖는 '샷'이 아니라서 shots 테이블에는
     * 안 들어간다. 다만 해당 타수 구간(그린까지/숏게임)의 타수는 그대로 늘어야 한다.
     * OB는 제자리 재티(+1)와 특설티 이동(+2)이 둘 다 있어서 [strokeCount]로 몇 타가
     * 늘어나는지 받는다 — 다만 "OB 몇 번 났는지"는 항상 1건으로 센다(벌타 크기와
     * 무관하게 penalties 테이블에는 한 행만 추가).
     * GPS를 못 잡으면(lat/lng == null) onStepperChange의 일반 샷과 동일하게 이 벌타
     * 자체를 기록하지 않는다 — strokesToGreen/strokesShortGame/strokesPutt이 이제
     * DB의 shots/penalties에서 매번 다시 계산되는 값이라, DB에 쓰이지 않은 변화를
     * 메모리에만 남겨둘 방법이 없다.
     */
    fun addPenalty(holeNumber: Int, phase: ShotPhase, type: PenaltyType, strokeCount: Int, lat: Double?, lng: Double?, onDone: () -> Unit = {}) {
        if (lat != null && lng != null) {
            val newValue = when (phase) {
                ShotPhase.TO_GREEN -> strokesToGreen + strokeCount
                ShotPhase.SHORT_GAME -> strokesShortGame + strokeCount
                ShotPhase.PUTT -> strokesPutt + strokeCount
            }
            viewModelScope.launch {
                penaltyRepository.addPenalty(
                    roundId, holeNumber, phase, type, newValue, strokeCount, lat, lng
                )
                onDone()
            }
        } else {
            onDone()
        }
    }

    /** 가장 최근에 추가한 이 종류의 벌타 1건을 되돌린다 — 그 건이 실제로 더한 타수만큼 뺀다. */
    fun removeLastPenalty(holeNumber: Int, phase: ShotPhase, type: PenaltyType, onDone: () -> Unit = {}) {
        val last = penaltiesFlow.value
            .filter { it.phase == phase.name && it.type == type.name }
            .maxByOrNull { it.penaltyIndex }
            ?: run { onDone(); return }
        viewModelScope.launch {
            penaltyRepository.removePenalty(roundId, holeNumber, phase, type, last.penaltyIndex)
            onDone()
        }
    }
}

class RoundPlayViewModelFactory(
    private val roundRepository: RoundRepository,
    private val courseRepository: CourseRepository,
    private val shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
    private val roundId: Long,
    private val courseId: Long,
    private val initialHoleNumber: Int,
    private val isReview: Boolean,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoundPlayViewModel(
            roundRepository,
            courseRepository,
            shotRepository,
            penaltyRepository,
            roundId,
            courseId,
            initialHoleNumber,
            isReview,
        ) as T
}

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
            // 녹화 서비스는 특정 라운드에 묶이지 않고 앱 전체에 하나만 떠서, 예전에
            // "완료"를 안 누르고 나온 다른 라운드가 있으면 백그라운드에서 계속
            // GPS를 추적하고 워치와도 계속 동기화된다 — 그 상태에서 전혀 무관한
            // (이미 끝난) 라운드를 리뷰만 해도 워치에 알림이 뜨는 문제가 있었다.
            // 골프는 한 번에 한 라운드만 플레이하므로, 리뷰 화면을 여는 시점엔
            // 실제로 진행 중인 라운드가 없다고 보고 안전하게 서비스를 끈다.
            RoundRecordingService.stop(context)
        } else {
            RoundRecordingService.start(context, viewModel.roundId, viewModel.courseId)
        }
    }

    // 코스를 선택한 순간 이미 라운드가 시작된 것으로 취급한다 — 진행된 홀이
    // 하나도 없어도 뒤로가기로 라운드를 통째로 지우지 않고 라운드 결과(요약)
    // 화면으로 보낸다. 필요하면 그 화면에서 명시적으로 삭제할 수 있다.
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
    ) { /* 결과와 무관하게 앱은 계속 동작한다 — 거부돼도 라운드 진행 중(포그라운드) 추적은 그대로 되고, 폰이 잠들어 있을 때만 최신 위치가 조금 덜 정확할 수 있다. */ }

    // 백그라운드 위치 권한은 반드시 포그라운드(FINE) 권한이 이미 허용된 뒤에 별도로
    // 요청해야 한다 — 한 번에 같이 요청하면 API 30+에서 조용히 거부된다.
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
            // 라이브 플레이에서는 걸어가는 동안 "그린까지" 거리가 바뀌어야 하므로(홀이 바뀌거나 위치 조정을
            // 누를 때만 갱신되면 숫자가 그대로다) 추적기가 3초마다 갱신하는 최신 위치를 같은 주기로 읽는다.
            // 끝난 라운드 리뷰는 보는 사람의 위치가 의미 없어 갱신하지 않는다.
            if (!viewModel.isReview) {
                while (true) {
                    delay(LIVE_LOCATION_POLL_MILLIS)
                    LocationTracker.latestLocation(context)?.let { currentLocation = it }
                }
            }
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
        // GPS fix를 기다리는 동안 홀이 바뀔 수 있어, 지금 화면의 홀 번호를 미리 캡처해
        // 넘긴다 — viewModel.currentHoleNumber를 나중에 다시 읽으면 이미 다음 홀로
        // 바뀌어 있을 수 있다.
        val holeNumber = viewModel.currentHoleNumber
        scope.launch {
            shotMutex.withLock {
                if (newValue > oldValue) {
                    val loc = if (hasLocationPermission) LocationTracker.latestLocation(context) else null
                    if (loc != null) {
                        viewModel.recordShot(holeNumber, phase, newValue, loc.lat, loc.lng)
                    } else {
                        // 위치가 없으면 아무 피드백 없이 그냥 무시돼서 버튼이 안 눌린 것처럼
                        // 보이는 문제가 있었다 — 최소한 왜 안 올라갔는지는 알려준다.
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
        // 홀 전환 경합을 피하려고 버튼 누른 시점의 홀 번호를 미리 캡처한다(onStepperChange와 동일 이유).
        val holeNumber = viewModel.currentHoleNumber
        // OB/해저드 위치는 그 순간의 GPS가 아니라 그 벌타를 유발한 직전 샷의 위치를 그대로
        // 쓴다 — 공을 못 찾아 헤매다 애매한 곳에서 벌타를 선언하는 경우가 많아서, 그 순간의
        // 현재 위치보다 "그 샷을 친 지점"이 훨씬 안정적이고 재현 가능한 기준점이다.
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

    val holeResults by viewModel.holeResults.collectAsStateWithLifecycle()
    // 지나온 홀만(지금 치는 홀 제외) 더한 파 대비 — holeNumber로 직접 걸러낸다. hole_records에는 지금
    // 치는 홀도 이미 upsert돼 있어서(RoundPlayViewModel.loadHole 참고) "저장됐는지"로는 걸러낼 수 없다.
    val currentScoreToPar = holeResults
        .filter { it.holeNumber < viewModel.currentHoleNumber }
        .sumOf { it.scoreToPar }
    val holeTotal = viewModel.strokesToGreen + viewModel.strokesShortGame + viewModel.strokesPutt
    val fixedLocation = currentLocation
    // DB에서 이 라운드의 실제 mapProvider를 읽어오는 동안(null)에는 지도를 아예 그리지 않는다 — KAKAO로
    // 기본값을 먼저 그렸다가 GOOGLE로 바뀌면서 다시 그리면, 해외 코스처럼 카카오 타일이 아예 없는 곳에서
    // 매번 빈/깨진 카카오 지도가 먼저 번쩍이고서야 구글 지도가 뜨는 문제가 있었다.
    val provider = viewModel.mapProvider
    // OB/해저드는 그린까지 가는 구간에서만 일어난다고 보고 숏게임에는 두지 않는다.
    val obToGreenCount = penalties.count {
        it.phase == ShotPhase.TO_GREEN.name && it.type == PenaltyType.OB.name
    }
    val hazardToGreenCount = penalties.count {
        it.phase == ShotPhase.TO_GREEN.name && it.type == PenaltyType.HAZARD.name
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onShowSummary() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = GolfTokens.TextPrimary)
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "HOLE ${viewModel.currentHoleNumber}",
                            fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            lineHeight = 34.sp,
                            color = GolfTokens.TextPrimary,
                        )
                        Text(
                            "파 $par",
                            color = GolfTokens.CardBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(bottom = 3.dp)
                                .background(GolfTokens.FieldGreen, RoundedCornerShape(GolfTokens.ChipCorner))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (viewModel.isReview) {
                            Text("이 홀", fontSize = 11.sp, color = GolfTokens.TextSecondary)
                            Text(
                                if (holeTotal == 0) "-" else "$holeTotal · ${scoreGradeLabel(holeTotal - par)}",
                                fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 24.sp,
                                color = GolfTokens.TextPrimary,
                            )
                        } else {
                            Text("현재 스코어", fontSize = 11.sp, color = GolfTokens.TextSecondary)
                            Text(
                                formatToPar(currentScoreToPar),
                                fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 24.sp,
                                color = GolfTokens.TextPrimary,
                            )
                        }
                    }
                }
                if (holeCount > 0) {
                    HoleProgressBar(
                        totalHoles = holeCount,
                        currentHoleNumber = viewModel.currentHoleNumber,
                        mode = if (viewModel.isReview) HoleProgressMode.REVIEW else HoleProgressMode.LIVE,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(GolfTokens.Background)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 라이브는 "이전 홀", 끝난 라운드 리뷰는 시안처럼 이웃 홀 번호("3번 홀")를 보여준다.
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            viewModel.goToHole(viewModel.currentHoleNumber - 1) {
                                RoundRecordingService.refreshState(context)
                            }
                        },
                        enabled = viewModel.currentHoleNumber > 1,
                        modifier = Modifier.width(110.dp).height(56.dp),
                        shape = RoundedCornerShape(GolfTokens.ButtonCorner),
                        border = BorderStroke(1.5.dp, GolfTokens.Border),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = GolfTokens.TextPrimary,
                        ),
                    ) {
                        Text(
                            if (viewModel.isReview && viewModel.currentHoleNumber > 1) {
                                "${viewModel.currentHoleNumber - 1}번 홀"
                            } else {
                                "이전 홀"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (holeCount == 0 || viewModel.currentHoleNumber < holeCount) {
                        Button(
                            onClick = {
                                viewModel.goToHole(viewModel.currentHoleNumber + 1) {
                                    RoundRecordingService.refreshState(context)
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(GolfTokens.ButtonCorner),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = GolfTokens.TextPrimary,
                                contentColor = GolfTokens.CardBackground,
                            ),
                        ) {
                            if (viewModel.isReview) {
                                Text("${viewModel.currentHoleNumber + 1}번 홀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("다음 홀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                // 한 타도 안 친 홀은 0 - par로 "버디 이하"가 떠서 거짓 정보가 되므로 숨긴다.
                                if (holeTotal > 0) {
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "이 홀 ${holeTotal}타 · ${scoreGradeLabel(holeTotal - par)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GolfTokens.CardBackground.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    } else if (!viewModel.isReview) {
                        // 이미 완료된 라운드를 리뷰 중이면 다시 완료할 이유가 없으니 버튼을 안 보여준다.
                        Button(
                            onClick = { showFinishConfirm = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(GolfTokens.ButtonCorner),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = GolfTokens.TextPrimary,
                                contentColor = GolfTokens.CardBackground,
                            ),
                        ) { Text("완료", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
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
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 시안: 지도 높이 라이브 248dp / 홀 상세 300dp. "그린 중앙까지" 박스·"위치 조정" 버튼·리뷰 안내는
            // 지도 위 요소라 루트 레이어의 MapOverlay(ui/map/GreenDistanceOverlay.kt)가 그린다.
            val mapHeight = if (viewModel.isReview) 300.dp else 248.dp
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(GolfTokens.FieldGreenDark, RoundedCornerShape(22.dp)),
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
                        height = mapHeight,
                        onRecenter = { recenterOnCurrentLocation() },
                    )
                    // provider를 아직 못 읽어온 온라인 상태 — 아래 오프라인 분기와 섞이지
                    // 않도록 별도 분기로 빼서 로딩이 끝날 때까지 빈 자리로 둔다.
                    online -> {}
                    // 거리 박스. 온라인일 땐 실제 지도 뷰가 이 화면 위에 겹쳐 그려져 여기 둔 요소가 가려지므로
                    // 온라인용은 MapOverlay가 맡고, 여기선 지도가 아예 없는 오프라인일 때만 같은 박스를 그린다
                    // (둘 다 라이브 플레이에서만). "오프라인 상태입니다." 문구와 겹치지 않도록 위아래로 쌓는다.
                    else -> Column(modifier = Modifier.padding(12.dp)) {
                        if (!viewModel.isReview && greenLocation != null && fixedLocation != null) {
                            val distance = haversineMeters(
                                fixedLocation.lat, fixedLocation.lng, greenLocation.lat, greenLocation.lng,
                            )
                            GreenDistanceBox(distanceMeters = distance.toInt())
                        }
                        Text(
                            "오프라인 상태입니다.",
                            color = GolfTokens.CardBackground,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (viewModel.isReview) {
                // 완료된 라운드는 홀 정보를 더 이상 고칠 수 없으니 입력 UI 대신 기록된
                // 값만 읽기 전용으로 보여준다.
                // 실수한 항목은 행 배경을 연한 빨강으로 하이라이트한다: GIR 실패(그린까지), OB/해저드,
                // 숏어프로치, 3퍼팅 이상. GIR 기준은 결과 화면과 같은 HoleResult.isGreenInRegulation.
                val reviewHole = HoleResult(
                    viewModel.currentHoleNumber, par, viewModel.strokesToGreen,
                    viewModel.strokesShortGame + viewModel.strokesPutt, strokesPutt = viewModel.strokesPutt,
                )
                // 읽기 전용 행은 자체 안쪽 여백(12dp)이 있어 카드 안쪽 여백을 줄여 합이 16dp가 되게 한다.
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(GolfTokens.CardBackground, RoundedCornerShape(22.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    ReadOnlyStepperRow(
                        label = "그린까지",
                        hint = "티샷부터 그린 도착",
                        value = viewModel.strokesToGreen,
                        highlight = !reviewHole.isGreenInRegulation,
                    )
                    ReadOnlyStepperRow(
                        label = "숏어프로치",
                        hint = "어프로치 · 칩샷",
                        value = viewModel.strokesShortGame,
                        highlight = reviewHole.strokesShortGame > 0,
                    )
                    ReadOnlyStepperRow(
                        label = "퍼팅",
                        hint = "그린 위",
                        value = viewModel.strokesPutt,
                        showDivider = false,
                        highlight = reviewHole.strokesPutt >= 3,
                    )
                }
                // 벌타는 시안처럼 스테퍼 카드와 별도의 흰 카드(OB/해저드가 있으면 연한 빨강).
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(
                            if (penalties.isNotEmpty()) GolfTokens.MistakeHighlight else GolfTokens.CardBackground,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "벌타",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GolfTokens.TextPrimary,
                    )
                    PenaltyDisplayChip("OB ${obToGreenCount}", PenaltyColor.OB)
                    PenaltyDisplayChip("해저드 ${hazardToGreenCount}", PenaltyColor.HAZARD)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(GolfTokens.CardBackground, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    StepperRow(
                        label = "그린까지",
                        hint = "티샷부터 그린 도착",
                        value = viewModel.strokesToGreen,
                        onIncrement = { onStepperChange(ShotPhase.TO_GREEN, viewModel.strokesToGreen, viewModel.strokesToGreen + 1) },
                        onDecrement = { onStepperChange(ShotPhase.TO_GREEN, viewModel.strokesToGreen, viewModel.strokesToGreen - 1) },
                    )
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
                // 시안: 스테퍼 카드 아래 "벌타" 라벨과 함께 한 줄.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("벌타", fontSize = 13.sp, color = GolfTokens.TextSecondary, modifier = Modifier.width(40.dp))
                    PenaltyButton(
                        text = "OB +1",
                        color = PenaltyColor.OB,
                        onClick = { onAddPenalty(ShotPhase.TO_GREEN, PenaltyType.OB, 1) },
                        modifier = Modifier.weight(1f).height(44.dp),
                    )
                    PenaltyButton(
                        text = "OB +2",
                        color = PenaltyColor.OB,
                        onClick = { onAddPenalty(ShotPhase.TO_GREEN, PenaltyType.OB, 2) },
                        modifier = Modifier.weight(1f).height(44.dp),
                    )
                    PenaltyButton(
                        text = "해저드 +1",
                        color = PenaltyColor.HAZARD,
                        onClick = { onAddPenalty(ShotPhase.TO_GREEN, PenaltyType.HAZARD, 1) },
                        modifier = Modifier.weight(1f).height(44.dp),
                    )
                }
                // 시안엔 OB/해저드를 되돌리는 UI가 없지만(+버튼만 보임), 기존에 되돌리기
                // 기능이 있었으므로(onRemovePenalty) 없앨 수 없다 — 건수 칩을 탭하면
                // 되돌리게 해서 기능은 유지하면서 화면은 깔끔하게 둔다. 0건이면 탭해도
                // onRemovePenalty가 조용히 아무것도 안 한다(ViewModel이 이미 처리).
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 48.dp, top = 8.dp),
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
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

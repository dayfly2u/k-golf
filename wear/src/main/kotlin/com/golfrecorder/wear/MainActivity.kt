package com.golfrecorder.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ProvideTextStyle
import com.golfrecorder.wearsync.WearSync
import kotlinx.coroutines.delay

private const val FAIL_VIBRATION_MS = 200L
private const val GPS_FAILURE_MESSAGE_MS = 2500L
private const val BATTERY_ALERT_MS = 5000L

private fun vibrateFailure(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    vibrator.vibrate(VibrationEffect.createOneShot(FAIL_VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
}

class MainActivity : ComponentActivity() {
    private val viewModel: RoundStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 워치 배터리 감시 시작(프로세스에 한 번만 등록) — 10%/5% 미만 경고는 WatchBatteryMonitor가 맡는다.
        WatchBatteryMonitor.start(applicationContext)
        setContent {
            MaterialTheme {
                // 한 줄 글자의 높이가 lineHeight와 같도록 줄 높이 동작을 명시한다(WatchScale.lineSp 참고).
                ProvideTextStyle(
                    value = LocalTextStyle.current.merge(
                        TextStyle(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None,
                            ),
                        ),
                    ),
                ) {
                    RoundControlScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun RoundControlScreen(viewModel: RoundStateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPhoneConnected by viewModel.isPhoneConnected.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 폰 화면의 "위치를 가져오지 못해 기록되지 않았습니다" Toast와 같은 의도 — 워치는
    // 화면이 작아 텍스트 대신 진동 + 잠깐 뜨는 안내 문구로 알려준다. 처음 값을 받을
    // 때는(baseline) 반응하지 않고, 그 뒤에 값이 갱신될 때만(=새로운 실패) 반응한다.
    var lastSeenFailedAt by remember { mutableStateOf<Long?>(null) }
    var showGpsFailureMessage by remember { mutableStateOf(false) }
    LaunchedEffect(state.lastFailedAt) {
        val seen = lastSeenFailedAt
        if (seen != null && state.lastFailedAt > seen) {
            vibrateFailure(context)
            showGpsFailureMessage = true
            delay(GPS_FAILURE_MESSAGE_MS)
            showGpsFailureMessage = false
        }
        lastSeenFailedAt = state.lastFailedAt
    }

    // 이 라운드가 "워치 기준"일 때만 워치 자체 GPS가 필요하다 — 앱을 열 때마다
    // 무조건 권한을 묻지 않고, 실제로 필요한 순간(워치 기준 라운드 진행 중)에만
    // 묻는다.
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(state.roundActive, state.useWatchLocation, hasLocationPermission) {
        if (state.roundActive && state.useWatchLocation && !hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // 폰의 RoundPlayScreen이 RoundRecordingService를 시작/정지하는 것과 같은 모양 —
    // 라운드가 살아있고 워치 기준이며 권한이 있을 때만 포그라운드 GPS 서비스를 띄운다.
    LaunchedEffect(state.roundActive, state.useWatchLocation, hasLocationPermission) {
        if (state.roundActive && state.useWatchLocation && hasLocationPermission) {
            WatchLocationService.start(context)
        } else {
            WatchLocationService.stop(context)
        }
    }

    // 워치 배터리가 10% 미만/5% 미만으로 떨어지면 라운드 중에 경고 화면을 5초(탭하면 바로) 띄우고, 경고 구간인 동안은
    // 상태 줄 알약으로 계속 알린다. 진동은 WatchBatteryMonitor가 울리고, 폰에는 알림이 간다.
    val battery by WatchBatteryMonitor.info.collectAsStateWithLifecycle()
    val pendingBatteryAlert by WatchBatteryMonitor.pendingAlert.collectAsStateWithLifecycle()
    var shownBatteryAlert by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.roundActive) {
        WatchBatteryMonitor.roundActive = state.roundActive
        if (state.roundActive) WatchBatteryMonitor.resend(context)
    }
    LaunchedEffect(pendingBatteryAlert, state.roundActive) {
        if (state.roundActive && pendingBatteryAlert != WearSync.BATTERY_WARNING_NONE) {
            shownBatteryAlert = pendingBatteryAlert
            WatchBatteryMonitor.consumeAlert()
        }
    }
    // 닫는 타이머는 따로 둔다 — 위 효과는 대기 경고를 비우면 다시 시작되므로 거기서 delay를 걸면 취소돼 안 닫힌다.
    LaunchedEffect(shownBatteryAlert) {
        if (shownBatteryAlert != null) {
            delay(BATTERY_ALERT_MS)
            shownBatteryAlert = null
        }
    }

    // ◀/▶를 누르면 홀 이동 요청을 세우고 액션을 보낸다 — 폰이 처리해서 홀 번호가 실제로
    // 바뀌면 홀 이동 화면이 잠깐 뜬다(HoleMoveOverlayState 참고).
    val overlay = rememberHoleMoveOverlay(state.holeNumber)
    val onMove: (HoleMoveDirection) -> Unit = { direction ->
        overlay.request(direction)
        viewModel.sendAction(
            if (direction == HoleMoveDirection.NEXT) WearSync.ACTION_NEXT_HOLE else WearSync.ACTION_PREV_HOLE,
        )
    }

    val shownDirection = overlay.shownDirection
    val batteryAlert = shownBatteryAlert
    when {
        !state.roundActive -> WatchIdleScreen(isPhoneConnected = isPhoneConnected)
        batteryAlert != null -> WatchBatteryAlertScreen(
            percent = battery?.percent ?: 0,
            warning = batteryAlert,
            onDismiss = { shownBatteryAlert = null },
        )
        shownDirection != null -> WatchHoleScreen(
            state = state,
            direction = shownDirection,
            onMove = onMove,
            onDismiss = overlay::dismiss,
        )
        else -> WatchPlayScreen(
            state = state,
            badge = statusBadge(isPhoneConnected, showGpsFailureMessage),
            battery = battery,
            onAction = viewModel::sendAction,
            onMove = onMove,
        )
    }
}

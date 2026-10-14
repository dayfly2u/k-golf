package com.golfrecorder.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.golfrecorder.wearsync.WearSync
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** 워치 배터리의 현재 퍼센트와 충전 여부, 거기서 나오는 경고 단계([WearSync].BATTERY_WARNING_*). */
data class BatteryInfo(val percent: Int, val charging: Boolean) {
    val warning: Int get() = batteryWarning(percent, charging)
}

/** 경고 단계 — 10% 미만이면 LOW, 5% 미만이면 CRITICAL. 충전 중이면 퍼센트와 상관없이 경고하지 않는다. */
fun batteryWarning(percent: Int, charging: Boolean): Int = when {
    charging -> WearSync.BATTERY_WARNING_NONE
    percent < WearSync.BATTERY_CRITICAL_PERCENT -> WearSync.BATTERY_WARNING_CRITICAL
    percent < WearSync.BATTERY_LOW_PERCENT -> WearSync.BATTERY_WARNING_LOW
    else -> WearSync.BATTERY_WARNING_NONE
}

/** 경고 단계가 올라갔는지 — 새로 임계값 아래로 떨어졌거나 더 낮은 단계로 내려갔을 때. 처음 읽은 값([old]가 null)은 경고
 * 없음에서 올라온 것으로 본다. */
fun batteryWarningRose(old: BatteryInfo?, new: BatteryInfo): Boolean =
    new.warning > (old?.warning ?: WearSync.BATTERY_WARNING_NONE)

/** 폰에 배터리 상태를 보낼지 — 경고 단계가 바뀔 때(시작/해제 포함), 경고 중 퍼센트가 바뀔 때. 첫 값이 경고 없음이면
 * 폰이 알 게 없으니 보내지 않는다. */
fun shouldReportBattery(old: BatteryInfo?, new: BatteryInfo): Boolean = when {
    old == null -> new.warning != WearSync.BATTERY_WARNING_NONE
    old.warning != new.warning -> true
    else -> new.warning != WearSync.BATTERY_WARNING_NONE && old.percent != new.percent
}

/** 상태 줄 알약 문구. 연결 끊김은 알약의 Wi-Fi 끊김 아이콘이 따로 알려준다. */
fun batteryPillText(percent: Int): String = "배터리 ${percent}%"

/** 경고 단계가 올라갈 때 진동 패턴(ms: 대기, 진동, 대기, 진동…). 경고가 아니면 null. LOW는 한 번 길게, CRITICAL은 세 번. */
fun batteryVibration(warning: Int): LongArray? = when (warning) {
    WearSync.BATTERY_WARNING_LOW -> longArrayOf(0, 500)
    WearSync.BATTERY_WARNING_CRITICAL -> longArrayOf(0, 400, 150, 400, 150, 400)
    else -> null
}

/**
 * 워치 배터리를 계속 지켜보다가 경고 단계가 바뀌면 (1) 화면에 띄울 경고를 [pendingAlert]에 올려두고, (2) 라운드 중이면
 * 진동하고, (3) 폰에 상태를 보낸다([WearSync.BATTERY_PATH]). 프로세스 하나에 하나만 있고([start]는 몇 번 불러도
 * 한 번만 등록), 앱 화면이 없어도 프로세스가 살아 있는 동안은(예: 워치 GPS 라운드의 포그라운드 서비스) 계속 동작한다.
 * 프로세스가 죽으면 같이 멈춘다 — 배터리 브로드캐스트는 매니페스트로 받을 수 없어서다.
 */
object WatchBatteryMonitor {
    private const val TAG = "WatchBatteryMonitor"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _info = MutableStateFlow<BatteryInfo?>(null)
    val info: StateFlow<BatteryInfo?> = _info

    // 아직 화면에 띄우지 않은 경고 단계(없으면 BATTERY_WARNING_NONE). 화면이 보여준 뒤 consumeAlert()로 비운다 —
    // 단계가 올라갈 때 앱이 안 열려 있었어도 다음에 열릴 때(라운드 중이면) 한 번 보여주기 위해 저장해 둔다.
    private val _pendingAlert = MutableStateFlow(WearSync.BATTERY_WARNING_NONE)
    val pendingAlert: StateFlow<Int> = _pendingAlert

    /** 라운드 진행 중인지 — 화면(RoundControlScreen)이 맞춰 둔다. 진동은 라운드 중일 때만 울린다. */
    @Volatile
    var roundActive: Boolean = false

    private var started = false

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                update(app, intent)
            }
        }
        // ACTION_BATTERY_CHANGED는 sticky라 등록하면 지금 값이 바로 돌아온다.
        val sticky = ContextCompat.registerReceiver(
            app,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { update(app, it) }
    }

    /** 화면이 경고를 보여줬으니 대기 중인 경고를 비운다. */
    fun consumeAlert() {
        _pendingAlert.value = WearSync.BATTERY_WARNING_NONE
    }

    /** 라운드가 시작될 때 지금 경고 중이면 폰이 알 수 있게 다시 보낸다 — 폰은 라운드 서비스가 떠 있을 때만 받는다. */
    fun resend(context: Context) {
        val current = _info.value ?: return
        if (current.warning != WearSync.BATTERY_WARNING_NONE) report(context.applicationContext, current)
    }

    private fun update(app: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val max = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || max <= 0) return
        val charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        apply(app, BatteryInfo(percent = level * 100 / max, charging = charging))
    }

    @Synchronized
    private fun apply(app: Context, new: BatteryInfo) {
        val old = _info.value
        if (old == new) return
        _info.value = new
        if (new.warning == WearSync.BATTERY_WARNING_NONE) _pendingAlert.value = WearSync.BATTERY_WARNING_NONE
        if (batteryWarningRose(old, new)) {
            _pendingAlert.value = new.warning
            if (roundActive) batteryVibration(new.warning)?.let { vibrate(app, it) }
        }
        if (shouldReportBattery(old, new)) report(app, new)
    }

    private fun report(app: Context, info: BatteryInfo) {
        scope.launch {
            try {
                val request = PutDataMapRequest.create(WearSync.BATTERY_PATH).apply {
                    dataMap.putInt(WearSync.KEY_BATTERY_LEVEL, info.percent)
                    dataMap.putBoolean(WearSync.KEY_BATTERY_CHARGING, info.charging)
                    dataMap.putInt(WearSync.KEY_BATTERY_WARNING, info.warning)
                    dataMap.putLong(WearSync.KEY_UPDATED_AT, System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(app).putDataItem(request).await()
                Log.d(TAG, "reported battery=${info.percent}% charging=${info.charging} warning=${info.warning}")
            } catch (e: Exception) {
                Log.e(TAG, "report battery FAILED", e)
            }
        }
    }

    private fun vibrate(app: Context, pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}

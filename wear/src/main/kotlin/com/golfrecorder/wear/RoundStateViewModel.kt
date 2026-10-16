package com.golfrecorder.wear

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.golfrecorder.wearsync.WearSync
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** 바로 이전 홀의 결과 — 워치 "홀 이동" 화면의 "{n}번 홀 {타수}타 · {등급}"용. */
data class PreviousHole(val strokes: Int, val par: Int)

data class RoundUiState(
    val roundActive: Boolean = false,
    val holeNumber: Int = 1,
    val par: Int = 4,
    val strokesToGreen: Int = 0,
    val strokesShortGame: Int = 0,
    val strokesPutt: Int = 0,
    /** 폰이 숏게임 타수를 따로 보내는지 — 구버전 폰이면 false(숏게임 줄은 0·비활성, [strokesPutt]는 숏게임 합산값). */
    val shortGameSupported: Boolean = true,
    val holeCount: Int = 18,
    /** 최근에 GPS를 못 잡아 버튼 입력이 기록되지 않은 시각(epoch ms), 없으면 0. */
    val lastFailedAt: Long = 0L,
    /** 이 라운드가 샷 위치를 워치 GPS 기준으로 기록하는지 — true면 워치가 자체
     * GPS를 계속 추적하고 타수 증가 액션에 좌표를 실어 보낸다. */
    val useWatchLocation: Boolean = false,
    /** 현재 홀 이전까지의 파 대비 합 — 폰이 보내지 않으면(구버전) 0. */
    val scoreBefore: Int = 0,
    /** 바로 이전 홀의 결과 — 1번 홀이거나 그 홀 타수가 0이면 null(폰이 키를 안 보냄). */
    val previousHole: PreviousHole? = null,
    /** 코스의 홀별 파(1번 홀부터) — 홀 이동을 폰 확인 전에 먼저 보여줄 때 그 홀의 파로 쓴다. 구버전 폰이면 비어 있다. */
    val holePars: List<Int> = emptyList(),
)

class RoundStateViewModel(application: Application) :
    AndroidViewModel(application),
    DataClient.OnDataChangedListener {

    // 폰이 보내주는 원본(확정) 상태 — round-state DataItem을 받을 때만 갱신된다.
    private var serverState = RoundUiState()
    // 아직 폰의 처리 완료 확인을 못 받은 액션들(타수 ±, 홀 이동). 블루투스가 끊긴 동안 버튼을 눌러도
    // 화면 숫자와 홀이 바로 움직이도록, 이 값을 serverState 위에 얹어서 보여준다(displayRound).
    // 폰이 처리하면 round-state에 실린 lastProcessedSeq로(상태와 함께 도착해 이중으로 안 보인다),
    // 또는 큐 아이템 삭제 이벤트로 빠진다. 키는 sendAction에서 만든 큐 경로의 타임스탬프.
    private val pending = PendingActions()
    private val _state = MutableStateFlow(RoundUiState())
    val state: StateFlow<RoundUiState> = _state

    // 폰과 블루투스로 연결돼 있는지 — 몇 초 간격으로 연결된 노드 목록을 확인해서
    // 반영한다. 초기값을 true로 두는 이유는 "연결 끊김" 배너가 앱을 막 열자마자
    // 잠깐 잘못 뜨는 것보다, 첫 확인 전까지는 조용히 있는 게 낫기 때문이다.
    private val _isPhoneConnected = MutableStateFlow(true)
    val isPhoneConnected: StateFlow<Boolean> = _isPhoneConnected

    private val dataClient = Wearable.getDataClient(application)
    private val nodeClient = Wearable.getNodeClient(application)

    init {
        dataClient.addListener(this)
        viewModelScope.launch {
            var items: DataItemBuffer? = null
            try {
                items = dataClient.dataItems.await()
                Log.d(TAG, "initial dataItems count=${items.count}")
                for (item in items) {
                    Log.d(TAG, "initial dataItem uri=${item.uri}")
                    if (item.uri.path == WearSync.STATE_PATH) {
                        applyDataMap(DataMapItem.fromDataItem(item).dataMap)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "initial dataItems FAILED", e)
            } finally {
                items?.release()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                val connected = try {
                    nodeClient.connectedNodes.await().isNotEmpty()
                } catch (e: Exception) {
                    Log.e(TAG, "connectedNodes check FAILED", e)
                    false
                }
                if (_isPhoneConnected.value != connected) {
                    Log.d(TAG, "phone connection changed: $connected")
                }
                _isPhoneConnected.value = connected
                delay(CONNECTION_POLL_INTERVAL_MS)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged count=${dataEvents.count}")
        var pendingChanged = false
        for (event in dataEvents) {
            Log.d(TAG, "onDataChanged event type=${event.type} uri=${event.dataItem.uri}")
            val path = event.dataItem.uri.path.orEmpty()
            if (path == WearSync.STATE_PATH) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    applyDataMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
                }
            } else if (path.startsWith(WearSync.ACTION_QUEUE_PATH_PREFIX)) {
                // 폰이 처리를 마치고 이 큐 아이템을 지우면(TYPE_DELETED) 그 액션이 확정된
                // 것 — round-state에 이미 반영됐을 것이므로 낙관적 표시에서 뺀다. (상태에 lastProcessedSeq가
                // 실려 오면 그쪽이 먼저 정리해서 보통 여기선 뺄 게 없다.)
                if (event.type == DataEvent.TYPE_DELETED) {
                    val seq = path.substringAfterLast('/').toLongOrNull()
                    if (seq != null && pending.remove(seq)) {
                        pendingChanged = true
                    }
                }
            }
        }
        dataEvents.release()
        if (pendingChanged) {
            _state.value = displayState()
        }
    }

    private fun applyDataMap(map: DataMap) {
        val split = strokeSplit(
            shortGame = if (map.containsKey(WearSync.KEY_STROKES_SHORT_GAME)) {
                map.getInt(WearSync.KEY_STROKES_SHORT_GAME)
            } else {
                null
            },
            putt = map.getInt(WearSync.KEY_STROKES_PUTT, 0),
        )
        val newServerState = RoundUiState(
            roundActive = map.getBoolean(WearSync.KEY_ROUND_ACTIVE, false),
            holeNumber = map.getInt(WearSync.KEY_HOLE_NUMBER, 1),
            par = map.getInt(WearSync.KEY_PAR, 4),
            strokesToGreen = map.getInt(WearSync.KEY_STROKES_TO_GREEN, 0),
            strokesShortGame = split.shortGame,
            strokesPutt = split.putt,
            shortGameSupported = split.shortGameSupported,
            holeCount = map.getInt(WearSync.KEY_HOLE_COUNT, 18),
            lastFailedAt = map.getLong(WearSync.KEY_LAST_FAILED_AT, 0L),
            useWatchLocation = map.getBoolean(WearSync.KEY_USE_WATCH_LOCATION, false),
            scoreBefore = map.getInt(WearSync.KEY_SCORE_BEFORE, 0),
            previousHole = if (
                map.containsKey(WearSync.KEY_PREV_HOLE_STROKES) && map.containsKey(WearSync.KEY_PREV_HOLE_PAR)
            ) {
                PreviousHole(
                    strokes = map.getInt(WearSync.KEY_PREV_HOLE_STROKES),
                    par = map.getInt(WearSync.KEY_PREV_HOLE_PAR),
                )
            } else {
                null
            },
            holePars = map.getIntegerArrayList(WearSync.KEY_HOLE_PARS)?.toList().orEmpty(),
        )
        // 폰이 이 상태에 이미 반영했다고 알려준 입력은 낙관적 표시에서 뺀다(없으면 0 → 뺄 것 없음).
        pending.confirmThrough(map.getLong(WearSync.KEY_LAST_PROCESSED_SEQ, 0L))
        // 라운드가 끝났거나 없어졌으면 쌓아둔 입력은 더 이상 의미가 없다.
        if (!newServerState.roundActive) {
            pending.clear()
        }
        serverState = newServerState
        _state.value = displayState()
    }

    private fun displayState(): RoundUiState = displayRound(serverState, pending.toList())

    /** 좌표를 함께 보내야 하는 액션인지 — 타수를 실제로 "추가"하는 세 액션만 위치가
     * 필요하다(감소/홀 이동은 기록할 좌표가 없다). */
    private fun needsLocation(action: String): Boolean =
        action == WearSync.ACTION_INCREMENT_TO_GREEN ||
            action == WearSync.ACTION_INCREMENT_SHORT_GAME ||
            action == WearSync.ACTION_INCREMENT_PUTT

    fun sendAction(action: String) {
        // DataItem 큐에 넣는다 — MessageClient(옛 방식)는 그 순간 폰과 블루투스가
        // 연결돼 있어야만 전달되고 끊겨 있으면 조용히 실패했지만, DataItem은 로컬에
        // 저장됐다가 재연결되면 자동으로 동기화된다. 경로에 타임스탬프를 붙여 액션마다
        // 고유하게 만드는 이유는, 같은 경로로 여러 번 보내면 나중 값이 이전 값을
        // 덮어써서 그 사이에 눌렀던 액션들이 통째로 사라지기 때문이다(WearSync 참고).
        val loc = if (needsLocation(action) && _state.value.useWatchLocation) {
            WatchLocationService.latestLocation()
        } else {
            null
        }
        val seq = System.currentTimeMillis()
        // 누른 순간 화면에 보이던 홀(아직 폰이 확인 안 한, 먼저 넘어간 홀 포함) — 폰이 이 입력을 늦게
        // (예: 워치가 꺼졌다 재연결된 뒤) 받았을 때 지금 홀과 다르면 낡은 입력으로 버릴 수 있게 같이
        // 보낸다(WearSync.KEY_QUEUED_HOLE).
        val pressedHole = _state.value.holeNumber
        // 폰의 처리 확인을 기다리지 않고 버튼을 누른 즉시 화면에 반영한다(타수·홀 이동 모두) — 블루투스가
        // 끊겨 있어도 누른 게 바로 보여야 하기 때문.
        pending.add(seq, PendingAction(action, pressedHole))
        _state.value = displayState()
        viewModelScope.launch {
            try {
                val request = PutDataMapRequest.create(
                    "${WearSync.ACTION_QUEUE_PATH_PREFIX}/$seq",
                ).apply {
                    dataMap.putString(WearSync.KEY_QUEUED_ACTION, action)
                    dataMap.putInt(WearSync.KEY_QUEUED_HOLE, pressedHole)
                    if (loc != null) {
                        dataMap.putDouble(WearSync.KEY_QUEUED_LAT, loc.first)
                        dataMap.putDouble(WearSync.KEY_QUEUED_LNG, loc.second)
                    }
                }.asPutDataRequest().setUrgent()
                val result = dataClient.putDataItem(request).await()
                Log.d(TAG, "sendAction($action) queued uri=${result.uri}")
            } catch (e: Exception) {
                Log.e(TAG, "sendAction($action) FAILED", e)
                // 큐에 넣는 것조차 실패했으면(로컬 저장 실패 등) 낙관적 표시도 되돌린다.
                if (pending.remove(seq)) {
                    _state.value = displayState()
                }
            }
        }
    }

    override fun onCleared() {
        dataClient.removeListener(this)
        super.onCleared()
    }

    companion object {
        private const val TAG = "RoundStateViewModel"
        private const val CONNECTION_POLL_INTERVAL_MS = 3000L
    }
}

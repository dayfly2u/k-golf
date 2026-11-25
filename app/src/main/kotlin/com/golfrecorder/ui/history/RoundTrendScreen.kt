package com.golfrecorder.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.golfrecorder.data.repository.RoundRepository
import com.golfrecorder.data.repository.ShotRepository
import com.golfrecorder.domain.model.RoundTrendPoint
import com.golfrecorder.domain.model.TREND_ROUND_COUNT
import com.golfrecorder.domain.model.buildRoundTrendPoint
import com.golfrecorder.domain.model.recentFullRounds
import com.golfrecorder.ui.common.GolfCard
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 화면을 열 때 한 번만 읽는다 — 이 화면에 있는 동안 기록이 바뀔 일이 없다. */
class RoundTrendViewModel(
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository,
) : ViewModel() {
    private val _points = MutableStateFlow<List<RoundTrendPoint>?>(null)

    /** null이면 아직 읽는 중. */
    val points: StateFlow<List<RoundTrendPoint>?> = _points.asStateFlow()

    init {
        viewModelScope.launch {
            val rounds = recentFullRounds(roundRepository.getRoundSummaries().first())
            _points.value = rounds.map { round ->
                buildRoundTrendPoint(
                    round,
                    roundRepository.getRoundWithHoleRecords(round.roundId).first()?.holeRecords.orEmpty(),
                    shotRepository.getShotsForRound(round.roundId).first(),
                )
            }
        }
    }
}

class RoundTrendViewModelFactory(
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoundTrendViewModel(roundRepository, shotRepository) as T
}

/** 그래프 하나 — [value]가 null인 라운드는 그 자리를 비운다. 점 위 숫자는 [format], 제목 옆 평균은 [averageFormat]. */
private data class TrendMetric(
    val title: String,
    val value: (RoundTrendPoint) -> Double?,
    val format: (Double) -> String,
    val averageFormat: (Double) -> String = format,
)

private val trendMetrics = listOf(
    TrendMetric(
        "타수",
        value = { it.totalStrokes.toDouble() },
        format = { "${it.roundToInt()}" },
        averageFormat = { "%.1f".format(it) },
    ),
    TrendMetric("드라이버 거리", value = { it.driverAvgMeters }, format = { "${it.roundToInt()}m" }),
    TrendMetric("홀당 퍼팅", value = { it.puttsPerHole }, format = { "%.1f".format(it) }),
    TrendMetric("GIR", value = { it.girPercent }, format = { "${it.roundToInt()}%" }),
)

private val dateFormat = SimpleDateFormat("M/d", Locale.KOREA)

/** 홈 헤더 "최근 5경기 추이"에서 들어오는 화면 — 끝난 18홀 라운드 최근 5개의 타수·드라이버 거리·홀당 퍼팅·GIR을
 * 꺾은선 그래프 4개로 보여 준다(왼쪽이 오래된 경기, 노란 점이 가장 최근). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundTrendScreen(viewModel: RoundTrendViewModel, onBack: () -> Unit) {
    val points by viewModel.points.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("최근 ${TREND_ROUND_COUNT}경기 추이", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
        containerColor = GolfTokens.Background,
    ) { padding ->
        val list = points ?: return@Scaffold
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (list.isEmpty()) {
                Text("끝난 18홀 라운드가 아직 없어요.", color = GolfTokens.TextPrimary)
                return@Column
            }
            TrendRoundList(list)
            trendMetrics.forEach { metric -> TrendCard(metric, list) }
        }
    }
}

/** 맨 위 — 그래프에 들어간 경기 목록(날짜 · 코스). */
@Composable
private fun TrendRoundList(points: List<RoundTrendPoint>) {
    GolfCard {
        Text(
            "끝난 18홀 라운드 ${points.size}경기 · 왼쪽이 오래된 경기, 노란 점이 가장 최근",
            fontSize = 12.sp,
            color = GolfTokens.TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            points.forEach { point ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dateFormat.format(Date(point.playedAt)),
                        fontFamily = GolfFonts.NumberFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GolfTokens.FieldGreen,
                        modifier = Modifier.width(48.dp),
                    )
                    Text(
                        point.courseName,
                        fontSize = 14.sp,
                        color = GolfTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendCard(metric: TrendMetric, points: List<RoundTrendPoint>) {
    val values = points.map(metric.value)
    val present = values.filterNotNull()
    GolfCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(metric.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
            if (present.isNotEmpty()) {
                Text("평균 ${metric.averageFormat(present.average())}", fontSize = 12.sp, color = GolfTokens.TextSecondary)
            }
        }
        if (present.isEmpty()) {
            Text(
                "기록이 없어요",
                fontSize = 13.sp,
                color = GolfTokens.TextSecondary,
                modifier = Modifier.padding(vertical = 20.dp),
            )
            return@GolfCard
        }
        Spacer(Modifier.height(4.dp))
        TrendChart(values, metric.format)
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    dateFormat.format(Date(point.playedAt)),
                    fontSize = 11.sp,
                    color = GolfTokens.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 점 [values].size개를 같은 간격(칸 가운데)에 놓는 꺾은선 — 아래 날짜 줄의 칸과 x 위치가 맞는다.
 * 세로 범위는 있는 값의 최소~최대(같으면 위아래로 1씩 띄움). null인 칸은 점 대신 "-"를 두고 선을 끊는다. */
@Composable
private fun TrendChart(values: List<Double?>, format: (Double) -> String) {
    val textMeasurer = rememberTextMeasurer()
    val valueStyle = TextStyle(
        fontFamily = GolfFonts.NumberFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = GolfTokens.TextPrimary,
    )
    val missingStyle = TextStyle(fontSize = 13.sp, color = GolfTokens.TextSecondary)
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val present = values.filterNotNull()
        var low = present.min()
        var high = present.max()
        if (high - low < 1e-9) {
            low -= 1
            high += 1
        }
        val plotTop = 30.dp.toPx() // 가장 높은 점 위 숫자가 들어갈 자리
        val plotBottom = size.height - 10.dp.toPx()
        val slot = size.width / values.size
        fun x(i: Int) = slot * (i + 0.5f)
        fun y(v: Double) = plotBottom - ((v - low) / (high - low)).toFloat() * (plotBottom - plotTop)

        listOf(plotTop, (plotTop + plotBottom) / 2, plotBottom).forEach { gridY ->
            drawLine(GolfTokens.Divider, Offset(0f, gridY), Offset(size.width, gridY), strokeWidth = 1.dp.toPx())
        }
        for (i in 0 until values.lastIndex) {
            val a = values[i] ?: continue
            val b = values[i + 1] ?: continue
            drawLine(
                GolfTokens.FieldGreen,
                Offset(x(i), y(a)),
                Offset(x(i + 1), y(b)),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        values.forEachIndexed { i, v ->
            if (v == null) {
                val dash = textMeasurer.measure("-", missingStyle)
                drawText(
                    dash,
                    topLeft = Offset(x(i) - dash.size.width / 2f, (plotTop + plotBottom) / 2 - dash.size.height / 2f),
                )
                return@forEachIndexed
            }
            val center = Offset(x(i), y(v))
            val radius = 5.dp.toPx()
            drawCircle(if (i == values.lastIndex) GolfTokens.Accent else GolfTokens.FieldGreen, radius, center)
            drawCircle(GolfTokens.FieldGreen, radius, center, style = Stroke(2.dp.toPx()))
            val label = textMeasurer.measure(format(v), valueStyle)
            drawText(
                label,
                topLeft = Offset(center.x - label.size.width / 2f, center.y - 7.dp.toPx() - label.size.height),
            )
        }
    }
}

package com.golfrecorder.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfIcons
import com.golfrecorder.ui.theme.GolfTokens
import com.golfrecorder.util.haversineMeters

private val OVERLAY_INSET = 12.dp

/** 시안의 어두운 반투명 박스 "그린 중앙까지 / 142m". 지도 위(온라인, [MapOverlay])와 지도가 없는
 * 오프라인 자리(RoundPlayScreen) 양쪽에서 같은 모양으로 쓴다. */
@Composable
fun GreenDistanceBox(distanceMeters: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(GolfTokens.TextPrimary.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text("그린 중앙까지", fontSize = 11.sp, color = GolfTokens.CardBackground.copy(alpha = 0.85f))
        Text(
            "${distanceMeters}m",
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 26.sp,
            color = GolfTokens.CardBackground,
        )
    }
}

/** 지도 우하단 흰 pill "위치 조정"(조준 아이콘) — 눌러서 지도를 지금 위치로 다시 맞춘다. */
@Composable
private fun RecenterPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(GolfTokens.CardBackground, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(GolfIcons.Crosshair, contentDescription = null, modifier = Modifier.size(16.dp), tint = GolfTokens.TextPrimary)
        Text("위치 조정", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GolfTokens.TextPrimary)
    }
}

/** 홀 상세(리뷰)의 지도 좌상단 안내 "직선 거리 · 고저차 미반영". */
@Composable
private fun MapNote(modifier: Modifier = Modifier) {
    Text(
        "직선 거리 · 고저차 미반영",
        fontSize = 11.sp,
        color = GolfTokens.CardBackground,
        modifier = modifier
            .background(GolfTokens.TextPrimary.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/**
 * 실제 지도 뷰는 액티비티 루트에 하나만 떠서 모든 화면 내용 위에 겹쳐 그려지므로, 화면 안(RoundPlayScreen)에
 * 둔 Compose 요소는 온라인일 때 지도 뒤에 가려진다 — 그래서 지도 위 요소(그린 거리 박스, 위치 조정 버튼,
 * 리뷰 안내)는 지도와 같은 루트 레이어에서 지도보다 나중에 그린다. 지도 자리([MapRequest.offset]/size)와 같은
 * 크기의 투명 Box를 깔고 그 안에서 모서리에 붙인다(Box 자체는 터치를 가로채지 않아 지도 조작은 그대로다).
 *
 * 라이브 플레이([MapRequest.preferCurrentLocation])는 거리 박스 + 위치 조정, 리뷰(끝난 홀)는 안내만 —
 * 보는 사람의 현재 GPS와 그 홀 그린의 거리는 의미가 없기 때문이다.
 */
@Composable
internal fun MapOverlay(state: MapSlotState) {
    val request = state.request ?: return
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { request.offset }
            .size(
                width = with(density) { request.size.width.toDp() },
                height = with(density) { request.size.height.toDp() },
            ),
    ) {
        if (request.preferCurrentLocation) {
            val green = request.greenLocation
            val current = request.currentLocation
            if (green != null && current != null) {
                val distance = haversineMeters(current.lat, current.lng, green.lat, green.lng)
                GreenDistanceBox(
                    distanceMeters = distance.toInt(),
                    modifier = Modifier.align(Alignment.TopStart).padding(OVERLAY_INSET),
                )
            }
            RecenterPill(
                onClick = request.onRecenter,
                modifier = Modifier.align(Alignment.BottomEnd).padding(OVERLAY_INSET),
            )
        } else {
            MapNote(modifier = Modifier.align(Alignment.TopStart).padding(OVERLAY_INSET))
        }
    }
}

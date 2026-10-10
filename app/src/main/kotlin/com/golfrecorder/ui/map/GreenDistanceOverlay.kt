package com.golfrecorder.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens
import com.golfrecorder.util.haversineMeters

private val OVERLAY_INSET = 8.dp

/** "그린까지 약 Nm" 칩. 지도 위(온라인, [GreenDistanceOverlay])와 지도가 없는
 * 오프라인 자리(RoundPlayScreen) 양쪽에서 같은 모양으로 쓴다. */
@Composable
fun GreenDistanceChip(distanceMeters: Int, modifier: Modifier = Modifier) {
    Text(
        "그린까지 약 ${distanceMeters}m",
        color = GolfTokens.CardBackground,
        fontFamily = GolfFonts.NumberFontFamily,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(GolfTokens.FieldGreen, RoundedCornerShape(GolfTokens.ChipCorner))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * 실제 지도 뷰는 액티비티 루트에 하나만 떠서 모든 화면 내용 위에 겹쳐 그려지므로,
 * 화면 안(RoundPlayScreen)에 둔 Compose 요소는 온라인일 때 지도 뒤에 가려진다 — 그래서
 * 온라인용 거리 칩은 지도와 같은 루트 레이어에서, 지도보다 나중에 그려 그 자리의 좌상단에
 * 올린다. 라이브 플레이([MapRequest.preferCurrentLocation])일 때만 보인다: 이미 끝난 홀을
 * 리뷰할 때는 보는 사람의 현재 GPS와 그 홀 그린의 거리가 의미 없기 때문이다.
 */
@Composable
internal fun GreenDistanceOverlay(state: MapSlotState) {
    val request = state.request ?: return
    if (!request.preferCurrentLocation) return
    val green = request.greenLocation ?: return
    val current = request.currentLocation ?: return
    val distance = haversineMeters(current.lat, current.lng, green.lat, green.lng)
    GreenDistanceChip(
        distanceMeters = distance.toInt(),
        modifier = Modifier.offset {
            IntOffset(
                request.offset.x + OVERLAY_INSET.roundToPx(),
                request.offset.y + OVERLAY_INSET.roundToPx(),
            )
        },
    )
}

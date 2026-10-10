package com.golfrecorder.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** material-icons-core에 없는 시안 전용 선 아이콘(24x24, 선 두께 2, 둥근 끝) — 색은 Icon의 tint로 정한다. */
object GolfIcons {
    /** 홈 헤더 로고(깃발). */
    val Flag: ImageVector by lazy {
        strokeIcon("GolfFlag") {
            moveTo(7f, 21f)
            verticalLineTo(3f)
            lineToRelative(10f, 4f)
            lineToRelative(-10f, 4f)
            moveTo(4f, 21f)
            horizontalLineTo(16f)
        }
    }

    /** 지도 "위치 조정" 버튼의 조준 아이콘. */
    val Crosshair: ImageVector by lazy {
        strokeIcon("GolfCrosshair") {
            moveTo(15f, 12f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 9f, y1 = 12f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 12f)
            close()
            moveTo(12f, 2f)
            verticalLineToRelative(4f)
            moveTo(12f, 18f)
            verticalLineToRelative(4f)
            moveTo(2f, 12f)
            horizontalLineToRelative(4f)
            moveTo(18f, 12f)
            horizontalLineToRelative(4f)
        }
    }

    /** 라운드 카드의 시간 줄 앞 시계. */
    val Clock: ImageVector by lazy {
        strokeIcon("GolfClock") {
            moveTo(21f, 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3f, y1 = 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 21f, y1 = 12f)
            close()
            moveTo(12f, 7f)
            verticalLineToRelative(5f)
            lineToRelative(3f, 2f)
        }
    }
}

private fun strokeIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        )
    }.build()

package com.golfrecorder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** GolfTokens 색상을 M3 ColorScheme에 공급하고, 본문 글꼴(GolfFonts.BodyFontFamily)을
 * 기본 Typography에 적용한다. GolfTokens.kt 자체는 여전히 평범한 Kotlin object로
 * 남고, 이 파일만 그 값을 M3가 요구하는 형태로 옮겨 담는다 — OutlinedButton/
 * OutlinedTextField/TopAppBar처럼 색을 직접 지정하지 않는 컨트롤이 기본값(M3 퍼플)
 * 대신 토큰 색을 쓰게 하기 위해서다(redesign-spec.md 3.7). */
private val GolfColorScheme = lightColorScheme(
    primary = GolfTokens.FieldGreen,
    onPrimary = GolfTokens.CardBackground,
    primaryContainer = GolfTokens.FieldGreen,
    onPrimaryContainer = GolfTokens.CardBackground,
    secondary = GolfTokens.Accent,
    onSecondary = GolfTokens.TextPrimary,
    secondaryContainer = GolfTokens.Accent,
    onSecondaryContainer = GolfTokens.TextPrimary,
    background = GolfTokens.Background,
    onBackground = GolfTokens.TextPrimary,
    surface = GolfTokens.CardBackground,
    onSurface = GolfTokens.TextPrimary,
    surfaceVariant = GolfTokens.Background,
    onSurfaceVariant = GolfTokens.TextSecondary,
    outline = GolfTokens.Border,
    outlineVariant = GolfTokens.Divider,
)

private val baseTypography = Typography()
private val GolfTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = GolfFonts.BodyFontFamily),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = GolfFonts.BodyFontFamily),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = GolfFonts.BodyFontFamily),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = GolfFonts.BodyFontFamily),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = GolfFonts.BodyFontFamily),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = GolfFonts.BodyFontFamily),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = GolfFonts.BodyFontFamily),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = GolfFonts.BodyFontFamily),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = GolfFonts.BodyFontFamily),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = GolfFonts.BodyFontFamily),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = GolfFonts.BodyFontFamily),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = GolfFonts.BodyFontFamily),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = GolfFonts.BodyFontFamily),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = GolfFonts.BodyFontFamily),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = GolfFonts.BodyFontFamily),
)

@Composable
fun GolfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GolfColorScheme, typography = GolfTypography, content = content)
}

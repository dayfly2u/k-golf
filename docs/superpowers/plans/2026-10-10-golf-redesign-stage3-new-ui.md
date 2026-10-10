# K-Golf 리디자인 스테이지 3 — 신규-UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 라운드/코스 데이터로 계산·표시만 추가한다(스키마 변경 없음) — GIR/L-GIR 이름 교체, 결과 화면 6칸 통계 + 홀 결과 비율 막대, 홈 상단 요약 + 파 대비, "최근 5회 중 N위" 뱃지, 18홀 진행바 + 현재 스코어, 그린까지 거리 오버레이. 스테이지 2에서 넘어온 미완료 항목(§3.7: M3 기본 테마, 스코어 배지 색상, 이모지→아이콘, 이전/다음 홀 고정)도 이 스테이지에서 함께 처리한다.

**Architecture:** 스테이지 1에서 만든 `GolfTokens`/`GolfFonts`와 `ui/common/`의 `StatGrid`/`HoleProgressBar`를 처음으로 실제 화면에 연결한다. 새 순수 로직(점수 등급 이름, 최근 순위, 집계)은 Compose에 의존하지 않는 `domain/model/` 함수로 뽑아 유닛 테스트를 붙인다. DB는 테이블 구조를 바꾸지 않고 `RoundDao`의 집계 SELECT만 넓힌다(컬럼 추가 아님, 마이그레이션 불필요).

**Tech Stack:** Jetpack Compose, Room(읅기 전용 쿼리 확장만), JUnit4(순수 함수 테스트).

## Global Constraints

- 버전 관리(`C:\github\CLAUDE.md`): 앱 코드가 바뀌는 커밋마다 `app/build.gradle.kts`의 `versionName` PATCH +1, 커밋 제목 끝에 `(vX.Y.Z)`. 이 플랜 시작 시점 버전은 `0.8.22` — Task 1부터 `0.8.23, 0.8.24, ...`로 하나씩 올린다. `versionCode`는 이 플랜과 무관(태그 시점에만).
- DB 마이그레이션 규칙(`C:\github\k-golf\CLAUDE.md`): 이 플랜은 테이블/컬럼 구조를 바꾸지 않는다 — `RoundDao`/`PenaltyDao`에 새 SELECT를 추가하거나 기존 SELECT의 집계 컬럼을 늘리는 것뿐이라 `Migration`이 필요 없다. `fallbackToDestructiveMigration`과 무관.
- 안드로이드 앱 검증 그라운드룰(`C:\github\CLAUDE.md`): 실기기 화면 확인은 전부 사용자 몫. Claude는 빌드(`./gradlew.bat compileDebugKotlin` 등)·설치·`logcat`·DB 값 직접 조회까지만 하고 화면을 직접 넘겨가며 캡처하지 않는다. 각 Task는 빌드 성공 확인(그리고 순수 함수가 있으면 유닛 테스트 통과)까지만 "완료" 기준으로 삼는다.
- UX 규칙: `ux-conventions-commercial` skill의 뒤로가기 아이콘 버튼 패턴, `GolfTokens` 색상 우선순위를 그대로 따른다. 이 플랜에서 새로 추가하는 화면 요소(진행바, 통계 그리드, 뱃지)는 전부 `GolfTokens`/`GolfFonts`를 쓴다 — 새 하드코딩 `Color(0x...)` 금지(단, 아래 Task 4/10에서 설명하는 기존 private color 상수를 토큰으로 교체하는 경우는 예외적으로 허용된 작업 그 자체다).
- **M3 `ColorScheme` 결정(2026-10-10, redesign-spec.md §3.7 참고)**: `GolfTokens` 기반 `lightColorScheme`+`Typography`를 `ui/theme/GolfTheme.kt`에 만들어 `MainActivity`의 `MaterialTheme {}`를 교체한다(Task 1). `GolfTokens.kt` 자체는 바뀌지 않는다 — 여전히 평범한 Kotlin object.
- **GolfTokens의 3단계 스코어 색(§3.7)**: `ScoreUnderParBorder`/`ScoreBogeyBackground`/`ScoreDoubleOrWorseBackground` 딱 3개뿐이다. 기존 코드(Home의 4단계 절대 타수 배지, 결과 화면의 4단계 파 대비 행 배색)를 이 3단계로 다시 묶을 때 세밀한 구분(예: 95~99타 vs 100+타)이 하나로 합쳐지는 손실이 있을 수 있다 — Task 4/10에서 각각 구체적인 매핑을 지정하므로 그대로 따른다(디자인 토론을 다시 열지 않는다).
- 새 순수 함수(`scoreGradeLabel`, `formatToPar`, `computeRecentRank`, `computeRoundAggregates`)는 전부 `domain/model/`에 두고 JUnit 테스트를 먼저 작성한다(TDD) — 이 플랜의 나머지(Compose 화면 변경)는 이 코드베이스의 기존 관례대로 Compose 유닛 테스트가 없으므로 빌드 확인으로 충분하다.

---

### Task 1: GolfTheme (M3 colorScheme + 본문 폰트)

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/theme/GolfTheme.kt`
- Modify: `app/src/main/kotlin/com/golfrecorder/MainActivity.kt:9,51`
- Modify: `app/build.gradle.kts` (`versionName`)

**Interfaces:**
- Produces: `@Composable fun GolfTheme(content: @Composable () -> Unit)` in package `com.golfrecorder.ui.theme`. 이후 모든 화면은 `MaterialTheme.colorScheme`/`MaterialTheme.typography`를 읒는 곳마다 자동으로 토큰 색/본문 폰트를 받는다 — 다른 화면 파일은 전혀 고칠 필요 없다.
- Consumes: `GolfTokens`(같은 패키지, import 불필요), `GolfFonts.BodyFontFamily`.

이 Task는 순수 로직이 없는 Compose 테마 래퍼라 TDD 사이클 대신 빌드 확인으로 완료를 판단한다(Global Constraints 참고).

- [ ] **Step 1: GolfTheme.kt 작성**

```kotlin
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
```

- [ ] **Step 2: MainActivity.kt에서 MaterialTheme → GolfTheme로 교체**

`MainActivity.kt`의 import 목록에서 `import androidx.compose.material3.MaterialTheme`를 지우고(MainActivity 안에서 더 이상 직접 참조하지 않음) 바로 아래 자리에 추가:

```kotlin
import com.golfrecorder.ui.theme.GolfTheme
```

`onCreate`의 `setContent { ... }` 블록:

```kotlin
        setContent {
            GolfTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
```

(끝 `}`는 그대로 — 여는 쪽 토큰만 `MaterialTheme`에서 `GolfTheme`로 바뀐다.)

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (OutlinedButton/OutlinedTextField/TopAppBar를 쓰는 다른 화면 파일은 전혀 고치지 않았는데도 컴파일된다 — 그 화면들이 받는 색만 바뀐다).

- [ ] **Step 4: 버전 올리고 커밋**

`app/build.gradle.kts`의 `versionName = "0.8.22"` → `versionName = "0.8.23"`.

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/golfrecorder/MainActivity.kt app/src/main/kotlin/com/golfrecorder/ui/theme/GolfTheme.kt
git commit -m "feat: GolfTokens 기반 M3 컬러스킴 + 본문 폰트 적용 (v0.8.23)"
```

---

### Task 2: GIR/L-GIR 라벨 교체 (4.1)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/common/GirSummaryLines.kt`

**Interfaces:**
- `GirSummaryLines`의 파라미터/시그니처는 그대로(`girCount`, `strictGirCount`, `totalHoles`, `singleLine` 등) — 화면 표시 텍스트만 바뀐다. Home(`RoundHistoryScreen`)이 이 컴포넌트를 그대로 호출하므로 Home에도 자동 반영된다. `RoundSummaryScreen`은 Task 8에서 이 컴포넌트 호출을 아예 없애고 6칸 그리드로 바꾸므로, 이 Task와 Task 8 사이에 순서 의존은 없다(이 Task를 먼저 해도 되고 나중에 해도 됨 — 플랜 순서상 먼저 한다).
- 의미(코드 프로퍼티 `isGreenInRegulation`/`isStrictGreenInRegulation`, 변수명 `girCount`/`strictGirCount`)는 그대로 — **표시 문자열만** 반대로 바꾼다(redesign-spec.md 4.1): `girCount`(= `isGreenInRegulation`, 느슨한 기준)는 화면에 **"L-GIR"**로, `strictGirCount`(= `isStrictGreenInRegulation`, 엄격한 기준)는 화면에 **"GIR"**로 보여준다.

빌드 확인으로 완료 판단(Compose 텍스트만 바뀌는 변경, 이 파일에 대한 기존 유닛 테스트 없음).

- [ ] **Step 1: singleLine 분기(Home 펼침) 라벨 교체**

`GirSummaryLines.kt`의 `if (singleLine) { ... }` 블록 안, 현재:

```kotlin
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Black)) {
                append("GIR $girCount/$totalHoles (${percent(girCount)}%)")
            }
            append(" / ")
            // 일반 GIR은 strokesToGreen만으로 판정해 숏어프로치가 있었어도 성공으로
            // 잡힐 수 있다 — 숏어프로치가 전혀 없었던 엄격 기준도 눈에 띄게 빨간색으로
            // 같이 보여준다.
            withStyle(SpanStyle(color = Color.Red)) {
                append("엄격 GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)")
            }
        }
```

다음으로 교체:

```kotlin
        val text = buildAnnotatedString {
            // 표시명과 코드명이 반대다 — girCount(isGreenInRegulation 기준)는 화면에
            // "L-GIR"로, strictGirCount(isStrictGreenInRegulation 기준)는 화면에
            // "GIR"로 보여준다(redesign-spec.md 4.1, 의미를 바꾸는 게 아니라 표시만 교체).
            withStyle(SpanStyle(color = Color.Black)) {
                append("L-GIR $girCount/$totalHoles (${percent(girCount)}%)")
            }
            append(" / ")
            // 숏어프로치가 전혀 없었던 엄격 기준(화면 표시명 "GIR")을 눈에 띄게
            // 빨간색으로 같이 보여준다.
            withStyle(SpanStyle(color = Color.Red)) {
                append("GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)")
            }
        }
```

- [ ] **Step 2: 기본(2줄) 분기 라벨 교체**

현재:

```kotlin
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            "GIR $girCount/$totalHoles (${percent(girCount)}%)",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "엄격 GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Red,
        )
    }
```

다음으로 교체:

```kotlin
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        // 표시명과 코드명이 반대다 — 위 singleLine 분기와 동일(redesign-spec.md 4.1).
        Text(
            "L-GIR $girCount/$totalHoles (${percent(girCount)}%)",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "GIR $strictGirCount/$totalHoles (${percent(strictGirCount)}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Red,
        )
    }
```

- [ ] **Step 3: 빌드 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 버전 올리고 커밋**

`versionName` `"0.8.23"` → `"0.8.24"`.

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/golfrecorder/ui/common/GirSummaryLines.kt
git commit -m "feat: GIR/L-GIR 표시명 교체 (v0.8.24)"
```

---

### Task 3: CourseManageScreen 이모지→벡터 아이콘 + 평점 별 색상 (3.7)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/course/CourseManageScreen.kt`

**Interfaces:** 콜백/시그니처 변화 없음 — 아이콘 교체 + 색상 1곳만.

- [ ] **Step 1: import 추가**

기존 `import androidx.compose.material.icons.automirrored.filled.ArrowBack` 바로 아래에 추가:

```kotlin
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
```

- [ ] **Step 2: 드래그 핸들 "≡" → Icons.Filled.Menu**

현재:

```kotlin
                                        Text(
                                            "≡",
                                            modifier = Modifier.dragHandle(index, dragState).padding(end = 12.dp),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = GolfTokens.TextSecondary,
                                        )
```

교체:

```kotlin
                                        Icon(
                                            Icons.Filled.Menu,
                                            contentDescription = "길게 눌러 순서 변경",
                                            modifier = Modifier.dragHandle(index, dragState).padding(end = 12.dp),
                                            tint = GolfTokens.TextSecondary,
                                        )
```

- [ ] **Step 3: 평점 별 색상 FieldGreen → Accent**

현재:

```kotlin
                                                if (course.rating != null) {
                                                    Text(
                                                        "★ ${"%.1f".format(course.rating)}  ",
                                                        color = GolfTokens.FieldGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
```

교체(색상 한 줄만):

```kotlin
                                                if (course.rating != null) {
                                                    Text(
                                                        "★ ${"%.1f".format(course.rating)}  ",
                                                        color = GolfTokens.Accent,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
```

- [ ] **Step 4: 수정 아이콘 "✏️" → Icons.Filled.Edit**

현재:

```kotlin
                                    Text(
                                        "✏️",
                                        modifier = Modifier
                                            .clickable { onEditCourse(course.id) }
                                            .padding(4.dp),
                                    )
```

교체:

```kotlin
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "코스 수정",
                                        tint = GolfTokens.TextSecondary,
                                        modifier = Modifier
                                            .clickable { onEditCourse(course.id) }
                                            .padding(4.dp),
                                    )
```

- [ ] **Step 5: 이모지를 가리키던 안내 문구 수정**

현재:

```kotlin
                                            Text(
                                                "입력된 리뷰 상세 정보가 없습니다. \"✏️\"에서 추가할 수 있습니다.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GolfTokens.TextSecondary,
                                            )
```

교체:

```kotlin
                                            Text(
                                                "입력된 리뷰 상세 정보가 없습니다. 수정 아이콘에서 추가할 수 있습니다.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GolfTokens.TextSecondary,
                                            )
```

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 버전 올리고 커밋**

`versionName` `"0.8.24"` → `"0.8.25"`.

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/golfrecorder/ui/course/CourseManageScreen.kt
git commit -m "feat: 코스 관리 이모지 아이콘을 벡터로, 평점 별 색상 교체 (v0.8.25)"
```

---

### Task 4: RoundSummaryScreen 스코어 행 색상 토큰화 (3.7)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt`

**Interfaces:** `scoreRowColor`/`scoreRowTextColor`는 둘 다 `private fun`으로 시그니처(`(Int) -> Color`) 그대로 — 내부 구현만 교체. 다른 Task가 이 두 함수를 호출하지 않는다.

**중요한 설계 결정**: 기존에는 4단계(버디 이하/파/보기 없음/더블/트리플 이상 — 보기(+1)는 `else -> Color.Transparent`로 색이 전혀 없었다)였는데, `GolfTokens`에는 3단계 토큰(`ScoreUnderParBorder`/`ScoreBogeyBackground`/`ScoreDoubleOrWorseBackground`)만 있다. "파 이하"(버디 이하 + 파, 둘 다 좋은 점수)는 배경 없음(투명, 카드/배경색 그대로)으로 통일하고, "보기"(+1)는 이제 처음으로 `ScoreBogeyBackground`(연한 블루그레이) 배경을 받고, "더블 이상"(+2 이상 전부, 기존엔 +2와 +3+가 연한/진한 빨강으로 나뉘어 있었다)은 `ScoreDoubleOrWorseBackground`(진한 블루그레이, 흰 글자)로 합친다 — `GolfTokens.kt`의 "스코어 셀(파 이하/보기/더블 이상)" 3단계 설계 의도 그대로.

- [ ] **Step 1: 색상 상수 제거 + 함수 교체**

현재(파일 상단, `formatToPar` 함수 바로 아래):

```kotlin
private val PAR_COLOR = Color(0xFFC8E6C9) // 연한 그린
private val BIRDIE_OR_BETTER_COLOR = Color(0xFFBBDEFB) // 연한 파랑
private val DOUBLE_BOGEY_COLOR = Color(0xFFFFCDD2) // 연한 빨강
private val WORSE_THAN_DOUBLE_BOGEY_COLOR = Color(0xFFB71C1C) // 진한 빨강
```

4줄을 전부 삭제한다(아래 Step에서 `scoreRowColor`가 이 상수들 대신 `GolfTokens`를 직접 쓴다).

그 아래 `WARNING_COLOR`/`WARNING_TEXT_COLOR`/`WarningBadge`는 그대로 둔다(이 Task와 무관 — 그린/숏/퍼팅 경고 칩은 행 배경과 별개).

`scoreRowColor`/`scoreRowTextColor` 현재:

```kotlin
private fun scoreRowColor(scoreToPar: Int): Color = when {
    scoreToPar == 0 -> PAR_COLOR
    scoreToPar <= -1 -> BIRDIE_OR_BETTER_COLOR
    scoreToPar == 2 -> DOUBLE_BOGEY_COLOR
    scoreToPar >= 3 -> WORSE_THAN_DOUBLE_BOGEY_COLOR
    else -> Color.Transparent
}

private fun scoreRowTextColor(scoreToPar: Int): Color =
    if (scoreToPar >= 3) Color.White else Color.Unspecified
```

교체:

```kotlin
// GolfTokens의 3단계(파 이하/보기/더블 이상)로 다시 묶는다(redesign-spec.md 3.7) —
// "파 이하"(버디 이하 + 파)는 배경 없음, "보기"(+1)는 처음으로 배경을 받고,
// "더블 이상"(+2부터 전부)은 기존에 나뉘어 있던 연한/진한 빨강을 하나로 합친다.
private fun scoreRowColor(scoreToPar: Int): Color = when {
    scoreToPar == 1 -> GolfTokens.ScoreBogeyBackground
    scoreToPar >= 2 -> GolfTokens.ScoreDoubleOrWorseBackground
    else -> Color.Transparent
}

private fun scoreRowTextColor(scoreToPar: Int): Color =
    if (scoreToPar >= 2) GolfTokens.CardBackground else GolfTokens.TextPrimary
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (더 이상 안 쓰는 `Color` import가 남아있어도 다른 곳(`WARNING_COLOR` 등)에서 여전히 쓰므로 import 정리는 필요 없다).

- [ ] **Step 3: 버전 올리고 커밋**

`versionName` `"0.8.25"` → `"0.8.26"`.

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt
git commit -m "feat: 결과 화면 스코어 행 색상을 GolfTokens 3단계로 교체 (v0.8.26)"
```

---

### Task 5: RoundPlayScreen 이전/다음 홀 버튼 하단 고정 (3.7)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt`

**Interfaces:** 콜백/ViewModel 로직 변화 없음 — `Scaffold`에 `bottomBar` 슬롯을 추가하고, 기존 스크롤 컬럼 맨 아래에 있던 이전/다음/완료 버튼 `Row`와 완료 확인 `AlertDialog`를 그 슬롯으로 그대로 옮긴다. `Scaffold`가 `bottomBar`의 실제 높이를 자동으로 재서 본문에 전달하는 `padding`에 반영하므로(스테이지 2에서 겪은 "플로팅 버튼이 마지막 항목을 가리는" 문제의 원인이었던 수동 여백 계산이 필요 없다), `PrimaryButtonHeight + 32.dp` 같은 보정값을 쓰지 않는다.

- [ ] **Step 1: Scaffold에 bottomBar 추가, 본문에서 버튼/다이얼로그 제거**

현재 `Scaffold(topBar = { TopAppBar(...) }) { padding -> Column(...) { ... } }` 구조에서, 본문 `Column`의 **맨 끝부분**(`Spacer(Modifier.height(24.dp))`부터 파일 끝까지):

```kotlin
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
                    // 이미 완료된 라운드를 리뷰 중이면 다시 완료할 이유가 없으니 버튼을 안 보여준다.
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

**전부 삭제**하고, 본문 `Column`을 아래처럼 `StepperRow`/`ReadOnlyStepperRow` 블록(`showDivider = false`로 끝나는 세 번째 스테퍼) 바로 다음에 작은 여백만 남기고 닫는다:

```kotlin
            Spacer(Modifier.height(16.dp))
        }
    }
}
```

(이 `Spacer`+닫는 중괄호 세 개가 본문 `Column`→`Scaffold` 람다→`RoundPlayScreen` 함수를 차례로 닫는다.)

이제 `Scaffold(...)` 호출 자체를 아래로 교체한다 — `topBar`는 그대로 두고 `bottomBar`를 추가:

```kotlin
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
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(GolfTokens.CardBackground)
                    .padding(16.dp),
            ) {
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
                        // 이미 완료된 라운드를 리뷰 중이면 다시 완료할 이유가 없으니 버튼을 안 보여준다.
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
```

(`topBar`는 바뀌지 않았다 — 기존 그대로 옮겨 적은 것뿐이다. `holeCount`/`context`/`viewModel`/`showFinishConfirm`은 `RoundPlayScreen` 함수 스코프에 있어 `bottomBar` 람다 안에서도 그대로 보인다.)

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

`versionName` `"0.8.26"` → `"0.8.27"`.

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt
git commit -m "feat: 홀 기록 화면 이전/다음 홀 버튼을 하단 고정으로 변경 (v0.8.27)"
```

---

### Task 6: RoundPlayScreen 18홀 진행바 + 현재 스코어 (4.5 + 4.6)

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/domain/model/ScoreFormatting.kt`
- Create: `app/src/test/kotlin/com/golfrecorder/domain/model/ScoreFormattingTest.kt`
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt` (ViewModel + Composable)
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt` (기존 private `formatToPar` 제거, 공용 함수로 교체)

**Interfaces:**
- Produces: `fun formatToPar(scoreToPar: Int): String`, `fun scoreGradeLabel(scoreToPar: Int): String` — 패키지 `com.golfrecorder.domain.model`. Task 8(결과 화면 비율 막대)이 `scoreGradeLabel`을 가져다 쓴다(Consumes).
- Produces: `RoundPlayViewModel.holeResults: StateFlow<List<HoleResult>>` — 이 라운드의 `hole_records`를 `HoleResult`로 매핑한 것(정렬됨, `RoundSummaryViewModel.holeResults`와 같은 패턴).
- Consumes: `HoleProgressBar`/`HoleProgressMode`/`holeSegmentColor`(스테이지 1, `ui/common/HoleProgressBar.kt`, 이미 있고 아직 어디서도 호출하지 않음).

- [ ] **Step 1: 실패하는 테스트 작성**

`app/src/test/kotlin/com/golfrecorder/domain/model/ScoreFormattingTest.kt`:

```kotlin
package com.golfrecorder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreFormattingTest {
    @Test
    fun `formatToPar는 0을 E로 표시한다`() {
        assertEquals("E", formatToPar(0))
    }

    @Test
    fun `formatToPar는 양수에 플러스를 붙인다`() {
        assertEquals("+3", formatToPar(3))
    }

    @Test
    fun `formatToPar는 음수를 그대로 표시한다`() {
        assertEquals("-2", formatToPar(-2))
    }

    @Test
    fun `scoreGradeLabel 버디 이하는 이글도 포함한다`() {
        assertEquals("버디 이하", scoreGradeLabel(-2))
        assertEquals("버디 이하", scoreGradeLabel(-1))
    }

    @Test
    fun `scoreGradeLabel 파는 0`() {
        assertEquals("파", scoreGradeLabel(0))
    }

    @Test
    fun `scoreGradeLabel 보기는 1`() {
        assertEquals("보기", scoreGradeLabel(1))
    }

    @Test
    fun `scoreGradeLabel 더블은 2`() {
        assertEquals("더블", scoreGradeLabel(2))
    }

    @Test
    fun `scoreGradeLabel 트리플 이상은 3 이상 전부`() {
        assertEquals("트리플+", scoreGradeLabel(3))
        assertEquals("트리플+", scoreGradeLabel(5))
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.domain.model.ScoreFormattingTest"`
Expected: FAIL (컴파일 에러 — `formatToPar`/`scoreGradeLabel` 아직 없음)

- [ ] **Step 3: 구현**

`app/src/main/kotlin/com/golfrecorder/domain/model/ScoreFormatting.kt`:

```kotlin
package com.golfrecorder.domain.model

/** 파 대비 점수를 "+3"/"E"/"-2" 형식으로. 결과 화면 스코어카드와 홀 기록 화면의
 * "현재 스코어"가 같은 포맷을 쓴다. */
fun formatToPar(scoreToPar: Int): String = when {
    scoreToPar == 0 -> "E"
    scoreToPar > 0 -> "+$scoreToPar"
    else -> "$scoreToPar"
}

/** 스코어 대 파 결과를 다섯 구간 이름으로 — 결과 화면 홀 결과 비율 막대(4.2)와
 * 홀 기록 화면의 "다음 홀" 버튼 서브텍스트(4.5)가 같은 구간 이름을 쓴다. */
fun scoreGradeLabel(scoreToPar: Int): String = when {
    scoreToPar <= -1 -> "버디 이하"
    scoreToPar == 0 -> "파"
    scoreToPar == 1 -> "보기"
    scoreToPar == 2 -> "더블"
    else -> "트리플+"
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.domain.model.ScoreFormattingTest"`
Expected: PASS (7개 전부)

- [ ] **Step 5: RoundSummaryScreen.kt의 기존 private formatToPar를 공용 함수로 교체**

`RoundSummaryScreen.kt`에서 현재:

```kotlin
private fun formatToPar(scoreToPar: Int): String = when {
    scoreToPar == 0 -> "E"
    scoreToPar > 0 -> "+$scoreToPar"
    else -> "$scoreToPar"
}
```

이 함수 정의를 **삭제**하고, import 목록(`com.golfrecorder.domain.model.DriverDistanceStats` 바로 아래)에 추가:

```kotlin
import com.golfrecorder.domain.model.formatToPar
```

(파일 안에서 `formatToPar(...)`를 호출하는 다른 곳들은 그대로 — import된 함수를 쓰게 된다.)

- [ ] **Step 6: RoundPlayViewModel에 holeResults StateFlow 추가**

`RoundPlayScreen.kt` 상단 import 목록에 추가:

```kotlin
import com.golfrecorder.domain.model.HoleResult
import com.golfrecorder.domain.model.scoreGradeLabel
import com.golfrecorder.ui.common.HoleProgressBar
import com.golfrecorder.ui.common.HoleProgressMode
```

`RoundPlayViewModel`의 `holes` StateFlow 선언 바로 다음(아직 `currentHoleNumber` 선언 전)에 추가:

```kotlin
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
```

- [ ] **Step 7: Composable에 진행바 + 현재 스코어 추가**

본문 `Column(...)`의 여는 중괄호 바로 다음, 현재:

```kotlin
        ) {
            val fixedLocation = currentLocation
            val showRecenterButton = !viewModel.isReview && hasLocationPermission && online
```

교체:

```kotlin
        ) {
            val holeResults by viewModel.holeResults.collectAsStateWithLifecycle()
            if (holeCount > 0) {
                HoleProgressBar(
                    totalHoles = holeCount,
                    currentHoleNumber = viewModel.currentHoleNumber,
                    mode = if (viewModel.isReview) HoleProgressMode.REVIEW else HoleProgressMode.LIVE,
                )
                if (!viewModel.isReview) {
                    // 지나온 홀만(지금 치는 홀 제외) 더한 파 대비 — holeNumber로 직접
                    // 걸러낸다. hole_records에는 지금 치는 홀도 이미 upsert돼 있어서
                    // (RoundPlayViewModel.loadHole 참고) "저장됐는지"로는 걸러낼 수 없다.
                    val currentScoreToPar = holeResults
                        .filter { it.holeNumber < viewModel.currentHoleNumber }
                        .sumOf { it.scoreToPar }
                    Text(
                        "현재 스코어 ${formatToPar(currentScoreToPar)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GolfTokens.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            val fixedLocation = currentLocation
            val showRecenterButton = !viewModel.isReview && hasLocationPermission && online
```

(`formatToPar`는 Step 6에서 추가한 `import com.golfrecorder.domain.model.scoreGradeLabel` 바로 위에 `import com.golfrecorder.domain.model.formatToPar`도 함께 추가한다.)

- [ ] **Step 8: "다음 홀" 버튼 서브텍스트 추가(bottomBar, Task 5에서 만든 블록)**

`bottomBar` 안의 `if (holeCount == 0 || viewModel.currentHoleNumber < holeCount) { Button(...) { Text("다음 홀") } }` 블록을 교체:

```kotlin
                    if (holeCount == 0 || viewModel.currentHoleNumber < holeCount) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            if (!viewModel.isReview) {
                                val liveTotal = viewModel.strokesToGreen + viewModel.strokesShortGame + viewModel.strokesPutt
                                Text(
                                    "이 홀 ${liveTotal}타 · ${scoreGradeLabel(liveTotal - par)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GolfTokens.TextSecondary,
                                )
                            }
                        }
                    } else if (!viewModel.isReview) {
```

(`par`는 `RoundPlayScreen` 함수 맨 앞에서 이미 `val par = currentHole?.par ?: 4`로 계산돼 있어 `bottomBar` 람다에서도 그대로 보인다. 이 줄 뒤의 `Button(완료)` 블록은 그대로 둔다.)

- [ ] **Step 9: 빌드 + 테스트 확인**

Run: `./gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin`
Expected: 전부 성공

- [ ] **Step 10: 버전 올리고 커밋**

`versionName` `"0.8.27"` → `"0.8.28"`.

```bash
git add app/build.gradle.kts \
  app/src/main/kotlin/com/golfrecorder/domain/model/ScoreFormatting.kt \
  app/src/test/kotlin/com/golfrecorder/domain/model/ScoreFormattingTest.kt \
  app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt \
  app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt
git commit -m "feat: 홀 기록 화면에 18홀 진행바 + 현재 스코어 추가 (v0.8.28)"
```

---

### Task 7: 그린까지 거리 오버레이 (4.7)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt`

**Interfaces:** 변화 없음 — 기존에 이미 계산되던 `haversineMeters(currentLocation, greenLocation)`를 오프라인 분기 전용에서 항상-표시 오버레이로 옮긴다. 네이티브 지도 SDK(`CourseMapSlot`/`PersistentMap`/`CourseMapView`)는 전혀 건드리지 않는다 — 지도 Box 위에 평범한 Compose `Text` 오버레이만 하나 추가한다.

- [ ] **Step 1: 지도 Box에 거리 오버레이 추가, 오프라인 전용 거리 텍스트 제거**

현재:

```kotlin
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
                    // provider를 아직 못 읽어온 온라인 상태 — 아래 오프라인 분기들과 섞이지
                    // 않도록 별도 분기로 빼서 로딩이 끝날 때까지 빈 자리로 둔다.
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
```

교체(오프라인 전용 거리 분기를 없애고, `when` 바깥에 항상-표시 오버레이를 추가):

```kotlin
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
                    // provider를 아직 못 읽어온 온라인 상태 — 아래 오프라인 분기와 섞이지
                    // 않도록 별도 분기로 빼서 로딩이 끝날 때까지 빈 자리로 둔다.
                    online -> {}
                    else -> Text(
                        "오프라인 상태입니다.",
                        color = GolfTokens.CardBackground,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                // 온라인 여부와 무관하게 늘 보여준다(redesign-spec.md 4.7) — 기존엔
                // 오프라인일 때만 텍스트로 노출됐다.
                if (greenLocation != null && fixedLocation != null) {
                    val distance = haversineMeters(
                        fixedLocation.lat, fixedLocation.lng, greenLocation.lat, greenLocation.lng,
                    )
                    Text(
                        "그린까지 약 ${distance.toInt()}m",
                        color = GolfTokens.CardBackground,
                        fontFamily = com.golfrecorder.ui.theme.GolfFonts.NumberFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(GolfTokens.FieldGreen, RoundedCornerShape(GolfTokens.ChipCorner))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
```

- [ ] **Step 2: 빌드 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

`versionName` `"0.8.28"` → `"0.8.29"`.

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/golfrecorder/ui/round/RoundPlayScreen.kt
git commit -m "feat: 그린까지 거리를 지도 오버레이로 항상 표시 (v0.8.29)"
```

---

### Task 8: RoundSummaryScreen 6칸 통계 + 홀 결과 비율 막대 (4.2)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/data/local/dao/PenaltyDao.kt`
- Modify: `app/src/main/kotlin/com/golfrecorder/data/repository/PenaltyRepository.kt`
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt` (ViewModel + Factory + Composable)
- Modify: `app/src/main/kotlin/com/golfrecorder/MainActivity.kt` (Factory 호출부)

**Interfaces:**
- Consumes: `scoreGradeLabel`(Task 6), `StatGrid`/`StatItem`(스테이지 1, `ui/common/StatGrid.kt`, 아직 어디서도 호출 안 함).
- Produces: `PenaltyRepository.getPenaltiesForRound(roundId: Long): Flow<List<PenaltyEntity>>`, `RoundSummaryViewModel.penaltyCounts: StateFlow<Pair<Int, Int>>`(OB, 해저드 — Task 9도 이 ViewModel을 고치지만 다른 필드를 추가하므로 직접적인 의존은 없다).
- `RoundSummaryViewModel`/`RoundSummaryViewModelFactory` 생성자에 `penaltyRepository: PenaltyRepository` 파라미터가 `shotRepository` 다음, `roundId` 앞에 추가된다 — `MainActivity.kt`의 호출부도 같이 고친다.

이 Task 이후 `GirSummaryLines`/`RoundStatsLine`은 더 이상 이 파일에서 호출되지 않는다(Home은 그대로 호출).

- [ ] **Step 1: PenaltyDao에 라운드 전체 조회 추가**

`PenaltyDao.kt`의 기존 `getPenalties` 쿼리 바로 다음에 추가:

```kotlin
    @Query("SELECT * FROM penalties WHERE roundId = :roundId")
    fun getPenaltiesForRound(roundId: Long): Flow<List<PenaltyEntity>>
```

- [ ] **Step 2: PenaltyRepository에 래퍼 추가**

`PenaltyRepository.kt`의 기존 `getPenalties` 바로 다음에 추가:

```kotlin
    fun getPenaltiesForRound(roundId: Long): Flow<List<PenaltyEntity>> = penaltyDao.getPenaltiesForRound(roundId)
```

- [ ] **Step 3: RoundSummaryScreen.kt import 추가**

```kotlin
import com.golfrecorder.data.repository.PenaltyRepository
import com.golfrecorder.domain.model.PenaltyType
import com.golfrecorder.domain.model.scoreGradeLabel
import com.golfrecorder.ui.common.StatGrid
import com.golfrecorder.ui.common.StatItem
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
```

(`GirSummaryLines`/`RoundStatsLine` import 2줄은 Step 6에서 호출부를 지울 때 함께 지운다.)

- [ ] **Step 4: RoundSummaryViewModel에 penaltyRepository 주입 + penaltyCounts 추가**

생성자 현재:

```kotlin
class RoundSummaryViewModel(
    private val roundRepository: RoundRepository,
    courseRepository: CourseRepository,
    shotRepository: ShotRepository,
    val roundId: Long,
```

교체:

```kotlin
class RoundSummaryViewModel(
    private val roundRepository: RoundRepository,
    courseRepository: CourseRepository,
    shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
    val roundId: Long,
```

`driverDistanceStats` StateFlow 선언 바로 다음에 추가:

```kotlin
    val penaltyCounts: StateFlow<Pair<Int, Int>> = penaltyRepository.getPenaltiesForRound(roundId)
        .map { list ->
            list.count { it.type == PenaltyType.OB.name } to list.count { it.type == PenaltyType.HAZARD.name }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)
```

- [ ] **Step 5: RoundSummaryViewModelFactory 수정**

현재:

```kotlin
class RoundSummaryViewModelFactory(
    private val roundRepository: RoundRepository,
    private val courseRepository: CourseRepository,
    private val shotRepository: ShotRepository,
    private val roundId: Long,
    private val courseId: Long?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoundSummaryViewModel(roundRepository, courseRepository, shotRepository, roundId, courseId) as T
}
```

교체:

```kotlin
class RoundSummaryViewModelFactory(
    private val roundRepository: RoundRepository,
    private val courseRepository: CourseRepository,
    private val shotRepository: ShotRepository,
    private val penaltyRepository: PenaltyRepository,
    private val roundId: Long,
    private val courseId: Long?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoundSummaryViewModel(roundRepository, courseRepository, shotRepository, penaltyRepository, roundId, courseId) as T
}
```

- [ ] **Step 6: 헤더 영역을 6칸 통계 + 비율 막대로 교체**

현재:

```kotlin
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
```

교체:

```kotlin
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(GolfTokens.Background)) {
            Text(
                "총 ${totalStrokes}타 (${formatToPar(totalScoreToPar)})",
                fontFamily = GolfFonts.NumberFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                color = GolfTokens.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val penaltyCounts by viewModel.penaltyCounts.collectAsStateWithLifecycle()
            val (obCount, hazardCount) = penaltyCounts
            StatGrid(
                items = listOf(
                    StatItem(label = "GIR", value = "$strictGirCount", sub = "/${holeResults.size}"),
                    StatItem(label = "L-GIR", value = "$girCount", sub = "/${holeResults.size}"),
                    StatItem(label = "퍼팅", value = "$totalPutts", sub = "평균 ${"%.1f".format(avgPutts)}"),
                    StatItem(
                        label = "드라이버 평균",
                        value = driverStats.avgMeters?.roundToInt()?.toString() ?: "-",
                        sub = "m",
                    ),
                    StatItem(
                        label = "드라이버 최장",
                        value = driverStats.maxMeters?.roundToInt()?.toString() ?: "-",
                        sub = "m",
                    ),
                    StatItem(label = "벌타", value = "OB $obCount", sub = "해저드 $hazardCount"),
                ),
                columns = 3,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            HoleResultRatioBar(
                holeResults = holeResults,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = GolfTokens.Divider)
```

(`girCount`/`strictGirCount`/`totalPutts`/`avgPutts`/`driverStats` 변수는 함수 위쪽에서 이미 계산돼 있다 — 그대로 둔다.)

이제 더 이상 쓰이지 않는 import 2줄을 지운다:

```kotlin
import com.golfrecorder.ui.common.GirSummaryLines
import com.golfrecorder.ui.common.RoundStatsLine
```

- [ ] **Step 7: 홀 결과 비율 막대 Composable 추가**

파일 끝(마지막 `}`— `RoundSummaryScreen` 함수를 닫는 중괄호) 바로 다음에 추가:

```kotlin

private val GRADE_ORDER = listOf("버디 이하", "파", "보기", "더블", "트리플+")

private val GRADE_COLOR = mapOf(
    "버디 이하" to GolfTokens.Accent,
    "파" to GolfTokens.FieldGreen,
    "보기" to GolfTokens.ScoreBogeyBackground,
    "더블" to GolfTokens.ScoreDoubleOrWorseBackground,
    "트리플+" to GolfTokens.TextSecondary,
)

/** 18홀 스코어를 다섯 구간(버디 이하/파/보기/더블/트리플+)으로 나눠 한 줄 비율 막대 +
 * 아래 범례(홀 수)로 보여준다. [scoreGradeLabel]로 구간을 나누고, 구간 순서는 항상
 * [GRADE_ORDER] 그대로 — 홀 수가 0인 구간은 막대에서 생략한다(Compose weight()는
 * 0을 허용하지 않는다). */
@Composable
private fun HoleResultRatioBar(holeResults: List<HoleResult>, modifier: Modifier = Modifier) {
    if (holeResults.isEmpty()) return
    val counts = GRADE_ORDER.associateWith { grade -> holeResults.count { scoreGradeLabel(it.scoreToPar) == grade } }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(GolfTokens.ChipCorner)),
        ) {
            GRADE_ORDER.filter { grade -> (counts[grade] ?: 0) > 0 }.forEach { grade ->
                Spacer(
                    modifier = Modifier
                        .weight((counts[grade] ?: 0).toFloat())
                        .fillMaxHeight()
                        .background(GRADE_COLOR.getValue(grade)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GRADE_ORDER.forEach { grade ->
                Text(
                    "$grade ${counts[grade] ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GolfTokens.TextSecondary,
                )
            }
        }
    }
}
```

`fillMaxHeight` import를 상단 import 목록에 추가:

```kotlin
import androidx.compose.foundation.layout.fillMaxHeight
```

- [ ] **Step 8: MainActivity.kt Factory 호출부 수정**

`Screen.RoundSummary` 분기의 현재:

```kotlin
                val vm = viewModel<RoundSummaryViewModel>(
                    factory = RoundSummaryViewModelFactory(
                        container.roundRepository,
                        container.courseRepository,
                        container.shotRepository,
                        screen.roundId,
                        screen.courseId,
                    ),
                    key = "round-summary-${System.identityHashCode(screen)}",
                )
```

교체:

```kotlin
                val vm = viewModel<RoundSummaryViewModel>(
                    factory = RoundSummaryViewModelFactory(
                        container.roundRepository,
                        container.courseRepository,
                        container.shotRepository,
                        container.penaltyRepository,
                        screen.roundId,
                        screen.courseId,
                    ),
                    key = "round-summary-${System.identityHashCode(screen)}",
                )
```

- [ ] **Step 9: 빌드 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: 버전 올리고 커밋**

`versionName` `"0.8.29"` → `"0.8.30"`.

```bash
git add app/build.gradle.kts \
  app/src/main/kotlin/com/golfrecorder/data/local/dao/PenaltyDao.kt \
  app/src/main/kotlin/com/golfrecorder/data/repository/PenaltyRepository.kt \
  app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt \
  app/src/main/kotlin/com/golfrecorder/MainActivity.kt
git commit -m "feat: 결과 화면에 6칸 통계 + 홀 결과 비율 막대 추가 (v0.8.30)"
```

---

### Task 9: RoundSummaryScreen "최근 5회 중 N위" 뱃지 (4.4)

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/domain/model/RoundRank.kt`
- Create: `app/src/test/kotlin/com/golfrecorder/domain/model/RoundRankTest.kt`
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt` (ViewModel + 헤더)

**Interfaces:**
- Produces: `fun computeRecentRank(rounds: List<RoundSummary>, targetRoundId: Long, windowSize: Int = 5): Pair<Int, Int>?`(순위, 그 구간 실제 라운드 수) — `targetRoundId`가 최근 `windowSize`개 안에 없으면 `null`.
- `RoundSummaryViewModel`에 `recentRank: StateFlow<Pair<Int, Int>?>` 추가 — 생성자 변경 없음(이미 있는 `roundRepository` 재사용).
- Task 8에서 바꾼 `RoundSummaryScreen.kt`를 이어서 수정한다(Task 8 다음에 실행).

- [ ] **Step 1: 실패하는 테스트 작성**

`app/src/test/kotlin/com/golfrecorder/domain/model/RoundRankTest.kt`:

```kotlin
package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundRankTest {
    private fun round(id: Long, playedAt: Long, strokes: Int) = RoundSummary(
        roundId = id, courseId = 1, playedAt = playedAt, finishedAt = playedAt + 1000,
        courseName = "course", price = null, companions = null, review = null, totalStrokes = strokes,
    )

    @Test
    fun `가장 최근 5개 중 타수가 가장 적으면 1위`() {
        val rounds = listOf(
            round(1, 500, 90), round(2, 400, 95), round(3, 300, 88), round(4, 200, 100), round(5, 100, 92),
        )
        assertEquals(1 to 5, computeRecentRank(rounds, targetRoundId = 3))
    }

    @Test
    fun `동률이면 더 최근 라운드가 더 높은 순위`() {
        val rounds = listOf(
            round(1, 500, 90), round(2, 400, 90), round(3, 300, 90),
        )
        // id=1이 가장 최근(playedAt=500)이면서 동률 90타 1위여야 한다.
        assertEquals(1 to 3, computeRecentRank(rounds, targetRoundId = 1))
        assertEquals(2 to 3, computeRecentRank(rounds, targetRoundId = 2))
        assertEquals(3 to 3, computeRecentRank(rounds, targetRoundId = 3))
    }

    @Test
    fun `전체 라운드가 5개 미만이면 모수가 그대로 줄어든다`() {
        val rounds = listOf(round(1, 200, 90), round(2, 100, 95))
        assertEquals(1 to 2, computeRecentRank(rounds, targetRoundId = 1))
    }

    @Test
    fun `최근 5개 밖의 라운드를 보면 null`() {
        val rounds = (1..6L).map { id -> round(id, playedAt = id * 100, strokes = 90) }
        // id=6이 가장 최근(playedAt=600), 최근 5개는 id=2..6 — id=1(playedAt=100)은 밖.
        assertNull(computeRecentRank(rounds, targetRoundId = 1))
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.domain.model.RoundRankTest"`
Expected: FAIL (컴파일 에러 — `computeRecentRank` 아직 없음)

- [ ] **Step 3: 구현**

`app/src/main/kotlin/com/golfrecorder/domain/model/RoundRank.kt`:

```kotlin
package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary

/** [targetRoundId]가 전체 라운드 중 가장 최근 [windowSize]개(코스 무관, playedAt
 * 내림차순, targetRoundId가 그 안에 들어있을 때만) 안에서 totalStrokes 기준 몇 위인지.
 * 동률이면 더 최근 라운드가 더 높은 순위(숫자가 더 작음). targetRoundId가 최근
 * [windowSize]개 안에 없으면(더 오래된 라운드를 보는 중) null. 반환값은
 * (순위, 그 구간에 실제로 들어간 라운드 수) — 전체 라운드가 [windowSize]개보다
 * 적으면 두 번째 값이 그 실제 개수로 줄어든다. */
fun computeRecentRank(rounds: List<RoundSummary>, targetRoundId: Long, windowSize: Int = 5): Pair<Int, Int>? {
    val recent = rounds.sortedByDescending { it.playedAt }.take(windowSize)
    if (recent.none { it.roundId == targetRoundId }) return null
    val ranked = recent.sortedWith(compareBy({ it.totalStrokes }, { -it.playedAt }))
    val rank = ranked.indexOfFirst { it.roundId == targetRoundId } + 1
    return rank to recent.size
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.domain.model.RoundRankTest"`
Expected: PASS (4개 전부)

- [ ] **Step 5: RoundSummaryViewModel에 recentRank 추가**

import 목록에 추가:

```kotlin
import com.golfrecorder.domain.model.computeRecentRank
```

`penaltyCounts` StateFlow(Task 8) 바로 다음에 추가:

```kotlin
    val recentRank: StateFlow<Pair<Int, Int>?> = roundRepository.getRoundSummaries()
        .map { all -> computeRecentRank(all, roundId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
```

- [ ] **Step 6: 헤더에 뱃지 추가**

헤더 `Row` 안, 현재:

```kotlin
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("삭제", color = GolfTokens.CardBackground)
                    }
```

교체:

```kotlin
                    Spacer(Modifier.weight(1f))
                    val recentRank by viewModel.recentRank.collectAsStateWithLifecycle()
                    recentRank?.let { (rank, pool) ->
                        Text(
                            "최근 ${pool}회 중 ${rank}위",
                            style = MaterialTheme.typography.bodySmall,
                            color = GolfTokens.TextPrimary,
                            modifier = Modifier
                                .background(GolfTokens.Accent, RoundedCornerShape(GolfTokens.ChipCorner))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("삭제", color = GolfTokens.CardBackground)
                    }
```

- [ ] **Step 7: 빌드 + 테스트 확인**

Run: `./gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin`
Expected: 전부 성공

- [ ] **Step 8: 버전 올리고 커밋**

`versionName` `"0.8.30"` → `"0.8.31"`.

```bash
git add app/build.gradle.kts \
  app/src/main/kotlin/com/golfrecorder/domain/model/RoundRank.kt \
  app/src/test/kotlin/com/golfrecorder/domain/model/RoundRankTest.kt \
  app/src/main/kotlin/com/golfrecorder/ui/round/RoundSummaryScreen.kt
git commit -m "feat: 결과 화면 헤더에 최근 5회 중 순위 뱃지 추가 (v0.8.31)"
```

---

### Task 10: RoundHistoryScreen 홈 요약 통계 + 스코어 배지 색상/파 대비 (4.3 + 3.7)

**Files:**
- Modify: `app/src/main/kotlin/com/golfrecorder/data/local/dto/RoundSummary.kt`
- Modify: `app/src/main/kotlin/com/golfrecorder/data/local/dao/RoundDao.kt`
- Create: `app/src/main/kotlin/com/golfrecorder/domain/model/RoundAggregates.kt`
- Create: `app/src/test/kotlin/com/golfrecorder/domain/model/RoundAggregatesTest.kt`
- Modify: `app/src/main/kotlin/com/golfrecorder/ui/history/RoundHistoryScreen.kt`

**Interfaces:**
- `RoundSummary`에 `totalPar: Int = 0`, `totalPutts: Int = 0` 필드 추가(기본값을 줘서 Task 9의 테스트 헬퍼가 깨지지 않는다 — Room은 쿼리 결과로 항상 실제 값을 넣어주므로 기본값은 테스트/수동 생성 편의용일 뿐 런타임에는 쓰이지 않는다).
- Produces: `data class RoundAggregates(avgTotalStrokes: Double, bestTotalStrokes: Int, avgPutts: Double)`, `fun computeRoundAggregates(rounds: List<RoundSummary>): RoundAggregates?`(비어있으면 null).
- 이 Task는 테이블/컬럼 구조를 바꾸지 않는다 — `RoundDao.getRoundSummaries()`의 SELECT에 집계 컬럼 2개(`totalPar`, `totalPutts`)를 추가하는 것뿐(Global Constraints 참고, 마이그레이션 불필요).

- [ ] **Step 1: RoundSummary dto에 필드 추가**

현재:

```kotlin
package com.golfrecorder.data.local.dto

data class RoundSummary(
    val roundId: Long,
    val courseId: Long?,
    val playedAt: Long,
    val finishedAt: Long?,
    val courseName: String,
    val price: Int?,
    val companions: String?,
    val review: String?,
    val totalStrokes: Int
)
```

교체:

```kotlin
package com.golfrecorder.data.local.dto

data class RoundSummary(
    val roundId: Long,
    val courseId: Long?,
    val playedAt: Long,
    val finishedAt: Long?,
    val courseName: String,
    val price: Int?,
    val companions: String?,
    val review: String?,
    val totalStrokes: Int,
    val totalPar: Int = 0,
    val totalPutts: Int = 0,
)
```

- [ ] **Step 2: RoundDao SQL에 집계 컬럼 추가**

`RoundDao.kt`의 `getRoundSummaries()` 쿼리 현재:

```kotlin
    @Query(
        """
        SELECT r.id AS roundId, r.courseId AS courseId, r.playedAt AS playedAt,
               r.finishedAt AS finishedAt,
               COALESCE(c.name, r.courseName) AS courseName,
               r.price AS price, r.companions AS companions, r.review AS review,
               SUM(hr.strokesToGreen + hr.strokesGreenToHoleOut) AS totalStrokes
        FROM rounds r
        LEFT JOIN courses c ON c.id = r.courseId
        LEFT JOIN hole_records hr ON hr.roundId = r.id
        GROUP BY r.id
        ORDER BY r.playedAt DESC
        """
    )
    fun getRoundSummaries(): Flow<List<RoundSummary>>
```

교체:

```kotlin
    @Query(
        """
        SELECT r.id AS roundId, r.courseId AS courseId, r.playedAt AS playedAt,
               r.finishedAt AS finishedAt,
               COALESCE(c.name, r.courseName) AS courseName,
               r.price AS price, r.companions AS companions, r.review AS review,
               SUM(hr.strokesToGreen + hr.strokesGreenToHoleOut) AS totalStrokes,
               SUM(hr.par) AS totalPar,
               SUM(hr.strokesPutt) AS totalPutts
        FROM rounds r
        LEFT JOIN courses c ON c.id = r.courseId
        LEFT JOIN hole_records hr ON hr.roundId = r.id
        GROUP BY r.id
        ORDER BY r.playedAt DESC
        """
    )
    fun getRoundSummaries(): Flow<List<RoundSummary>>
```

- [ ] **Step 3: 실패하는 테스트 작성**

`app/src/test/kotlin/com/golfrecorder/domain/model/RoundAggregatesTest.kt`:

```kotlin
package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundAggregatesTest {
    private fun round(strokes: Int, putts: Int) = RoundSummary(
        roundId = 1, courseId = 1, playedAt = 0, finishedAt = 1000,
        courseName = "course", price = null, companions = null, review = null,
        totalStrokes = strokes, totalPar = 72, totalPutts = putts,
    )

    @Test
    fun `라운드가 없으면 null`() {
        assertNull(computeRoundAggregates(emptyList()))
    }

    @Test
    fun `평균과 베스트와 평균 퍼팅을 계산한다`() {
        val result = computeRoundAggregates(listOf(round(90, 32), round(100, 36), round(88, 30)))
        assertEquals(92.666, result!!.avgTotalStrokes, 0.01)
        assertEquals(88, result.bestTotalStrokes)
        assertEquals(32.666, result.avgPutts, 0.01)
    }
}
```

- [ ] **Step 4: 테스트 실패 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.domain.model.RoundAggregatesTest"`
Expected: FAIL (컴파일 에러 — `computeRoundAggregates`/`RoundAggregates` 아직 없음)

- [ ] **Step 5: 구현**

`app/src/main/kotlin/com/golfrecorder/domain/model/RoundAggregates.kt`:

```kotlin
package com.golfrecorder.domain.model

import com.golfrecorder.data.local.dto.RoundSummary

/** 홈 화면 상단 요약 3칸(전체 평균 스코어, 베스트 스코어, 평균 퍼팅) — [rounds]가
 * 비어있으면 null(표시할 라운드가 아예 없음). */
data class RoundAggregates(val avgTotalStrokes: Double, val bestTotalStrokes: Int, val avgPutts: Double)

fun computeRoundAggregates(rounds: List<RoundSummary>): RoundAggregates? {
    if (rounds.isEmpty()) return null
    return RoundAggregates(
        avgTotalStrokes = rounds.map { it.totalStrokes }.average(),
        bestTotalStrokes = rounds.minOf { it.totalStrokes },
        avgPutts = rounds.map { it.totalPutts }.average(),
    )
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.domain.model.RoundAggregatesTest"`
Expected: PASS (2개 전부)

- [ ] **Step 7: RoundHistoryScreen.kt import 추가**

```kotlin
import com.golfrecorder.domain.model.computeRoundAggregates
import com.golfrecorder.domain.model.formatToPar
import com.golfrecorder.ui.common.StatGrid
import com.golfrecorder.ui.common.StatItem
```

- [ ] **Step 8: STROKE_* 상수를 GolfTokens 3단계로 교체**

현재:

```kotlin
private val STROKE_LIGHT_BLUE = Color(0xFFBBDEFB)
private val STROKE_LIGHT_GREEN = Color(0xFFC8E6C9)
private val STROKE_LIGHT_RED = Color(0xFFFFCDD2) // 연한 빨강 — RoundSummaryScreen의 DOUBLE_BOGEY_COLOR와 동일 톤
private val STROKE_DARK_RED = Color(0xFFC62828) // 진한 빨강 — RoundSummaryScreen의 GIR_MISS_BG_COLOR와 동일 톤

private fun strokeScoreColor(strokes: Int): Color? = when {
    strokes in 80..89 -> STROKE_LIGHT_BLUE
    strokes in 90..94 -> STROKE_LIGHT_GREEN
    strokes in 95..99 -> STROKE_LIGHT_RED
    strokes >= 100 -> STROKE_DARK_RED
    else -> null
}

// 진한 빨강 배경은 검정 텍스트로는 대비가 약해서 흰 글자로 바꾼다 — RoundSummaryScreen의
// scoreRowTextColor와 동일한 패턴.
private fun strokeScoreTextColor(strokes: Int): Color =
    if (strokes >= 100) Color.White else Color.Unspecified
```

교체:

```kotlin
// GolfTokens의 "스코어 셀(파 이하/보기/더블 이상)" 3단계를 재사용한다(redesign-spec.md
// 3.7) — 80대는 "파 이하"급 좋은 구간, 90대는 "보기"급 보통 구간, 95타 이상은 전부
// "더블 이상"급 나쁜 구간으로 묶는다(이전엔 95~99/100+가 연한/진한 빨강으로 따로
// 나뉘어 있었지만, GolfTokens에 그 두 단계를 구분하는 토큰이 없다).
private fun strokeScoreColor(strokes: Int): Color? = when {
    strokes in 80..89 -> GolfTokens.ScoreUnderParBorder
    strokes in 90..94 -> GolfTokens.ScoreBogeyBackground
    strokes >= 95 -> GolfTokens.ScoreDoubleOrWorseBackground
    else -> null
}

private fun strokeScoreTextColor(strokes: Int): Color =
    if (strokes >= 95) GolfTokens.CardBackground else GolfTokens.TextPrimary
```

- [ ] **Step 9: 카드 배지에 파 대비 서브텍스트 추가**

현재:

```kotlin
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
```

교체:

```kotlin
                            val backgroundColor = strokeScoreColor(round.totalStrokes)
                            Column(horizontalAlignment = Alignment.End) {
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
                                if (round.totalPar > 0) {
                                    Text(
                                        formatToPar(round.totalStrokes - round.totalPar),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GolfTokens.TextSecondary,
                                    )
                                }
                            }
```

- [ ] **Step 10: LazyColumn 맨 위에 상단 요약 3칸 추가**

현재:

```kotlin
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = GolfTokens.PrimaryButtonHeight + 32.dp,
                ),
            ) {
                items(rounds, key = { it.roundId }) { round ->
```

교체:

```kotlin
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = GolfTokens.PrimaryButtonHeight + 32.dp,
                ),
            ) {
                val aggregates = computeRoundAggregates(rounds)
                if (aggregates != null) {
                    item {
                        GolfCard(modifier = Modifier.padding(bottom = 10.dp)) {
                            StatGrid(
                                items = listOf(
                                    StatItem(label = "평균 스코어", value = "%.1f".format(aggregates.avgTotalStrokes)),
                                    StatItem(label = "베스트 스코어", value = "${aggregates.bestTotalStrokes}"),
                                    StatItem(label = "평균 퍼팅", value = "%.1f".format(aggregates.avgPutts)),
                                ),
                                columns = 3,
                            )
                        }
                    }
                }
                items(rounds, key = { it.roundId }) { round ->
```

- [ ] **Step 11: 빌드 + 테스트 확인**

Run: `./gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin`
Expected: 전부 성공

- [ ] **Step 12: 버전 올리고 커밋**

`versionName` `"0.8.31"` → `"0.8.32"`.

```bash
git add app/build.gradle.kts \
  app/src/main/kotlin/com/golfrecorder/data/local/dto/RoundSummary.kt \
  app/src/main/kotlin/com/golfrecorder/data/local/dao/RoundDao.kt \
  app/src/main/kotlin/com/golfrecorder/domain/model/RoundAggregates.kt \
  app/src/test/kotlin/com/golfrecorder/domain/model/RoundAggregatesTest.kt \
  app/src/main/kotlin/com/golfrecorder/ui/history/RoundHistoryScreen.kt
git commit -m "feat: 홈 화면 상단 요약 통계 + 스코어 배지 색상/파 대비 추가 (v0.8.32)"
```

---

## Self-Review (완료)

**스펙 커버리지**: 4.1→Task 2, 4.2→Task 8, 4.3→Task 10, 4.4→Task 9, 4.5+4.6→Task 6, 4.7→Task 7, §3.7의 6개 항목(M3 테마→Task 1, 스코어 배지 색→Task 4/10, 이모지→아이콘→Task 3, 이전/다음 홀 고정→Task 5, 평점 별 색→Task 3, 본문 폰트→Task 1) 전부 태스크가 있다. 빠진 항목 없음.

**플레이스홀더 스캔**: "TBD"/"나중에"/"적절히 처리" 패턴 없음 — 모든 스텝에 실제 코드 블록이 있다.

**타입 일관성 점검**: `formatToPar`/`scoreGradeLabel`(Task 6에서 `domain/model/ScoreFormatting.kt`에 생성) 시그니처가 Task 4/8/9/10에서 쓰는 호출부와 일치. `RoundSummaryViewModelFactory`/`RoundSummaryViewModel` 생성자 파라미터 순서(Task 8에서 `penaltyRepository`를 `shotRepository` 다음, `roundId` 앞에 추가)가 `MainActivity.kt` 호출부(Task 8 Step 8)와 일치. `RoundSummary.totalPar`/`totalPutts`(Task 10)에 기본값 `= 0`을 줘서 Task 9가 먼저 추가한 `RoundRankTest.kt`의 `round(...)` 헬퍼(두 필드를 안 쓴다)가 Task 10 이후에도 깨지지 않는다 — 두 Task 사이에 수정이 필요 없다.

**Task 순서 근거**: 같은 파일을 건드리는 Task는 전부 이전 Task가 남긴 코드를 "현재 상태"로 가정하고 다음 Step을 썼다 — Task 5→6→7(RoundPlayScreen.kt), Task 4→8→9(RoundSummaryScreen.kt), Task 6 Step 5가 RoundSummaryScreen.kt의 기존 `private fun formatToPar`를 지우므로 Task 8(RoundSummaryScreen.kt를 더 고침)은 Task 6 완료 후에 실행돼야 한다 — 플랜에 쓰인 순서(1→10)를 그대로 따르면 자동으로 지켜진다.


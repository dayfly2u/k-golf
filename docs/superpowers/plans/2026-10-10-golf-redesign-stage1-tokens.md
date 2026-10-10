# K-Golf 리디자인 스테이지 1 — 디자인 토큰 + 공용 컴포넌트 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 리디자인 스펙(`docs/design/2026-10-redesign/redesign-spec.md`)의 디자인 토큰(`GolfTokens`)과 글꼴, 6개 공용 Composable, `ux-conventions-commercial` 스킬을 만든다. 이 단계는 기존 화면을 하나도 바꾸지 않는다 — 다음 스테이지(기존 화면 교체)가 이 산출물을 가져다 쓴다.

**Architecture:** 색상/모서리/터치영역 토큰은 `ui/theme/GolfTokens.kt`의 평범한 Kotlin `object`로, 글꼴은 `ui/theme/GolfFonts.kt`의 `FontFamily` 상수로 둔다. 공용 Composable은 `ui/common/`에 파일당 하나씩 — 기존 `MoreBelowIndicator.kt`/`GirSummaryLines.kt`/`RoundStatsLine.kt`와 같은 패턴(작은 단일 책임 파일, 필요한 곳에서 import). 순수 로직이 있는 컴포넌트(`HoleProgressBar`의 구간 색 결정)만 별도 순수 함수로 뽑아 JUnit으로 테스트한다 — 이 프로젝트엔 Compose UI 테스트 인프라가 없고(`app/build.gradle.kts`에 `androidx.compose.ui:ui-test-junit4` 등이 없음, 기존 테스트는 전부 `app/src/test`의 순수 JVM 단위테스트) 실기기 시각 확인은 사용자 몫이라, 레이아웃만 있는 컴포넌트에 가짜 테스트를 만들지 않는다.

**Tech Stack:** Jetpack Compose, JUnit4(`app/src/test`), Google Fonts(OFL 라이선스, `res/font` 번들).

## Global Constraints

- (스펙 Global Constraints 전체 적용) DB 마이그레이션 없음 — 이 스테이지는 스키마 변경 없음.
- 앱 코드가 바뀌는 커밋마다 `app/build.gradle.kts`의 `versionName` PATCH를 1 올리고 커밋 제목 끝에 `(vX.Y.Z)`를 붙인다(계획 작성 시점 버전은 `0.8.4` — 실행 시점에 실제 파일을 열어 현재 값을 확인하고 거기서 +1 할 것, 이 숫자를 맹신하지 말 것). `versionCode`는 건드리지 않는다(태그 시점에만 올림).
- 각 Composable 파일은 기존 `ui/common/MoreBelowIndicator.kt` 스타일(package 선언, 필요한 import만, 파일 상단에 용도 설명 KDoc 한 줄)을 따른다.
- `ux-conventions-commercial` 스킬은 `C:\github\.claude\skills\ux-conventions-commercial\SKILL.md`에 쓴다(저장소 `k-golf` 안이 아니라 워크스페이스 공용 스킬 디렉터리 — `ux-conventions`와 같은 위치).
- 이 스테이지에서는 어떤 기존 화면 파일도 수정하지 않는다(새 파일만 추가). 기존 화면이 새 토큰/컴포넌트를 실제로 쓰기 시작하는 건 스테이지 2.

---

## File Structure

| 파일 | 역할 |
|---|---|
| `app/src/main/kotlin/com/golfrecorder/ui/theme/GolfTokens.kt` | 색상·모서리·터치영역 상수 |
| `app/src/test/kotlin/com/golfrecorder/ui/theme/GolfTokensTest.kt` | 토큰 값 검증 |
| `app/src/main/res/font/*.ttf` | IBM Plex Sans KR(4 weight) + Barlow Condensed(3 weight) |
| `app/src/main/kotlin/com/golfrecorder/ui/theme/GolfFonts.kt` | `FontFamily` 상수 |
| `app/src/main/kotlin/com/golfrecorder/ui/common/GolfCard.kt` | 리스트 카드 |
| `app/src/main/kotlin/com/golfrecorder/ui/common/PrimaryCtaButton.kt` | 하단 고정 큰 버튼 |
| `app/src/main/kotlin/com/golfrecorder/ui/common/StatGrid.kt` | 통계 그리드(4/6칸) |
| `app/src/main/kotlin/com/golfrecorder/ui/common/HoleProgressBar.kt` | 18홀 진행바 + 순수 색상 로직 |
| `app/src/test/kotlin/com/golfrecorder/ui/common/HoleProgressBarTest.kt` | 구간 색 결정 로직 검증 |
| `app/src/main/kotlin/com/golfrecorder/ui/common/StepperRow.kt` | 타수 입력 행(편집/읽기전용) |
| `app/src/main/kotlin/com/golfrecorder/ui/common/PenaltyChip.kt` | OB/해저드 버튼·칩 |
| `C:\github\.claude\skills\ux-conventions-commercial\SKILL.md` | 상용화 트랙 UX 규칙(ux-conventions와 다른 점만) |

---

### Task 1: GolfTokens

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/theme/GolfTokens.kt`
- Test: `app/src/test/kotlin/com/golfrecorder/ui/theme/GolfTokensTest.kt`

**Interfaces:**
- Produces: `object GolfTokens` with `val`색상(`Color`)·모서리(`Dp`)·터치영역(`Dp`) 상수 — 아래 Step 3 전체 목록이 이후 모든 Task가 참조하는 정식 이름.

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.golfrecorder.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class GolfTokensTest {
    @Test
    fun `핵심 색상이 스펙 팔레트와 정확히 일치한다`() {
        assertEquals(Color(0xFF1F5C3F), GolfTokens.FieldGreen)
        assertEquals(Color(0xFF123A28), GolfTokens.FieldGreenDark)
        assertEquals(Color(0xFFF2C14E), GolfTokens.Accent)
        assertEquals(Color(0xFFF4F5EF), GolfTokens.Background)
        assertEquals(Color(0xFFFFFFFF), GolfTokens.CardBackground)
        assertEquals(Color(0xFF18241D), GolfTokens.TextPrimary)
        assertEquals(Color(0xFF56635A), GolfTokens.TextSecondary)
        assertEquals(Color(0xFFECEFE7), GolfTokens.Divider)
        assertEquals(Color(0xFFCDD3C8), GolfTokens.Border)
    }

    @Test
    fun `벌타 색상이 스펙과 정확히 일치한다`() {
        assertEquals(Color(0xFFFFF6E8), GolfTokens.ObBackground)
        assertEquals(Color(0xFFE3A34A), GolfTokens.ObBorder)
        assertEquals(Color(0xFF7A4A0C), GolfTokens.ObText)
        assertEquals(Color(0xFFEEF3F9), GolfTokens.HazardBackground)
        assertEquals(Color(0xFF6B8FB8), GolfTokens.HazardBorder)
        assertEquals(Color(0xFF24476E), GolfTokens.HazardText)
    }

    @Test
    fun `스코어 셀 색상이 스펙과 정확히 일치한다`() {
        assertEquals(GolfTokens.Border, GolfTokens.ScoreUnderParBorder)
        assertEquals(Color(0xFFDCE6F1), GolfTokens.ScoreBogeyBackground)
        assertEquals(Color(0xFF6B8FB8), GolfTokens.ScoreDoubleOrWorseBackground)
        assertEquals(Color(0xFFDADFD4), GolfTokens.ProgressRemaining)
    }

    @Test
    fun `모서리와 터치영역이 스펙 범위 중앙값과 일치한다`() {
        assertEquals(20.dp, GolfTokens.CardCorner)
        assertEquals(16.dp, GolfTokens.ButtonCorner)
        assertEquals(999.dp, GolfTokens.ChipCorner)
        assertEquals(44.dp, GolfTokens.MinTouchTarget)
        assertEquals(58.dp, GolfTokens.PrimaryButtonHeight)
    }
}
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.ui.theme.GolfTokensTest"`
Expected: FAIL (컴파일 에러 — `GolfTokens`가 아직 없음)

- [ ] **Step 3: GolfTokens 구현**

```kotlin
package com.golfrecorder.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 리디자인 확정 시안(docs/design/2026-10-redesign/redesign-brief.md)의 색상·모서리·
 * 터치영역 값. 화면마다 흩어진 private Color 상수를 여기 하나로 모아서, 서로 다른
 * 화면의 색이 조용히 어긋나는 문제(예: 경고 배지 색이 행 배경과 겹쳐 안 보이던 버그)를
 * 원천적으로 막는다. */
object GolfTokens {
    // 기본
    val FieldGreen = Color(0xFF1F5C3F)
    val FieldGreenDark = Color(0xFF123A28)
    val Accent = Color(0xFFF2C14E) // CTA, 현재 홀
    val Background = Color(0xFFF4F5EF)
    val CardBackground = Color(0xFFFFFFFF)

    // 텍스트
    val TextPrimary = Color(0xFF18241D)
    val TextSecondary = Color(0xFF56635A)
    val Divider = Color(0xFFECEFE7)
    val Border = Color(0xFFCDD3C8)

    // 벌타
    val ObBackground = Color(0xFFFFF6E8)
    val ObBorder = Color(0xFFE3A34A)
    val ObText = Color(0xFF7A4A0C)
    val HazardBackground = Color(0xFFEEF3F9)
    val HazardBorder = Color(0xFF6B8FB8)
    val HazardText = Color(0xFF24476E)

    // 스코어 셀(파 이하/보기/더블 이상) — 스코어카드 그리드·홈 배지 공용
    val ScoreUnderParBorder: Color = Border // 흰 바탕 + 테두리
    val ScoreBogeyBackground = Color(0xFFDCE6F1)
    val ScoreDoubleOrWorseBackground = Color(0xFF6B8FB8) // 흰 글자

    // 18홀 진행바 — 아직 안 지나온 홀
    val ProgressRemaining = Color(0xFFDADFD4)

    // 모서리
    val CardCorner: Dp = 20.dp
    val ButtonCorner: Dp = 16.dp
    val ChipCorner: Dp = 999.dp

    // 터치 영역
    val MinTouchTarget: Dp = 44.dp
    val PrimaryButtonHeight: Dp = 58.dp
}
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.ui.theme.GolfTokensTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: 버전 올리고 커밋**

`app/build.gradle.kts`의 `versionName` PATCH +1(현재 값 확인 후 적용), 커밋 제목에 `(vX.Y.Z)` 붙여서:

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/theme/GolfTokens.kt \
        app/src/test/kotlin/com/golfrecorder/ui/theme/GolfTokensTest.kt \
        app/build.gradle.kts
git commit -m "feat: 리디자인 색상/모서리/터치영역 토큰 추가 (vX.Y.Z)"
```

---

### Task 2: 글꼴 번들 (IBM Plex Sans KR + Barlow Condensed)

**Files:**
- Create: `app/src/main/res/font/ibm_plex_sans_kr_regular.ttf` (weight 400)
- Create: `app/src/main/res/font/ibm_plex_sans_kr_medium.ttf` (weight 500)
- Create: `app/src/main/res/font/ibm_plex_sans_kr_semibold.ttf` (weight 600)
- Create: `app/src/main/res/font/ibm_plex_sans_kr_bold.ttf` (weight 700)
- Create: `app/src/main/res/font/barlow_condensed_medium.ttf` (weight 500)
- Create: `app/src/main/res/font/barlow_condensed_semibold.ttf` (weight 600)
- Create: `app/src/main/res/font/barlow_condensed_bold.ttf` (weight 700)
- Create: `app/src/main/kotlin/com/golfrecorder/ui/theme/GolfFonts.kt`

**Interfaces:**
- Produces: `val BodyFontFamily: FontFamily`(IBM Plex Sans KR, 본문), `val NumberFontFamily: FontFamily`(Barlow Condensed, 숫자 전용) — 이후 모든 화면 교체 Task가 `Text(..., fontFamily = GolfFonts.NumberFontFamily)`로 가져다 쓴다.

이 Task는 바이너리 폰트 파일을 받아오는 작업이라 "실패하는 테스트"가 의미 없다 — 대신 컴파일(리소스 참조가 유효한지)로 검증한다.

- [ ] **Step 1: 폰트 파일 받기**

https://fonts.google.com/download?family=IBM%20Plex%20Sans%20KR 와 https://fonts.google.com/download?family=Barlow%20Condensed 에서 zip을 받는다(두 폰트 다 OFL 라이선스, `res/font`에 그대로 번들 가능 — 브리프 1절 확인됨). zip 안의 `static/` 폴더에서 아래 굵기만 골라 Android 리소스 파일명 규칙(소문자+언더스코어, 하이픈 금지)으로 리네임해 `app/src/main/res/font/`에 넣는다:

| 원본(static/ 안) | 리네임 |
|---|---|
| `IBMPlexSansKR-Regular.ttf` | `ibm_plex_sans_kr_regular.ttf` |
| `IBMPlexSansKR-Medium.ttf` | `ibm_plex_sans_kr_medium.ttf` |
| `IBMPlexSansKR-SemiBold.ttf` | `ibm_plex_sans_kr_semibold.ttf` |
| `IBMPlexSansKR-Bold.ttf` | `ibm_plex_sans_kr_bold.ttf` |
| `BarlowCondensed-Medium.ttf` | `barlow_condensed_medium.ttf` |
| `BarlowCondensed-SemiBold.ttf` | `barlow_condensed_semibold.ttf` |
| `BarlowCondensed-Bold.ttf` | `barlow_condensed_bold.ttf` |

- [ ] **Step 2: GolfFonts.kt 작성**

```kotlin
package com.golfrecorder.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.golfrecorder.R

/** 리디자인 확정 시안의 본문/숫자 글꼴 — 둘 다 Google Fonts(OFL), res/font에 번들.
 * 숫자 글꼴(Barlow Condensed)은 스코어·타수·거리·통계 숫자에만 쓴다(브리프 1절). */
object GolfFonts {
    val BodyFontFamily = FontFamily(
        Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_kr_medium, FontWeight.Medium),
        Font(R.font.ibm_plex_sans_kr_semibold, FontWeight.SemiBold),
        Font(R.font.ibm_plex_sans_kr_bold, FontWeight.Bold),
    )

    val NumberFontFamily = FontFamily(
        Font(R.font.barlow_condensed_medium, FontWeight.Medium),
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (R.font.* 참조가 전부 resolve됨 — 파일명이 하나라도 틀리면 여기서 "unresolved reference" 에러)

- [ ] **Step 4: 버전 올리고 커밋**

```bash
git add app/src/main/res/font app/src/main/kotlin/com/golfrecorder/ui/theme/GolfFonts.kt app/build.gradle.kts
git commit -m "feat: 리디자인 글꼴(IBM Plex Sans KR, Barlow Condensed) 번들 (vX.Y.Z)"
```

---

### Task 3: GolfCard

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/common/GolfCard.kt`

**Interfaces:**
- Consumes: `GolfTokens.CardBackground`, `GolfTokens.CardCorner`(Task 1)
- Produces: `@Composable fun GolfCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)` — 홈 라운드 카드, 코스 카드 등 스테이지 2가 가져다 쓴다.

순수 로직이 없는 레이아웃 컴포넌트라 단위테스트 없이 바로 구현한다(이 프로젝트에 Compose UI 테스트 인프라 없음 — 시각 확인은 스테이지 2에서 실제 화면에 쓰일 때 사용자가 확인).

- [ ] **Step 1: 구현**

```kotlin
package com.golfrecorder.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.golfrecorder.ui.theme.GolfTokens

/** 리디자인 공용 리스트 카드 — 흰 배경, 둥근 모서리, 옅은 그림자. [onClick]을 주면
 * 카드 전체가 탭 타깃이 된다(홈 라운드 카드의 "탭하면 펼침" 패턴). */
@Composable
fun GolfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        },
        shape = RoundedCornerShape(GolfTokens.CardCorner),
        colors = CardDefaults.cardColors(containerColor = GolfTokens.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/common/GolfCard.kt app/build.gradle.kts
git commit -m "feat: 리디자인 공용 카드 컴포넌트 GolfCard 추가 (vX.Y.Z)"
```

---

### Task 4: PrimaryCtaButton

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/common/PrimaryCtaButton.kt`

**Interfaces:**
- Consumes: `GolfTokens.Accent`, `GolfTokens.ButtonCorner`, `GolfTokens.PrimaryButtonHeight`, `GolfTokens.TextPrimary`(Task 1)
- Produces: `@Composable fun PrimaryCtaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)` — 홈의 "새 라운딩 시작!", 코스 관리의 "새 코스 추가"가 스테이지 2에서 이걸로 교체된다.

- [ ] **Step 1: 구현**

```kotlin
package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfTokens

/** 화면 하단에 고정으로 두는 큰 CTA 버튼(새 라운딩 시작, 새 코스 추가). */
@Composable
fun PrimaryCtaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(GolfTokens.PrimaryButtonHeight),
        shape = RoundedCornerShape(GolfTokens.ButtonCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = GolfTokens.Accent,
            contentColor = GolfTokens.TextPrimary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}
```

버튼 높이(`GolfTokens.PrimaryButtonHeight` = 58dp)는 `Modifier.height()`로 고정 크기를 지정하므로, M3 `Button`이 내부적으로 적용하는 최소 높이(`ButtonDefaults.MinHeight` = 40dp)보다 크면 그대로 적용된다 — 호출부는 바닥 고정 위치(`Modifier.align`/`padding`)만 추가로 넘기면 된다.

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/common/PrimaryCtaButton.kt app/build.gradle.kts
git commit -m "feat: 리디자인 공용 CTA 버튼 PrimaryCtaButton 추가 (vX.Y.Z)"
```

---

### Task 5: StatGrid / StatChip

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/common/StatGrid.kt`

**Interfaces:**
- Consumes: `GolfTokens.TextSecondary`, `GolfFonts.NumberFontFamily`(Task 1, 2)
- Produces: `data class StatItem(val label: String, val value: String, val sub: String? = null)`, `@Composable fun StatGrid(items: List<StatItem>, columns: Int, modifier: Modifier = Modifier)` — 홈 펼침의 4개 항목(GIR/퍼팅/드라이버/벌타, `columns=4`, 1행), 결과 화면의 6개 항목(스테이지 3, `columns=3`, 2행 — `Summary.dc.html` 확인: `grid-template-columns: repeat(3, ...)`이지 6열이 아님)이 둘 다 이걸로 구현된다.

- [ ] **Step 1: 구현**

```kotlin
package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens

/** 통계 그리드 한 칸 — "22%" / "GIR" 처럼 큰 숫자(Barlow Condensed) + 작은 라벨.
 * [sub]를 주면 숫자 옆에 작은 보조 텍스트(예: "m", "4/18")를 붙인다. */
data class StatItem(val label: String, val value: String, val sub: String? = null)

@Composable
private fun StatCell(item: StatItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(6.dp)) {
        Text(item.label, fontSize = 11.sp, color = GolfTokens.TextSecondary)
        Row {
            Text(
                item.value,
                fontFamily = GolfFonts.NumberFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
            if (item.sub != null) {
                Text(
                    " " + item.sub,
                    fontSize = 11.sp,
                    color = GolfTokens.TextSecondary,
                )
            }
        }
    }
}

/** [columns]칸 고정 그리드 — 홈 펼침은 4개 항목에 columns=4(1행), 결과 화면은 6개
 * 항목에 columns=3(2행). 항목이 4~6개뿐이라
 * `LazyVerticalGrid`를 쓰지 않고 직접 행으로 나눈다 — `LazyVerticalGrid`/`LazyColumn`은
 * 세로 스크롤 부모 안에 중첩되면 "무한 높이" 제약으로 크래시하는 문제가 있어(이 코드베이스
 * 다른 곳에서 겪은 적 있는 패턴), 항목 수가 고정이고 적은 이 용도엔 가장 단순하고
 * 안전한 방식이다. */
@Composable
fun StatGrid(items: List<StatItem>, columns: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { item -> StatCell(item, modifier = Modifier.weight(1f)) }
                // 마지막 행이 columns보다 적게 채워지면 남은 칸만큼 빈 공간으로 메워
                // 셀 너비가 줄어들지 않게 한다.
                repeat(columns - rowItems.size) { Column(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/common/StatGrid.kt app/build.gradle.kts
git commit -m "feat: 리디자인 공용 통계 그리드 StatGrid 추가 (vX.Y.Z)"
```

---

### Task 6: HoleProgressBar

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/common/HoleProgressBar.kt`
- Test: `app/src/test/kotlin/com/golfrecorder/ui/common/HoleProgressBarTest.kt`

**Interfaces:**
- Consumes: `GolfTokens.FieldGreen`, `GolfTokens.Accent`, `GolfTokens.ProgressRemaining`(Task 1)
- Produces: `enum class HoleProgressMode { LIVE, REVIEW }`, `fun holeSegmentColor(holeNumber: Int, currentHoleNumber: Int, mode: HoleProgressMode): Color`(순수 함수, 단위테스트 대상), `@Composable fun HoleProgressBar(totalHoles: Int, currentHoleNumber: Int, mode: HoleProgressMode, modifier: Modifier = Modifier)` — 스테이지 3(4.5/4.6)에서 `RoundPlayScreen`의 라이브/리뷰 양쪽이 이걸 쓴다.

색상 결정 로직(라이브: 지난 홀=초록/현재=앰버/남은 홀=회색, 리뷰: 현재만 앰버/나머지 전부 초록)이 실수하기 쉬운 분기라 순수 함수로 뽑아 테스트한다.

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.golfrecorder.ui.common

import com.golfrecorder.ui.theme.GolfTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class HoleProgressBarTest {
    @Test
    fun `라이브 모드 - 지나온 홀은 FieldGreen`() {
        assertEquals(GolfTokens.FieldGreen, holeSegmentColor(holeNumber = 3, currentHoleNumber = 7, mode = HoleProgressMode.LIVE))
    }

    @Test
    fun `라이브 모드 - 현재 홀은 Accent`() {
        assertEquals(GolfTokens.Accent, holeSegmentColor(holeNumber = 7, currentHoleNumber = 7, mode = HoleProgressMode.LIVE))
    }

    @Test
    fun `라이브 모드 - 남은 홀은 ProgressRemaining`() {
        assertEquals(GolfTokens.ProgressRemaining, holeSegmentColor(holeNumber = 12, currentHoleNumber = 7, mode = HoleProgressMode.LIVE))
    }

    @Test
    fun `리뷰 모드 - 보고 있는 홀만 Accent`() {
        assertEquals(GolfTokens.Accent, holeSegmentColor(holeNumber = 4, currentHoleNumber = 4, mode = HoleProgressMode.REVIEW))
    }

    @Test
    fun `리뷰 모드 - 나머지는 전부 FieldGreen(이전 이후 구분 없음)`() {
        assertEquals(GolfTokens.FieldGreen, holeSegmentColor(holeNumber = 2, currentHoleNumber = 4, mode = HoleProgressMode.REVIEW))
        assertEquals(GolfTokens.FieldGreen, holeSegmentColor(holeNumber = 15, currentHoleNumber = 4, mode = HoleProgressMode.REVIEW))
    }
}
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.ui.common.HoleProgressBarTest"`
Expected: FAIL (컴파일 에러 — `holeSegmentColor`/`HoleProgressMode`가 아직 없음)

- [ ] **Step 3: 구현**

```kotlin
package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.golfrecorder.ui.theme.GolfTokens

enum class HoleProgressMode { LIVE, REVIEW }

/** 18홀 진행바 한 칸의 색. 라이브: 지나온 홀=FieldGreen, 현재 홀=Accent, 남은 홀=
 * ProgressRemaining. 리뷰(완료된 라운드): "남은 홀" 개념이 없으므로 지금 보는 홀만
 * Accent, 나머지는 전·후 구분 없이 전부 FieldGreen(시안 Hole.dc.html 그대로). */
fun holeSegmentColor(holeNumber: Int, currentHoleNumber: Int, mode: HoleProgressMode): Color = when (mode) {
    HoleProgressMode.LIVE -> when {
        holeNumber < currentHoleNumber -> GolfTokens.FieldGreen
        holeNumber == currentHoleNumber -> GolfTokens.Accent
        else -> GolfTokens.ProgressRemaining
    }
    HoleProgressMode.REVIEW -> if (holeNumber == currentHoleNumber) GolfTokens.Accent else GolfTokens.FieldGreen
}

@Composable
fun HoleProgressBar(
    totalHoles: Int,
    currentHoleNumber: Int,
    mode: HoleProgressMode,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (holeNumber in 1..totalHoles) {
            Surface(
                modifier = Modifier.weight(1f).height(6.dp),
                shape = RoundedCornerShape(3.dp),
                color = holeSegmentColor(holeNumber, currentHoleNumber, mode),
            ) {}
        }
    }
}
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.golfrecorder.ui.common.HoleProgressBarTest"`
Expected: PASS (5 tests)

- [ ] **Step 5: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/common/HoleProgressBar.kt \
        app/src/test/kotlin/com/golfrecorder/ui/common/HoleProgressBarTest.kt \
        app/build.gradle.kts
git commit -m "feat: 리디자인 18홀 진행바 HoleProgressBar 추가 (vX.Y.Z)"
```

---

### Task 7: StepperRow

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/common/StepperRow.kt`

**Interfaces:**
- Consumes: `GolfTokens.Border`, `GolfTokens.Accent`, `GolfTokens.TextSecondary`, `GolfTokens.Divider`, `GolfFonts.NumberFontFamily`(Task 1, 2)
- Produces: `@Composable fun StepperRow(label: String, hint: String, value: Int, onIncrement: () -> Unit, onDecrement: () -> Unit, showDivider: Boolean = true, modifier: Modifier = Modifier)`(편집용, −/+ 버튼), `@Composable fun ReadOnlyStepperRow(label: String, hint: String, value: Int, showDivider: Boolean = true, modifier: Modifier = Modifier)`(숫자만) — 스테이지 2에서 `RoundPlayScreen`의 그린까지/숏게임/퍼팅 행이 이걸로 교체된다(라이브는 `StepperRow`, 리뷰는 `ReadOnlyStepperRow`).

- [ ] **Step 1: 구현**

```kotlin
package com.golfrecorder.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfFonts
import com.golfrecorder.ui.theme.GolfTokens

private const val STEPPER_BUTTON_SIZE_DP = 48

/** 그린까지/숏게임/퍼팅처럼 "라벨 + 힌트 + 값"을 한 행으로 보여주는 공용 틀. [trailing]에
 * 편집용 −/+ 버튼 또는 읽기전용 숫자를 끼워 넣는다 — [StepperRow]/[ReadOnlyStepperRow]가
 * 각각 이 틀을 감싼다. */
@Composable
private fun StepperRowScaffold(
    label: String,
    hint: String,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(hint, fontSize = 12.sp, color = GolfTokens.TextSecondary)
            }
            trailing()
        }
        if (showDivider) HorizontalDivider(color = GolfTokens.Divider)
    }
}

@Composable
fun StepperRow(
    label: String,
    hint: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
) {
    StepperRowScaffold(label, hint, showDivider, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDecrement,
                modifier = Modifier.size(STEPPER_BUTTON_SIZE_DP.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GolfTokens.TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GolfTokens.Border),
            ) { Text("−", fontSize = 20.sp) }
            Text(
                "$value",
                fontFamily = GolfFonts.NumberFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                modifier = Modifier.size(STEPPER_BUTTON_SIZE_DP.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Button(
                onClick = onIncrement,
                modifier = Modifier.size(STEPPER_BUTTON_SIZE_DP.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GolfTokens.Accent),
            ) { Text("+", fontSize = 20.sp) }
        }
    }
}

@Composable
fun ReadOnlyStepperRow(
    label: String,
    hint: String,
    value: Int,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
) {
    StepperRowScaffold(label, hint, showDivider, modifier) {
        Text(
            "$value",
            fontFamily = GolfFonts.NumberFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
        )
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/common/StepperRow.kt app/build.gradle.kts
git commit -m "feat: 리디자인 공용 스테퍼 행 StepperRow/ReadOnlyStepperRow 추가 (vX.Y.Z)"
```

---

### Task 8: PenaltyChip

**Files:**
- Create: `app/src/main/kotlin/com/golfrecorder/ui/common/PenaltyChip.kt`

**Interfaces:**
- Consumes: `GolfTokens.ObBackground/ObBorder/ObText`, `GolfTokens.HazardBackground/HazardBorder/HazardText`(Task 1)
- Produces: `enum class PenaltyColor { OB, HAZARD }`, `@Composable fun PenaltyButton(text: String, color: PenaltyColor, onClick: () -> Unit, modifier: Modifier = Modifier)`(입력용 — "OB +1"/"OB +2"/"해저드 +1"), `@Composable fun PenaltyDisplayChip(text: String, color: PenaltyColor, modifier: Modifier = Modifier)`(표시용 — "OB 0"/"해저드 0" 칩) — 스테이지 2(라이브 입력), 스테이지 2(리뷰 표시 칩)가 둘 다 쓴다.

- [ ] **Step 1: 구현**

```kotlin
package com.golfrecorder.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golfrecorder.ui.theme.GolfTokens

enum class PenaltyColor { OB, HAZARD }

private data class PenaltyPalette(val background: Color, val border: Color, val text: Color)

private fun PenaltyColor.palette(): PenaltyPalette = when (this) {
    PenaltyColor.OB -> PenaltyPalette(GolfTokens.ObBackground, GolfTokens.ObBorder, GolfTokens.ObText)
    PenaltyColor.HAZARD -> PenaltyPalette(GolfTokens.HazardBackground, GolfTokens.HazardBorder, GolfTokens.HazardText)
}

/** OB +1 / OB +2 / 해저드 +1 입력 버튼. */
@Composable
fun PenaltyButton(text: String, color: PenaltyColor, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = color.palette()
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        // ButtonDefaults(OutlinedButtonDefaults가 아님)가 buttonColors()/outlinedButtonColors()/
        // textButtonColors() 등을 전부 제공하는 단일 객체다 — Task 7에서 같은 실수로 한 번
        // 고친 적 있음(OutlinedButtonDefaults는 실제로 존재하지 않는 클래스).
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = palette.background,
            contentColor = palette.text,
        ),
        border = BorderStroke(1.5.dp, palette.border),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** "OB 0" / "해저드 0" 읽기전용 칩(홀 상세/리뷰). */
@Composable
fun PenaltyDisplayChip(text: String, color: PenaltyColor, modifier: Modifier = Modifier) {
    val palette = color.palette()
    Text(
        text,
        color = palette.text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = modifier
            .background(palette.background, RoundedCornerShape(GolfTokens.ChipCorner))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 버전 올리고 커밋**

```bash
git add app/src/main/kotlin/com/golfrecorder/ui/common/PenaltyChip.kt app/build.gradle.kts
git commit -m "feat: 리디자인 공용 벌타 버튼/칩 PenaltyChip 추가 (vX.Y.Z)"
```

---

### Task 9: `ux-conventions-commercial` 스킬

**Files:**
- Create: `C:\github\.claude\skills\ux-conventions-commercial\SKILL.md`

**Interfaces:**
- Consumes: 없음(문서 전용 작업)
- Produces: 이후 모든 화면 교체 작업에서 참조할 "상용화 트랙 UX 규칙" 문서.

**Files 범위 밖**: 이 Task는 `k-golf` 저장소 바깥(`C:\github\.claude\skills\`)에 파일을 쓴다 — 이 플랜을 실행하는 agent가 `k-golf` 워크트리 안에서만 작업 중이면(예: `using-git-worktrees`로 격리된 상태) 이 경로에 못 쓸 수 있으니, 이 Task는 메인 워크스페이스(`C:\github`)에서 직접 실행한다. 버전 커밋 대상이 아니다(앱 코드 아님).

- [ ] **Step 1: 스킬 파일 작성**

```markdown
---
name: ux-conventions-commercial
description: K-Golf 리디자인(상용화 트랙)에서 ux-conventions와 달라지는 UX 규칙만 담는다. 레이아웃/배치/다이얼로그 등 바뀌지 않는 부분은 ux-conventions를 그대로 따른다 — 이 스킬은 "다른 점"만 정의한다.
---

# UX 컨벤션 — 상용화 트랙 (K-Golf 리디자인)

이 스킬은 `ux-conventions`(개인용 심플 앱 기준, `k-home-note`/`k-ott` 등 계속 사용)를
수정하지 않고, K-Golf가 상용화 트랙으로 가면서 달라지는 규칙만 별도로 담는다. 여기
없는 내용(버튼 배치, TopAppBar 사용, 섹션 헤더 배지, "더 있음" 표시, 삭제 확인
다이얼로그 등)은 `ux-conventions`를 그대로 따른다.

## 디자인 토큰

색상·모서리·터치영역 값은 하드코딩하지 않고 `com.golfrecorder.ui.theme.GolfTokens`
(색상)와 `com.golfrecorder.ui.theme.GolfFonts`(글꼴)를 가져다 쓴다. 전체 값은
`docs/design/2026-10-redesign/redesign-spec.md` 1절 참고.

## 뒤로가기 버튼

`ux-conventions`는 `TopAppBar`의 `navigationIcon`에 `Text("< 뒤로")` 텍스트를 쓰지만,
K-Golf 리디자인은 텍스트 없는 아이콘 전용 버튼을 쓴다:

```kotlin
navigationIcon = {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
    }
}
```

## 카드 탭 → 펼침 → 상세 이동 패턴

홈 라운드 카드처럼 "탭하면 펼쳐지고, 펼친 안에 상세 화면으로 가는 링크가 있는" 카드는
접힌 상태에 별도 아이콘 버튼(🔍 등)을 두지 않는다 — 카드 전체가 펼침 전용 탭 타깃이고,
상세 이동 링크는 펼쳤을 때만 나타난다(`GolfCard`의 `onClick`이 펼침을 토글, 펼친 내용
안의 `Text`가 상세 이동).

## 공용 컴포넌트

카드/CTA 버튼/통계 그리드/18홀 진행바/스테퍼 행/벌타 칩은 전부
`com.golfrecorder.ui.common`의 `GolfCard`/`PrimaryCtaButton`/`StatGrid`/
`HoleProgressBar`/`StepperRow`·`ReadOnlyStepperRow`/`PenaltyButton`·`PenaltyDisplayChip`을
가져다 쓴다 — 화면마다 비슷한 레이아웃을 새로 만들지 않는다.
```

- [ ] **Step 2: 확인**

Run: `cat "C:\github\.claude\skills\ux-conventions-commercial\SKILL.md"` (파일이 의도대로 저장됐는지 눈으로 확인 — 이 Task는 Android 빌드와 무관해 컴파일 확인 단계가 없음)
Expected: 위 Step 1 내용 그대로 출력

- [ ] **Step 3: 커밋 없음**

이 파일은 `k-golf` 저장소 밖(`C:\github\.claude\skills\`)이라 `k-golf` 레포 커밋 대상이 아니다. 워크스페이스 전체를 git으로 관리하지 않는다면(이 세션 기준 `C:\github`는 git 저장소 아님) 커밋 단계 자체가 없다 — 파일이 그 자리에 있으면 완료.

---

## 완료 기준

- [ ] Task 1~8의 모든 테스트 PASS, `./gradlew.bat :app:assembleDebug` 전체 빌드 성공
- [ ] 실기기에 설치해 기존 6개 화면이 전과 똑같이 보이고 동작하는지 확인(이 스테이지는 새 컴포넌트를 "만들기만" 하고 화면에 쓰지는 않으므로, 회귀가 없어야 정상)
- [ ] `ux-conventions-commercial` 스킬 파일 존재 확인
- [ ] 스테이지 2 계획(`docs/superpowers/plans/`에 새 파일)을 이 스테이지 완료 후에 작성 — 지금 만든 컴포넌트의 실제 함수 시그니처를 그대로 참조해야 하므로, 이 스테이지가 실제로 구현된 뒤에 쓴다.

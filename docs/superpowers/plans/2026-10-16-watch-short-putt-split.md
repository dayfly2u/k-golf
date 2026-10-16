# 워치 홀 기록 숏게임·퍼팅 분리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (또는 subagent-driven-development)로 task 단위 실행. 체크박스(`- [ ]`)로 진행을 추적한다.

**Goal:** 워치 홀 기록 화면을 "그린까지 / 숏게임 / 퍼팅" 세 줄(A안 배치)로 바꾸고, 숏게임 +/-가 폰의 `ShotPhase.SHORT_GAME`으로 기록·동기화되게 한다.

**Architecture:** `WearSync`(app/wear 두 벌)에 숏게임 액션·상태 키를 추가한다. 폰 `RoundRecordingService`가 새 액션을 처리하고 상태에 숏게임/퍼팅을 따로 싣는다. 워치는 `RoundUiState`/`OptimisticRound`에 숏게임 카운터를 더하고 `WatchPlayScreen`을 3줄로 바꾼다. 구버전 폰(키 없음)이면 숏게임 줄은 0·비활성.

**Tech Stack:** Kotlin, Jetpack Compose for Wear OS, Wearable Data Layer, JUnit4.

**요청서(확정 사양):** `docs/design/2026-10-redesign/watch-short-putt-split.md` · 시안 `docs/design/2026-10-redesign/WatchPlay.dc.html`

## Global Constraints

- DB 변경 없음(마이그레이션 없음).
- `WearSync.kt`는 `app/`과 `wear/`에 같은 내용으로 두 벌 — 항상 같이 수정.
- 상수 값: `ACTION_INCREMENT_SHORT_GAME = "INCREMENT_SHORT_GAME"`, `ACTION_DECREMENT_SHORT_GAME = "DECREMENT_SHORT_GAME"`, `KEY_STROKES_SHORT_GAME = "strokesShortGame"`.
- `KEY_STROKES_PUTT`는 이제 **PUTT만**(전엔 SHORT_GAME+PUTT 합). 새 워치 + 구버전 폰(`KEY_STROKES_SHORT_GAME` 없음)이면 퍼팅 값을 합산값으로 보고 숏게임 줄은 0, +/-는 비활성.
- "이 홀 N타" = 그린까지 + 숏게임 + 퍼팅.
- 화면: 라벨 17px SemiBold 흰색 왼쪽 정렬(남는 폭), −/+ 46px 원형, 숫자 Barlow 40px 폭 34 가운데, 줄 영역 좌우 36·위 100·줄 간격 6, 하단 요약 한 줄 12px(`· 합계` 회색, 숫자 Barlow 16) 아래 여백 38. 헤더/링/홀 이동/대기/배터리 화면은 그대로.
- 198dp(40mm) 워치에서 세로가 넘치므로 글자·버튼 하한은 WatchScale의 기존 규칙(`touch`/`sp`의 min)으로 두되 3줄이 들어가게 낮춘다.
- 버전: app/wear 코드 커밋은 `gradle.properties` `appVersionName` PATCH +1, 제목 끝 `(vX.Y.Z)`. 문서 전용 커밋은 버전 없음. (이번 변경은 예외로 v0.11.0에 포함 — Task 5 참고)

---

### Task 1: WearSync 상수 (두 벌)

**Files:** Modify `app/src/main/kotlin/com/golfrecorder/wearsync/WearSync.kt`, `wear/src/main/kotlin/com/golfrecorder/wearsync/WearSync.kt`

- [ ] `ACTION_INCREMENT_PUTT/DECREMENT_PUTT` 위의 "워치는 숏어프로치는 다루지 않고…" 주석을 지우고 아래로 교체, 두 파일에 똑같이:

```kotlin
    const val ACTION_INCREMENT_TO_GREEN = "INCREMENT_TO_GREEN"
    const val ACTION_DECREMENT_TO_GREEN = "DECREMENT_TO_GREEN"
    const val ACTION_INCREMENT_SHORT_GAME = "INCREMENT_SHORT_GAME"
    const val ACTION_DECREMENT_SHORT_GAME = "DECREMENT_SHORT_GAME"
    const val ACTION_INCREMENT_PUTT = "INCREMENT_PUTT"
    const val ACTION_DECREMENT_PUTT = "DECREMENT_PUTT"
```

- [ ] `KEY_STROKES_PUTT` 위에 키 추가(주석 포함), 두 파일 동일:

```kotlin
    /** 현재 홀의 숏게임(ShotPhase.SHORT_GAME) 타수. 이 키가 없으면(구버전 폰) 워치는 [KEY_STROKES_PUTT]를
     * 숏게임+퍼팅 합으로 보고 숏게임 줄을 0·비활성으로 둔다. */
    const val KEY_STROKES_SHORT_GAME = "strokesShortGame"
    /** 현재 홀의 퍼팅(ShotPhase.PUTT) 타수 — 신버전 폰은 퍼팅만 보낸다(구버전 폰은 숏게임과 합산). */
    const val KEY_STROKES_PUTT = "strokesPutt"
```

### Task 2: 폰 RoundRecordingService

**Files:** Modify `app/src/main/kotlin/com/golfrecorder/service/RoundRecordingService.kt`

- [ ] `handleAction`의 `when`에 숏게임 분기 추가:

```kotlin
            WearSync.ACTION_INCREMENT_SHORT_GAME -> incrementStroke(holeNumber, ShotPhase.SHORT_GAME, watchLocation)
            WearSync.ACTION_DECREMENT_SHORT_GAME -> decrementStroke(holeNumber, ShotPhase.SHORT_GAME)
```

- [ ] `pushState`의 `KEY_STROKES_PUTT` 합산 블록(주석 포함)을 교체:

```kotlin
                dataMap.putInt(
                    WearSync.KEY_STROKES_SHORT_GAME,
                    StrokeCalculator.currentTotal(shots, penalties, ShotPhase.SHORT_GAME),
                )
                dataMap.putInt(
                    WearSync.KEY_STROKES_PUTT,
                    StrokeCalculator.currentTotal(shots, penalties, ShotPhase.PUTT),
                )
```

### Task 3: 워치 상태·낙관적 반영 (TDD)

**Files:** Modify `wear/.../RoundStateViewModel.kt`, `OptimisticRound.kt`, `RoundDisplay.kt`; Test `wear/src/test/kotlin/com/golfrecorder/wear/OptimisticRoundTest.kt`, 새 `RoundDisplayTest.kt`(없으면 생성)

**Interfaces:** Produces `RoundUiState.strokesShortGame: Int = 0`, `RoundUiState.shortGameSupported: Boolean = true`, `fun strokeSplit(shortGame: Int?, putt: Int): StrokeSplit`, `data class StrokeSplit(shortGame, putt, shortGameSupported)`.

- [ ] 실패하는 테스트 먼저: `OptimisticRoundTest`에 (a) 같은 홀에서 숏게임 +/-가 서버 값에 얹히고 0에서 막힌다, (b) 다음 홀로 넘어간 뒤 누른 숏게임이 그 홀에 쌓인다, (c) 숏게임이 직전 홀 총타수·합계 기준에 들어간다; `RoundDisplayTest`에 `strokeSplit(null, 4)` → (0, 4, false), `strokeSplit(1, 3)` → (1, 3, true), `holeSummary`와 무관.
- [ ] 테스트 실행해 컴파일 실패/실패 확인: `./gradlew.bat :wear:testDebugUnitTest`
- [ ] `RoundUiState`에 두 필드 추가, `RoundDisplay.kt`에 `StrokeSplit`/`strokeSplit` 추가, `RoundStateViewModel.applyDataMap`에서 `strokeSplit(if (map.containsKey(WearSync.KEY_STROKES_SHORT_GAME)) map.getInt(WearSync.KEY_STROKES_SHORT_GAME) else null, map.getInt(WearSync.KEY_STROKES_PUTT, 0))`로 세 필드를 채움, `needsLocation`에 `ACTION_INCREMENT_SHORT_GAME` 포함.
- [ ] `OptimisticRound.kt`: `counters`가 `Triple(toGreen, shortGame, putt)`를 돌려주고 `displayRound`가 `strokesShortGame`을 채움, `totalAt`의 증감 분기에 숏게임 액션 포함.
- [ ] `./gradlew.bat :wear:testDebugUnitTest` 전체 통과 확인.

### Task 4: WatchPlayScreen 3줄 + A안 배치

**Files:** Modify `wear/.../WatchPlayScreen.kt`

- [ ] `holeStrokes = state.strokesToGreen + state.strokesShortGame + state.strokesPutt`.
- [ ] 스테퍼를 세 줄(그린까지/숏게임/퍼팅)로 — 숏게임 줄은 `enabled = state.shortGameSupported`(구버전 폰이면 −/+ 모두 비활성).
- [ ] `StepperRow`를 `라벨(남는 폭, 왼쪽) [−] 숫자 [+]` 한 줄로: 좌우 여백 `scale.dp(36f)`, 버튼 `scale.touch(46f, minDp = 28f)`, 숫자 Barlow `sp(40f, minSp = 22f)`·최소 폭 `scale.dp(34f)` 가운데, 라벨 `sp(17f, minSp = 12f)` SemiBold 흰색 왼쪽 안쪽 여백 `scale.dp(6f)`, 줄 사이 `scale.dp(6f)`.
- [ ] 하단 요약을 한 줄로: `이 홀 {N}타 · {등급} · 합계 {±N}` — "· 합계"만 `WatchColors.Label` 회색 Regular, 숫자(타수·합계)는 Barlow, 아래 여백 `scale.dp(38f)`. `WatchTotalLine`은 홀 이동 화면 등 다른 곳에서 쓰면 그대로 둔다.
- [ ] `./gradlew.bat :wear:assembleDebug :app:assembleDebug` 빌드 통과.

### Task 5: 버전·문서·검증

- [x] 버전: 사용자 결정으로 이 변경을 v0.11.0 릴리즈에 포함시키기로 해서 `appVersionName`을 올리지 않고 0.11.0 그대로 둔다(커밋 제목 끝 `(v0.11.0)`, 릴리즈 태그 v0.11.0을 이 커밋으로 옮김).
- [ ] 폰·워치 둘 다 빌드 → force-stop 후 `install -r` → 사용자에게 확인 요청(실기기 화면 확인은 사용자 몫): 워치에서 숏게임 +1 → 폰 홀 기록 "숏어프로치" 숫자 증가, 폰에서 숏어프로치 입력 → 워치 숏게임 줄 반영, 워치 GPS 라운드에서 숏게임 샷 좌표가 지도에 찍힘, 40mm 워치에서 세 줄·하단 한 줄이 링에 안 닿는지.
- [ ] 사용 설명서(`docs/user-guide.md`, `docs/user-guide/index.html`) 워치 항목을 세 줄 기준으로 수정하고 `watch-play.png`를 새 시안으로 다시 렌더 (문서 전용 커밋, 버전 없음).

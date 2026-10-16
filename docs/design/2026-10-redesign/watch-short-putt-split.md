# 워치 홀 기록: 숏게임·퍼팅 분리 + 줄 배치 변경 (Claude Code 작업 요청서)

- 작성일: 2026-10-16 · 요청자: Kyle
- 기준 코드: dayfly2u/k-golf `main` (2327ad2, v0.11.0 이후)
- 시안: https://claude.ai/artifact/MzjUzRwBpQfZSmyLzSbhpY (갤럭시 워치 줄 첫 화면) · 소스 `mockups/WatchPlay.dc.html`
- 진행 방식: superpowers (brainstorming → writing-plans → executing-plans)

## 1. 무엇을 바꾸나

지금 워치 홀 기록 화면(`wear/.../WatchPlayScreen.kt`)은 "그린까지"와 "숏 · 퍼팅" 두 줄이다.
이를 **"그린까지", "숏게임", "퍼팅" 세 줄**로 나누고, 각 줄 배치를 **A안**으로 바꾼다.

```
 ┌──────────────────────────────┐
 │        ● 폰 연결됨            │
 │   [‹]   HOLE 7 · 파 4   [›]   │   ← 지금과 동일
 │                              │
 │  그린까지     (−)  2  (+)     │
 │  숏게임       (−)  1  (+)     │   ← 새 줄
 │  퍼팅         (−)  2  (+)     │
 │                              │
 │  이 홀 5타 · 보기 · 합계 +6    │   ← 두 줄을 한 줄로
 └──────────────────────────────┘
```

## 2. 화면 (A안, Kyle 확정)

360px 원형 기준 디자인 px. 기존 `WatchScale`(`scale.dp()/scale.sp()`)로 환산하는 지금 방식을 그대로 쓴다.

| 요소 | 지금 | 변경 |
|---|---|---|
| 줄 수 | 2 (그린까지, 숏 · 퍼팅) | 3 (그린까지, 숏게임, 퍼팅) |
| 줄 배치 | `[−] 라벨/숫자(세로) [+]` | `라벨  [−] 숫자 [+]` 한 줄 |
| 라벨 | 11px, 회색, 숫자 위 | **17px SemiBold, 흰색, 왼쪽 정렬, 남는 폭 차지**(왼쪽 안쪽 여백 6) |
| −/+ 버튼 | 52px 원형 | 46px 원형 (− `#1E2A22`, + 강조 초록) |
| 숫자 | Barlow 46px | Barlow 40px, 폭 34 가운데 정렬, −와 + 사이 |
| 줄 영역 | 좌우 48 안쪽 | 좌우 36 안쪽, 위 100, 줄 간격 6 |
| 하단 요약 | "이 홀 N타 · 등급" + "합계 ±N" 두 줄 | **한 줄**: `이 홀 5타 · 보기 · 합계 +6` (12px, "· 합계"는 회색, 숫자는 Barlow 16), 아래 여백 38 |

- 헤더(연결 상태, ‹ HOLE · 파 ›), 테두리 18칸 링, 홀 이동/대기/배터리 화면은 그대로.
- 3줄이 원 안에 들어가는지 작은 워치(40mm)에서도 확인. 넘치면 버튼 44px, 숫자 36px까지 줄여도 됨(터치 영역 44px 아래로는 내리지 않음).
- "이 홀 N타"는 그린까지 + 숏게임 + 퍼팅 합.

## 3. 통신 변경 (신규 기능)

폰은 이미 `ShotPhase.SHORT_GAME`을 따로 저장한다. 지금은 워치 "숏 · 퍼팅" 버튼이 항상 `PUTT`으로 기록되고,
폰은 워치로 `KEY_STROKES_PUTT`에 SHORT_GAME + PUTT 합을 보낸다(`RoundRecordingService.kt` 264행 부근 주석).

`WearSync.kt`는 `app/`과 `wear/`에 같은 파일이 두 벌 있으니 **둘 다 똑같이** 바꾼다.

1. **WearSync**
   - 액션 추가: `ACTION_INCREMENT_SHORT_GAME = "INCREMENT_SHORT_GAME"`, `ACTION_DECREMENT_SHORT_GAME = "DECREMENT_SHORT_GAME"`
   - 상태 키 추가: `KEY_STROKES_SHORT_GAME = "strokesShortGame"`
2. **폰 `RoundRecordingService`**
   - 새 액션 처리: `incrementStroke(holeNumber, ShotPhase.SHORT_GAME, watchLocation)` / `decrementStroke(holeNumber, ShotPhase.SHORT_GAME)`
   - 상태 보낼 때: `KEY_STROKES_SHORT_GAME` = SHORT_GAME 합, `KEY_STROKES_PUTT` = **PUTT만** (지금의 합산 주석 정리)
3. **워치**
   - `RoundUiState`에 `strokesShortGame` 추가, `RoundStateViewModel`에서 읽기
   - `needsLocation()`에 `ACTION_INCREMENT_SHORT_GAME` 포함 (워치 GPS 라운드에서 샷 좌표 기록)
   - `OptimisticRound`의 낙관적 반영(93~113행)에 숏게임 카운터와 총타수 증감 추가
   - `WatchPlayScreen`에 세 번째 `StepperRow` 추가 + 2장 배치 변경
4. **버전 섞임 처리**
   - 새 워치 + 구버전 폰: `KEY_STROKES_SHORT_GAME`이 없으면(`map.containsKey`로 판단) 예전처럼 `KEY_STROKES_PUTT`를 합산값으로 보고 숏게임 줄을 0으로 둔다. 구버전 폰은 새 액션을 모르므로, 이때 숏게임 +/- 는 비활성화하거나 PUTT로 보낸다(기본안: 비활성화).
   - 새 폰 + 구버전 워치: 구버전 워치의 "숏 · 퍼팅"에 퍼팅만 보이게 됨. 폰·워치 apk를 같은 릴리즈로 함께 배포하는 것으로 충분.
5. **DB 변경 없음** (마이그레이션 불필요).

## 4. 확인할 것

- 단위 테스트: `OptimisticRound`의 숏게임 증감/총타수, 버전 섞임(키 없음) 분기
- 실기기: 워치에서 숏게임 +1 → 폰 홀 기록 화면의 숏어프로치 숫자가 늘어나는지, 폰에서 숏어프로치 입력 → 워치 숏게임 줄에 반영되는지, 워치 GPS 라운드에서 숏게임 샷 좌표가 지도에 찍히는지
- 사용 설명서(`docs/user-guide.md` 워치 항목)의 "그린까지·퍼팅" 설명을 세 줄 기준으로 수정

# golf-round-tracker 프로젝트 규칙

(워크스페이스 공통 규칙은 `C:\github\CLAUDE.md` 참고. 이 파일은 이 프로젝트에만 해당하는 규칙.)

## DB 마이그레이션 (2026-08-14 이후 필수)

2026-08-14부터 실기기에 실제 필드 라운딩 기록이 쌓이기 시작했다. **이 시점부터 저장된 라운딩 기록(rounds, hole_records, shots, penalties 등)은 절대 유실되면 안 되는 실데이터다.**

지금까지는 `AppDatabase.kt`에서 스키마를 바꿀 때마다 `fallbackToDestructiveMigration(dropAllTables = true)`로 기존 데이터를 통째로 날리고 새로 시작하는 방식이었다 (버전 8→11까지 전부 이 방식, 프리릴리즈 개인 테스트 단계라 허용됐던 컨벤션).

**앞으로는 이 방식을 쓰면 안 된다.** 스키마를 바꿔야 할 일이 생기면:

1. `fallbackToDestructiveMigration`으로 넘어가지 말고, 반드시 명시적인 `Migration(oldVersion, newVersion) { ... }` 객체를 작성해서 `.addMigrations(...)`로 등록한다.
2. 컬럼 추가처럼 단순한 변경은 `ALTER TABLE ... ADD COLUMN ...` SQL로 처리 가능. 테이블 구조가 크게 바뀌는 경우(컬럼 삭제/타입 변경/테이블 분리 등)는 임시 테이블 생성 → 데이터 복사 → 기존 테이블 삭제 → rename 패턴으로 기존 행을 보존한다.
3. 마이그레이션 작성 후에는 실기기(또는 이전 버전 DB 파일)를 대상으로 업그레이드 경로가 실제로 동작하는지, 기존 라운딩 기록이 그대로 남아있는지 확인한다 — 앱 재설치로 우회 검증하지 않는다(재설치는 DB를 초기화시키므로 마이그레이션 검증이 안 됨).
4. `exportSchema = true`이므로 `app/schemas/`에 버전별 스키마 JSON이 쌓인다 — 마이그레이션 작성 시 이전 버전 스키마 JSON을 참고해서 정확한 컬럼/타입을 맞춘다.

**절대 하면 안 되는 것**: "테스트 데이터 몇 개 정도야 괜찮겠지"라는 판단으로 `fallbackToDestructiveMigration`을 그대로 두고 버전만 올리는 것. 데이터 보존이 필요 없다고 확신이 서는 예외적 상황이라도, 사용자에게 먼저 명시적으로 확인 받은 뒤에만 destructive migration을 고려한다.

## UX 작업 시 참고 스킬

화면 UI/UX를 새로 만들거나 고치는 작업이면 `ux-conventions-commercial` 스킬을 `ux-conventions`보다 먼저 참조한다 — 이 프로젝트는 상용화 트랙이라 그 스킬에 이 프로젝트에서만 다른 규칙(뒤로가기 아이콘 버튼, GolfTokens 색상 우선순위, 카드 탭→펼침→상세 링크 패턴 등)이 정리돼 있다. 그 외(버튼 배치, TopAppBar, 삭제 확인 다이얼로그 등)는 `ux-conventions`를 그대로 따른다.

## 버전 관리 (폰 앱 + 워치 앱 공용, 2026-10-12 이후)

이 프로젝트는 폰 앱(`app/`)과 워치 앱(`wear/`)이 `applicationId`가 같고 `WearSync` 통신 규약을 같이 맞춰야 해서 **버전을 하나로 통일**한다 (워크스페이스 `C:\github\CLAUDE.md`의 "버전 관리"를 이 프로젝트 구조에 맞춘 것 — `android/`/`web/` 구분 없이 `app/`과 `wear/`가 대상).

- **버전은 루트 `gradle.properties` 한 곳에서만 올린다**: `appVersionName`, `appVersionCode`. `app/build.gradle.kts`와 `wear/build.gradle.kts`는 이 값을 읽기만 하므로 두 모듈 파일에는 버전 숫자를 직접 쓰지 않는다.
- **커밋마다 PATCH**: `app/` 또는 `wear/` 코드·빌드 설정이 바뀌는 커밋은 `appVersionName`의 PATCH를 +1 하고 커밋 제목 끝에 `(vX.Y.Z)`를 붙인다. 폰만 바뀌든 워치만 바뀌든 번호는 하나의 줄로 이어진다.
- **문서/설정 전용 커밋**(CLAUDE.md, README, `docs/**` 등 앱 코드에 영향 없는 것)은 버전을 올리지 않고 제목에 버전도 붙이지 않는다.
- **MINOR/MAJOR와 `appVersionCode`는 태그 찍을 때만** 올린다(사용자가 태그를 요청했을 때). 태그 이름은 `appVersionName`과 `v` 접두사만 다르다.
- 폰과 워치에 설치된 `versionName`이 같으면 서로 맞는 빌드라는 뜻이다. 통신 규약(`WearSync`)을 바꾼 뒤에는 둘 다 설치한다.

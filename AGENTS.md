# AGENTS.md

Operational guide for coding agents working in `C:\appDev\MusicPlayer`.
Use this file as the default playbook for making safe, minimal, repo-aligned changes.

## 1) Instruction Precedence

When instructions conflict, apply this order:
1. Direct user request
2. System/developer instructions from the runtime
3. Repository rules (`.cursorrules`, `.cursor/rules/*`, `.github/copilot-instructions.md`)
4. This `AGENTS.md`

## 2) Repository Snapshot

- Project type: Android app (single module: `:app`)
- Language: Kotlin
- UI: Jetpack Compose (Material 3)
- Build: Gradle Kotlin DSL (`*.gradle.kts`)
- JVM target: 17
- Key libraries: Media3, Room (+KSP), WorkManager, Coroutines, Jsoup, Coil
- Unit tests: JUnit4 + `kotlinx-coroutines-test`

## 3) Cursor / Copilot Rules Status

Scanned and found **no extra rule files** at this time:
- `.cursorrules` → not present
- `.cursor/rules/` → no files present
- `.github/copilot-instructions.md` → not present

If these files are added later, treat them as higher-priority supplements and update this document.

## 4) Environment & Execution Basics

- Use JDK 17
- Run commands from repo root: `C:\appDev\MusicPlayer`
- Windows commands use `gradlew.bat`
- macOS/Linux equivalents use `./gradlew`

## 5) Build / Lint / Test Commands

### Core commands

- Build debug app:
  - `gradlew.bat :app:assembleDebug`
- Run lint:
  - `gradlew.bat :app:lint`
- Run unit tests (debug):
  - `gradlew.bat :app:testDebugUnitTest`
- Run instrumented tests (device/emulator required):
  - `gradlew.bat :app:connectedDebugAndroidTest`

### Combined verification (recommended before handoff)

- `gradlew.bat :app:lint :app:testDebugUnitTest :app:assembleDebug`

### Clean verification

- `gradlew.bat clean :app:assembleDebug`

### Run a single test (important)

- Single test method:
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.drive.DriveViewModelTest.invalidUrlSetsErrorImmediately"`
- Single test class:
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.drive.DriveViewModelTest"`
- Pattern of tests:
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.drive.*"`

### Diagnostics

- List all gradle tasks:
  - `gradlew.bat tasks --all`
- Show warning details (useful for upgrades):
  - `gradlew.bat :app:testDebugUnitTest --warning-mode all`

## 6) High-Level Source Layout

- `app/src/main/java/com/example/musicplayer/core` → core models/types
- `.../data` → repositories, data sources, DB/Room, offline sync
- `.../features` → feature screens + ViewModels
- `.../playback` → Media3 playback/service/controller code
- `.../ui` → app shell, compose components, theme, now playing
- `app/src/test/java/...` → local JVM tests

## 7) Code Style Guidelines

Follow Kotlin official style (`kotlin.code.style=official`) and existing repository patterns.

### 7.1 Imports

- Keep imports explicit; avoid wildcard imports.
- Remove unused imports before finalizing changes.
- Prefer stable ordering enforced by IDE/formatter; do not hand-tune unnecessarily.
- Avoid fully qualified names inline unless needed to disambiguate.

### 7.2 Formatting

- 4-space indentation, no tabs.
- Keep functions small and readable.
- Use trailing commas only when consistent with surrounding file style.
- Wrap long argument lists vertically.
- Ensure newline at end of file.

### 7.3 Types and State Modeling

- Prefer `val` over `var`.
- Use `data class` for immutable state containers (e.g., `UiState`).
- Keep nullability explicit; avoid platform-type assumptions.
- Prefer enums/sealed types over stringly-typed state.
- Avoid `Any` and unchecked casts.

### 7.4 Naming Conventions

- Types (`class`, `object`, `interface`, `enum`): `PascalCase`
- Functions/properties/local vars: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- ViewModels end with `ViewModel`
- UI state types end with `UiState`
- Composables use clear UI-oriented names (`*Screen`, `*Bar`, `*Host`)

### 7.5 Compose Conventions

- Prefer stateless composables; hoist mutable state upward.
- Pass events as callbacks instead of reaching into global singletons.
- Use lifecycle-aware state collection (`collectAsStateWithLifecycle`).
- Keep screen-level orchestration in top-level UI shell.
- Use meaningful `label` values in animations/transitions.

### 7.6 ViewModel & Coroutines

- Launch async work in `viewModelScope`.
- Expose immutable `StateFlow`, keep mutable flow private.
- Use `MutableStateFlow.update { ... }` for atomic transitions.
- Respect coroutine cancellation; do not swallow `CancellationException`.
- Guard concurrent loads/races when necessary (mutex/request-id patterns are used in repo).

### 7.7 Error Handling

- Validate input early and fail fast with actionable messages.
- Use `runCatching` where it improves clarity.
- Do not silently ignore exceptions.
- Prefer user-friendly error messages in UI state.
- Keep message wording consistent with existing screens.

### 7.8 Data / Persistence

- Keep repository and data-source responsibilities focused.
- Keep Room entity/DAO naming explicit and feature-scoped.
- For schema changes, include migration/version impact in the same change.
- Keep mapping logic deterministic and side-effect-light.

### 7.9 Testing Standards

- Add/update tests for all non-trivial logic changes.
- For bug fixes, add a regression test when feasible.
- Use deterministic coroutine tests (`runTest`, dispatcher rules).
- Test both happy path and failure path/state transitions.
- Reuse existing fake/test patterns in `app/src/test`.

## 8) Agent Workflow Expectations

- Prefer minimal diffs over broad refactors unless requested.
- Keep architecture consistent with current app container/repository layering.
- Do not add new frameworks or tooling without clear justification.
- Run relevant lint/tests for touched areas before finishing.
- If behavior, commands, or architecture changed, update docs (`AGENTS.md` and related docs).

## 9) Quick Command Cheat Sheet

- Build debug: `gradlew.bat :app:assembleDebug`
- Lint: `gradlew.bat :app:lint`
- Unit tests: `gradlew.bat :app:testDebugUnitTest`
- Single test: `gradlew.bat :app:testDebugUnitTest --tests "fully.qualified.TestClass.testMethod"`
- Instrumented: `gradlew.bat :app:connectedDebugAndroidTest`

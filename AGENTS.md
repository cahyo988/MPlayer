# AGENTS.md

Operational guide for agentic coding assistants in `C:\appDev\MusicPlayer`.
Goal: produce safe, minimal, repo-aligned changes with fast verification.

## 1) Instruction precedence

If instructions conflict, follow this order:
1. Direct user request
2. System/developer runtime instructions
3. Repo rule files (`.cursorrules`, `.cursor/rules/*`, `.github/copilot-instructions.md`)
4. This `AGENTS.md`

## 2) Repository snapshot

- Android app, single module: `:app`
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Build system: Gradle Kotlin DSL (`*.gradle.kts`)
- Java/Kotlin target: 17 (`jvmTarget = "17"`)
- Core libs: Media3, Room (+KSP), WorkManager, Coroutines, Jsoup, Coil
- Unit testing: JUnit4 + `kotlinx-coroutines-test`

## 3) Cursor/Copilot rules status

Checked on 2026-03-18:
- `.cursorrules` → file not found
- `.cursor/rules/` → directory not found
- `.github/copilot-instructions.md` → file not found

If any are added later:
- Treat them as higher priority than this file.
- Update this section and align workflow decisions.

## 4) Environment defaults

- Repo root: `C:\appDev\MusicPlayer`
- Use JDK 17
- Run commands from repo root unless explicitly required otherwise
- Gradle wrapper by OS:
  - Windows: `gradlew.bat`
  - macOS/Linux: `./gradlew`
- Do not change global machine configuration from agents.

## 5) Build / lint / test commands

Use the wrapper for your OS (`gradlew.bat` or `./gradlew`).

### Core commands

- Build debug APK:
  - `gradlew.bat :app:assembleDebug`
- Run lint:
  - `gradlew.bat :app:lint`
- Run local unit tests:
  - `gradlew.bat :app:testDebugUnitTest`
- Run instrumentation tests (requires emulator/device):
  - `gradlew.bat :app:connectedDebugAndroidTest`

### Fast iteration

- Compile Kotlin only:
  - `gradlew.bat :app:compileDebugKotlin`
- Run test subset by package pattern:
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.*"`

### Pre-handoff verification (recommended)

- `gradlew.bat :app:lint :app:testDebugUnitTest :app:assembleDebug`

### Clean verification

- `gradlew.bat clean :app:assembleDebug`

### Run a single test (important)

- Single test method:
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.drive.DriveViewModelTest.invalidUrlSetsErrorImmediately"`
- Single test class:
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.drive.DriveViewModelTest"`
- Single package/pattern:
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.drive.*"`
- Multiple explicit filters (repeat `--tests`):
  - `gradlew.bat :app:testDebugUnitTest --tests "com.example.musicplayer.features.playlist.PlaylistsViewModelTest" --tests "com.example.musicplayer.features.recents.RecentsViewModelTest"`
- Wildcard fallback for discovery edge cases:
  - `gradlew.bat :app:testDebugUnitTest --tests "*DriveViewModelTest.invalidUrlSetsErrorImmediately"`

Notes:
- Keep each `--tests` argument quoted.
- `--tests` uses Gradle test filtering patterns (not full regex).
- Prefer fully qualified class names for stable matching.
- Iterate with targeted tests first, then run full unit suite.

### Diagnostics

- List tasks: `gradlew.bat tasks --all`
- Show warnings during tests: `gradlew.bat :app:testDebugUnitTest --warning-mode all`
- Add stacktraces on failures: `gradlew.bat :app:testDebugUnitTest --stacktrace`

## 6) Source layout (high level)

- `app/src/main/java/com/example/musicplayer/core` → core types/models
- `.../data` → repositories, Room/db, data sources, sync
- `.../di` → dependency injection wiring
- `.../features` → feature screens + ViewModels
- `.../playback` → Media3 playback/service/controller
- `.../ui` → app shell/components/theme
- `app/src/test/java/com/example/musicplayer/...` → local unit tests

## 7) Code style guidelines

Project uses `kotlin.code.style=official`. Match surrounding local patterns.

### 7.1 Imports
- Prefer explicit imports; avoid wildcard imports.
- Remove unused imports.
- Keep import order formatter/IDE-driven.
- Avoid inline fully qualified names unless disambiguation is required.

### 7.2 Formatting
- 4 spaces; no tabs.
- Keep functions short and focused.
- Wrap long parameter lists/chains across lines.
- Keep line length review-friendly.
- Use trailing commas only where consistent nearby.
- Ensure newline at EOF.

### 7.3 Types and state
- Prefer `val` over `var`.
- Keep nullability explicit and intentional.
- Use `data class` for immutable UI/domain state.
- Prefer sealed classes/enums over string flags.
- Avoid unsafe casts and `Any`-heavy APIs.
- Keep mutable state private; expose immutable views.

### 7.4 Naming conventions
- Classes/interfaces/objects/enums: `PascalCase`
- Functions/properties/locals: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- ViewModels: suffix `ViewModel`
- UI state models: suffix `UiState`
- Composables: descriptive names (`*Screen`, `*Item`, `*Dialog`, etc.)

### 7.5 Compose + coroutines
- Prefer stateless composables; hoist mutable state.
- Pass actions via callbacks; avoid hidden coupling.
- Use `collectAsStateWithLifecycle` for Flow-backed UI state.
- Keep effects explicit (`LaunchedEffect`, `DisposableEffect`).
- Use `viewModelScope` for async work.
- Keep mutable flows private; expose immutable `StateFlow`.
- Use `MutableStateFlow.update` for atomic updates.
- Never swallow `CancellationException`.

### 7.6 Error handling and data
- Validate input early.
- Surface actionable error states/messages to UI.
- Prefer explicit error handling over silent failure.
- Use `runCatching` where it improves clarity.
- Log/report unexpected failures at layer boundaries.
- Keep repository and data-source responsibilities clear.
- Ship Room schema version and migrations together.

### 7.7 Testing expectations
- Add or update tests for non-trivial logic changes.
- Add regression tests for bug fixes when practical.
- Use deterministic coroutine testing (`runTest`).
- Cover both success and failure state transitions.
- Reuse fakes/test helpers in `app/src/test`.

## 8) Workflow and safety checklist

- Prefer minimal diffs over broad refactors.
- Preserve architecture unless user asks for redesign.
- Do not introduce frameworks without clear justification.
- Run relevant checks for touched areas before handoff.
- Update docs when behavior/API expectations change.
- Never commit or push unless explicitly requested.
- Ensure no secrets/credentials are introduced.

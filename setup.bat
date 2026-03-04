@echo off
setlocal ENABLEDELAYEDEXPANSION

echo ==================================================
echo MusicPlayer - Windows Fast Setup
echo ==================================================

set ROOT_DIR=%~dp0
cd /d "%ROOT_DIR%"

echo.
echo [1/6] Checking Java...
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java not found in PATH. Please install JDK 17.
  goto :fail
)
java -version
if errorlevel 1 (
  echo [ERROR] Java exists but failed to run.
  goto :fail
)

echo.
echo [2/6] Checking Android SDK environment variables...
set SDK_DIR=
if not "%ANDROID_SDK_ROOT%"=="" set SDK_DIR=%ANDROID_SDK_ROOT%
if "%SDK_DIR%"=="" if not "%ANDROID_HOME%"=="" set SDK_DIR=%ANDROID_HOME%

if "%SDK_DIR%"=="" (
  echo [WARN] ANDROID_SDK_ROOT / ANDROID_HOME not set.
) else (
  echo [OK] Android SDK: %SDK_DIR%
)

echo.
echo [3/6] Checking ADB...
where adb >nul 2>nul
if errorlevel 1 (
  echo [WARN] adb not found in PATH. Device checks may be skipped.
) else (
  adb version
)

echo.
echo [4/6] Ensuring Gradle wrapper...
if exist gradlew.bat (
  echo [OK] gradlew.bat already exists.
) else (
  echo [INFO] gradlew.bat not found. Trying to generate with global Gradle...
  where gradle >nul 2>nul
  if errorlevel 1 (
    echo [ERROR] gradlew.bat not found and global gradle unavailable.
    echo         Please install Gradle or generate wrapper manually.
    goto :fail
  )
  gradle wrapper
  if errorlevel 1 (
    echo [ERROR] Failed generating Gradle wrapper.
    goto :fail
  )
)

echo.
echo [5/6] Running unit tests (debug)...
call gradlew.bat testDebugUnitTest
if errorlevel 1 (
  echo [ERROR] Unit tests failed.
  goto :fail
)

echo.
echo [6/6] Building debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo [ERROR] Debug build failed.
  goto :fail
)

echo.
echo ======================================
echo Setup finished successfully.
echo ======================================
echo Next: open project in Android Studio.
exit /b 0

:fail
echo.
echo Setup failed. See message above.
exit /b 1

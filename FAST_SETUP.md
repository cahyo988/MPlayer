# Fast Setup (Windows)

Panduan cepat untuk menjalankan project **MusicPlayer** di Windows.

## Prasyarat

1. **JDK 17**
2. **Android Studio** (disarankan versi terbaru)
3. **Android SDK** + platform/build-tools sesuai `compileSdk`
4. (Opsional) **Gradle** global, hanya kalau `gradlew.bat` belum ada

## Setup cepat

Jalankan dari root project:

```bat
setup.bat
```

Script akan:
- Cek Java
- Cek Android SDK (`ANDROID_SDK_ROOT` / `ANDROID_HOME`)
- Cek `adb`
- Generate `gradlew.bat` (jika belum ada dan Gradle global tersedia)
- Menjalankan validasi dasar (`testDebugUnitTest` + `assembleDebug`)

## Kalau setup gagal

### 1) `java` tidak ditemukan
- Install JDK 17
- Pastikan `java -version` jalan di terminal baru

### 2) Android SDK belum terdeteksi
- Set salah satu environment variable berikut:
  - `ANDROID_SDK_ROOT`
  - `ANDROID_HOME`

Contoh (PowerShell):

```powershell
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk", "User")
```

### 3) `gradlew.bat` belum ada
- Install Gradle global, lalu jalankan ulang `setup.bat`
- Atau generate manual:

```bat
gradle wrapper
```

## Menjalankan app

Setelah setup sukses:

```bat
gradlew.bat assembleDebug
```

Lalu buka via Android Studio / install APK debug ke device.

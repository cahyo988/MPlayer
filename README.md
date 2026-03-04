# Music Player Android (Local + Google Drive)

Dokumen ini merangkum rencana produk, arsitektur, requirement, dan roadmap implementasi untuk aplikasi pemutar musik Android yang mendukung:

- Play musik lokal (file di HP)
- Play musik dari Google Drive
- Rencana support format tambahan/non-umum seperti **.mpc**

---

## 1) Scope & Fitur (MVP dulu)

### MVP (versi 1 yang rilis cepat)

- Library musik lokal (scan storage) + list lagu
- Play / Pause / Next / Prev
- Seekbar, repeat, shuffle
- Background playback + notifikasi media (MediaSession)
- Playlist sederhana (manual)
- Integrasi Google Drive:
  - Input URL folder public
  - Browse file audio dari folder public
  - Stream audio dari Drive (tanpa download penuh)
  - Cache opsional (hemat kuota & loading lebih cepat)

### Nice-to-have (versi 2+)

- Equalizer, sleep timer
- Lyrics (local file / fetch)
- Smart playlist (artist/genre/most played)
- Download manager untuk offline dari Drive
- Multi-account Drive / Shared Drive
- Chromecast / Bluetooth enhancements
- Waveform / visualizer

---

## 2) Arsitektur yang Disarankan (Android)

### Pilihan teknologi

Stack yang paling praktis:

- **Kotlin + Jetpack Compose** (UI modern)
- **Media3 / ExoPlayer** (playback, streaming, caching, MediaSession)
- **Room** (metadata, playlist, history)
- **WorkManager** (scan library & background sync)
- **Public Google Drive folder URL + parser** (file listing + streaming)

### Struktur modul (rapi & scalable)

- `app` (UI, navigation)
- `player` (service playback, MediaSession)
- `data-local` (scan file, Room, repository)
- `data-drive` (public Drive URL parser + repository)
- `core` (utils, models, common)

---

## 3) Tantangan Penting yang Harus Diputuskan dari Awal

### A) Akses file lokal (Android 11+)

Pendekatan:

1. **MediaStore (recommended)**
   - Lebih aman dari sisi policy
   - Cocok untuk musik yang terdeteksi sebagai media
2. **SAF (Storage Access Framework)**
   - User pilih folder musik secara manual
   - Lebih fleksibel, implementasi lebih kompleks

**Saran:** pakai MediaStore dulu untuk MVP, tambahkan SAF jika dibutuhkan.

### B) Streaming dari Google Drive

Flow umum:

1. User input URL folder public Drive
2. Parse daftar file audio dari folder public
3. Playback:
   - Gunakan endpoint stream/download publik
   - Feed ke ExoPlayer via DataSource
4. Tambahkan cache (`CacheDataSource` di Media3)

### C) Support “mpc”

Jika yang dimaksud adalah **Musepack (.mpc)**, format ini tidak selalu didukung native Android.

Opsi:

- Transcode on-the-fly (kompleks & berat)
- Decode via FFmpeg (fleksibel, APK size naik, perlu cek lisensi)
- Server-side convert (jika ada backend / pre-convert file)

**Saran realistis:**

- MVP: dukung format umum dulu (**mp3, m4a/aac, flac, ogg, wav**)
- `.mpc`: jadikan milestone lanjutan berbasis FFmpeg

---

## 4) Kebutuhan (Requirements)

### Functional requirements

- Scan & tampilkan lagu lokal (title, artist, album, duration)
- Playback controls lengkap
- Background playback + lockscreen controls
- Playlist create/edit/delete
- Google Drive:
  - Input URL folder public
  - Browse file audio dari folder public
  - Play file dari Drive
  - Cache opsional & recently played
- Error handling:
  - File hilang
  - Permission ditolak
  - URL folder invalid/private
  - Jaringan putus

### Non-functional requirements

- Playback smooth (minim stutter), buffering masuk akal
- Offline resilience via cache
- Battery-friendly (hindari scanning berulang)
- Privacy: tidak upload data library user tanpa izin
- Compliance policy Google (permission, data usage disclosure)

---

## 5) UI/UX yang Perlu Disiapkan (Screen List)

- **Home**: tabs `Local | Drive | Playlists | Recents`
- **Local Library**:
  - Songs / Albums / Artists
  - Search
- **Drive Browser**:
  - Folder navigation
  - Filter tipe audio
  - Search
- **Now Playing**:
  - Artwork, title, artist
  - Queue
  - Lyrics (opsional)
- **Playlist**:
  - Daftar playlist + editor
- **Settings**:
  - Cache size
  - Default Drive folder
  - Audio focus / Bluetooth behavior

---

## 6) Plan Pengerjaan (Milestone Realistis)

### Milestone 0 — Setup & Fondasi (1–2 minggu efektif)

- Setup project, Compose, Media3
- Buat `PlaybackService` + `MediaSession` + notification
- Basic Now Playing UI

### Milestone 1 — Local Music MVP

- Scan via MediaStore
- List lagu & play dari list
- Playlist sederhana + Room DB

### Milestone 2 — Google Drive Integration MVP

- Input URL folder public + list file audio
- Browse file audio dari folder public
- Stream playback + handle network error
- Stream cache

### Milestone 3 — Polish & Release Readiness

- Search, sorting, artwork fetching (embedded art)
- Error UX (network, permission)
- QA device matrix (Android 8–14)
- Release build + Play Store compliance

### Milestone 4 — “mpc” Support (Advanced)

- Riset decode Musepack (kemungkinan FFmpeg)
- Integrasi decoding pipeline → ExoPlayer custom renderer/decoder
- Benchmark performa, ukuran APK, legal/licensing check

---

## 7) Checklist Kebutuhan Teknis (Detail)

### Permissions & config

- Android 13+: `READ_MEDIA_AUDIO`
- Android 12 ke bawah: `READ_EXTERNAL_STORAGE` (dengan limitasi)
- Foreground service permission (sesuai target SDK)
- `INTERNET` permission (Drive streaming)

### Library/SDK

- `androidx.media3` (ExoPlayer/MediaSession)
- Parser folder public Drive (tanpa login)
- Parser folder public (HTML)
- (Opsional) OkHttp untuk networking lanjutan
- Room, WorkManager
- (Opsional) Coil untuk artwork loading

### Data model minimal

- `Track(id, title, artist, album, duration, uri, source=LOCAL/DRIVE, driveFileId, mimeType)`
- `Playlist(id, name)`
- `PlaylistTrack(playlistId, trackId, position)`
- `PlayHistory(trackId, lastPlayedAt, playCount)`

---

## 8) Checklist Compliance (Penting untuk Publish)

- Jelaskan di Privacy Policy:
  - Akses media lokal hanya untuk playback/library
  - Akses Drive hanya untuk browse/stream sesuai fitur
- Jangan minta akses akun Google jika fitur memakai folder public URL
- Jangan minta permission yang tidak perlu
- Untuk mode public URL, tidak perlu alur **sign out/revoke token**

---

## Catatan Implementasi

Fokus awal sebaiknya: **fitur inti playback stabil + UX sederhana + compliance aman**. Setelah rilis MVP stabil, lanjutkan ke fitur advanced (equalizer, smart playlist, `.mpc`, dan lainnya).

# MPlayer

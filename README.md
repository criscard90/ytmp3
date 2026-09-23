# ytmp3 🎵

App Android dal **design ispirato a YouTube** (tema scuro, rosso YouTube) per **cercare video su YouTube senza login Google** e **scaricare l'audio in MP3 alla massima qualità** (encoding `libmp3lame` a 320 kbps con metadata ID3).

## Funzionalità

- 🔍 **Ricerca YouTube senza account** tramite l'API pubblica Innertube (nessun login Google richiesto)
- ▶️ **Riproduzione video in-app** (Media3/ExoPlayer, fino a 1080p, video+audio fusi) e **apertura nel link YouTube** (app YouTube o browser)
- 🎧 **Download audio in MP3 320 kbps**: viene selezionato automaticamente lo stream audio di **massima qualità** disponibile (fino a ~130 kbps sorgente AAC/Opus, ri-codificato a 320 kbps)
- 🏷️ **Metadata ID3**: titolo e artista scritti nel file MP3
- 💾 **Salvataggio in `Music/ytmp3`** (visibile in File/Musica tramite MediaStore, niente permessi extra su Android 10+)
- 📱 **UI moderna**: barra di ricerca a pillola, lista risultati con thumbnail/durata/avatar, bottom sheet di conferma, tab *Download* con riproduzione ed eliminazione
- 🔔 **Foreground service + notifica** con progresso di download e conversione, anche a schermo spento
- 📦 **APK per ABI + universale** allegati a ogni Release

## Build

Requisiti: JDK 17, Android SDK (platform 34), Gradle wrapper incluso.

```powershell
./gradlew assembleDebug    # build di sviluppo
./gradlew assembleRelease  # build release (firmata con debug key se keystore.properties assente)
```

### Firmare la release con una chiave propria (opzionale)

Creare `keystore.properties` nella root (gitignored):

```properties
storeFile=path/relativo/a/release.jks
storePassword=***
keyAlias=***
keyPassword=***
```

oppure, su GitHub, impostare i secret `KEYSTORE_B64` (keystore codificato in base64), `KEYSTORE_STORE_PASSWORD`, `KEYSTORE_KEY_ALIAS`, `KEYSTORE_KEY_PASSWORD` per far firmare la CI la release.

## Continuous Delivery (GitHub Actions)

Workflow: [`.github/workflows/release.yml`](.github/workflows/release.yml)

- **Ad ogni push** sulla repo la Action esegue `./gradlew assembleRelease`
- Rinomina gli APK in `ytmp3-<versione>-<abi>.apk` (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `universal`)
- Pubblica/aggiorna una **GitHub Release** `build-<run_number>` con tutti gli APK allegati

## Architettura

```
app/src/main/java/com/criscard90/ytmp3/
├── MainActivity.kt                 # entry point, edge-to-edge, tema Compose
├── network/Http.kt                 # client OkHttp condiviso
├── youtube/Innertube.kt            # API Innertube: search (WEB) + player (ANDROID/ANDROID_VR)
├── youtube/Models.kt               # modelli: VideoSearchItem, AudioStreamInfo
├── download/DownloadManager.kt     # coda download → ffmpeg (MP3) → MediaStore
├── download/DownloadService.kt     # foreground service + notifica progresso
├── downloads/DownloadsRepository.kt# elenco/eliminazione MP3 da MediaStore
├── ui/                             # schermate Compose (Search, Downloads, bottom sheet)
└── util/Format.kt                  # formattazione date/bytes + sanitizzazione nomi file
```

### Flusso di download

1. `youtubei/v1/player` con client `ANDROID` (fallback `ANDROID_VR`) → `streamingData.adaptiveFormats` con URL diretti (nessun login)
2. Download dello stream audio sorgente (m4a/opus) con progresso
3. Conversione con **FFmpeg (`ffmpeg-kit-audio`)** → `-c:a libmp3lame -b:a 320k` + metadata
4. Scrittura su `Music/ytmp3/<titolo>.mp3` via **MediaStore** (`IS_PENDING`)

## Note legali

Il download di contenuti YouTube può violare i Termini di Servizio di YouTube. Questo progetto è pensato per **contenuti di dominio pubblico, Creative Commons o di cui possiedi i diritti**, e per un uso strettamente personale. I marchi citati appartengono ai rispettivi proprietari; l'app non è affiliata a Google/YouTube.

# AI Agent Context and Directives: VaaniSetu (Phase 1)

## Project Identity and Mission
**Project Name:** VaaniSetu
**Hackathon:** Smart India Hackathon (SIH) - VaaniSetu (PS 26173) by ISRO
**Mission:** VaaniSetu is an offline Android walkie-talkie and emergency alert app designed to drastically reduce bandwidth requirements. It converts voice to text using Speech-to-Text (STT), transmits text over peer-to-peer networks (Bluetooth/Wi-Fi Direct), and reconstructs audio on the receiver using Text-to-Speech (TTS), collapsing bandwidth from kilobits/sec to bytes/sec.

## HARD RULES (MUST NEVER VIOLATE)
1. **OPEN SOURCE ONLY:** No proprietary, closed-source, or commercial voice SDKs. No Google Cloud Speech API, AWS Polly, Azure Cognitive Services, etc.
2. **FULLY OFFLINE:** No internet API calls during STT or TTS inference. No server-side processing. The app must function completely without an internet connection.
3. **ANDROID APP ONLY:** No web backend, no REST API, no cloud integration.
4. **API 24+ ONLY:** Must run on API 24+ (Android 7.0+) and support low/mid-range phones.
5. **NO CLOUD SERVICES:** Never add a cloud STT/TTS API call, even as a fallback.
6. **NO FIREBASE:** Never add Firebase (except for emulator UI testing if absolutely necessary; no production use).
7. **NO COMPOSE:** Never use Jetpack Compose. Use traditional Views/XML layouts ONLY.
8. **NO AUDIO ANIMATIONS:** Never add animations that run during active audio listening to preserve battery and processing power.
9. **NO TTS QUEUE FLUSH:** Never use `QUEUE_FLUSH` for TTS as it drops messages. Always use `QUEUE_ADD`.
10. **EMERGENCY ALARM USAGE:** Never call TextToSpeech for emergency audio without setting `AudioAttributes.USAGE_ALARM`.
11. **PERMISSION HANDLING:** Never skip requesting `NEARBY_WIFI_DEVICES` and `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` permissions on Android 12+.
12. **PERSISTENT STORAGE:** Never store message history in memory only. It MUST persist in the Room Database.
13. **NO HARDCODED LANGUAGES:** Never hardcode language codes. Use the defined enum/constants.

## Technology Stack (Phase 1)
- **Language:** Kotlin
- **UI:** XML Layouts, Views, Material Design 3, Dark Theme by default
- **Architecture:** MVVM (ViewModel + LiveData), Repository pattern
- **Local Storage:** Room Database (SQLite) for message history, SharedPreferences for settings
- **STT (Phase 1 Exception):** `android.speech.SpeechRecognizer`
- **TTS (Phase 1 Exception):** `android.speech.tts.TextToSpeech`
- **Networking:** Google Play Services Nearby Connections API (`com.google.android.gms:play-services-nearby`), `STRATEGY_CLUSTER`
- **Background Operations:** Foreground Service for networking/audio when app is backgrounded.
- **Min SDK:** 24, **Target SDK:** 34

## File Structure / Package Structure
Follow this standard MVVM structure:
```
com.VaaniSetu.vaanisetup
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entity
│   │   └── AppDatabase.kt
│   └── repository
├── di
├── network
│   ├── nearby
│   └── payload
├── service
│   └── BackgroundAudioNetworkService.kt
├── ui
│   ├── main
│   ├── chat
│   └── settings
├── utils
│   ├── Constants.kt
│   └── AudioUtils.kt
└── viewmodel
```

## Coding Standards
- Use Kotlin Coroutines and Flow/LiveData for asynchronous tasks.
- Keep UI logic out of ViewModels and Repository.
- Document all public functions, especially networking and audio related code.
- Prefix all resource files correctly (e.g., `activity_main.xml`, `fragment_chat.xml`).
- Use standard naming conventions (CamelCase for classes, camelCase for functions/variables).

## Phase 1 Specific Scope
- **IN SCOPE:** Native Android `SpeechRecognizer` and `TextToSpeech` for prototyping. Nearby Connections API for local networking. Complete UI with MVVM.
- **OUT OF SCOPE:** Custom ONNX models, LoRa hardware integration, custom mesh routing (handled by Nearby Connections in Phase 1).

## Message Payload Format Specification
Format: `[sender_name]|[channel_id]|[lang_code]|[urgency_flag]|[text_content]`
Example Normal: `Amit|global|hi-IN|0|Rasta bandh hai`
Example Emergency: `Priya|global|en-IN|1|Send ambulance immediately`

### Languages Enum (Use EXACTLY these codes):
- HINDI: `hi-IN`
- GUJARATI: `gu-IN`
- MARATHI: `mr-IN`
- KANNADA: `kn-IN`
- MALAYALAM: `ml-IN`
- TAMIL: `ta-IN`
- TELUGU: `te-IN`
- ODIA: `or-IN`
- BENGALI: `bn-IN`
- ENGLISH: `en-IN`

## Channel System Rules
- A `global` channel always exists, and all users auto-join it.
- Channel switching is implemented by changing local filters in the UI/ViewModel; there is NO hardware reconnection required.
- TTS must read messages as: `"[sender_name] says: [text_content]"` in the specified `[lang_code]` language.
- Simultaneous senders result in messages being queued via `QUEUE_ADD`. NEVER use `QUEUE_FLUSH`.

## Audio Behavior Rules
- **Normal Messages (`urgency_flag=0`):** Normal TTS volume and routing.
- **Emergency Messages (`urgency_flag=1`):** Must use `AudioAttributes.USAGE_ALARM`. Must set stream volume to max via `setStreamVolume(STREAM_ALARM, max, 0)`. Must bypass Do Not Disturb (DND) settings.

## Testing Checklist
Before marking a task as complete, verify:
- [ ] App compiles and runs without internet.
- [ ] No cloud SDKs are included in `build.gradle`.
- [ ] Message payload perfectly matches the pipe-delimited format.
- [ ] Emergency messages trigger Alarm stream at maximum volume.
- [ ] Simultaneous messages are queued and read sequentially (no dropped messages).
- [ ] Message history is saved to Room DB and survives app restart.
- [ ] No Compose dependencies in the project.

# AI Agent Context and Directives: VaaniSetu (Phase 2 - Grand Finale)

## Project Identity and Mission
**Project Name:** VaaniSetu
**Hackathon:** Smart India Hackathon (SIH) - VaaniSetu (PS 26173) by ISRO
**Mission:** VaaniSetu is an offline Android walkie-talkie and emergency alert app designed to drastically reduce bandwidth requirements. It converts voice to text, transmits text over peer-to-peer networks (Wi-Fi Direct/Bluetooth/LoRa), and reconstructs audio on the receiver using fully offline AI models, collapsing bandwidth from kilobits/sec to bytes/sec.

## HARD RULES (MUST NEVER VIOLATE)
1. **OPEN SOURCE ONLY:** No proprietary, closed-source, or commercial voice SDKs. No Google Cloud Speech API, AWS Polly, Azure Cognitive Services, etc.
2. **FULLY OFFLINE:** No internet API calls during STT or TTS inference. No server-side processing. The app must function completely without an internet connection.
3. **ANDROID APP ONLY:** No web backend, no REST API, no cloud integration.
4. **API 24+ ONLY:** Must run on API 24+ (Android 7.0+) and support low/mid-range phones.
5. **NO CLOUD SERVICES:** Never add a cloud STT/TTS API call, even as a fallback.
6. **NO FIREBASE:** Never add Firebase.
7. **NO COMPOSE:** Never use Jetpack Compose. Use traditional Views/XML layouts ONLY.
8. **NO AUDIO ANIMATIONS:** Never add animations that run during active audio listening to preserve battery and processing power.
9. **NO TTS QUEUE FLUSH:** Never use `QUEUE_FLUSH` for TTS as it drops messages. Always queue incoming messages sequentially.
10. **EMERGENCY ALARM USAGE:** Emergency audio must use `AudioAttributes.USAGE_ALARM` and bypass DND.
11. **PERMISSION HANDLING:** Ensure `NEARBY_WIFI_DEVICES`, Bluetooth, and Location permissions are properly requested.
12. **PERSISTENT STORAGE:** Message history MUST persist in the Room Database.
13. **NO HARDCODED LANGUAGES:** Use the defined enum/constants.
14. **PHASE 2 MODEL RULE:** MUST use `sherpa-onnx`. NEVER use native `SpeechRecognizer` or `TextToSpeech` for Phase 2.
15. **PERFORMANCE:** Model inference (STT/TTS) must complete in <500ms on a mid-range device.

## Technology Stack (Phase 2)
- **Language:** Kotlin + C++ (JNI/NDK)
- **UI:** XML Layouts, Views, Material Design 3, Dark Theme by default
- **Architecture:** MVVM, Repository pattern
- **Local Storage:** Room Database (SQLite) for message history, SharedPreferences for settings
- **STT (Phase 2):** AI4Bharat IndicConformer via `sherpa-onnx` ONNX Runtime Mobile
- **TTS (Phase 2):** AI4Bharat VITS Rasa 13 via `sherpa-onnx` ONNX Runtime Mobile
- **Networking:** `WifiP2pManager` (Wi-Fi Direct) + `BluetoothSocket` RFCOMM + LoRa ESP32 integration
- **Background Operations:** Foreground Service for networking/audio when app is backgrounded.
- **Min SDK:** 24, **Target SDK:** 34

## File Structure / Package Structure
Follow standard MVVM structure similar to Phase 1, but include JNI/C++ directories:
```
com.VaaniSetu.vaanisetup
├── cpp
│   ├── CMakeLists.txt
│   └── onnx_inference.cpp
├── data
│   └── ...
├── di
├── network
│   ├── mesh
│   └── lora
├── service
│   └── BackgroundAudioNetworkService.kt
├── ui
├── utils
│   ├── Constants.kt
│   └── AudioUtils.kt
└── viewmodel
```

## Phase 2 Model Integration Rules
- Models must be loaded from the Android `assets` folder.
- Ensure strict memory management. Pre-load models into memory during app startup (Foreground Service) and keep them loaded to avoid high initialization latency on every message.
- Release C++ pointers appropriately when the service is destroyed to prevent memory leaks.

## NDK/JNI Rules and Warnings
- Ensure ABI compatibility (`armeabi-v7a`, `arm64-v8a`).
- Catch all C++ exceptions at the JNI boundary to prevent the JVM from crashing.
- Pass byte arrays efficiently between Java and C++ using `DirectByteBuffer`s or `GetByteArrayElements` with appropriate release calls.

## LoRa Hardware Communication Rules
- The Android app connects to the ESP32 LoRa module via Bluetooth RFCOMM or USB Serial.
- Packets sent to the LoRa module must strictly follow the defined Message Payload Format.
- Ensure backpressure handling if LoRa transmission speed is slower than message generation.

## Mesh Routing Rules
- Custom Wi-Fi Direct / Bluetooth routing requires a mesh mechanism.
- **TTL (Time to Live):** Set a maximum TTL (e.g., 3-5 hops) for broadcast messages to prevent infinite flooding.
- **Loop Prevention:** Maintain a cache of recently seen Message IDs (hash of payload + timestamp) in Room DB/Memory to drop duplicate incoming messages and prevent re-broadcasting.

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
- Channel switching is implemented by changing local filters in the UI/ViewModel; NO hardware reconnection required.
- TTS must read messages as: `"[sender_name] says: [text_content]"` in the specified `[lang_code]` language.
- Simultaneous senders result in messages being queued sequentially. NEVER flush the queue.

## Audio Behavior Rules
- **Normal Messages (`urgency_flag=0`):** Normal TTS volume and routing.
- **Emergency Messages (`urgency_flag=1`):** Must use `AudioAttributes.USAGE_ALARM`. Must set stream volume to max via `setStreamVolume(STREAM_ALARM, max, 0)`. Must bypass Do Not Disturb (DND) settings.

## Grand Finale Demo Preparation Checklist
- [ ] Verify complete removal of native `SpeechRecognizer` and `TextToSpeech`.
- [ ] Verify `sherpa-onnx` STT and TTS work completely in Airplane mode.
- [ ] Measure STT/TTS latency (must be <500ms).
- [ ] Confirm LoRa ESP32 integration successfully sends and receives the exact payload format.
- [ ] Test multi-hop mesh routing with at least 3 devices; confirm TTL and loop prevention work.
- [ ] Trigger an emergency message and ensure it overrides system volume/DND on receiving devices.
- [ ] Check memory usage during continuous TTS synthesis to ensure no JNI memory leaks.

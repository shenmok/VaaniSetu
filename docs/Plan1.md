# VaaniSetu (PS 26173) - VaaniSetu Phase 1 Implementation Plan

**Goal:** Complete the Phase 1 MVP using Native Android APIs and Google Nearby Connections.

## Phase 1.1: Project Setup & Core Architecture
**Dependencies:** None
- [x] Initialize Android Studio project (Kotlin, Empty Views Activity).
- [x] Add dependencies to `app/build.gradle.kts`:
  - `implementation("com.google.android.gms:play-services-nearby:19.0.0")`
  - `implementation("androidx.room:room-runtime:2.6.1")`
  - `ksp("androidx.room:room-compiler:2.6.1")`
  - `implementation("androidx.room:room-ktx:2.6.1")`
  - Material Design 3 dependencies.
- [x] Update `AndroidManifest.xml` with permissions:
  - `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`
  - `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES`
  - `RECORD_AUDIO`
- [x] Set up Room Database:
  - Create `MessageEntity.kt` (id, channelName, sender, text, timestamp).
  - Create `MessageDao.kt` (insert, getByChannel).
  - Create `AppDatabase.kt`.
- [x] Configure `strings.xml` for English, Hindi (`values-hi/strings.xml`), and Marathi (`values-mr/strings.xml`).

**Acceptance Criteria:** App builds successfully, permissions are declared, and Room DB compiles.

## Phase 1.2: Onboarding & Permissions
**Dependencies:** Phase 1.1
- [x] Create `OnboardingActivity.kt` and `activity_onboarding.xml`.
- [x] Implement system language detection to set default app locale.
- [x] Build runtime permission request screen (Microphone, Location, Nearby Devices).
- [x] Build Profile setup screen (User Name input).
- [x] Build Language selection screen (English, Hindi, Marathi).
- [x] Save preferences using `SharedPreferencesManager.kt`.
- [x] Route to `MainActivity` if onboarding is complete.

**Acceptance Criteria:** App launches Onboarding on first run, asks for permissions, saves Name/Lang, then boots to Main. Subsequent launches bypass Onboarding.

## Phase 1.3: Networking (Nearby Connections)
**Dependencies:** Phase 1.1
- [x] Create `NearbyConnectionsManager.kt`.
- [x] Implement `startAdvertising()` and `startDiscovery()` using P2P_CLUSTER strategy.
- [x] Handle `ConnectionLifecycleCallback` (auto-accept connections).
- [x] Handle `PayloadCallback` for receiving JSON messages (sender name, channel, message type, payload text).
- [x] Implement `sendMessage(payload)` to all connected endpoints.
- [x] Expose connected peers count via `LiveData` or `StateFlow`.

**Acceptance Criteria:** Two devices can discover each other, connect automatically, and exchange text payloads. Connected peer count updates accurately.

## Phase 1.4: STT & TTS Integration
**Dependencies:** Phase 1.1
- [x] Create `SpeechManager.kt`.
- [x] Initialize Android native `SpeechRecognizer` with selected locale.
- [x] Implement `RecognitionListener` to capture text results.
- [x] Initialize Android native `TextToSpeech` with selected locale.
- [x] Implement `speak(text)` prefixing with "[Sender Name] says:".
- [x] Handle stealth mode logic (check proximity sensor `Sensor.TYPE_PROXIMITY` in `MainActivity`). If near, suppress TTS and trigger haptic feedback.

**Acceptance Criteria:** App can convert voice to text and text to voice in the selected language. TTS respects stealth mode.

## Phase 1.5: Main UI & Channel System
**Dependencies:** Phase 1.2, Phase 1.3, Phase 1.4
- [x] Create `MainActivity.kt` and `activity_main.xml` using Material 3 Views.
- [x] Build Channel Selector UI (Spinner or Tabs) + "Create Channel" dialog.
- [x] Implement Channel switching logic (filter incoming/outgoing messages by channel tag).
- [x] Build Message History RecyclerView with `MessageAdapter.kt`.
- [x] Integrate `Room DB` to load/save messages for the active channel.
- [x] Implement Speaking Indicator UI (pulsing mic icon when `SpeechRecognizer` is active or TTS is playing).

**Acceptance Criteria:** Users can create channels, switch channels, and view channel-specific message history.

## Phase 1.6: App Modes (Phone, PTT, Emergency)
**Dependencies:** Phase 1.5
- [x] Implement Phone Mode (Full-duplex UI toggle, continuous STT listening).
- [x] Implement PTT Mode (Half-duplex, Hold-to-talk button, starts STT on touch down, stops on touch up).
- [x] Implement Emergency Alert Mode (Override button).
  - Sends high-priority payload.
  - Receiver ignores stealth/mute, plays alert sound at Max Volume.
  - UI flashes red.

**Acceptance Criteria:** User can toggle between Phone and PTT modes. Emergency button overrides receiver settings.

## Phase 1.7: Settings & Polish
**Dependencies:** Phase 1.6
- [ ] Create `SettingsActivity.kt` and `activity_settings.xml`.
- [ ] Add toggles for Alert Preference (Max Volume / Vibrate / Muted), User Name, and Language.
- [x] Ensure all hardcoded strings are moved to `strings.xml`.

**Acceptance Criteria:** Settings persist and affect app behavior immediately.

## Phase 1.8: Demo Video Preparation
**Dependencies:** Phases 1.1 - 1.7
- [ ] Write demo script covering: Onboarding, PTT, Offline translation (simulated via native APIs), Emergency Alert, Stealth Mode.
- [ ] Prepare 2 physical Android devices for recording.
- [ ] Record 3-minute video showing real-time offline communication.
- [ ] Edit video and compress for submission.

**Acceptance Criteria:** High-quality demo video ready for upload.

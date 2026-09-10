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
- [x] Route to `HomeActivity` if onboarding is complete.
- [x] Support edit mode (profile icon → re-edit name/language).
- [x] Re-request missing permissions even after onboarding.

**Acceptance Criteria:** App launches Onboarding on first run, asks for permissions, saves Name/Lang, then boots to Home. Subsequent launches bypass Onboarding. Profile can be re-edited.

## Phase 1.3: Networking (Nearby Connections)
**Dependencies:** Phase 1.1
- [x] Create `NearbyConnectionsManager.kt`.
- [x] Implement `startAdvertising()` and `startDiscovery()` using P2P_CLUSTER strategy.
- [x] Handle `ConnectionLifecycleCallback` (auto-accept connections).
- [x] Handle `PayloadCallback` for receiving messages (sender name, channel, message type, payload text).
- [x] Implement `sendMessage(payload)` to all connected endpoints.
- [x] Expose connected peers count via `StateFlow`.
- [x] Track peer display names from handshake (ConnectionInfo.endpointName).

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

## Phase 1.5: Home Screen & Connected Devices
**Dependencies:** Phase 1.2, Phase 1.3
- [x] Create `HomeActivity.kt` and `activity_home.xml` as new launcher.
- [x] Build Connected Peers RecyclerView with `PeerAdapter.kt`.
- [x] Add Call button per peer (launches DuplexCallActivity).
- [x] Add PTT Channel Mode button (launches MainActivity).
- [x] Add single ALERT button (sends emergency payload + overlay).
- [x] Add connectivity indicators (BT/WiFi icons).
- [x] Add profile icon (opens OnboardingActivity in edit mode).
- [x] Add language dropdown on Home screen.

**Acceptance Criteria:** Home screen shows connected peers, connectivity status, and provides clear navigation to all modes.

## Phase 1.6: Full Duplex Call
**Dependencies:** Phase 1.4, Phase 1.5
- [x] Create `DuplexCallActivity.kt` and `activity_duplex_call.xml`.
- [x] Implement continuous STT loop (auto-restart on result/error).
- [x] Implement echo suppression (pause STT while TTS speaks).
- [x] Show live transcript (sender, text, timestamp).
- [x] TTS reads incoming as "[Name] says: [text]" using `QUEUE_ADD`.

**Acceptance Criteria:** Two-way voice-to-text-to-voice call with live transcript and no echo loops.

## Phase 1.7: PTT Channel Mode
**Dependencies:** Phase 1.5
- [x] Refactor `MainActivity.kt` for PTT-only scope.
- [x] Channel Selector UI (Tabs) + "Create Channel" dialog.
- [x] Channel switching logic (filter by channel tag).
- [x] Message History RecyclerView.
- [x] Integrate Room DB to persist messages.
- [x] PTT button with haptic feedback (11ms + 43ms two-pulse).
- [x] Speaking Indicator (size pulse 8dp→11dp).
- [x] All messages read as "[Name] says: [text]" via TTS.

**Acceptance Criteria:** Users can create channels, switch channels, hold-to-talk, and hear messages spoken aloud.

## Phase 1.8: Emergency Alert System
**Dependencies:** Phase 1.5
- [x] Single ALERT button on Home screen.
- [x] Emergency overlay (solid #B71C1C, 31sp bold white text).
- [x] `USAGE_ALARM` + max volume to bypass DND.
- [x] Incoming `urgency_flag=1` triggers overlay + max volume TTS automatically.
- [x] Overlay dismissible by tap.
- [x] All strings localized (EN/HI/MR).
- [x] All hardcoded strings moved to `strings.xml`.

**Acceptance Criteria:** Emergency alert sends and receives at max volume, bypasses DND, and shows full-screen red overlay.

## Phase 1.9: Demo Video Preparation
**Dependencies:** Phases 1.1 - 1.8
- [ ] Write demo script covering: Onboarding, PTT, Offline translation (simulated via native APIs), Emergency Alert, Stealth Mode.
- [ ] Prepare 2 physical Android devices for recording.
- [ ] Record 3-minute video showing real-time offline communication.
- [ ] Edit video and compress for submission.

**Acceptance Criteria:** High-quality demo video ready for upload.

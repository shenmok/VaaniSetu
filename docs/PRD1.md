# Product Requirements Document: VaaniSetu (Phase 1 MVP)

## 1. Project Overview & PS Context
**Project Name:** VaaniSetu (VaaniSetu)
**Team:** AlomVilom
**Problem Statement:** PS 26173 (Smart India Hackathon, ISRO)
**Context:** In disaster scenarios or remote areas lacking internet/cellular connectivity, communication is critical. Traditional walkie-talkies rely on heavy audio transmission which limits range, congests bandwidth, and lacks multilingual support. 
**Solution:** VaaniSetu is a native Android application operating as a multilingual walkie-talkie and emergency alert system using offline AI (Speech-to-Text and Text-to-Speech) over Bluetooth/Wi-Fi Direct peer-to-peer connections. By transmitting text payloads instead of audio, bandwidth requirements are reduced by ~1000x.

## 2. Problem Being Solved
- **Lack of Connectivity:** No internet, cellular networks, or cloud access in disaster zones.
- **Bandwidth Constraints:** Sending raw audio over low-bandwidth peer-to-peer connections (Bluetooth/Wi-Fi Direct) causes dropouts and congestion.
- **Language Barriers:** First responders and locals often speak different languages.
- **Hardware Limitations:** Dedicated radio hardware is expensive and not widely available to civilians; everyone has a smartphone.

## 3. Target Users & Personas
- **Rescue Workers (e.g., NDRF, Local Police):** Require reliable, low-latency communication to coordinate efforts across teams (Channels). 
- **Civilians in Distress:** Need an intuitive, universally accessible way to broadcast emergencies regardless of language.
- **Elderly Users:** Require simple interfaces (icon-first, minimal text) and loud, non-interruptible alerts.
- **Illiterate Users:** Depend on icon-first UI, voice inputs, and audio feedback rather than reading text.

## 4. Core MVP Features & Acceptance Criteria

### 4.1 Onboarding & Setup
- **Feature:** Fast, accessible initial setup.
- **Acceptance Criteria:**
  - System language detection is applied on startup.
  - App requests necessary permissions (Microphone, Nearby Devices, Location).
  - User can set their display name by typing or speaking.
  - User can select their preferred language from the 10 supported Indian languages.

### 4.2 Three Operating Modes
- **Normal Phone Mode (PTT OFF):** Full-duplex communication.
  - *AC:* App transmits text continuously without holding a button. UI is Blue.
- **Push-to-Talk (PTT ON):** Half-duplex walkie-talkie.
  - *AC:* User holds a prominent on-screen button to speak, releases to send/listen. UI is Orange.
- **Emergency Alert Mode:** Distress broadcast.
  - *AC:* Triggering specific distress phrases forces receiver devices to play maximum volume, non-interruptible TTS alerts. UI is Red.

### 4.3 Channel System
- **Feature:** Organized communication streams.
- **Acceptance Criteria:**
  - Users join a 'Global' channel by default.
  - Users can create named channels.
  - Channels appear as tabs/lists.
  - Channel switching is instant via text packet tags (no network reconnection).
  - A pulsing green 'speaking' indicator appears on active channels.
  - Local text transcript history is saved per channel using SQLite (Room DB).
  - **Garbage Collection:** Custom channels are automatically deleted and their history purged after 5 minutes of total peer inactivity to maintain tactical focus.
  - Simultaneous messages are queued and read sequentially (e.g., "Amit says: [message]").

### 4.4 Multilingual Support
- **Feature:** Cross-language translation using native APIs.
- **Acceptance Criteria:**
  - Supports 10 languages: hi-IN, gu-IN, mr-IN, kn-IN, ml-IN, ta-IN, te-IN, or-IN, bn-IN, en-IN.
  - Language dropdown is always visible.
  - Sender language tag travels with the packet; receiver's device translates/reads in the receiver's chosen language.
  - UI Localization for English, Hindi, and Marathi.

### 4.5 Stealth & Settings
- **Feature:** Safety features and user preferences.
- **Acceptance Criteria:**
  - Settings screen allows modification of Alert mode (Max Volume, Vibrate Only, Muted), Name, and Language.
  - **Stealth Mode:** App detects phone face-down/in-pocket via proximity sensor and switches to haptic vibration instead of audio.
  - UI uses no animations during active listening to save CPU.

## 5. User Stories
1. **As a rescue worker**, I want to create a "Medical Team" channel so that I can coordinate specific tasks without cluttering the Global channel.
2. **As a civilian**, I want to hold a large button to talk (PTT) so that I don't accidentally broadcast background noise.
3. **As an elderly user**, I want incoming emergency alerts to bypass my phone's silent mode and play at maximum volume so I don't miss critical warnings.
4. **As an illiterate user**, I want to navigate the app using recognizable icons and color codes (Blue, Orange, Red) so I don't have to read text.
5. **As a user in a hostile environment**, I want the app to switch to vibration when placed in my pocket so that I am not given away by loud TTS readouts.
6. **As a Malayalam speaker**, I want to receive messages from a Hindi speaker in Malayalam audio so that we can communicate seamlessly.
7. **As a first responder**, I want to see a pulsing indicator on channel tabs so I know which team is actively transmitting information.
8. **As a user in a noisy environment**, I want multiple incoming messages to queue up and announce the sender's name so that communication doesn't turn into a jumbled noise.

## 6. Non-Functional Requirements
- **Performance:** App must not crash under continuous use.
- **Device Target:** Minimum Android API 24 (Nougat). Must run on low-to-mid-range devices with at least 2GB RAM.
- **Latency:** End-to-end latency (Speech -> Text -> Network -> TTS) should be under 2.5 seconds on average.
- **Battery/CPU:** Minimal CPU usage during idle/listening states (achieved via disabling animations).

## 7. Out of Scope for Phase 1
- Custom AI models (using native Android APIs for now).
- Hardware integrations (e.g., LoRa modules).
- Multi-hop mesh networking routing.
- Full UI localization for all 10 languages (only EN, HI, MR for MVP).
- Advanced Voice Activity Detection (VAD) algorithms.

## 8. PS Compliance Checklist
- [x] Fully offline, no cloud/internet requirement.
- [x] Transmits text to save bandwidth (not raw audio).
- [x] Supports specified languages.
- [x] Runs on low/mid-range Android devices.
- [x] Open-source libraries only (Google Nearby Connections, Native APIs).
- [x] Debug Utilities: Includes a "Simulate Peer" mode allowing developers to test multi-peer environments, queueing, and DND emergency bypasses on a single emulator.

## 9. Success Metrics
- Successful P2P connection established within 5 seconds.
- 100% of text packets successfully received over a distance of 10 meters.
- App effectively queues and processes 3 simultaneous messages without crashing.
- Emergency alert triggers bypass system volume settings 100% of the time.

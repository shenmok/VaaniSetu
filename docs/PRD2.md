# Product Requirements Document: VaaniSetu (Phase 2 Grand Finale)

## 1. Project Overview & PS Context
**Project Name:** VaaniSetu (VaaniSetu)
**Team:** AlomVilom
**Problem Statement:** PS 26173 (Smart India Hackathon, ISRO)
**Context:** In disaster scenarios or remote areas lacking internet/cellular connectivity, communication is critical. Traditional walkie-talkies rely on heavy audio transmission which limits range, congests bandwidth, and lacks multilingual support. 
**Solution:** VaaniSetu is a native Android application operating as a multilingual walkie-talkie and emergency alert system using specialized offline AI (Speech-to-Text and Text-to-Speech) over advanced mesh networks and LoRa. By transmitting tiny text payloads or semantic templates instead of audio, bandwidth is reduced by ~1000x, allowing extended-range communication over hardware interfaces.

## 2. Problem Being Solved
- **Lack of Connectivity:** No internet, cellular networks, or cloud access in disaster zones.
- **Bandwidth Constraints:** Sending raw audio over low-bandwidth peer-to-peer connections causes dropouts. LoRa hardware cannot support raw audio streams.
- **Language Barriers:** First responders and locals often speak different languages. Native offline APIs are often inaccurate for regional dialects.
- **Hardware Limitations:** Dedicated radio hardware is expensive; leveraging smartphones paired with cheap LoRa nodes democratizes access.

## 3. Target Users & Personas
- **Rescue Workers (e.g., NDRF, Local Police):** Require reliable, low-latency communication across wide areas (10-20km) to coordinate efforts. 
- **Civilians in Distress:** Need an intuitive, universally accessible way to broadcast emergencies.
- **Elderly Users:** Require simple interfaces and loud, non-interruptible alerts.
- **Illiterate Users:** Depend on icon-first UI, voice inputs, and high-accuracy native dialect AI audio feedback.

## 4. Core Features & Acceptance Criteria (Phase 2)

### 4.1 Onboarding & Setup
- **Feature:** Fast, accessible initial setup.
- **Acceptance Criteria:**
  - System language detection is applied on startup.
  - App requests necessary permissions.
  - User can set their display name by typing or speaking.
  - Full UI Localization across all 10 supported Indian languages.

### 4.2 Three Operating Modes
- **Normal Phone Mode (PTT OFF):** Full-duplex communication over P2P/Mesh.
- **Push-to-Talk (PTT ON):** Half-duplex walkie-talkie, robust VAD (Voice Activity Detection).
  - *AC:* Two-stage VAD (energy threshold + Silero VAD) ensures silence isn't processed.
- **Emergency Alert Mode:** Distress broadcast.
  - *AC:* Uses semantic template matching for distress phrases. Sends 2-3 byte `template_id` over the network rather than full text, ensuring instant transmission over LoRa even at ultra-low bitrates.

### 4.3 Channel System
- **Feature:** Organized communication streams.
- **Acceptance Criteria:**
  - Users join a 'Global' channel by default. Custom named channels supported.
  - Channel switching is instant via text packet tags.
  - A pulsing green 'speaking' indicator appears on active channels.
  - Local text transcript history is saved per channel.
  - Simultaneous messages are queued and read sequentially (e.g., "Amit says: [message]").

### 4.4 Advanced Offline AI (Indic STT & TTS)
- **Feature:** High-accuracy, entirely offline translation using specialized Indic models.
- **Acceptance Criteria:**
  - **STT:** Integrates AI4Bharat IndicConformer (120M params, quantized INT8 ONNX, ~120MB per language).
  - **TTS:** Integrates AI4Bharat VITS Rasa 13 (quantized INT8 ONNX, ~40MB).
  - **Runtime:** Built upon `sherpa-onnx` (Apache 2.0, Android AAR/JNI).
  - Supports 10 Indian languages (hi, gu, mr, kn, ml, ta, te, or, bn, en) with tag-based recipient synthesis.

### 4.5 Advanced Networking (Mesh & LoRa)
- **Feature:** Long-range, resilient communications.
- **Acceptance Criteria:**
  - **P2P:** Raw Wi-Fi Direct (WifiP2pManager) + Bluetooth Classic SPP for local device-to-device routing.
  - **Mesh Networking:** Implements multi-hop text packet routing to bounce messages across connected peers.
  - **Hardware Integration:** Communicates with ESP32-LoRa modules via Bluetooth to broadcast text payloads up to 10-20km.

### 4.6 Stealth & Settings
- **Feature:** Safety features and user preferences.
- **Acceptance Criteria:**
  - Settings screen allows modification of Alert mode, Name, and Language.
  - **Stealth Mode:** Uses proximity sensor to switch to haptic vibration instead of audio.
  - No animations during active listening to minimize CPU usage.

## 5. User Stories
1. **As a rescue worker**, I want to create a "Medical Team" channel so that I can coordinate specific tasks without cluttering the Global channel.
2. **As a civilian**, I want to hold a large button to talk (PTT) so that I don't accidentally broadcast background noise.
3. **As an elderly user**, I want incoming emergency alerts to bypass my phone's silent mode and play at maximum volume.
4. **As an illiterate user**, I want to navigate the app using recognizable icons and color codes (Blue, Orange, Red).
5. **As a user in a hostile environment**, I want the app to switch to vibration when placed in my pocket.
6. **As a Malayalam speaker**, I want to receive messages from a Hindi speaker in Malayalam audio.
7. **As a rescue team leader**, I want our communications to securely bridge across a 15km gap using ESP32-LoRa modules attached to our phones.
8. **As a disaster victim**, I want my "Help me" phrase to match a semantic template so it can broadcast as a 2-byte SOS instantly over a congested mesh network.
9. **As a user in a completely dead zone without LoRa**, I want my message to hop through three other civilians' phones (Mesh Networking) to reach a responder 500 meters away.
10. **As a rural user**, I want the AI to accurately detect my regional dialect using AI4Bharat IndicConformer rather than generic native APIs.

## 6. Non-Functional Requirements
- **Performance:** App must handle continuous operation with optimized battery drain.
- **Device Target:** Minimum Android API 24. Low/mid-range devices must run INT8 ONNX models within memory limits (target max RAM usage: ~300MB).
- **Accuracy:** AI4Bharat STT WER (Word Error Rate) & TTS quality must exceed base Android native API benchmarks (comprises 40% of PS score).
- **Efficiency:** STT models must be quantized (INT8) to achieve small footprint and low CPU idle. CPU idle and RAM utilization comprise 20% of PS score.
- **Latency:** End-to-end delta (STT completion, transmission, TTS start) must be highly optimized (comprises 20% of PS score). Target < 1.5 seconds.

## 7. Data Requirements
- **Model Files:** AI4Bharat IndicConformer (STT, ~120MB per language), AI4Bharat VITS Rasa 13 (TTS, ~40MB), Silero VAD ONNX.
- **Dictionaries:** Semantic template dictionary mapping critical distress/rescue phrases to 1-3 byte hex codes.

## 8. Integration Requirements
- **LoRa Hardware Spec:** App must pair via Bluetooth Classic to an ESP32-based LoRa module. 
- **Protocol:** Serial Port Profile (SPP) handling byte streams; app sends `<Tag><SenderLang><Text/TemplateID>` payloads.
- **sherpa-onnx:** JNI integration for seamless C++ based inference execution on Android hardware.

## 9. PS Compliance Checklist
- [x] Fully offline, no cloud/internet requirement.
- [x] Transmits text to save bandwidth (not raw audio).
- [x] Supports specified 10 languages fully in STT, TTS, and UI.
- [x] Runs efficiently on low/mid-range Android devices.
- [x] Open-source only (`sherpa-onnx`, AI4Bharat models under permissible licenses).
- [x] Meets high benchmarks for Accuracy, Efficiency, and Latency as graded by ISRO.

## 10. Success Metrics
- Successful P2P and Mesh connections bridging at least 3 hops.
- Seamless BLE transmission to LoRa hardware with confirmed long-range packet delivery.
- STT processes native dialects with < 15% WER on custom datasets.
- Emergency templates delivered over LoRa in < 50ms network flight time.

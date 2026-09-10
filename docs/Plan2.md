# VaaniSetu (PS 26173) - VaaniSetu Phase 2 Grand Finale Implementation Plan

**Goal:** Upgrade the Phase 1 MVP to a fully decentralized, custom AI-powered mesh network application using offline ONNX models and low-level networking.

## UPGRADE PATH: Phase 1 vs Phase 2
* **Kept:** `MainActivity` UI layout, Room Database (`MessageEntity`, `MessageDao`), Onboarding Flow UI, Settings UI.
* **Replaced:** 
  * `NearbyConnectionsManager.kt` -> Replaced by `MeshNetworkManager.kt` (WifiP2pManager + BluetoothSocket + LoRa).
  * `SpeechManager.kt` -> Replaced by `SherpaOnnxManager.kt` (IndicConformer + VITS).
  * Native TTS/STT -> Replaced by `sherpa-onnx` JNI bindings.
* **Added:** Hardware integration (ESP32 LoRa), VAD pipeline, Semantic compression, Mesh routing, 7 new languages.

## Baseline: Phase 1 Completion Status
- [x] Phase 1.1: Project Setup & Core Architecture
- [x] Phase 1.2: Onboarding Flow
- [x] Phase 1.3: Networking (Nearby Connections)
- [x] Phase 1.4: STT & TTS Integration
- [x] Phase 1.5: Main UI & Channel System
- [x] Phase 1.6: App Modes (Phone, PTT, Emergency)
- [x] Phase 1.7: Settings & Polish
- [x] Phase 1.8: Demo Video Preparation

## Phase 2.1: Model Setup & ONNX Conversion
- [ ] Download AI4Bharat IndicConformer (STT) and VITS Rasa 13 (TTS) models.
- [ ] Quantize models to INT8 using ONNX Runtime tools for mobile optimization.
- [ ] Export models to `.onnx` format and place in Android `assets/` folder.
- [ ] Generate required `tokens.txt`, `lexicon.txt`, and configuration files for sherpa-onnx.

## Phase 2.2: sherpa-onnx Integration (STT/TTS)
- [ ] Add `sherpa-onnx` dependency or compile AAR.
- [ ] Create `SherpaOnnxManager.kt` to replace `SpeechManager.kt`.
- [ ] Implement Offline STT: Initialize `OfflineRecognizer` with IndicConformer assets.
- [ ] Implement Offline TTS: Initialize `OfflineTts` with VITS assets.
- [ ] Integrate Silero VAD (Voice Activity Detection):
  - Create two-stage pipeline: Silero VAD filters silence -> sherpa-onnx processes speech chunks.
- [ ] Wire `SherpaOnnxManager` into MainActivity PTT and Phone modes.

## Phase 2.3: Semantic Compression & FEC
- [ ] Create `PayloadCompressor.kt`.
- [ ] Implement Semantic Template Matcher (e.g., map "I need help at location X" to a 2-byte code + coordinate payload).
- [ ] Integrate Brotli compression for non-templated text payloads.
- [ ] Implement Reed-Solomon Forward Error Correction (FEC) to encode payload bytes before transmission.

## Phase 2.4: Mesh Networking (Raw Wi-Fi Direct + BLE)
- [ ] Remove `NearbyConnectionsManager.kt`.
- [ ] Create `MeshNetworkManager.kt`.
- [ ] Implement Wi-Fi Direct using `WifiP2pManager` for high-bandwidth links.
- [ ] Implement Bluetooth Classic / BLE using `BluetoothSocket` as a fallback layer.
- [ ] Implement multi-hop mesh routing table (A -> B -> C packet forwarding).
- [ ] Handle packet deduplication and TTL (Time-To-Live).

## Phase 2.5: LoRa Hardware Integration
- [ ] Program ESP32 with LoRa SX1278 module to act as a serial Bluetooth bridge.
- [ ] Update `MeshNetworkManager.kt` to connect to ESP32 via Bluetooth Serial (RFCOMM).
- [ ] Implement extremely low-bandwidth routing protocol for LoRa (using semantic templates only).
- [ ] Expose LoRa link status to the UI.

## Phase 2.6: Expanded Localization (10 Languages)
- [ ] Update `strings.xml` and UI to support all 10 target languages:
  - hi-IN, gu-IN, mr-IN, kn-IN, ml-IN, ta-IN, te-IN, or-IN, bn-IN, en-IN.
- [ ] Ensure ONNX models cover the acoustic/token space for these 10 languages.

## Phase 2.7: Performance Benchmarking & Testing
- [ ] Conduct 2-phone physical tests without routers.
- [ ] Benchmark STT/TTS latency (Target: < 500ms).
- [ ] Benchmark Mesh recovery time (Target: < 2s for node drop).
- [ ] Test LoRa range in physical environment.
- [ ] Verify battery consumption profile.

## Phase 2.8: Finale Presentation & Demo
- [ ] Prepare Grand Finale PPT focusing on architectural shift from Phase 1.
- [ ] Rehearse live demo showing seamless fallback: Wi-Fi -> BLE -> LoRa.
- [ ] Create hardware connection diagram for the judges.
- [ ] Finalize "How it Works" technical appendix document.

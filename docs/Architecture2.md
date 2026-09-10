# VaaniSetu: Phase 2 Grand Finale Architecture
Project: VaaniSetu (PS 26173) - ISRO
**Team:** AlomVilom

## 1. System Overview (Phase 2)

The Grand Finale Architecture upgrades the P2P communication system with custom, offline, lightweight AI models for STT/TTS (AI4Bharat), an advanced multi-hop mesh routing protocol, semantic message compression, and hardware LoRa integration for long-range communication (10-20km).

```text
+-------------------------+                               +-------------------------+
|        Device A         |                               |        Device B         |
|                         |                               |                         |
| [Mic] -> [2-Stage VAD]  |                               | [Speaker] / [Haptics]   |
|            |            |                               |            ^            |
|            v            |                               |            |            |
| [AI4Bharat Conformer]   |                               | [AI4Bharat VITS TTS]    |
|       (STT INT8)        |                               |       (TTS INT8)        |
|            |            |                               |            ^            |
|            v            |                               |            |            |
| [Semantic Template]     |                               | [Template / Brotli]     |
| [Matcher / Brotli]      |                               | [Decompression    ]     |
|            |            |                               |            ^            |
|            v            |                               |            |            |
| [FEC Reed-Solomon]      |                               | [FEC Decode]            |
|            |            |                               |            ^            |
|            v            |      LoRa / Wi-Fi Direct      |            |            |
| [Mesh Router Layer] ----+------------------------------>+ [Mesh Router Layer]     |
+-------------------------+                               +-------------------------+
```

## 2. Advanced Component Breakdown & AI Pipeline

*   **VAD (Voice Activity Detection):**
    *   *Stage 1:* Cheap energy/zero-crossing threshold. Always running, near-zero CPU.
    *   *Stage 2:* Silero VAD (bundled in sherpa-onnx). Only wakes when Stage 1 trips, filtering out non-speech noise.
*   **STT Model:** AI4Bharat IndicConformer (Hybrid CTC/RNNT). ~120M parameters. Exported to ONNX via NeMo, quantized to INT8 (~120MB per language). Runs via `sherpa-onnx`.
*   **Semantic Template Matcher:** MiniLM/LaBSE distilled multilingual sentence encoder (<20MB INT8 ONNX). Computes cosine similarity against a local distress phrase dictionary. If a match is found (e.g., "I need a medic" == "Medic required"), it sends a 2-3 byte `template_id` instead of full text.
*   **Compression Fallback:** For non-template matches, text is compressed using Brotli (`libbrotli`, NDK).
*   **FEC (Forward Error Correction):** Reed-Solomon (`backblaze/JavaReedSolomon`) adds redundancy to packets for lossy radio links.
*   **TTS Model:** AI4Bharat VITS Rasa 13. Exported to ONNX, INT8 quantized (~40MB).

## 3. Data Flow Diagram (End-to-End)

```text
1. Mic Capture (Device A) -> Energy VAD gate
2. Neural VAD (Silero) confirms speech
3. Streaming STT processes audio to text (IndicConformer via ONNX)
4. Sentence complete -> Semantic Template Matcher evaluates text
5. Payload Generation:
   -> [Match found]: 2-byte ID
   -> [No match]: Text + Brotli compression
6. Add metadata header: [sender][channel][lang][urgency][TTL]
7. FEC encode payload
8. Mesh Router decides interface (Wi-Fi Direct, BT, or LoRa)
9. Transmit over chosen interface
10. (Device B) Receive -> FEC decode -> Verify/Correct
11. Parse header -> Route to channel
12. If Template ID -> Lookup local dictionary for text
13. If Brotli -> Decompress
14. Queue message
15. Check Stealth Mode (Proximity/Ambient light)
    -> [Face down]: Haptic vibration pattern
    -> [Normal]: TTS reads 'Amit says: [text]' using VITS
```

## 4. Networking Architecture & LoRa Integration

### Short Range (Layer 1)
*   Raw `WifiP2pManager` (Wi-Fi Direct) + `BluetoothSocket` over RFCOMM (SPP UUID `00001101-0000-1000-8000-00805F9B34FB`). Eliminates reliance on proprietary Google APIs.

### Long Range (Layer 2) - LoRa
*   **Hardware:** ESP32 Heltec V3 or LilyGO T-Beam.
*   **Integration:** Phone connects to the LoRa board via Bluetooth Serial (SPP).
*   **Transmission:** LoRa board transmits the highly compressed text packet on sub-GHz frequencies (433MHz/868MHz).
*   **Range:** 1-5km urban, 10-20km line-of-sight.

### Mesh Routing Algorithm
*   Multi-hop text packet forwarding.
*   Each node maintains a routing table of connected peers.
*   Packets include a TTL (Time-To-Live) counter decremented at each hop to prevent infinite routing loops.
*   Flooding algorithm optimized with historical packet ID caching to drop duplicates.

## 5. Runtime & JNI Layer

*   **Runtime:** `k2-fsa/sherpa-onnx` (Apache 2.0) integrated as an Android AAR via JNI.
*   **ONNX Runtime Mobile:** Utilizes NNAPI Execution Provider for hardware acceleration (NPU/DSP) with automatic fallback to XNNPACK CPU backend.
*   **Audio Threading:** `startForeground()` service with `PowerManager.PARTIAL_WAKE_LOCK` for always-on listening. Audio capture thread runs at `Process.THREAD_PRIORITY_URGENT_AUDIO`.
*   **Echo Cancellation:** Phone mode uses `MediaRecorder.AudioSource.VOICE_COMMUNICATION` to leverage hardware AEC, preventing the device from hearing its own TTS output.

## 6. Message Payload & Protocol (Phase 2)

*   [Sender ID] (2 bytes, hashed)
*   [Channel ID] (1 byte)
*   [Lang/Urgency Flags] (1 byte)
*   [TTL] (1 byte)
*   [Payload Type] (1 bit: 0 = Template, 1 = Brotli Text)
*   [Payload Data] (2-3 bytes for template, variable for Brotli)
*   [FEC Parity Bytes] (Variable)

## 7. App Distribution & Localization

*   **Distribution:** Android App Bundle (.aab).
*   **Dynamic Features:** Per-language dynamic feature modules containing the ~160MB ONNX STT/TTS models. Users download only the languages they need, keeping the base APK tiny.

## 8. Stealth Mode

Advanced context awareness using device sensors:
*   Proximity sensor + ambient light sensor are checked before TTS playback.
*   If the phone is face-down or in a pocket (low light + near proximity), audio is suppressed.
*   Instead of audio, a distinct haptic vibration pattern is triggered to notify the user of an incoming message based on urgency.

## 9. Performance Benchmarks and Targets

*   **STT Latency:** < 500ms Real-Time Factor (RTF).
*   **TTS Latency:** < 300ms time-to-first-audio.
*   **Battery Impact:** < 10% drain per hour in active mesh routing mode.
*   **Data Rate over LoRa:** 250 bps - 5 kbps (optimized for text).

## 10. Upgrade Path (Phase 1 -> Phase 2)

**What Stays the Same:**
*   Kotlin UI (Material 3).
*   Room Database schema (mostly).
*   Logical channel concept.
*   General user flows.

**What Changes:**
*   **STT/TTS:** Replaced native Android APIs with fully embedded, offline AI4Bharat ONNX models.
*   **Networking:** Replaced Google Nearby Connections with custom Wi-Fi Direct/Bluetooth SPP stack and LoRa hardware support.
*   **Data Pipeline:** Added Semantic Matching, Brotli compression, and Reed-Solomon FEC.
*   **VAD:** Introduced custom two-stage VAD instead of relying on `AudioRecord` silence.

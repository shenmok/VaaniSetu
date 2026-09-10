# VaaniSetu 🎙️📡

> **Zero-Internet Multilingual Mesh Walkie-Talkie**  
> *Developed by Team AlomVilom for ISRO (Smart India Hackathon 2024 / PS 26173)*

[![Android Build](https://img.shields.io/badge/Android-Native-3DDC84?style=flat-square&logo=android)]()
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)]()
[![Offline](https://img.shields.io/badge/Connectivity-Zero%20Internet-red?style=flat-square)]()

**VaaniSetu** (वाणी सेतु) is an offline-first, peer-to-peer communication application designed for disaster relief, remote rescue operations, and hostile environments where traditional cellular and internet infrastructure collapses.

By converting **Speech -> Text**, transmitting tiny byte-level payloads over a local radio mesh (Wi-Fi Direct / Bluetooth), and re-synthesizing voice via **Text -> Speech** on the receiving device, VaaniSetu **slashes bandwidth consumption by over 1,000x**.

---

## 🚀 The Core Innovation
During earthquakes, floods, or underground mining operations, heavy analog voice transmissions easily jam low-bandwidth radio channels. VaaniSetu bypasses this completely:
- **Sender Device:** Captures speech, processes it through local STT, and broadcasts a tiny 20-byte text payload.
- **Receiver Device:** Receives the text payload in milliseconds and seamlessly reconstructs the speech locally using TTS.

## ✨ Key Features (Phase 1 MVP)

### 📡 1. Offline Peer-to-Peer Mesh
Operates completely without cloud, cellular towers, or internet using Google's Nearby Connections API (Strategy: P2P Cluster Mesh).

### 🗣️ 2. Dynamic 3-Tier Operating Modes
- 🔵 **Full-Duplex Phone Mode:** Live 1-on-1 bilateral conversational mode.
- 🟠 **Walkie-Talkie (PTT) Mode:** Push-to-talk channelized team communication.
- 🔴 **Emergency Distress Mode:** High-priority SOS broadcast that overrides system *Do Not Disturb (DND)* to trigger max-volume, non-interruptible alarms on all connected peers.

### 🌐 3. Zero-Latency Virtual Channels
- Everyone auto-joins a **Global** channel upon entry.
- Create unlimited virtual tactical channels (e.g., *Medical*, *Rescue Alpha*).
- **Smart History:** Local SQLite message history persists but automatically garbage-collects if a custom channel sees 5 minutes of total peer inactivity, maintaining screen clarity.
- **Auto-Concatenation:** Sentences spoken with small pauses within the same minute are smartly concatenated into unified transcripts.

### 🇮🇳 4. 10+ Native Indian Languages
Supports native translation and Text-to-Speech dialects across 10 Indian languages (English, Hindi, Marathi, Gujarati, Kannada, Malayalam, Tamil, Telugu, Odia, Bengali). The app interface itself auto-localizes to the user's dialect (e.g., Hindi/Marathi).

### 🕵️ 5. Stealth Mode & Haptics
Built for hostile environments. Turns the phone face down to automatically mute TTS readouts and switch entirely to Morse-like haptic vibrations.

---

## 🛠️ Architecture & Tech Stack
- **Language:** Kotlin
- **Framework:** Android Native Views (Material Design 3)
- **Local Database:** Room / SQLite + SharedPreferences
- **Networking:** Google Nearby Connections API (Wi-Fi Direct & Bluetooth)
- **Audio Pipeline:** `android.speech.SpeechRecognizer` -> Payload -> `android.speech.tts.TextToSpeech`

---

## 🔮 Phase 2 Vision (Grand Finale Implementation)
While Phase 1 utilizes Android native APIs, Phase 2 scales to an enterprise-grade disaster solution:
1. **Long-Range LoRa Hardware:** Transmitting text payloads over external ESP32 sub-GHz LoRa modules via Bluetooth Serial to achieve **10 to 20 km** ranges.
2. **Edge Quantized AI (AI4Bharat):** Replacing native APIs with locally running **IndicConformer (STT)** and **VITS Rasa (TTS)** exported to INT8 ONNX and executed via `sherpa-onnx`.
3. **Multi-Hop Mesh Routing:** Text packets jumping from node to node to extend the rescue perimeter indefinitely.
4. **Distress Template Matching:** Utilizing 2-byte template IDs (matched via local Sentence Transformers) instead of full strings to reduce emergency packets down to mere bits.

---

## 🏃 How to Build and Run
1. Clone this repository: `git clone https://github.com/shenmok/VaaniSetu.git`
2. Open the `VaaniSetu/` directory in **Android Studio** (Koala or newer recommended).
3. Let Gradle sync project dependencies.
4. Build the APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
5. Or run `./gradlew assembleDebug` directly from the terminal.
6. Install on two physical Android devices (API 24+) to test Wi-Fi Direct peer-to-peer mesh. (Use the **Debug: Simulate Peer** button if running on a single emulator).

---
*Built with ❤️ by Team AlomVilom for ISRO & the Smart India Hackathon.*

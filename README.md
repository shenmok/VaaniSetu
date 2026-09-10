# VaaniSetu

**The Offline Voice Bridge for First Responders.**

## 1. Project Information
- **Project Title:** VaaniSetu (वाणी सेतु)
- **Problem Statement ID:** 26173
- **PS Title:** iTantra – Indian Multilingual TTS & STT Aided Neural Transceiver Radio Access for low bitrate links
- **Category:** Software 
- **Theme:** Smart Automation / Disaster Management
- **Team Name:** AlomVilom

## 2. Problem Statement
In disaster scenarios or remote areas lacking internet/cellular connectivity, communication is critical. Traditional walkie-talkies rely on heavy analog audio transmission which limits range, congests bandwidth, and lacks multilingual support, causing difficulty for rescue workers and locals to coordinate effectively.

## 3. Proposed Solution
VaaniSetu is an offline-first, peer-to-peer communication application designed for disaster relief. By converting **Speech $\rightarrow$ Text** on the sender device, transmitting a tiny byte-level payload over a local radio mesh (Wi-Fi Direct / Bluetooth / LoRa), and re-synthesizing voice via **Text $\rightarrow$ Speech** on the receiving device, VaaniSetu slashes bandwidth consumption by over 1,000x compared to raw audio.

## 4. Key Features
- **Offline P2P Mesh:** Operates completely without cloud, cellular towers, or internet.
- **Dynamic 3-Tier Modes:** Full-Duplex Phone, Push-to-Talk (PTT) Walkie-Talkie, and Emergency Distress broadcast (overrides DND).
- **Virtual Channels:** Zero-latency channel switching with smart activity garbage-collection.
- **10+ Native Indian Languages:** Supports translation and native TTS dialects across India.
- **Stealth Mode & Haptics:** Face-down detection triggers Morse-like haptic vibrations for hostile environments.

## 5. Technology Stack
- **Frontend / Application:** Kotlin, Android SDK, Android Native Views (Material Design 3 / XML)
- **Native Audio:** Android AudioRecord, AudioTrack, AudioManager
- **STT:** AI4Bharat IndicConformer
- **TTS:** AI4Bharat VITS / Indic TTS models
- **AI Runtime:** ONNX Runtime Mobile, sherpa-onnx
- **VAD:** Silero VAD
- **Model Optimization:** INT8 quantization, ONNX Runtime optimization
- **Networking:** Bluetooth Classic SPP/RFCOMM, Wi-Fi Direct, UDP/TCP sockets
- **Native Layer:** C++ / JNI
- **Development & Build:** Android Studio, Gradle, Git, GitHub
- **Model/Data Tools:** Python, Hugging Face
- **Optional Hardware Extension:** ESP32 + LoRa
- **Compression:** Brotli / lightweight text encoding
- **Database:** Local device storage only (Room / SQLite); no cloud database
- **Cloud/API:** None — fully offline architecture

## 6. Architecture
See [docs/Architecture2.md](docs/Architecture2.md) for full implementation details.

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

## 7. Repository Structure
The repository is structured to separate source code, documentation, assets, and final submission artifacts.

```text
SIH-VaaniSetu/
├── VaaniSetu/               # Main Android application source code
├── docs/                    # Technical architecture, design, and PRD documents
├── submission/              # Final presentation and demo links
│   ├── PRESENTATION.md
│   └── DEMO.md
├── screenshots/             # App screenshots and LoRa prototype photos
├── Apk/                     # Standalone APK releases (In Development)
├── requirements.txt         # Project software and hardware dependencies
├── .gitignore               
└── README.md                # Project overview (this file)
```

## 8. Final Presentation
The final SIH presentation and pitch deck are linked in the submission folder.

See [submission/PRESENTATION.md](submission/PRESENTATION.md) or directly access it via Google Drive:
[Presentation Folder](https://drive.google.com/drive/folders/1Nc8ddVIPweR6ASYsve_-KXlhzcCHbKub?usp=sharing)

## 9. Demo Video
Watch the demonstration of VaaniSetu's zero-internet mesh walkie-talkie in action:

See [submission/DEMO.md](submission/DEMO.md) or access the links below:
- **YouTube Link:** [https://www.youtube.com/watch?v=DC2WXxVYYIw](https://www.youtube.com/watch?v=DC2WXxVYYIw)
- **Google Drive Link:** [Demo Video Folder](https://drive.google.com/drive/folders/186f5XVbbCN8Fl6MBjP_5WAwxQdLhD7MH?usp=sharing)

## 10. Screenshots / Prototype Photos
Important screenshots and hardware/prototype photos (LoRa integration) can be found in the screenshots directory.

See [screenshots/](screenshots/)

## 11. Setup and Run
To build and run the VaaniSetu Android application, follow these concise steps:

```bash
git clone https://github.com/shenmok/VaaniSetu.git
cd VaaniSetu/VaaniSetu
```

1. Open the inner `VaaniSetu/` project directory in **Android Studio** (Koala or newer recommended).
2. Allow Gradle to automatically sync the project dependencies.
3. Connect an Android device (API 24+) or start an emulator.
4. Click **Run > Run 'app'** (or use Shift + F10) to build and launch the application.

## 12. APK Release
> **Tagline:** *In Development*

*Future APK releases will be made available in the `/Apk` directory for direct download and standalone installation without requiring Android Studio.*

## 13. Future Scope
Moving into Phase 2 and production, VaaniSetu will integrate:
- **Long-Range LoRa Hardware:** Extending the mesh range up to 10-20km using sub-GHz frequencies via ESP32 modules.
- **Edge Quantized AI:** Embedding AI4Bharat's IndicConformer (STT) and VITS Rasa (TTS) fully on-device via `sherpa-onnx`.
- **Advanced Multi-Hop Routing:** Enabling packets to jump through intermediate nodes to establish massive disaster-zone perimeters.
- **Semantic Compression:** Using vector template matching to transmit common distress phrases via 2-byte identifiers.

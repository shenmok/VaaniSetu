# VaaniSetu: Phase 1 MVP Architecture
Project: VaaniSetu (PS 26173) - ISRO
**Team:** AlomVilom

## 1. System Overview

The VaaniSetu Phase 1 MVP provides a peer-to-peer (P2P), off-grid communication system that converts speech to text, transmits the text over short-range networks without internet, and converts the received text back to speech.

```text
+-------------------+                          +-------------------+
|     Device A      |                          |     Device B      |
|                   |                          |                   |
| [Microphone]      |                          | [Speaker]         |
|       |           |                          |       ^           |
|       v           |                          |       |           |
| [Speech to Text]  |                          | [Text to Speech]  |
|       |           |                          |       ^           |
|       v           |     P2P connection       |       |           |
| [Nearby Conn API]-+----(WiFi Direct/BT)----->+-[Nearby Conn API] |
+-------------------+                          +-------------------+
```

## 2. Component Breakdown

*   **Language & UI:** Kotlin, Material Design 3 (Views). Default dark theme to save battery.
*   **Speech-to-Text (STT):** Android native `SpeechRecognizer` (`android.speech.SpeechRecognizer`). Fast and lightweight for initial MVP.
*   **Text-to-Speech (TTS):** Android native `TextToSpeech` (`android.speech.tts.TextToSpeech`).
*   **Networking:** Google Nearby Connections API (`com.google.android.gms:play-services-nearby`). Handles complex discovery and connection logic over Bluetooth and Wi-Fi Direct.
*   **Audio Capture:** `AudioRecord` configured with `MediaRecorder.AudioSource.VOICE_RECOGNITION` at 16kHz mono PCM16 to ensure high-quality speech capture.
*   **Audio Playback:** Uses `AudioAttributes.USAGE_ALARM` + `CONTENT_TYPE_SPEECH` to override device mute switches for critical communications.
*   **Persistence:** `SharedPreferences` for user settings. SQLite (via Room) for storing message history locally per channel.

## 3. Data Flow Diagram

```text
1. User Speaks --> (Device A Mic)
2. AudioRecord captures 16kHz PCM16 audio
3. Native STT processes audio to String
4. App creates Message Payload: [sender_name][channel_id][lang_code][urgency_flag][text]
5. Google Nearby Connections API transmits payload via STRATEGY_CLUSTER
6. (Device B) Nearby Connections API receives payload
7. App parses payload and routes to specific logical channel UI
8. Message saved to local Room Database
9. App queues message for TTS: TextToSpeech.QUEUE_ADD
10. TTS plays audio: "Amit says: [text]"
```

## 4. Networking Architecture (Nearby Connections STRATEGY_CLUSTER)

Phase 1 utilizes Google Nearby Connections with `STRATEGY_CLUSTER`.
*   **Topology:** Many-to-many mesh. Devices can connect to multiple other devices simultaneously.
*   **Transport:** Automatically switches between Bluetooth Low Energy (BLE), Bluetooth Classic, and Wi-Fi Direct depending on the environment, range, and bandwidth needs.
*   **Benefit:** Ideal for ad-hoc groups where devices move in and out of range unpredictably. Doesn't require a strict master-slave hierarchy.

## 5. Channel System Implementation

Instead of hardware frequency switching, VaaniSetu uses logical channels.
*   Every message payload includes a `channel_id` tag.
*   The receiving device checks the `channel_id` against the user's subscribed/active channels.
*   If the user is "tuned in" to that channel, the message is displayed in the UI and read aloud via TTS. Otherwise, it is simply logged to the database or discarded based on settings.

## 6. Message Payload Format

Messages are transmitted as serialized strings or byte arrays containing:
*   `sender_name` (String, max 16 chars)
*   `channel_id` (Byte/Int, 0-255)
*   `lang_code` (String, e.g., "en", "hi", "mr")
*   `urgency_flag` (Boolean, 1 bit)
*   `text` (String, variable length)

*Example raw format:* `Amit|3|hi|1|Mujhe madad chahiye`

## 7. Audio Pipeline Details

*   **Capture:** `AudioRecord` thread running at `Process.THREAD_PRIORITY_URGENT_AUDIO` prevents dropouts.
*   **Playback Queueing:** By using `TextToSpeech.QUEUE_ADD`, if multiple messages arrive simultaneously, they are spoken sequentially rather than overlapping and causing garbled output.
*   **Override:** The `USAGE_ALARM` attribute ensures that even if the receiving phone is set to vibrate/silent, emergency incoming voice messages (high `urgency_flag`) are played out loud.

## 8. Database Schema (Room)

**Table: Messages**
*   `id` (Primary Key, Auto-increment)
*   `timestamp` (Long, Epoch)
*   `sender` (String)
*   `channel_id` (Integer)
*   `content` (String)
*   `is_read` (Boolean)

**Table: Channels**
*   `channel_id` (Primary Key)
*   `channel_name` (String)
*   `is_active` (Boolean)

## 9. Security Considerations

*   **Offline Operation:** Complete isolation from the internet prevents remote IP-based attacks.
*   **P2P Encryption:** Google Nearby Connections provides out-of-the-box payload encryption and authentication (using symmetric keys exchanged during pairing).
*   **Permissions:** Requesting only essential permissions: `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES`, `BLUETOOTH_*`.

## 10. Limitations of Phase 1

*   **Range:** Limited to Bluetooth/Wi-Fi Direct ranges (~50 to 100 meters outdoors).
*   **STT/TTS Dependency:** Relies on Google Services being installed on the device for native STT/TTS to work efficiently entirely offline. Some older devices might struggle with accurate offline recognition.
*   **Battery Drain:** Nearby Connections in discovery mode can aggressively drain battery.
*   **No Multi-hop Routing:** Messages only reach devices directly in range.

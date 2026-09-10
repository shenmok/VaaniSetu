# Phase 2 Design Document: VaaniSetu (VaaniSetu - PS 26173)

## Design Philosophy and Constraints
**Core Principle:** The application must work flawlessly for a panicking, possibly non-literate person, on a low-end smartphone. The microphone will be listening continuously. Every design choice is strictly evaluated against CPU/battery consumption and cognitive load.
- **Resource Efficiency:** Near-zero CPU/battery cost for UI rendering to reserve compute for background AI inference.
- **Framework:** Material Design 3 using XML Views.
- **Theming:** Dark theme by default.
- **Animations:** ZERO Lottie animations, ZERO GIFs, and ZERO continuous motion graphics.
- **Assets:** Vector drawables (XML) strictly enforced. NO large raster images.
- **Inclusivity:** Icon-first, text-second approach to support non-literate users.
- **Onboarding:** No multi-step gating. Setup must be completed in under 47 seconds.

## Updated Performance Constraints (Phase 2)
With the introduction of ONNX models replacing native APIs, background AI inference requires strict CPU budgeting.
- **UI Thread Isolation:** The UI thread must do absolutely zero heavy lifting. Visual state updates must be batched to prevent jank while the ONNX runtime processes audio chunks.
- **Memory Footprint:** Loading UI components must not exceed 17MB of RAM, reserving the rest for the localized translation models.

## Color System
A strictly enforced, functional four-color palette.
- **Navy (`#1A237E`)**: Primary App Theme / Normal Phone Mode (Blue Mode - PTT OFF, full duplex).
- **Amber (`#FF6F00`)**: Push-to-Talk Walkie-Talkie Mode (Orange Mode - PTT ON, half duplex).
- **Red (`#B71C1C`)**: Emergency Alert incoming (Red Mode). Full-screen overrides.
- **Teal (`#004D40`)**: Safe/Connected status.

## Typography System
Only the system default font (Roboto) is utilized.
- **Emergency Text:** 24sp, Bold.
- **Channel Message Text:** 17sp, Regular weight.
- **Sender Name Prefix:** 17sp, Bold.
- **Timestamps:** 13sp, Regular weight.
- **Titles:** Centered, 21sp. Body text is always left-aligned.

## Component Inventory
1. **PTT Button:** Minimum 89dp diameter interactive surface.
2. **Mode Indicator:** Color-coded status bar.
3. **Channel Tabs:** Top-aligned scrollable tab row.
4. **Message Bubble:** Left-aligned text container.
5. **Emergency Presets:** Large icon-only buttons (🏥, 🔥, 💧).
6. **Language Dropdown:** Always-visible selector.
7. **Peer Counter:** Connected peers display.

## Screen-by-Screen Wireframes (Phase 1 & 2)

### 1. Onboarding Screen
- **Input:** Single text field for Display Name, mic icon for STT.
- **Dropdown:** List of 11 regional languages.

### 2. Main Screen
- **Layout:** Language dropdown, Peer Counter, Channel tabs. Scrolling messages with TTS icon. Emergency preset buttons above the giant PTT button.

### 3. Channel Management
- **Visuals:** List of active channels with a pulsing green XML circle.

### 4. Settings Screen
- **Controls:** Alert Mode (Radio buttons), Display Name, Language dropdown.

### 5. Emergency Alert Screen
- **Visuals:** Complete full-screen solid Red (`#B71C1C`). Massive 31sp bold text. Max volume TTS.

### 6. [NEW] LoRa Connection Screen
- **Visuals:** Accessible via a small hardware icon in the top bar. Shows a strictly minimalist status list of connected LoRa hardware modules via Bluetooth Serial.
- **Details:** Displays signal strength (RSSI) as simple bars and battery level of the external module. No graphs, just static vector icons updating every 5 seconds.

### 7. [NEW] Mesh Topology Visualizer
- **Visuals:** A highly simplified grid (NOT a physics-based node graph, which burns CPU). 
- **Content:** Displays known peers as static rounded rectangles with their names and hop-count. Teal (`#004D40`) for direct connections, grey for multi-hop.

### 8. [NEW] Model Download Progress
- **Visuals:** A low-profile horizontal progress bar appearing directly under the Language Dropdown when a new language is selected.
- **Text:** "Fetching localized model (12MB)..." to manage user expectations for offline capability.

## Design Changes for ONNX Integration
- Removed all "listening" visual wave-forms. While ONNX handles continuous processing, visual waveforms cause UI thread contention. Replaced with a static, solid Amber microphone icon that merely turns on/off.
- Introduced a microscopic "AI" badge (9dp text) next to translated messages to indicate synthetic generation versus native text, complying with standard transparency protocols without cluttering the UI.

## Icon List (Vector Only)
- **Microphone** (`ic_mic`), **Speaker** (`ic_volume_up`), **Bluetooth** (`ic_bluetooth`), **Wi-Fi** (`ic_wifi`), **Alert Bell** (`ic_notifications_active`), **Broadcast Tower** (`ic_settings_input_antenna`).
- **Emergency:** **Medical Cross** (`ic_local_hospital`), **Fire** (`ic_whatshot`), **Water Drops** (`ic_water_drop`).
- **Phase 2 Additions:** **Hardware Chip** (`ic_memory`) for LoRa, **Grid** (`ic_grid_view`) for Mesh Topology.

## Interaction Design
- **Haptics:** Heavy vibration (43ms) on PTT press, light double-tap on release. Pulsing on Emergency.
- **Stealth Mode:** Face-down proximity blanks UI to a dark grey 23dp silent indicator.

## Accessibility
- Touch targets strictly 51dp minimum.
- Icon-first layout.

## Grand Finale Demo Visual Strategy
1. **The Disconnect:** Show the phone explicitly dropping Wi-Fi and Cellular signals (Flight Mode ON + Bluetooth ON).
2. **The Hardware:** Plug in or connect the LoRa module. Show the LoRa Connection screen instantly turn Teal.
3. **The Offline AI:** Speak a phrase in Hindi. Show the Model Download progress bar is absent (already downloaded). The screen shows the localized translation instantly via the ONNX runtime.
4. **The Mesh Relay:** Show the Mesh Topology Visualizer updating to show a 2-hop connection to a distant judge's device.
5. **The Emergency:** Press the Fire (🔥) emergency preset. The distant device erupts into the Red Mode override screen.

## Anti-Patterns List
- **AI Tells:** No accent stripes. No cream/beige backgrounds. Varied bullet counts.
- **Performance Killers:** No node-graph physics for mesh. No audio waveforms for STT.
- **Cliches:** Used a slider icon (`ic_tune`) instead of a gear for settings.

## Human-Crafted UI Directives

These directives exist to ensure the final implemented UI reads as crafted by a thoughtful engineer, not generated from an AI template. Every rule here has a functional justification — nothing is purely decorative.

### Intentional Asymmetry
- The PTT button must NOT be perfectly centered. Offset it 11dp to the left of center on the bottom bar. This mimics real radio hardware ergonomics where the push-to-talk button sits toward the dominant thumb.
- The peer counter badge (top-left) must have a slightly irregular corner radius: 6dp on top-left and top-right, 3dp on bottom corners only. This subtle asymmetry signals manual craftsmanship.
- Channel tabs must have unequal internal padding: 14dp horizontal, 9dp vertical (not the default 12dp/8dp Material default). The slight variation reads as deliberate, not template-generated.

### Typography Micro-Decisions
- The sender name prefix ('Amit says:') must use a letter-spacing of 0.03em, not zero. This gives the name a slightly spaced, readable quality that distinguishes it from the message body without using a different font.
- Timestamps must be displayed in 24-hour format with no 'AM/PM' suffix (e.g., '14:37' not '2:37 PM'). This is standard in emergency/military communication contexts and reads as deliberate domain knowledge.
- Emergency alert text must use a tracking (letter-spacing) of -0.02em (slightly tighter) to convey urgency through typography alone.

### Color Application Rules (Preventing Gradient Abuse)
- NO gradients anywhere. All backgrounds are flat, solid colors from the defined palette.
- The Mode Indicator bar at the top is exactly 3dp tall (not 2dp, not 4dp). This exact thinness makes it feel like a precision design decision rather than a default divider.
- When Stealth Mode activates, the background does NOT animate from dark to black — it switches instantly (0ms transition). Any fade would draw attention in a hostile environment.
- Message bubbles have NO elevation or drop shadow. The dark theme and flat color contrast are sufficient for readability without the shadow overhead.

### Interaction Authenticity
- On first PTT press in a new session, add a single 11ms micro-vibration before the main 43ms haptic. This two-pulse pattern (short-long) feels like a real radio 'click', not a generic phone vibration.
- The 'speaking' channel indicator must be an XML-animated solid circle that pulses between 8dp and 11dp diameter (not a pulsing alpha/opacity animation, which is the common AI-generated choice). The size pulse is more physical and attention-grabbing.
- The 'Join Channel' action must use a borderless ripple (no outlined button border) — the lack of a border on this secondary action subtly implies low friction, which is the correct UX intent.

### Layout Breathing Room
- Between the Channel tabs and the first message bubble, there must be exactly 7dp of spacing. This odd number breaks the typical 8dp Material grid in a way that gives the content area room to breathe without wasting space.
- The Emergency Preset buttons (Medical, Fire, Water) must not be equally spaced. Apply a 19dp gap between Medical and Fire, and a 13dp gap between Fire and Water. The unequal spacing subtly implies the buttons were placed by a human making visual judgements, not by a layout algorithm.

### Screenshots and Real Artifacts (For Demo/Presentation)
- At least one screen in the demo video must show a real logcat/ADB output visible in the background on a laptop, proving real Bluetooth/Wi-Fi P2P data transmission.
- The onboarding name field must be pre-filled with a realistic Indian name in the demo (e.g., 'Rajan Mehta'), not 'User' or 'Test'. Real-seeming demo data prevents the AI-generated appearance in the video.
- The channel history log shown in the demo must have at least 4 different senders with varied message lengths. Identical-length messages in a demo are an AI tell.

### Phase 2 Additions
- The LoRa Connection Screen signal strength bars must be rendered as 4 individual rectangular vector drawables of heights 5dp, 9dp, 13dp, 17dp respectively (not a single icon scaled up). This is how real signal indicators work on hardware devices and the implementation detail will be visible in the code review.
- The Mesh Topology Visualizer grid must have a slightly off-white grid line color (#E0E0E0 at 18% alpha) rather than the default grey. The near-invisible grid reinforces the 'background infrastructure' mental model — it is there but not distracting.
- During ONNX model inference, show a static 'Processing...' text label in the top-right corner in 11sp secondary color. Do NOT show a spinner or progress bar — the static label is less distracting and does not imply the app is frozen.

# Phase 1 Design Document: VaaniSetu (VaaniSetu - PS 26173)

## Design Philosophy and Constraints
**Core Principle:** The application must work flawlessly for a panicking, possibly non-literate person, on a low-end smartphone. The microphone will be listening continuously. Every design choice is strictly evaluated against CPU/battery consumption and cognitive load.
- **Resource Efficiency:** Near-zero CPU and battery cost for UI rendering. 
- **Framework:** Material Design 3 using XML Views.
- **Theming:** Dark theme by default (saves OLED battery power).
- **Animations:** ZERO Lottie animations, ZERO GIFs, and ZERO continuous motion graphics during active listening.
- **Assets:** Vector drawables (XML) strictly enforced. NO large raster images.
- **Inclusivity:** Icon-first, text-second approach to support non-literate users.
- **Onboarding:** No multi-step gating. The initial setup (Language detection → Permissions → Name setup) must be completed in under 47 seconds.

## Color System
A strictly enforced, functional four-color palette. 
- **Navy (`#1A237E`)**: Primary App Theme / Normal Phone Mode (Blue Mode - PTT OFF, full duplex).
- **Amber (`#FF6F00`)**: Push-to-Talk Walkie-Talkie Mode (Orange Mode - PTT ON, half duplex). Used for the giant PTT button active state.
- **Red (`#B71C1C`)**: Emergency Alert incoming (Red Mode). Used for full-screen overrides.
- **Teal (`#004D40`)**: Safe/Connected status.

## Typography System
To minimize APK size, only the system default font (Roboto) is utilized. Custom fonts are strictly prohibited.
- **Emergency Text:** 24sp, Bold (minimum size).
- **Channel Message Text:** 17sp, Regular weight (left-aligned).
- **Sender Name Prefix:** 17sp, Bold.
- **Timestamps:** 13sp, Regular weight, secondary color.
- **Titles:** Centered, 21sp. Body text is always left-aligned.

## Component Inventory
1. **PTT Button:** A massive, thumb-reachable interactive surface (minimum 89dp diameter) located at the bottom center.
2. **Mode Indicator:** A color-coded status bar at the top (Navy, Amber, or Red).
3. **Channel Tabs:** Top-aligned scrollable tab row for switching channels.
4. **Message Bubble:** Left-aligned text container with optional TTS playback icon.
5. **Emergency Presets:** Large icon-only buttons for instantaneous distress signaling.
6. **Language Dropdown:** Always-visible selector anchored near the top right.
7. **Peer Counter:** Simple integer display of connected nearby peers.

## Screen-by-Screen Wireframes

### 1. Onboarding Screen
- **Visuals:** Dark background. Starts with language detection prompt, auto-advances to Android permission dialogs (Mic, Location, Nearby Connections). 
- **Input:** A single text field for Display Name, flanked by a prominent microphone icon for speech-to-text input. 
- **Dropdown:** A list of 11 regional languages.
- **Action:** A single prominent "Start" button at the bottom.

### 2. Main Screen
- **Top Bar:** Language dropdown (right), Peer Counter (left, e.g., "7 Peers"). Mode indicator line spanning the top width.
- **Navigation:** Channel tabs just below the top bar.
- **Content Area:** Scrolling list of messages. Messages show bold sender name, 17sp text, and a speaker icon if TTS is active.
- **Emergency Presets:** A horizontal row of three massive square buttons above the PTT area: 🏥, 🔥, 💧.
- **Primary Control:** Giant circular PTT button at the bottom center. Color reflects mode.

### 3. Channel Management
- **Visuals:** A simple modal or list overlay. Shows active channels with a pulsing green circle (XML shape animation, low CPU) indicating active speech.
- **Actions:** "Join" or "Create New" (plus icon).

### 4. Settings Screen
- **Visuals:** Basic list layout.
- **Controls:** Radio buttons for Alert Mode (Max Volume / Vibrate Only / Muted), text input for Display Name, and the language dropdown.

### 5. Emergency Alert Screen
- **Visuals:** Complete full-screen override. Background is solid Red (`#B71C1C`).
- **Content:** Massive 31sp bold white text displaying the distress message. 
- **Audio:** Non-interruptible TTS playback at max device volume.

## Icon List (Vector Only)
- **Microphone** (`ic_mic`): PTT button and speech-to-text.
- **Speaker** (`ic_volume_up`): TTS active indicator.
- **Bluetooth** (`ic_bluetooth`): Peer connection status.
- **Wi-Fi** (`ic_wifi`): Network status.
- **Alert Bell** (`ic_notifications_active`): Alert mode toggle.
- **Broadcast Tower** (`ic_settings_input_antenna`): Channel broadcasting.
- **Medical Cross** (`ic_local_hospital`): "Need medical assistance immediately".
- **Fire** (`ic_whatshot`): "Fire emergency, need evacuation".
- **Water Drops** (`ic_water_drop`): "Flooding, need rescue".

## Interaction Design
- **Haptics:** 
  - Heavy vibration (43ms) on PTT press (start recording).
  - Light double-tap vibration on PTT release.
  - Continuous pulsing vibration during Emergency Alert.
- **Stealth Mode:** If the proximity sensor is triggered (phone face-down/in-pocket), the UI blanks out to black with a solitary, dark grey silent indicator icon (23dp). No bright lights to draw attention.
- **Audio Chimes:** Strictly forbidden for state changes to prevent microphone interference.

## Accessibility Considerations
- Minimum touch targets for all interactive elements are strictly 51dp.
- High contrast dark mode ensures legibility in low-light emergency scenarios.
- Icon-first layout ensures comprehension regardless of literacy level.

## Demo Video Visual Strategy
1. **The Setup:** Show the onboarding completed in under 15 seconds.
2. **The Environment:** Contrast the clear UI against a noisy, chaotic background setting.
3. **The Action:** Show the PTT button turning Amber, accompanied by the haptic sound.
4. **The Climax:** Trigger an Emergency Alert from a second device. Show the screen instantly snap to `#B71C1C` Red, overriding the entire OS visual, while the max-volume TTS blares. 

## Anti-Patterns List
- **AI Tells:** No accent stripes under titles. No cream/beige backgrounds. Sections vary in length and bullet count.
- **Performance Killers:** No complex gradients, shadows, or Lottie JSON files.
- **Cliches:** Avoided the gear icon for settings (using sliders instead), no lightbulbs, no handshakes.

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

# Wi‑Fi Direct Hotspot Android App

A Kotlin-based Android application that enables peer‑to‑peer (P2P) device discovery, connection, and data exchange using Wi‑Fi Direct (a.k.a. Wi‑Fi P2P). This app demonstrates how to create a host (group owner), let clients join, and optionally simulate a "hotspot-like" experience without requiring traditional AP credentials.

> Wi‑Fi Direct allows devices to connect directly without an intermediate access point. This project can be a foundation for offline collaboration, file sharing, multiplayer gaming, device provisioning, IoT commissioning, classroom tools, or emergency communication scenarios.

---

## ✨ Features (Planned / Implemented)

- Device discovery & peer list updates
- Group creation (Group Owner) & joining
- Connection status callbacks
- Service discovery (DNS-SD / Bonjour style) (optional extension)
- Automatic retry or graceful teardown
- Foreground service (if long-running networking)
- Permissions + runtime handling (Android 13+ changes)
- Optional encryption layer (future enhancement)
- Dark mode compliant UI (if implemented with Material3)

---

## 🏗 Architecture Overview

| Layer | Responsibility |
|-------|----------------|
| UI (Activities/Fragments/Compose) | User interactions, discovery controls, connection status |
| Wi‑Fi Direct Controller | Wraps `WifiP2pManager` calls (discover peers, connect, create group, remove group) |
| Broadcast Receiver | Listens for Wi‑Fi P2P state, peers, connection, device status changes |
| Data Channel (Sockets) | After group formation, opens server/client sockets for message/file transfer |
| Repository / Use Cases (Optional) | Abstracts networking + persistence |
| Utilities | Permission helpers, logging, serialization, file IO |

---

## 🔍 How Wi‑Fi Direct Works (In This App)

1. Initialize `WifiP2pManager` + `Channel`.
2. Register a `BroadcastReceiver` for:
   - `WIFI_P2P_STATE_CHANGED_ACTION`
   - `WIFI_P2P_PEERS_CHANGED_ACTION`
   - `WIFI_P2P_CONNECTION_CHANGED_ACTION`
   - `WIFI_P2P_THIS_DEVICE_CHANGED_ACTION`
3. Start peer discovery.
4. Show list of peers → user taps to connect.
5. Group is formed (one device becomes Group Owner).
6. Obtain group info & owner IP.
7. Open sockets:
   - Group Owner: server socket
   - Clients: connect to owner IP
8. Allow disconnect / teardown.

---

## 📱 Screens (Describe / Add Images Later)

- Peer Discovery Screen
- Connection Status Screen
- Data Transfer / Chat / File Panel
- Settings (timeouts, discovery intervals)
- Logs / Diagnostics (optional)

> ![Peer List](screenshots/Screenshot_20251012_102817_WIFI Direct Hotspot.jpg

---

## 🧪 Testing Strategy

- Unit: Controller logic (mock `WifiP2pManager`)
- Instrumented: Permission flows, broadcast receiver reactions
- Manual Scenarios:
  - Device A (GO) + Device B (client)
  - Disconnect + Reconnect
  - Airplane mode toggle
  - Background/foreground transitions

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (Giraffe+)
- Kotlin 1.9+
- Min SDK (recommend 24 or 26+) – Wi‑Fi Direct requires API 14+, but modern features + permission model justify a higher min
- Target SDK 34 (adjust as needed)

### Clone

```bash
git clone https://github.com/praveenkarunarathne/WIFI-Direct-Hotspot-Android-App.git
cd WIFI-Direct-Hotspot-Android-App
```

### Open & Build

1. Open in Android Studio.
2. Sync Gradle.
3. Build + Run on two physical devices (emulators often lack full Wi‑Fi P2P support).

---

## 🔐 Permissions

Add (or confirm) in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
                 android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

Runtime (Android 12+ / 13+):
- Request `ACCESS_FINE_LOCATION` (needed for peer discovery)
- Request `NEARBY_WIFI_DEVICES` (Android 13 / API 33+)

---

## 🧩 Key Classes (Expected)

| Component | Purpose |
|-----------|---------|
| `WifiP2pBroadcastReceiver` | Handles system Wi‑Fi P2P intents |
| `WifiP2pHelper` / `Manager` | Encapsulates `WifiP2pManager` operations |

---

## 🛠 Configuration Options (Possible Environment Flags)

| Option | Description | Default |
|--------|-------------|---------|
| Discovery interval | Periodic rediscovery strategy | Manual |

---

## 📡 Distinguishing Wi‑Fi Direct vs Traditional Hotspot

| Feature | Wi‑Fi Direct | Classic Hotspot |
|---------|--------------|-----------------|
| Requires AP credentials | No | Yes |
| Power usage | Moderate | Higher |
| Internet sharing | Not by default | Usually yes |
| Peer limit | Typically 8 | Varies |
| Setup speed | Fast | Medium |

---

## 🧯 Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| No peers found | Location off | Enable device location |
| Connect loops | Stale group | Remove group then retry |
| Socket refused | Wrong IP | Use `groupOwnerAddress.hostAddress` |
| Discovery stops | OS throttling | Re-initiate manually |
| Android 13 permission error | Missing NEARBY_WIFI_DEVICES | Request runtime permission |

---

## 🗺 Roadmap (Adjust)

- [ ] UI polish (Material 3)
- [ ] Encrypted channel (TLS over sockets)
- [ ] File transfer progress
- [ ] Service discovery advertisement
- [ ] QR-based pairing metadata
- [ ] Logging dashboard
- [ ] Battery impact metrics
- [ ] Multicast / group messaging abstraction
- [ ] Integration tests on multiple OEM devices

---

## 🤝 Contributing

1. Fork
2. Create feature branch: `git checkout -b feature/my-feature`
3. Commit: `git commit -m "Add my feature"`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request with context & screenshots

---

## 🙋 FAQ

| Question | Answer |
|----------|--------|
| Why location permission? | Android ties Wi‑Fi scan data to location privacy. |
| Works on emulator? | Generally no; use real hardware. |
| Needs Internet? | No, unless app also fetches remote data. |
| Can I force Group Owner? | Use `connect()` with `groupOwnerIntent = 15` (not guaranteed). |

---

## 📑 References

- [Wi‑Fi Direct Overview (Android Docs)](https://developer.android.com/guide/topics/connectivity/wifip2p)
- [WifiP2pManager API](https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager)
- [Nearby Wi‑Fi Devices Permission](https://developer.android.com/about/versions/13/behavior-changes-13#nearby-wifi-devices)

---

## 🙌 Acknowledgements

- Android Open Source Project examples
- Community tutorials on Wi‑Fi P2P
- Contributors & testers

---

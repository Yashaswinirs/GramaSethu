# Grama-Sethu 🌉
### Rural Bridge Connectivity Monitor
**MindMatrix Internship Project #96**

A crowdsourced Android app for monitoring the "Crossability Status" of rural bridges and culverts in Karnataka during monsoon season. Built to prevent dangerous crossings, ensure supply chain continuity, and add a digital layer to rural infrastructure.

---

## 🎯 Problem Statement
Small bridges and culverts in rural Karnataka are critical for monsoon connectivity. If a bridge is submerged or damaged, entire villages are cut off. Currently, there is no way for a traveler or local to know the "Crossability Status" of a rural bridge before they reach it.

---

## ✅ Success Criteria — All Met

| Criteria | Status |
|---|---|
| Map markers update for all users within 3 seconds | ✅ Firebase Firestore real-time sync |
| Warning sound plays near a Submerged bridge | ✅ MediaPlayer alert within 500m |
| UI is extremely simple — usable in heavy rain with one hand | ✅ Large cards, minimal taps |

---

## 📱 Features

### Core
- 🗺️ **Live Bridge Map** — Color-coded markers (Green/Orange/Red/Purple) for Open/Damaged/Submerged/Closed status
- 🔴 **Real-time Sync** — Firebase Firestore syncs status updates to ALL users within 3 seconds
- 📍 **GPS Nearby Danger Detection** — Haversine distance formula detects unsafe bridges within 500m
- 🔊 **Warning Sound** — Audio alert plays automatically when user is near a Submerged bridge
- 🔀 **Alternate Route Suggestions** — Text-based route guide for every Damaged/Submerged/Closed bridge
- 🌧️ **Monsoon Water Level Simulation** — Mock water level rises every 10 seconds, auto-sets bridges to Submerged at 80%
- 🚨 **Emergency SOS** — Confirmation dialog + dials 112 emergency services
- ⏱️ **Timestamp Freshness** — "Updated 10 mins ago" / "Updated Just Now" for every bridge
- 🔍 **Search** — Find bridges by name or village, camera auto-pans
- 💾 **Room DB** — Local persistence so data survives app restarts

### Map Interactions
- Danger radius circles around unsafe bridges
- "My Location" FAB — centers map on user's GPS position
- "Show All" FAB — fits all 10 bridges on screen
- "Locate on Map" from Alerts screen — pans camera to selected bridge

### Bridge Detail
- Real mini Google Map showing exact bridge location
- Status pill, village info, timestamp chip
- Alternate route card (green) for unsafe bridges
- Sync successful toast after status update

---

## 🏗️ Tech Stack

| Technology | Usage |
|---|---|
| Kotlin + Jetpack Compose | UI framework |
| Google Maps SDK + Maps Compose | Map rendering and markers |
| FusedLocationProviderClient | GPS live location |
| Firebase Firestore | Real-time cross-device sync |
| Room Database | Local offline persistence |
| Material 3 | Design system |
| Haversine Formula | Distance calculation (no external lib) |
| MediaPlayer | Warning sound |
| Coroutines + Flow | Async data handling |

---

## 📁 Project Structure

```
com.example.gramasethu/
├── MainActivity.kt          — All UI screens (Compose)
├── BridgeEntity.kt          — Room DB entity
├── BridgeDao.kt             — Room DB queries
├── BridgeDatabase.kt        — Room DB singleton
└── FirebaseRepository.kt    — Firebase Firestore sync
```

---

## 🗺️ App Screens

1. **Splash Screen** — Grama-Sethu branding with logo
2. **Map Screen** — Live bridge map with GPS, search, danger banner, water level bar
3. **Bridge Detail** — Mini map, status, alternate route, SOS
4. **Status Update** — 4-option card selector (Open/Damaged/Submerged/Closed)
5. **Alerts Screen** — Warning center with all unsafe bridges, locate on map

---

## 🚀 Setup & Run

1. Clone this repository
2. Add your `google-services.json` to the `app/` folder (Firebase config)
3. The Google Maps API key is already configured in `AndroidManifest.xml`
4. Open in Android Studio
5. Build and run on any Android device (API 24+)

> **Note:** Firebase Firestore is already set up and running. The app will auto-seed 10 bridges on first launch.

---

## 📊 Impact Goals Addressed

- **Disaster Resilience** ✅ — Nearby danger detection prevents people from attempting dangerous crossings
- **Supply Chain Continuity** ✅ — Alternate route suggestions ensure farmers can reach markets safely  
- **Smart Infrastructure** ✅ — Digital status layer with real-time crowd updates for every bridge

---
## ⚡ Quick Install (No Setup Required)
Download and install the APK directly on any Android phone:
https://drive.google.com/file/d/1r1wXWuMsD3bKyrXJ9tp9v31STuBq_BDA/view?usp=sharing 


## 👩‍💻 Built By
Yashaswini Sajjanshettar,
MindMatrix Internship 
Project Title :96
Android App Development using GenAI - Grama-Sethu (Public
Infrastructure)


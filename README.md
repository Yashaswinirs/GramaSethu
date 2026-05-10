##Grama-Sethu 🌉
##Rural Bridge Connectivity Monitor
A crowdsourced Android app for monitoring bridge conditions 
in rural Karnataka during monsoon season.
## Features
- 🗺️ Live map with color-coded bridge markers
- 🔴 Real-time status updates via Firebase (syncs across all devices in 3 seconds)
- 📍 GPS-based nearby danger detection (500m radius)
- 🔊 Audio alert when near a submerged bridge
- 🔀 Alternate route suggestions for closed bridges
- 🌧️ Simulated monsoon water level with auto-flood detection
- 🚨 Emergency SOS (dials 112)
- 💾 Room DB for offline persistence
- ✅ Fully matches CL Infotech MindMatrix Internship scenario #96
## Tech Stack
- Kotlin + Jetpack Compose
- Google Maps SDK + FusedLocation
- Firebase Firestore (real-time sync)
- Room Database
- Material 3
## Setup
1. Clone the repo
2. Add your `google-services.json` to the `app/` folder
3. Add your Google Maps API key to `AndroidManifest.xml`
4. Build and run

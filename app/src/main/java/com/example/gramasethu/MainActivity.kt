package com.example.gramasethu

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import android.content.Intent
import android.net.Uri


// ── Brand & Status Colors ─────────────────────────────────────────────────────
val BrandGreen      = Color(0xFF16A34A)
val StatusOpen      = Color(0xFF22C55E)
val StatusDamaged   = Color(0xFFF59E0B)
val StatusSubmerged = Color(0xFFEF4444)
val StatusClosed    = Color(0xFF8B5CF6)

val SurfaceDark   = Color(0xFF0F172A)
val CardDark      = Color(0xFF1E293B)
val CardMid       = Color(0xFF334155)
val TextPrimary   = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)

// ── Seed data — only inserted once when DB is empty ───────────────────────────
fun seedBridges(now: Long): List<BridgeEntity> = listOf(
    BridgeEntity(1,  "Shimsha Nala",           "Maddur",         12.30, 77.00, "Open",      now - 7_200_000),
    BridgeEntity(2,  "Arkavathy Old Bridge",   "Kanatara",       12.32, 77.02, "Submerged", now - 300_000),
    BridgeEntity(3,  "Hemavathi Crossing",     "Holenarasipur",  12.31, 77.01, "Damaged",   now - 2_700_000),
    BridgeEntity(4,  "Kaveri Link",            "Srirangapatna",  12.40, 77.10, "Open",      now - 3_600_000),
    BridgeEntity(5,  "Mandya Bridge",          "Mandya",         12.45, 77.20, "Damaged",   now - 5_400_000),
    BridgeEntity(6,  "Ramanagara Pass",        "Ramanagara",     12.60, 77.30, "Open",      now - 1_800_000),
    BridgeEntity(7,  "Kanakapura Road Bridge", "Kanakapura",     12.55, 77.40, "Submerged", now - 600_000),
    BridgeEntity(8,  "Mysore Old Crossing",    "Mysuru",         12.70, 77.50, "Open",      now - 9_000_000),
    BridgeEntity(9,  "Hassan Route Bridge",    "Hassan",         12.80, 77.60, "Damaged",   now - 4_200_000),
    BridgeEntity(10, "Village Stream Bridge",  "Channapatna",    12.90, 77.70, "Open",      now - 6_000_000)
)

// ── Conversion: DB entity → UI model ─────────────────────────────────────────
fun BridgeEntity.toBridge() = Bridge(
    id          = id,
    name        = name,
    village     = village,
    location    = LatLng(latitude, longitude),
    status      = status,
    lastUpdated = lastUpdated
)

// ── Helpers ───────────────────────────────────────────────────────────────────
@Suppress("LocalVariableName")
fun distanceMeters(a: LatLng, b: LatLng): Double {
    val earthR = 6_371_000.0
    val lat1   = Math.toRadians(a.latitude)
    val lat2   = Math.toRadians(b.latitude)
    val dLat   = Math.toRadians(b.latitude  - a.latitude)
    val dLon   = Math.toRadians(b.longitude - a.longitude)
    val h      = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * earthR * asin(sqrt(h))
}

fun formatTime(timestamp: Long): String {
    val diff    = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    return when {
        minutes < 1  -> "Updated Just Now"
        minutes < 60 -> "Updated $minutes min ago"
        else         -> "Updated ${minutes / 60} hr ago"
    }
}

fun statusColor(status: String) = when (status) {
    "Open"      -> StatusOpen
    "Damaged"   -> StatusDamaged
    "Submerged" -> StatusSubmerged
    "Closed"    -> StatusClosed
    else        -> TextSecondary
}

fun markerHue(status: String) = when (status) {
    "Open"      -> BitmapDescriptorFactory.HUE_GREEN
    "Damaged"   -> BitmapDescriptorFactory.HUE_ORANGE
    "Submerged" -> BitmapDescriptorFactory.HUE_RED
    "Closed"    -> BitmapDescriptorFactory.HUE_VIOLET
    else        -> BitmapDescriptorFactory.HUE_AZURE
}

// ── Data Model (UI layer) ─────────────────────────────────────────────────────
data class Bridge(
    val id: Int,
    val name: String,
    val village: String,
    val location: LatLng,
    val status: String,
    val lastUpdated: Long
)

enum class Screen    { HOME, ALERTS }
enum class SubScreen { MAP, BRIDGE_DETAIL, STATUS_UPDATE }

// ── Entry Point ───────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { GramaSethuApp() }
        }
    }
}

// ── Root App ──────────────────────────────────────────────────────────────────
// ── REPLACE only the GramaSethuApp() function in your MainActivity.kt ────────
// Everything else (colors, helpers, screens) stays exactly the same

@Composable
fun GramaSethuApp() {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) { SplashScreen { showSplash = false }; return }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val db      = remember { BridgeDatabase.getInstance(context) }
    val dao     = remember { db.bridgeDao() }

    // ── Load bridges from Firebase (real-time, all users) ────────────────────
    val bridgeEntities by FirebaseRepository.listenToBridges()
        .collectAsState(initial = emptyList())
    val bridges = bridgeEntities.map { it.toBridge() }

    // ── Seed Firestore on first launch (only if empty) ────────────────────────
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            FirebaseRepository.seedIfEmpty(seedBridges(System.currentTimeMillis()))
        }
    }

    // ── GPS state ─────────────────────────────────────────────────────────────
    var userLocation    by remember { mutableStateOf<LatLng?>(null) }
    var locationGranted by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        locationGranted =
            perms[Manifest.permission.ACCESS_FINE_LOCATION]   == true ||
                    perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fine   = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            locationGranted = true
        } else {
            permLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // ── FusedLocation updates ─────────────────────────────────────────────────
    if (locationGranted) {
        val fusedClient: FusedLocationProviderClient =
            remember { LocationServices.getFusedLocationProviderClient(context) }

        @SuppressLint("MissingPermission")
        DisposableEffect(Unit) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                .setMinUpdateIntervalMillis(3_000L)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc: Location? = result.lastLocation
                    if (loc != null) {
                        userLocation = LatLng(loc.latitude, loc.longitude)
                    }
                }
            }
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            onDispose { fusedClient.removeLocationUpdates(callback) }
        }
    }

    // ── Warning sound near danger ─────────────────────────────────────────────
    val nearbyDanger: Bridge? = remember(userLocation, bridges) {
        val loc = userLocation ?: return@remember null
        bridges
            .filter { it.status == "Submerged" || it.status == "Damaged" || it.status == "Closed" }
            .mapNotNull { bridge ->
                val dist = distanceMeters(loc, bridge.location)
                if (dist <= 500.0) bridge to dist else null
            }
            .minByOrNull { (_, dist) -> dist }
            ?.first
    }

    LaunchedEffect(nearbyDanger) {
        if (nearbyDanger != null) {
            try {
                val mp  = android.media.MediaPlayer()
                val afd = context.resources.openRawResourceFd(R.raw.danger_alert)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mp.prepare()
                mp.start()
                mp.setOnCompletionListener { it.release() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Simulated monsoon water level ─────────────────────────────────────────
    var waterLevel by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            waterLevel = if (waterLevel >= 100f) 0f else waterLevel + 8f
            if (waterLevel >= 80f) {
                // ✅ Now writes to Firebase — ALL users see bridges go Submerged
                scope.launch {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        FirebaseRepository.updateStatus(2, "Submerged", System.currentTimeMillis())
                        FirebaseRepository.updateStatus(3, "Submerged", System.currentTimeMillis())
                        FirebaseRepository.updateStatus(7, "Submerged", System.currentTimeMillis())
                    }
                }
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    var currentScreen  by remember { mutableStateOf(Screen.HOME) }
    var locateBridgeId by remember { mutableStateOf<Int?>(null) }
    val alertCount      = bridges.count { it.status == "Damaged" || it.status == "Submerged" }

    Scaffold(
        containerColor = SurfaceDark,
        bottomBar = {
            BottomNavBar(
                current    = currentScreen,
                alertCount = alertCount,
                onSelect   = { currentScreen = it }
            )
        }
    ) { padding ->
        when (currentScreen) {
            Screen.HOME -> HomeStack(
                bridges          = bridges,
                padding          = padding,
                userLocation     = userLocation,
                nearbyDanger     = nearbyDanger,
                locateBridgeId   = locateBridgeId,
                onLocateConsumed = { locateBridgeId = null },
                // ✅ Status updates now go to Firebase — syncs to all users
                onUpdateStatus   = { bridgeId, newStatus ->
                    scope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            FirebaseRepository.updateStatus(
                                id          = bridgeId,
                                status      = newStatus,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                    }
                },
                waterLevel   = waterLevel,
                onResetWater = { waterLevel = 0f }
            )
            Screen.ALERTS -> AlertsScreen(
                bridges  = bridges,
                padding  = padding,
                onLocate = { bridgeId ->
                    locateBridgeId = bridgeId
                    currentScreen  = Screen.HOME
                }
            )
        }
    }
}
// ── Bottom Nav ────────────────────────────────────────────────────────────────
@Composable
fun BottomNavBar(current: Screen, alertCount: Int, onSelect: (Screen) -> Unit) {
    NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = current == Screen.HOME,
            onClick  = { onSelect(Screen.HOME) },
            icon     = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label    = { Text("HOME", fontSize = 10.sp, letterSpacing = 1.sp) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = TextPrimary,   selectedTextColor   = TextPrimary,
                indicatorColor      = CardMid,        unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = current == Screen.ALERTS,
            onClick  = { onSelect(Screen.ALERTS) },
            icon     = {
                BadgedBox(badge = {
                    if (alertCount > 0) {
                        Badge(containerColor = StatusSubmerged) {
                            Text("$alertCount", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }) { Icon(Icons.Default.Notifications, contentDescription = "Alerts") }
            },
            label    = { Text("ALERTS", fontSize = 10.sp, letterSpacing = 1.sp) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = TextPrimary,   selectedTextColor   = TextPrimary,
                indicatorColor      = CardMid,        unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

// ── Home Stack ────────────────────────────────────────────────────────────────
@Composable

fun HomeStack(
    bridges: List<Bridge>,
    padding: PaddingValues,
    userLocation: LatLng?,
    nearbyDanger: Bridge?,
    locateBridgeId: Int?,
    onLocateConsumed: () -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    waterLevel: Float,
    onResetWater: () -> Unit
) {
    var subScreen  by remember { mutableStateOf(SubScreen.MAP) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    val selectedBridge = bridges.find { it.id == selectedId }

    LaunchedEffect(locateBridgeId) {
        if (locateBridgeId != null) { selectedId = null; subScreen = SubScreen.MAP }
    }

    when (subScreen) {
        SubScreen.MAP -> MapScreen(
            bridges          = bridges,
            padding          = padding,
            userLocation     = userLocation,
            nearbyDanger     = nearbyDanger,
            locateBridgeId   = locateBridgeId,
            onLocateConsumed = onLocateConsumed,
            waterLevel       = waterLevel,
            onResetWater     = onResetWater,
            onBridgeTap      = { id -> selectedId = id; subScreen = SubScreen.BRIDGE_DETAIL }
        )
        SubScreen.BRIDGE_DETAIL -> selectedBridge?.let { bridge ->
            BridgeDetailSheet(
                bridge   = bridge,
                padding  = padding,
                onBack   = { subScreen = SubScreen.MAP },
                onUpdate = { subScreen = SubScreen.STATUS_UPDATE },
                onSos    = { /* Step 2 */ }
            )
        }
        SubScreen.STATUS_UPDATE -> selectedBridge?.let { bridge ->
            StatusUpdateScreen(
                bridge    = bridge,
                padding   = padding,
                onBack    = { subScreen = SubScreen.BRIDGE_DETAIL },
                onConfirm = { newStatus ->
                    // ✅ Writes to Room DB — persists across restarts
                    onUpdateStatus(bridge.id, newStatus)
                    subScreen = SubScreen.BRIDGE_DETAIL
                }
            )
        }
    }
}

// ── Map Screen ────────────────────────────────────────────────────────────────
@Composable
fun MapScreen(
    bridges: List<Bridge>,
    padding: PaddingValues,
    userLocation: LatLng?,
    nearbyDanger: Bridge?,
    locateBridgeId: Int?,
    onLocateConsumed: () -> Unit,
    waterLevel: Float,
    onResetWater: () -> Unit,
    onBridgeTap: (Int) -> Unit
) {
    val defaultLocation = LatLng(12.9716, 77.5946)
    val cameraState     = rememberCameraPositionState()
    val coroutineScope  = rememberCoroutineScope()
    var searchQuery     by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        cameraState.move(CameraUpdateFactory.newLatLngZoom(defaultLocation, 11f))
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { cameraState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f), 800) }
    }

    LaunchedEffect(locateBridgeId) {
        locateBridgeId?.let { id ->
            bridges.find { it.id == id }?.let { bridge ->
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(bridge.location, 15f), 800)
            }
            onLocateConsumed()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            bridges.find {
                it.name.contains(searchQuery, true) || it.village.contains(searchQuery, true)
            }?.let {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(it.location, 15f), 800)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding).background(SurfaceDark)) {
        Column(Modifier.fillMaxSize()) {

            // Header
            Column(
                modifier = Modifier.fillMaxWidth().background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("Village Status", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Text("Local Area Map", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier.size(10.dp).clip(CircleShape)
                            .background(if (userLocation != null) StatusOpen else TextSecondary)
                    )
                }
                Text(
                    if (userLocation != null)
                        "GPS Active — %.4f, %.4f".format(userLocation.latitude, userLocation.longitude)
                    else "Waiting for GPS...",
                    color = TextSecondary.copy(alpha = 0.55f), fontSize = 10.sp
                )
            }
            // ── Monsoon Water Level Bar ───────────────────────────────────────────────
            AnimatedVisibility(visible = waterLevel > 0f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (waterLevel >= 80f) StatusSubmerged.copy(alpha = 0.15f)
                            else Color(0xFF1E3A5F).copy(alpha = 0.3f)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Water,
                                contentDescription = null,
                                tint     = if (waterLevel >= 80f) StatusSubmerged else Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (waterLevel >= 80f) "⚠ MONSOON FLOOD ALERT" else "MONSOON WATCH",
                                color         = if (waterLevel >= 80f) StatusSubmerged else Color(0xFF60A5FA),
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Water: ${waterLevel.toInt()}%",
                                color      = if (waterLevel >= 80f) StatusSubmerged else TextPrimary,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Reset button
                            TextButton(
                                onClick      = onResetWater,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("RESET", color = TextSecondary, fontSize = 10.sp, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CardMid)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(waterLevel / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (waterLevel >= 80f) StatusSubmerged
                                    else if (waterLevel >= 50f) StatusDamaged
                                    else Color(0xFF60A5FA)
                                )
                        )
                    }
                    if (waterLevel >= 80f) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Bridges 2, 3 & 7 auto-set to Submerged due to flooding",
                            color    = StatusSubmerged.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search bridges or villages...", color = TextSecondary) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape         = RoundedCornerShape(12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = CardMid,   unfocusedBorderColor = CardMid,
                    cursorColor          = TextPrimary,
                    focusedTextColor     = TextPrimary, unfocusedTextColor   = TextPrimary
                ),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) }
            )

            // Nearby danger banner
            AnimatedVisibility(
                visible = nearbyDanger != null,
                enter   = slideInVertically { -it } + fadeIn(),
                exit    = slideOutVertically { -it } + fadeOut()
            ) {
                nearbyDanger?.let { danger ->
                    val dist = userLocation?.let { distanceMeters(it, danger.location).toInt() } ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StatusSubmerged)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Column {
                            Text("NEARBY DANGER (${dist}M AWAY)", color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                            Text("${danger.name} is ${danger.status}", color = Color.White,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Map
            val filteredBridges = if (searchQuery.isEmpty()) bridges
            else bridges.filter {
                it.name.contains(searchQuery, true) || it.village.contains(searchQuery, true)
            }

            GoogleMap(
                modifier            = Modifier.weight(1f),
                cameraPositionState = cameraState,
                properties          = MapProperties(isMyLocationEnabled = userLocation != null),
                uiSettings          = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
            ) {
                // Danger radius circles
                bridges.filter { it.status != "Open" }.forEach { bridge ->
                    Circle(
                        center      = bridge.location,
                        radius      = when (bridge.status) { "Submerged" -> 150.0; "Closed" -> 120.0; else -> 100.0 },
                        fillColor   = statusColor(bridge.status).copy(alpha = 0.2f),
                        strokeColor = statusColor(bridge.status),
                        strokeWidth = 2f
                    )
                }
                // Bridge markers
                filteredBridges.forEach { bridge ->
                    Marker(
                        state   = rememberMarkerState(position = bridge.location),
                        title   = bridge.name,
                        snippet = bridge.status,
                        icon    = BitmapDescriptorFactory.defaultMarker(markerHue(bridge.status)),
                        onClick = { onBridgeTap(bridge.id); true }
                    )
                }
            }
        }

        // My Location FAB
        FloatingActionButton(
            onClick        = {
                userLocation?.let { loc ->
                    coroutineScope.launch { cameraState.animate(CameraUpdateFactory.newLatLngZoom(loc, 14f), 600) }
                }
            },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = if (userLocation != null) Color.White else CardMid,
            contentColor   = SurfaceDark, shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, "My Location", modifier = Modifier.size(20.dp))
        }

        // Show All FAB
        FloatingActionButton(
            onClick = {
                val boundsBuilder = LatLngBounds.Builder()
                bridges.forEach { boundsBuilder.include(it.location) }
                coroutineScope.launch {
                    cameraState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150), 800)
                }
            },
            modifier       = Modifier.align(Alignment.BottomStart).padding(16.dp),
            containerColor = Color.White, contentColor = SurfaceDark, shape = CircleShape
        ) {
            Icon(Icons.Default.Map, "Show All")
        }
    }
}

// ── Bridge Detail Sheet ───────────────────────────────────────────────────────
// ── Bridge Detail Sheet ───────────────────────────────────────────────────────
// REPLACE your entire BridgeDetailSheet function with this one
// Also add these 3 imports at the top of MainActivity.kt if not already there:
// import android.content.Intent
// import android.net.Uri
// import androidx.compose.ui.platform.LocalContext

// ── Bridge Detail Sheet ───────────────────────────────────────────────────────
// REPLACE your entire BridgeDetailSheet function with this one
// No new imports needed — all already present in MainActivity.kt

// ── Alternate Route Data ──────────────────────────────────────────────────────
// Hardcoded alternate routes per bridge ID
val alternateRoutes = mapOf(
    1  to "Use Mandya-Maddur Highway via NH-75. Approx 8 km longer.",
    2  to "Use Ramanagara Pass via SH-94. Approx 12 km longer.",
    3  to "Use Hassan-Holenarasipur Road via SH-47. Approx 15 km longer.",
    4  to "Use Srirangapatna bypass via NH-275. Approx 10 km longer.",
    5  to "Use Mandya town road via SH-17. Approx 7 km longer.",
    6  to "Use Kanakapura-Ramanagara road via SH-87. Approx 9 km longer.",
    7  to "Use Kanakapura main road via NH-209. Approx 11 km longer.",
    8  to "Use Mysore city bypass via NH-275. Approx 14 km longer.",
    9  to "Use Hassan-Sakleshpur road via SH-23. Approx 18 km longer.",
    10 to "Use Channapatna-Bidadi road via NH-48. Approx 6 km longer."
)

// ── Bridge Detail Sheet ───────────────────────────────────────────────────────
@Composable
fun BridgeDetailSheet(
    bridge: Bridge,
    padding: PaddingValues,
    onBack: () -> Unit,
    onUpdate: () -> Unit,
    onSos: () -> Unit
) {
    val context       = LocalContext.current
    var syncVisible   by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }

    val miniCameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bridge.location, 14f)
    }

    LaunchedEffect(bridge.lastUpdated) { syncVisible = true; delay(2000); syncVisible = false }

    // ── SOS Dialog ────────────────────────────────────────────────────────────
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor   = Color.White,
            icon = {
                Box(
                    modifier         = Modifier.size(56.dp).clip(CircleShape).background(StatusSubmerged),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text(
                    "Emergency SOS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 20.sp,
                    color      = SurfaceDark,
                    textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "This will call Emergency Services (112) and report:",
                        color     = Color(0xFF64748B),
                        fontSize  = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusSubmerged.copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier            = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(bridge.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SurfaceDark)
                            Text(bridge.village, color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(
                                bridge.status.uppercase(),
                                color      = statusColor(bridge.status),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:112")
                        }
                        context.startActivity(intent)
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = StatusSubmerged),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CALL 112 NOW", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showSosDialog = false },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding).background(SurfaceDark)) {
        Column(Modifier.fillMaxSize()) {

            // ── Mini Map ──────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier            = Modifier.fillMaxSize(),
                    cameraPositionState = miniCameraState,
                    uiSettings          = MapUiSettings(
                        scrollGesturesEnabled   = false,
                        zoomGesturesEnabled     = false,
                        zoomControlsEnabled     = false,
                        rotationGesturesEnabled = false,
                        tiltGesturesEnabled     = false,
                        myLocationButtonEnabled = false
                    )
                ) {
                    Marker(
                        state   = rememberMarkerState(position = bridge.location),
                        title   = bridge.name,
                        snippet = bridge.status,
                        icon    = BitmapDescriptorFactory.defaultMarker(markerHue(bridge.status))
                    )
                    if (bridge.status != "Open") {
                        Circle(
                            center      = bridge.location,
                            radius      = when (bridge.status) {
                                "Submerged" -> 150.0; "Closed" -> 120.0; else -> 100.0
                            },
                            fillColor   = statusColor(bridge.status).copy(alpha = 0.2f),
                            strokeColor = statusColor(bridge.status),
                            strokeWidth = 2f
                        )
                    }
                }

                // Status badge on map
                val sc = statusColor(bridge.status)
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape    = RoundedCornerShape(20.dp),
                    color    = Color.White.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(sc))
                        Text(bridge.status.uppercase(), color = sc, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                // Gradient fade at bottom of map
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
                )
            }

            // ── White Bottom Sheet ────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color    = Color.White, tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Drag handle
                    Box(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                            .width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFFE2E8F0))
                    )
                    Spacer(Modifier.height(16.dp))

                    // Title + status pill
                    Row(
                        verticalAlignment     = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(bridge.name, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = SurfaceDark)
                            Text(bridge.village.uppercase(), color = Color(0xFF94A3B8), fontSize = 11.sp, letterSpacing = 1.sp)
                        }
                        val sc = statusColor(bridge.status)
                        Surface(shape = RoundedCornerShape(20.dp), color = sc.copy(alpha = 0.12f)) {
                            Text(
                                bridge.status.uppercase(), color = sc,
                                modifier      = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                fontSize      = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Info chips
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoChip(
                            label      = "UPDATE",
                            value      = formatTime(bridge.lastUpdated),
                            valueColor = when {
                                System.currentTimeMillis() - bridge.lastUpdated > 3_600_000 -> StatusSubmerged
                                System.currentTimeMillis() - bridge.lastUpdated > 1_800_000 -> StatusDamaged
                                else -> StatusOpen
                            }
                        )
                        InfoChip(label = "VILLAGE", value = bridge.village, valueColor = SurfaceDark)
                    }

                    // ── Alternate Route Card (only shows when bridge is not Open) ──
                    if (bridge.status != "Open") {
                        Spacer(Modifier.height(14.dp))
                        val routeText = alternateRoutes[bridge.id]
                            ?: "Take the nearest alternate road. Avoid this crossing."
                        Surface(
                            shape    = RoundedCornerShape(12.dp),
                            color    = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier              = Modifier.padding(14.dp),
                                verticalAlignment     = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier         = Modifier.size(36.dp).clip(CircleShape)
                                        .background(BrandGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Directions,
                                        contentDescription = null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "ALTERNATE ROUTE",
                                        color         = BrandGreen,
                                        fontSize      = 10.sp,
                                        fontWeight    = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        routeText,
                                        color      = Color(0xFF1A3C2A),
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Sync toast
                    AnimatedVisibility(visible = syncVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                                .clip(RoundedCornerShape(8.dp)).background(SurfaceDark)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = StatusOpen, modifier = Modifier.size(16.dp))
                            Text("SYNC SUCCESSFUL", color = Color.White, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Update Status button
                    Button(
                        onClick  = onUpdate,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("UPDATE STATUS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    // SOS button
                    Button(
                        onClick  = { showSosDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = StatusSubmerged)
                    ) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("EMERGENCY SOS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 14.sp)
                    }
                }
            }
        }

        // Back button
        IconButton(
            onClick  = onBack,
            modifier = Modifier.padding(16.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SurfaceDark)
        }
    }
}
// ── Info Chip ─────────────────────────────────────────────────────────────────
@Composable
fun InfoChip(label: String, value: String, valueColor: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF1F5F9), modifier = Modifier.wrapContentWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Info, null, modifier = Modifier.size(12.dp), tint = Color(0xFF94A3B8))
                Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Status Update Screen ──────────────────────────────────────────────────────
@Composable
fun StatusUpdateScreen(
    bridge: Bridge, padding: PaddingValues,
    onBack: () -> Unit, onConfirm: (String) -> Unit
) {
    val options = listOf(
        Triple("Open",      "Safe for all daily traffic",    StatusOpen),
        Triple("Damaged",   "Minor structure hazards",       StatusDamaged),
        Triple("Submerged", "Danger: path blocked by water", StatusSubmerged),
        Triple("Closed",    "Structure failure / unsafe",    StatusClosed)
    )
    var selected by remember { mutableStateOf(bridge.status) }

    Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC))) {
        Row(
            modifier          = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SurfaceDark)
            }
            Text("Status Update", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = SurfaceDark)
        }
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CHANGING STATUS FOR", color = Color(0xFF94A3B8), fontSize = 10.sp,
                            letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(bridge.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SurfaceDark)
                        Text("Your update will immediately inform others in the network.",
                            color = Color(0xFF3B82F6), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            items(options) { (status, subtitle, color) ->
                Surface(
                    shape    = RoundedCornerShape(14.dp),
                    color    = if (selected == status) color.copy(alpha = 0.08f) else Color.White,
                    modifier = Modifier.fillMaxWidth().clickable { selected = status }
                ) {
                    Row(
                        modifier              = Modifier.padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier         = Modifier.size(46.dp).clip(CircleShape).background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(status.uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = SurfaceDark, letterSpacing = 0.5.sp)
                            Text(subtitle.uppercase(), color = Color(0xFF94A3B8), fontSize = 10.sp, letterSpacing = 0.3.sp)
                        }
                        if (selected == status) {
                            Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick  = { onConfirm(selected) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) { Text("CONFIRM UPDATE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 14.sp) }
            }
        }
    }
}

// ── Alerts Screen ─────────────────────────────────────────────────────────────
@Composable
fun AlertsScreen(bridges: List<Bridge>, padding: PaddingValues, onLocate: (Int) -> Unit) {
    val alerts = bridges.filter { it.status == "Damaged" || it.status == "Submerged" }
        .sortedByDescending { it.lastUpdated }

    Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC))) {
        Row(
            modifier              = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("WARNING CENTER", color = Color(0xFF94A3B8), fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                Text("Critical Alerts", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = SurfaceDark)
            }
            BadgedBox(badge = {
                if (alerts.isNotEmpty()) {
                    Badge(containerColor = StatusSubmerged) { Text("${alerts.size}", color = Color.White, fontSize = 10.sp) }
                }
            }) { Icon(Icons.Default.Notifications, null, tint = SurfaceDark, modifier = Modifier.size(26.dp)) }
        }

        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = StatusOpen, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("All bridges are safe!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SurfaceDark)
                    Text("No active alerts right now.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }
            return@Column
        }

        LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.fillMaxSize()
        ) {
            items(alerts) { bridge ->
                val color    = statusColor(bridge.status)
                val isRecent = System.currentTimeMillis() - bridge.lastUpdated < 600_000
                Surface(
                    shape    = RoundedCornerShape(16.dp),
                    color    = color.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier         = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                if (isRecent) "UPDATED JUST NOW" else formatTime(bridge.lastUpdated).uppercase(),
                                color = if (isRecent) StatusOpen else Color(0xFF94A3B8),
                                fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(bridge.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SurfaceDark)
                        Text(bridge.status.uppercase(), color = color, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick  = { onLocate(bridge.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = SurfaceDark)
                        ) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("LOCATE ON MAP", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Splash Screen ─────────────────────────────────────────────────────────────
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); onFinish() }
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier         = Modifier.size(90.dp).clip(RoundedCornerShape(22.dp)).background(BrandGreen),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("Grama-Sethu", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = BrandGreen)
            Spacer(Modifier.height(6.dp))
            Text("Rural Connectivity Monitoring", color = Color(0xFF64748B), fontSize = 14.sp)
        }
    }
}
package com.solarimaginglab.meego.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.solarimaginglab.meego.model.AppInfo
import com.solarimaginglab.meego.utils.*
import com.solarimaginglab.meego.services.NotificationService
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MeeGoFeed() {
    val notes = NotificationService.currentNotifications
    var temp by remember { mutableStateOf("--") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val res = URL("https://api.open-meteo.com/v1/forecast?latitude=40.41&longitude=-3.70&current_weather=true").readText()
                val json = JSONObject(res).getJSONObject("current_weather")
                temp = "${json.getDouble("temperature").toInt()}°"
            } catch (e: Exception) { temp = "!!" }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(60.dp))
        Text(SimpleDateFormat("EEEE, d MMMM", Locale("es", "ES")).format(Date()).uppercase(), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.ExtraLight)
        Text(temp, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(40.dp))
        Text("NOTIFICACIONES", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        notes.forEach { note ->
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(note.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(note.text, color = Color.LightGray, fontSize = 14.sp)
                HorizontalDivider(Modifier.padding(top = 8.dp), color = Color(0xFF1A1A1A))
            }
        }
        Spacer(Modifier.height(150.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MeeGoAppGrid(apps: List<AppInfo>, columns: Int, onAppClick: (AppInfo) -> Unit, onLongClick: (AppInfo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 150.dp),
        flingBehavior = ScrollableDefaults.flingBehavior() // Scroll más natural
    ) {
        items(
            items = apps,
            key = { it.packageName } // CRÍTICO para rendimiento: evita redibujar todo al hacer scroll
        ) { app ->
            val iconPainter = rememberDrawablePainter(app.icon)

            Column(
                Modifier
                    .padding(8.dp)
                    .graphicsLayer { // Renderizado por GPU
                        clip = true
                        shape = RoundedCornerShape(16.dp)
                    }
                    .combinedClickable(onClick = { onAppClick(app) }, onLongClick = { onLongClick(app) }),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (columns > 4) 48.dp else 60.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                if (columns <= 5) {
                    Text(
                        text = app.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MeeGoSwitcher(recentApps: List<AppInfo>, onAppClick: (AppInfo) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 150.dp)) {
        items(recentApps, key = { "recent_${it.packageName}" }) { app ->
            Box(Modifier.padding(8.dp).aspectRatio(0.85f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF111111)).clickable { onAppClick(app) }, contentAlignment = Alignment.Center) {
                app.icon?.let { Image(rememberDrawablePainter(it), null, Modifier.size(50.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MeeGoDock(isVisible: Boolean, dockSize: Int, onDismiss: () -> Unit, onAppClick: (AppInfo) -> Unit, onLongClick: (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
            Row(Modifier.padding(bottom = 50.dp).background(Color(0xFF1A1A1A).copy(0.98f), RoundedCornerShape(32.dp)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(32.dp)).padding(horizontal = 15.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(dockSize) { i ->
                    val app = LauncherUtils.dockApps.getOrNull(i)
                    Box(Modifier.size(if (dockSize > 4) 46.dp else 54.dp).background(if (app == null) Color.White.copy(0.05f) else Color.Transparent, CircleShape).combinedClickable(onClick = { app?.let { onAppClick(it) } }, onLongClick = { onLongClick(i) }), contentAlignment = Alignment.Center) {
                        app?.icon?.let { Image(rememberDrawablePainter(it), null, Modifier.fillMaxSize()) }
                        if (app == null) Text("+", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(currentCols: Int, currentDock: Int, onDismiss: () -> Unit, onPackSelected: (String?) -> Unit, onColsChanged: (Int) -> Unit, onDockChanged: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { IconPackManager(context) }
    val packs = remember { manager.getInstalledIconPacks() }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ajustes MeeGo", color = Color.White) }, containerColor = Color(0xFF1A1A1A), text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Columnas Rejilla: $currentCols", color = Color.Gray); Slider(value = currentCols.toFloat(), onValueChange = { onColsChanged(it.toInt()) }, valueRange = 3f..6f, steps = 2)
            Spacer(Modifier.height(16.dp))
            Text("Iconos en Dock: $currentDock", color = Color.Gray); Slider(value = currentDock.toFloat(), onValueChange = { onDockChanged(it.toInt()) }, valueRange = 3f..6f, steps = 2)
            Spacer(Modifier.height(20.dp)); Text("Icon Pack", color = Color.White, fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.height(200.dp)) {
                item { TextButton(onClick = { onPackSelected(null) }) { Text("Original", color = Color.Cyan) } }
                items(packs) { pkg -> TextButton(onClick = { onPackSelected(pkg) }) { Text(pkg, color = Color.White) } }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
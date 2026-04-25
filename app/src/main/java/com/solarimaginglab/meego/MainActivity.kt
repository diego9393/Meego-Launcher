package com.solarimaginglab.meego

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import com.solarimaginglab.meego.ui.*
import com.solarimaginglab.meego.utils.*
import com.solarimaginglab.meego.model.AppInfo
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val utils = LauncherUtils(this)

        setContent {
            MaterialTheme {
                val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 1)
                val context = LocalContext.current

                var currentPack by remember { mutableStateOf(utils.getSelectedIconPack()) }
                var gridColumns by remember { mutableIntStateOf(utils.getGridColumns()) }
                var dockSize by remember { mutableIntStateOf(utils.getDockSize()) }
                var refreshTrigger by remember { mutableIntStateOf(0) }

                var isDockVisible by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var selectedAppActions by remember { mutableStateOf<AppInfo?>(null) }

                val apps by produceState(initialValue = emptyList<AppInfo>(), currentPack, refreshTrigger) {
                    value = withContext(Dispatchers.IO) {
                        utils.fetchInstalledApps(currentPack)
                    }
                }

                BackHandler(enabled = isDockVisible) { isDockVisible = false }

                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    HorizontalPager(state = pagerState, userScrollEnabled = !isDockVisible) { page ->
                        when (page) {
                            0 -> MeeGoFeed()
                            1 -> Box {
                                MeeGoAppGrid(apps, gridColumns, { if (!isDockVisible) utils.launchApp(it) }, { selectedAppActions = it })
                                IconButton(onClick = { showSettings = true }, Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                                    Text("⚙️", fontSize = 24.sp)
                                }
                            }
                            2 -> MeeGoSwitcher(LauncherUtils.customRecentHistory) { if (!isDockVisible) utils.launchApp(it) }
                        }
                    }

                    // Zona Gesto Dock
                    Box(Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter).pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount < -15f && !isDockVisible) { isDockVisible = true; change.consume() }
                        }
                    })

                    if (isDockVisible) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount -> if (dragAmount > 15f) isDockVisible = false }
                        })
                    }

                    MeeGoDock(isDockVisible, dockSize, { isDockVisible = false }, { utils.launchApp(it); isDockVisible = false }, { utils.removeDockApp(it) })

                    if (showSettings) {
                        SettingsDialog(gridColumns, dockSize, { showSettings = false }, { utils.saveIconPack(it); currentPack = it }, { utils.saveGridColumns(it); gridColumns = it }, { utils.saveDockSize(it); dockSize = it })
                    }

                    // --- DIÁLOGO DE ACCIONES MODIFICADO ---
                    if (selectedAppActions != null) {
                        AlertDialog(
                            onDismissRequest = { selectedAppActions = null },
                            title = { Text(selectedAppActions!!.name, color = Color.White) },
                            containerColor = Color(0xFF1A1A1A),
                            text = {
                                Column {
                                    Text("Asignar al Dock:", color = Color.Gray, fontSize = 14.sp)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceEvenly) {
                                        (0 until dockSize).forEach { i ->
                                            Button(
                                                onClick = {
                                                    utils.setDockApp(i, selectedAppActions!!)
                                                    selectedAppActions = null
                                                    isDockVisible = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                                contentPadding = PaddingValues(0.dp)
                                            ) { Text("${i + 1}") }
                                        }
                                    }

                                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

                                    // NUEVO: ABRIR INFO DE APP PARA DESINSTALAR
                                    Button(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", selectedAppActions!!.packageName, null)
                                            }
                                            context.startActivity(intent)

                                            selectedAppActions = null
                                            // Al volver de ajustes, refrescamos por si la ha borrado
                                            refreshTrigger++
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444))
                                    ) {
                                        Text("Info de la Aplicación", color = Color.White)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { selectedAppActions = null }) {
                                    Text("CERRAR", color = Color.White.copy(0.6f))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
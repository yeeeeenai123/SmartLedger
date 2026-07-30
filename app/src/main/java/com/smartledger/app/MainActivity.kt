package com.smartledger.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartledger.app.ui.screens.HomeScreen
import com.smartledger.app.ui.screens.StatsScreen
import com.smartledger.app.ui.theme.*
import com.smartledger.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查权限状态
        checkPermissions()

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Green500,
                    secondary = Orange500,
                    background = Background,
                    surface = Surface,
                    error = Red500
                )
            ) {
                MainApp(
                    viewModel = viewModel,
                    onStartAccessibility = ::openAccessibilitySettings,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onAddManualExpense = {}
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次回到前台时重新检查权限状态
        checkPermissions()
    }

    private fun checkPermissions() {
        // 检查无障碍服务
        viewModel.setAccessibilityEnabled(isAccessibilityEnabled())

        // 检查悬浮窗权限
        viewModel.setOverlayPermissionGranted(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else true
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices
            .lowercase()
            .contains("com.smartledger.app".lowercase())
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    onStartAccessibility: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onAddManualExpense: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()

    // 首次启动 / 权限未开启时的引导弹窗
    var showPermissionGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // App 启动时检查，如果权限没开就弹引导
        if (!isAccessibilityEnabled || !isOverlayPermissionGranted) {
            showPermissionGuide = true
        }
    }

    // 权限引导弹窗
    if (showPermissionGuide && (!isAccessibilityEnabled || !isOverlayPermissionGranted)) {
        AlertDialog(
            onDismissRequest = { showPermissionGuide = false },
            title = { Text("开启自动记账", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "自动弹窗记账需要开启以下两个权限，缺一不可：",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    if (!isAccessibilityEnabled) {
                        Text(
                            "1. 无障碍服务 — 监控付款页面\n   点击下方「去开启无障碍」",
                            fontSize = 13.sp,
                            color = Red500,
                            lineHeight = 20.sp
                        )
                    }
                    if (!isOverlayPermissionGranted) {
                        Text(
                            "2. 悬浮窗权限 — 弹出记账窗口\n   点击下方「去开启悬浮窗」",
                            fontSize = 13.sp,
                            color = Red500,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "两个都开启后，付款时会自动弹出记账窗口。",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isAccessibilityEnabled) {
                        TextButton(onClick = onStartAccessibility) {
                            Text("去开启无障碍", color = Green500)
                        }
                    }
                    if (!isOverlayPermissionGranted) {
                        TextButton(onClick = onOpenOverlaySettings) {
                            Text("去开启悬浮窗", color = Green500)
                        }
                    }
                    TextButton(onClick = { showPermissionGuide = false }) {
                        Text("稍后")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "智能记账",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                actions = {
                    // 服务状态指示器
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = if (isAccessibilityEnabled && isOverlayPermissionGranted)
                            Green500.copy(alpha = 0.15f)
                        else
                            Red500.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            if (isAccessibilityEnabled && isOverlayPermissionGranted) " ● 监控中" else " ○ 未监控",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = if (isAccessibilityEnabled && isOverlayPermissionGranted) Green700 else Red500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "记录") },
                    label = { Text("记录") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = "统计") },
                    label = { Text("统计") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onStartAccessibility = onStartAccessibility,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    isOverlayPermissionGranted = isOverlayPermissionGranted,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onAddManualExpense = {}
                )
                1 -> StatsScreen(viewModel = viewModel)
            }
        }
    }
}

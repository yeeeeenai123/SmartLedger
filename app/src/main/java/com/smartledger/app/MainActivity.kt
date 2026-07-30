package com.smartledger.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import com.smartledger.app.service.FloatingWindowService
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
                    onOpenBatterySettings = ::openBatterySettings,
                    onTestPopup = ::testPopup,
                    onAddManualExpense = {}
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        viewModel.setAccessibilityEnabled(isAccessibilityEnabled())
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

    /**
     * 打开电池优化设置 — OPPO/vivo/小米等国产手机
     * 必须关闭电池优化，否则无障碍服务会被系统杀死
     */
    private fun openBatterySettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    // 请求忽略电池优化
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "已关闭电池优化 ✅", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // 如果直接请求不行，跳转到电池优化列表
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "请手动在设置中关闭电池优化", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 手动测试弹窗 — 验证悬浮窗权限和无障碍服务是否正常工作
     */
    private fun testPopup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show()
            openOverlaySettings()
            return
        }

        val intent = Intent(this, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_SHOW
            putExtra(FloatingWindowService.EXTRA_AMOUNT, 88.88)
            putExtra(FloatingWindowService.EXTRA_SOURCE_APP, "测试")
            putExtra(FloatingWindowService.EXTRA_SOURCE_PACKAGE, "com.smartledger.app")
            putExtra(FloatingWindowService.EXTRA_CONFIDENCE, 1.0f)
            putExtra(FloatingWindowService.EXTRA_CATEGORY, "餐饮")
            putExtra(FloatingWindowService.EXTRA_TYPE, "expense")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d("MainActivity", "测试弹窗已发送")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    onStartAccessibility: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onTestPopup: () -> Unit,
    onAddManualExpense: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()

    var showPermissionGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isAccessibilityEnabled || !isOverlayPermissionGranted) {
            showPermissionGuide = true
        }
    }

    // 权限引导弹窗（含电池优化提示）
    if (showPermissionGuide && (!isAccessibilityEnabled || !isOverlayPermissionGranted)) {
        AlertDialog(
            onDismissRequest = { showPermissionGuide = false },
            title = { Text("🔧 开启自动记账", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "自动弹窗记账需要以下设置：",
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
                        "⚠️ OPPO/vivo/小米用户必看：",
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Orange500,
                        lineHeight = 20.sp
                    )
                    Text(
                        "3. 关闭电池优化 — 防止系统杀死后台监控\n   点击下方「去关闭电池优化」",
                        fontSize = 13.sp,
                        color = Orange500,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "以上全部开启后，付款时才会自动弹窗哦～",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isAccessibilityEnabled) {
                            TextButton(onClick = onStartAccessibility) {
                                Text("开启无障碍", color = Green500)
                            }
                        }
                        if (!isOverlayPermissionGranted) {
                            TextButton(onClick = onOpenOverlaySettings) {
                                Text("开启悬浮窗", color = Green500)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onOpenBatterySettings) {
                            Text("关闭电池优化", color = Orange500)
                        }
                        TextButton(onClick = { showPermissionGuide = false }) {
                            Text("稍后")
                        }
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
                    onOpenBatterySettings = onOpenBatterySettings,
                    onTestPopup = onTestPopup,
                    isOverlayPermissionGranted = isOverlayPermissionGranted,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onAddManualExpense = {}
                )
                1 -> StatsScreen(viewModel = viewModel)
            }
        }
    }
}

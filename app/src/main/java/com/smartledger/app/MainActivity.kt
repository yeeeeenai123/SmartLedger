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
                MainApp(viewModel = viewModel, onStartAccessibility = ::openAccessibilitySettings)
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

    private fun openOverlaySettings() {
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
    onStartAccessibility: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsState()

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
                        color = if (isAccessibilityEnabled)
                            Green500.copy(alpha = 0.15f)
                        else
                            Red500.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            if (isAccessibilityEnabled) " ● 监控中" else " ○ 未监控",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = if (isAccessibilityEnabled) Green700 else Red500
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
                    onAddManualExpense = {}
                )
                1 -> StatsScreen(viewModel = viewModel)
            }
        }
    }
}

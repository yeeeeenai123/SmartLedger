package com.smartledger.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机自启动广播接收器
 * 确保无障碍服务在设备重启后可用
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "设备启动完成，智能记账服务已就绪")
            // 无障碍服务由系统自动绑定，无需手动启动
            // 这里可以做一个通知提醒用户检查服务状态
        }
    }
}

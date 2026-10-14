package com.golfrecorder.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.golfrecorder.MainActivity
import com.golfrecorder.domain.model.WatchBatteryNoticeAction
import com.golfrecorder.domain.model.watchBatteryNoticeAction
import com.golfrecorder.domain.model.watchBatteryNoticeBody
import com.golfrecorder.domain.model.watchBatteryNoticeTitle
import com.golfrecorder.wearsync.WearSync

/**
 * 워치가 보낸 배터리 상태([WearSync.BATTERY_PATH])를 폰 알림으로 보여준다. 폰은 주머니나 카트에 있는 경우가 많아
 * 화면 안 배너보다 헤드업 알림이 확실하다. 10% 미만, 5% 미만으로 단계가 올라갈 때마다 한 번 알리고(소리·진동),
 * 같은 단계에서 퍼센트가 바뀌면 조용히 내용만 갱신하며, 경고가 풀리면 지운다. 라운드 서비스([RoundRecordingService])가
 * 떠 있을 때만 호출되고, 서비스가 끝나면 [clear]로 지운다. 알림 권한이 없으면 아무것도 하지 않는다.
 */
object WatchBatteryNotifier {
    private const val TAG = "WatchBatteryNotifier"
    private const val CHANNEL_ID = "watch_battery"
    private const val NOTIFICATION_ID = 3001

    @Volatile
    private var lastWarning = WearSync.BATTERY_WARNING_NONE

    @SuppressLint("MissingPermission") // areNotificationsEnabled()로 권한/채널 차단 여부를 먼저 확인한다
    fun onReport(context: Context, level: Int, warning: Int) {
        val action = watchBatteryNoticeAction(lastWarning, warning)
        lastWarning = warning
        val manager = NotificationManagerCompat.from(context)
        if (action == WatchBatteryNoticeAction.CANCEL) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "notifications are disabled — skip battery warning level=$level warning=$warning")
            return
        }
        ensureChannel(context)
        val body = watchBatteryNoticeBody(warning)
        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(watchBatteryNoticeTitle(level))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            // 단계가 올라갈 때만 소리·진동·헤드업, 같은 단계의 퍼센트 갱신은 조용히.
            .setOnlyAlertOnce(action != WatchBatteryNoticeAction.ALERT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** 라운드가 끝났거나 서비스가 멈출 때 — 알림을 지우고 단계를 초기화한다. */
    fun clear(context: Context) {
        lastWarning = WearSync.BATTERY_WARNING_NONE
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "와치 배터리 경고", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "워치 배터리가 10% 미만, 5% 미만으로 떨어질 때 알려줍니다"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

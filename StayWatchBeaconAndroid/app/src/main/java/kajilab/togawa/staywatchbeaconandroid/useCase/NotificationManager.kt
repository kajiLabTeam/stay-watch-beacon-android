package kajilab.togawa.staywatchbeaconandroid.useCase

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.getSystemService
import kajilab.togawa.staywatchbeaconandroid.MainActivity


class NotificationManager {
    val CHANNEL_ID = "stay6101"

//    private fun createNotificationChannel() {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = getString(R.string)
//            val descriptionText = getString(R.string.channel_description)
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//            // Register the channel with the system
//            val notificationManager: NotificationManager =
//                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }


    fun notifyBeaconActive(context: Context, intent: Intent){
        // ①インテントの作成
//        val intent = Intent(this, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

//        var builder = NotificationCompat.Builder(context, CHANNEL_ID)
////            .setSmallIcon(R.drawable.notification_icon)
//            .setContentTitle("タイトル")
//            .setContentText("コンテンツ")
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//        with(NotificationCompat.from(context)){
//            notify(2222, builder.build())
//        }

//        val channelId = "my_channel_id"
//        val channelName = "My Channel"
//        val importance = NotificationManager.IMPORTANCE_HIGH
//        val channel = NotificationChannel(channelId, channelName, importance)
//
//        val notificationManager = getSystemService(NotificationManager::class.java)
//        notificationManager.createNotificationChannel(channel)
//
//        val notificationId = 1
//        val title = "通知タイトル"
//        val message = "通知メッセージ"



//        //1．通知領域タップで戻ってくる先のActivity
//        val openIntent = Intent(context, MainActivity::class.java).let {
//            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
//        }
//
//        //2．通知チャネル登録
//        val channelId = CHANNEL_ID
//        val channelName = "TestService Channel"
//        val channel = NotificationChannel(
//            channelId, channelName,
//            NotificationManager.IMPORTANCE_DEFAULT
//        )
//        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        manager.createNotificationChannel(channel)
//
//        //4．通知の作成（ここでPendingIntentを通知領域に渡す）
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID )
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("電波発生中")
//            .setContentText("出てます出てます電波が出てます")
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setContentIntent(openIntent)
//            .build()
//
//        //5．フォアグラウンド開始。
//        startForeground(2222, notification)
    }
}
package kajilab.togawa.staywatchbeaconandroid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kajilab.togawa.staywatchbeaconandroid.MainActivity
import kajilab.togawa.staywatchbeaconandroid.R
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import java.util.UUID

class BlePeripheralService: Service() {

    companion object {
        const val CHANNEL_ID = "stw344"
        const val CHANNEL_TITLE = "滞在ウォッチ作動中"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("Service", "スタートアップサービスが起動")

        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        //2．通知チャネル登録
        val channelId = CHANNEL_ID
        val channelName = "TestService Channel"
        val channel = NotificationChannel(
            channelId, channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        //4. 通知の作成
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(CHANNEL_TITLE)
            .setContentText("BLE出してるよ")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .build()

        //5. 通知の表示
        startForeground(1212, notification)

        // BLEアドバタイズのクラスのインスタンス化
        val peripheralManager = BlePeripheralServerManager(this)
        val advertisingUUID:UUID = UUID.fromString("8ebc2114-4abd-ba0d-b7c6-0a00200055")
        peripheralManager.startAdvertising(advertisingUUID)
        //peripheralManager.startAdvertising(UUID.randomUUID())

//        Thread(
//            Runnable {
//                // 10秒かかる処理 (スリープで代用)
//                for (i in 0..200) {
//                    Log.i("service", "サービス中" + i.toString())
//                    Thread.sleep(1000)
//                }
//
//                // フォアグラウンドの停止
//                stopForeground(Service.STOP_FOREGROUND_REMOVE)
//
//                // サービスの停止
//                stopSelf()
//            }).start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("Service", "サービスが終了")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
package kajilab.togawa.staywatchbeaconandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import kajilab.togawa.staywatchbeaconandroid.MainActivity
import kajilab.togawa.staywatchbeaconandroid.R
import kajilab.togawa.staywatchbeaconandroid.broadcast.BeaconBroadcastReceiver
import kajilab.togawa.staywatchbeaconandroid.broadcast.BluetoothStateBroadcastReceiver
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class BlePeripheralService: Service() {

    companion object {
        const val CHANNEL_ID = "stw344"
        const val CHANNEL_TITLE = "滞在ウォッチ作動中"
    }

    private val peripheralServerManager = BlePeripheralServerManager(this)

    private lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        // BroadcastReceiverを登録
        val broadcastReceiver = BluetoothStateBroadcastReceiver()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // notificationManagerの初期化
        notificationManager = NotificationManagerCompat.from(this)

        // サービスが強制終了されて再起動する場合
        if(intent == null){
            setupService()
            return START_STICKY
        }

        when(intent.action){
            Intent.ACTION_SEND -> {
                if(intent.type == "text/bluetooth"){
                    when(intent.getStringExtra("state")){
                        "on" -> {
                            Log.d("Service", "Bluetoothオンを受け取ったよ")
                            onBluetooth()
                        }
                        "off" -> {
                            Log.d("Service", "Bluetoothオフを受け取ったよ")
                            offBluetooth()
                        }
                    }
                }
            }
            else -> {
                // 初期実行
                setupService()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("Service", "サービスが終了")
        //val peripheralManager = BlePeripheralServerManager(this)
        peripheralServerManager.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun setupService() {
        Log.d("Service", "スタートアップサービスが起動")

        // ROOMでデータベースの立ち上げ
        val db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "beacon_database"
        ).build()

        notificationManager = NotificationManagerCompat.from(this)
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("滞在ウォッチ動作中")
            .build()
        notificationManager.createNotificationChannel(channel)

        //manager.notify(CHANNEL_ID, 新しいnotification)

        // アクティビティを起動するIntentを作成
        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.staywatch)
            .setContentTitle("滞在ウォッチ動作中")
            .setContentText("ビーコンアプリが動作中です")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            // BLEアドバタイズ
            val advertisingUUID = peripheralServerManager.getAdvertisingUUID(db)
            if(advertisingUUID == null){
                // UUIDが正しくない場合
                return@launch
            }
            peripheralServerManager.startAdvertising(advertisingUUID)

            //5. 通知の表示
            startForeground(1212, notification)
        }
    }

    private fun updateNotification(title: String, content: String) {
        // アクティビティを起動するIntentを作成
        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.staywatch)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(1212, notification)
    }

    private fun offBluetooth() {
        Log.d("Service", "Bluetoothオフになったよ")

        peripheralServerManager.clear()
        updateNotification("滞在ウォッチ停止中", "Bluetoothをオンにすると再開します")
    }

    private fun onBluetooth(){
        Log.d("Service", "Bluetoothオンになったよ")

        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                this@BlePeripheralService,
                AppDatabase::class.java,
                "beacon_database"
            ).build()

            val advertisingUUID = peripheralServerManager.getAdvertisingUUID(db)
            if(advertisingUUID == null){
                // UUIDが正しくない場合
                return@launch
            }
            peripheralServerManager.startAdvertising(advertisingUUID)
        }
        updateNotification("滞在ウォッチ動作中", "ビーコンアプリが動作中です")
    }

}
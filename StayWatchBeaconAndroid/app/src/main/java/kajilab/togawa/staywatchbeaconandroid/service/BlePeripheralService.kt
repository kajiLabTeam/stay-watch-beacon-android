package kajilab.togawa.staywatchbeaconandroid.service

import android.Manifest
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
import androidx.annotation.RequiresPermission
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
import kajilab.togawa.staywatchbeaconandroid.useCase.CommonUtils
import kajilab.togawa.staywatchbeaconandroid.useCase.SipHash24
import kajilab.togawa.staywatchbeaconandroid.utils.StatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

class BlePeripheralService: Service() {

    companion object {
        const val CHANNEL_ID = "stw344"
        const val CHANNEL_TITLE = "滞在ウォッチ作動中"
        val statusCode = StatusCode
        const val START_ADVERTISE_DELAY:Long = 10000
    }

    private val peripheralServerManager = BlePeripheralServerManager(this)

    private lateinit var notificationManager: NotificationManagerCompat

    private lateinit var commonUtils: CommonUtils
    private lateinit var bluetoothBroadcastReceiver: BluetoothStateBroadcastReceiver
    private lateinit var beaconBroadcastReceiver: BeaconBroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        commonUtils = CommonUtils()
        // BroadcastReceiverを登録
        bluetoothBroadcastReceiver = BluetoothStateBroadcastReceiver()
        val bluetoothIntentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothBroadcastReceiver, bluetoothIntentFilter)

        beaconBroadcastReceiver = BeaconBroadcastReceiver()
        val beaconIntentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        }
        registerReceiver(beaconBroadcastReceiver, beaconIntentFilter)
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
            Intent.ACTION_USER_PRESENT -> {
            // Log.d("ServiceBroadcast", "がめんろっくかいじょされたよおおおお")
                peripheralServerManager.clear()
                handleOnScreen()
            }
            Intent.ACTION_SEND -> {
                if(intent.type == "text/bluetooth"){
                    when(intent.getStringExtra("state")){
                        "on" -> {
                            //Log.d("Service", "Bluetoothオンを受け取ったよ")
                            peripheralServerManager.clear()
                            onBluetooth()
                        }
                        "off" -> {
                            //Log.d("Service", "Bluetoothオフを受け取ったよ")
                            peripheralServerManager.clear()
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
        //Log.d("Service", "サービスが終了")
        // --- ① BLE の停止 ---
        try {
            peripheralServerManager.clear()
        } catch (e: Exception) {
            Log.e("Service", "Failed to clear BLE peripheral", e)
        }
        // --- ② BroadcastReceiver の解除 ---
        try {
            unregisterReceiver(bluetoothBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            // 既に解除されている可能性あり → 無視
        }
        try {
            unregisterReceiver(beaconBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            // 既に解除されている可能性あり → 無視
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setupService() {
        //Log.d("Service", "スタートアップサービスが起動")
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
        //5. 通知の表示
        startForeground(1212, notification)

        CoroutineScope(Dispatchers.IO).launch {
            // BLEアドバタイズ
            // SipHash
            val privBeaconKey = peripheralServerManager.getPrivBeaconKey(db)
            if(privBeaconKey == null){
                // PrivBeaconKeyがない場合
                return@launch
            }
            startAdvertisingPrivBeaconKey(privBeaconKey)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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

    // 画面ロックを解除した際に実行
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleOnScreen() {
//        Log.d("Service", "Screen ON")
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                this@BlePeripheralService,
                AppDatabase::class.java,
                "beacon_database"
            ).build()

            delay(START_ADVERTISE_DELAY)
            // SipHash
            val privBeaconKey = peripheralServerManager.getPrivBeaconKey(db)
            if(privBeaconKey == null){
                // PrivBeaconKeyがない場合
                return@launch
            }
            restartAdvertisingPrivBeaconKey(privBeaconKey)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun offBluetooth() {
        Log.d("Service", "Bluetooth off")

        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                this@BlePeripheralService,
                AppDatabase::class.java,
                "beacon_database"
            ).build()

            delay(START_ADVERTISE_DELAY)
            if(peripheralServerManager.canAdvertise){
                // SipHash
                val privBeaconKey = peripheralServerManager.getPrivBeaconKey(db)
                if(privBeaconKey == null){
                    // PrivBeaconKeyがない場合
                    return@launch
                }
                val msdString = getMSDFromPrivBeaconKey(privBeaconKey)
                delay(START_ADVERTISE_DELAY)
                val err = peripheralServerManager.startAdvertising(null, commonUtils.hexStringToByteArray(msdString))
                if(err == statusCode.NOT_PERMISSION){
                    updateNotification("滞在ウォッチ停止中", "権限「付近のデバイス」を許可してください")
                }else {
                    updateNotification("滞在ウォッチ動作中", "Bluetoothがオフのときは正常に動作しない場合があります")
                }
            }else {
                updateNotification("滞在ウォッチ停止中", "Bluetoothをオンにすると再開します")
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun onBluetooth(){
        Log.d("Service", "Bluetooth on")

        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                this@BlePeripheralService,
                AppDatabase::class.java,
                "beacon_database"
            ).build()

            // SipHash
            val privBeaconKey = peripheralServerManager.getPrivBeaconKey(db)
            if(privBeaconKey == null){
                // PrivBeaconKeyがない場合
                return@launch
            }
            val msdString = getMSDFromPrivBeaconKey(privBeaconKey)
            delay(START_ADVERTISE_DELAY)
            val err = peripheralServerManager.startAdvertising(null, commonUtils.hexStringToByteArray(msdString))
            if(err == statusCode.NOT_PERMISSION){
                updateNotification("滞在ウォッチ停止中", "権限「付近のデバイス」を許可してください")
            }else {
                updateNotification("滞在ウォッチ動作中", "ビーコンアプリが動作中です")
            }
        }
    }

    fun generateRandomHex(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length / 2) // 16進数2文字で1バイト
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun restartAdvertisingPrivBeaconKey(privBeaconKey: String) {    // 通知を新たに出したくない時
        val msdString = getMSDFromPrivBeaconKey(privBeaconKey)
        delay(START_ADVERTISE_DELAY)
        val err = peripheralServerManager.startAdvertising(null, commonUtils.hexStringToByteArray(msdString))
        if(err == statusCode.NOT_PERMISSION){
            updateNotification("滞在ウォッチ停止中", "権限「付近のデバイス」を許可してください")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    suspend fun startAdvertisingPrivBeaconKey(privBeaconKey: String) {  // 通知を新たに出しても良い場合（頻繁にこれを呼び出すと通知がうるさくなる）
        val msdString = getMSDFromPrivBeaconKey(privBeaconKey)
        delay(START_ADVERTISE_DELAY)
//            val err = peripheralServerManager.startAdvertising(null, byteArrayOf(0x0A, 0x0A))
        val err = peripheralServerManager.startAdvertising(null, commonUtils.hexStringToByteArray(msdString))
        if(err == statusCode.NOT_PERMISSION){
            updateNotification("滞在ウォッチ停止中", "権限「付近のデバイス」を許可してください") // updateでもたまに通知が新たに作られる判定になるため頻繁に呼び出しすぎ注意
        }else {
            updateNotification("滞在ウォッチ動作中", "ビーコンアプリが動作中です")
        }
    }

    fun getMSDFromPrivBeaconKey(privBeaconKey: String): String {
        val keyBytes = commonUtils.hexStringToByteArray(privBeaconKey)
        val k0 = commonUtils.toLongLE(keyBytes, 0)
        val k1 = commonUtils.toLongLE(keyBytes, 8)
//            val k0 = 0x03f6f8e76aaa602eL    // 鍵
//            val k1 = 0x10753e3e67b09a0eL    // 鍵
        val sip = SipHash24(k0, k1)

        val msg = generateRandomHex(24)
        Log.d("Service", "ランダムな値は $msg")
//            val msg = "487a1a91364e213d7c67906d".toByteArray()
        val hash = sip.digest(msg.toByteArray())
        Log.d("Service", "ハッシュ値は $hash")
        val msdString = hash.toULong().toString(16) + msg
        Log.d("Service", "発信するMSDは $msdString")
        return msdString
    }

}
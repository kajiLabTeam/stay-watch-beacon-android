package kajilab.togawa.staywatchbeaconandroid

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kajilab.togawa.staywatchbeaconandroid.component.SignInView
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.api.GoogleAuthUiClient
import kajilab.togawa.staywatchbeaconandroid.api.StayWatchClient
import kajilab.togawa.staywatchbeaconandroid.broadcast.BeaconBroadcastReceiver
import kajilab.togawa.staywatchbeaconandroid.component.BeaconView
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.ui.theme.StayWatchBeaconAndroidTheme
import kajilab.togawa.staywatchbeaconandroid.viewModel.BeaconViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : ComponentActivity() {
    private val PERMISSION_REQUEST_CODE = 1


    val peripheralServiceManager = BlePeripheralServerManager(this)
    private val viewModel: BeaconViewModel by viewModels()

    // BLEAdvertise関連
    private lateinit var bleGattCharacteristic: BluetoothGattCharacteristic
    private lateinit var bleGattServer: BluetoothGattServer
    private lateinit var bleLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var bleAdapter: BluetoothAdapter
    private lateinit var bleManager: BluetoothManager


    // firebase関連
//    private lateinit var googleSignInClient: GoogleSignInClient
//    //private lateinit var firebaseAuth: FirebaseAuth
//    private val RC_SIGN_IN = 9001
//    private val firebaseAuth: FirebaseAuth by lazy {
//        FirebaseAuth.getInstance()
//    }
    //private var firebaseAuth: FirebaseAuth? = null

    // firebase関連例のYoutube
    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    // ブロードキャストレシーバー
    private val br: BroadcastReceiver = BeaconBroadcastReceiver()


    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // GoogleSignInOptionsの設定
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(applicationContext.getString(R.string.web_client_id)) // Firebaseコンソールから取得したクライアントID
            .requestEmail()
            .build()

        // ROOMでデータベースの立ち上げ
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "beacon_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        // バッテリーの最適化を外させる
        val tmpIntent = Intent()
        val packageName = packageName
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            tmpIntent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            tmpIntent.data = Uri.parse("package:$packageName")
            startActivity(tmpIntent)
        }

//        val locationPermission = Manifest.permission.BLUETOOTH_ADVERTISE// or ACCESS_COARSE_LOCATION
//        val permissionDenied = ContextCompat.checkSelfPermission(this, locationPermission) == PackageManager.PERMISSION_DENIED
//
//        if (permissionDenied) {
//            Toast.makeText(application, "位置情報の許可をください", Toast.LENGTH_SHORT).show()
//            tmpIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//            tmpIntent.data = Uri.fromParts("package", packageName, null)
//            startActivity(tmpIntent)
//        } else {
//            // すでに位置情報の権限が許可されている場合の処理
//        }


        // 要求する権限
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECEIVE_BOOT_COMPLETED, // 電源起動時のブロードキャストを受け取るための権限
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,    // 付近のデバイス
            Manifest.permission.POST_NOTIFICATIONS
        )

        // パーミッションが許可されていない時の処理
        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            // パーミッションが許可されていない時の処理
            EasyPermissions.requestPermissions(
                this,
                "このアプリでは次の権限が必要です",
                PERMISSION_REQUEST_CODE,
                *permissions
            )
        } else {
            // パーミッションが許可されている時の処理
        }

        val mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (mBluetoothManager != null) {
            val mBluetoothAdapter = mBluetoothManager.adapter
        }

        bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleAdapter = bleManager.getAdapter()

        // BroadcastReceiverを登録
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
        }
        registerReceiver(br, intentFilter)

        // viewModelのステートにデータベースからのユーザ情報を入れる
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.startViewModel(db, application)
        }

        setContent {
            StayWatchBeaconAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //Greeting("Android")
                    if (viewModel.email == null) {
                        SignInView(
                            viewModel,
                            googleAuthUiClient,
                            db,
                            application,
                            peripheralServiceManager
                        )
                    } else {
                        BeaconView(
                            viewModel,
                            googleAuthUiClient,
                            peripheralServiceManager,
                            application,
                            db
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(br)
    }
}
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File


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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Spacer(modifier = Modifier.padding(top = 48.dp))
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
                // ============ 評価実験用のCSVファイル共有ボタン =================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { uploadCSV() },
                        colors = ButtonDefaults.buttonColors(Color(0xFFF8CC45)),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            .size(width = 300.dp, height = 40.dp)
                    ) {
                        Text("評価実験用データ共有", color = Color.Black, fontSize = 16.sp)
                    }
                }
                // ==============================================================
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(br)
    }

    fun uploadCSV(){
        Log.d("TEST", "ボタン押したよ")
        val fileName = "onScreenTimeStamp.csv"
        val csvText = openFileInput(fileName).bufferedReader().readText()
        Log.d("saveJsonTimeStamp", "CSVは")
        Log.d("saveJsonTimeStamp", csvText)
        val file = File(filesDir, fileName)
        val uri = FileProvider.getUriForFile(application, "${application.packageName}.provider", file)
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(
            Intent.createChooser(shareIntent, null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
package kajilab.togawa.staywatchbeaconandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kajilab.togawa.staywatchbeaconandroid.component.BeaconView
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.ui.theme.StayWatchBeaconAndroidTheme
import kajilab.togawa.staywatchbeaconandroid.viewModel.BeaconViewModel
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : ComponentActivity() {
    private val PERMISSION_REQUEST_CODE = 1

//    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
//        // ユーザの許可が得られた場合
//        recreate()
//    }
//
//    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
//        // ユーザの許可が得られなかった場合
//        finish()
//    }


    //private val viewModel = viewModels<BeaconViewModel>()
    val peripheralServiceManager = BlePeripheralServerManager(this)
    private val viewModel: BeaconViewModel by viewModels()
    //private val peripheralServerManager = BlePeripheralServerManager(this)

    // BLEAdvertise関連
    private lateinit var bleGattCharacteristic: BluetoothGattCharacteristic
    private lateinit var bleGattServer: BluetoothGattServer
    private lateinit var bleLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var bleAdapter: BluetoothAdapter
    private lateinit var bleManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 要求する権限
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )

        // パーミッションが許可されていない時の処理
        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            // パーミッションが許可されていない時の処理
            Log.d("debug", "権限欲しいよ")
            EasyPermissions.requestPermissions(this, "権限の説明", PERMISSION_REQUEST_CODE, *permissions)
        }else{
            // パーミッションが許可されている時の処理
            Log.d("debug", "権限許可されているよ")
        }

        val mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (mBluetoothManager != null) {
            val mBluetoothAdapter = mBluetoothManager.adapter
        }

        bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleAdapter = bleManager.getAdapter()

        setContent {
            StayWatchBeaconAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //Greeting("Android")
                    //SignInView()
                    BeaconView(viewModel, peripheralServiceManager)
                }
            }
        }
    }
}



//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    StayWatchBeaconAndroidTheme {
//        //SignInView()
//        BeaconView()
//    }
//}
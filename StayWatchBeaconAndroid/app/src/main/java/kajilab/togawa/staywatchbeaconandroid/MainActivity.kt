package kajilab.togawa.staywatchbeaconandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kajilab.togawa.staywatchbeaconandroid.component.BeaconView
import kajilab.togawa.staywatchbeaconandroid.component.SignInScreen
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.model.FirebaseAuthenticationModel
import kajilab.togawa.staywatchbeaconandroid.ui.theme.StayWatchBeaconAndroidTheme
import kajilab.togawa.staywatchbeaconandroid.viewModel.BeaconViewModel
import kajilab.togawa.staywatchbeaconandroid.viewModel.SignInViewModel
import kotlinx.coroutines.launch
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
    private lateinit var googleSignInClient: GoogleSignInClient
    //private lateinit var firebaseAuth: FirebaseAuth
    private val RC_SIGN_IN = 9001
    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // GoogleSignInOptionsの設定
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(applicationContext.getString(R.string.web_client_id)) // Firebaseコンソールから取得したクライアントID
            .requestEmail()
            .build()

        // GoogleSignInClientの初期化
        googleSignInClient = GoogleSignIn.getClient(this, gso)

//        firebaseAuth = Firebase.auth

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
                    //BeaconView(viewModel, peripheralServiceManager, application)
                    Button(onClick = { signIn() }) {
                        Text("サインイン！")
                    }
                }
            }
        }
    }

    private fun signIn() {
        Log.d("GoogleAuth", "signIn()開始")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        Log.d("GoogleAuth", "signIn()終了")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("GoogleAuth", "onActivityResult()開始")

        // FirebaseAppの初期化
        FirebaseApp.initializeApp(this)

        Log.d("GoogleAuth", "FirebaseAppの初期化完了")

        if (requestCode == RC_SIGN_IN) {
            Log.d("GoogleAuth", "requestCodeおうけい")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Log.d("GoogleAuth", "task=... 完了")
            try {
                Log.d("GoogleAuth", "tryのなか開始")
                // Googleアカウントから認証情報を取得
                val account = task.getResult(ApiException::class.java)

                // ログイン処理のメソッドを呼ぶ
                //firebaseAuthWithGoogle(account)

                // FirebaseでGoogleログインを行う
                Log.d("GoogleAuth", "FirebaseでGoogleログイン開始")
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                Log.d("GoogleAuth", "Googleログイン2")
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { authTask ->
                        Log.d("GoogleAuth", "Googleログイン3")
                        if (authTask.isSuccessful) {
                            // ログイン成功
                            val user = firebaseAuth.currentUser
                            // ここでユーザー情報を利用できます
                            Log.d("MainActivity", user.toString())

                            Log.d("MainActivity", firebaseAuth.toString())
                        } else {
                            // ログイン失敗
                            Log.d("MainActivity", "ログイン失敗")
                        }
                    }
            } catch (e: Exception) {
                // Googleログイン失敗
                Log.d("GoogleAuth", "Googleログイン失敗")
                println(e)
                Log.d("error", e.toString())
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
package kajilab.togawa.staywatchbeaconandroid.viewModel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kajilab.togawa.staywatchbeaconandroid.api.StayWatchClient
import kajilab.togawa.staywatchbeaconandroid.api.UserDetail
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.model.ForegroundBeaconOutputService
import kajilab.togawa.staywatchbeaconandroid.model.SignInResult
import kajilab.togawa.staywatchbeaconandroid.model.User
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService
import kajilab.togawa.staywatchbeaconandroid.state.SignInState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class BeaconViewModel(): ViewModel() {
    // ここでインスタンスの初期化をする
    private val count = MutableLiveData(0)
    var beaconStatus:String by mutableStateOf("停止中")
    var isAdvertising = MutableLiveData(false)

    // firebaseAuth関連


    fun testUser(){
        Log.d("ViewModel", "testUserが実行開始")
        CoroutineScope(Dispatchers.IO).launch {
            userTask()
        }
        Log.d("ViewModel", "testUserが実行終了")
    }

    private suspend fun userTask(){
//        val client = StayWatchClient()
//        val userJson = client.getUser()
//        Log.d("ViewModel", userJson.toString())

        Log.d("ViewModel", "userTask開始")
        //delay(10_000)
        val client = StayWatchClient()
        val user = client.getUser()
        Log.d("Coroutine", user.toString())
        Log.d("ViewModel", "userTask終了")
    }

    // 本番では使わない
    fun startBleAdvertising(peripheralServiceManager: BlePeripheralServerManager){
        Log.d("viewModel", "startBleAdvertisingを開始するよ")
        peripheralServiceManager.clear()
        peripheralServiceManager.startAdvertising(UUID.randomUUID())
    }

    fun startPeripheralService(application: Context){
        val intent = Intent(application, BlePeripheralService::class.java)
        startForegroundService(application, intent)
    }

    fun stopBleAdvertising(peripheralServiceManager: BlePeripheralServerManager){
        Log.d("viewModel", "stopBleAdvertisingを開始するよ")
        peripheralServiceManager.clear()
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startAdvertising(){
//        val intent = Intent(ForegroundBeaconOutputService::class.java)
//        intent.putExtra("UUID", "1")
//        startForegroundService(intent)
//    }
}
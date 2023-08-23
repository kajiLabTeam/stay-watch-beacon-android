package kajilab.togawa.staywatchbeaconandroid.viewModel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.model.ForegroundBeaconOutputService
import java.util.UUID

class BeaconViewModel(): ViewModel() {
    // ここでインスタンスの初期化をする
    private val count = MutableLiveData(0)
    var beaconStatus:String by mutableStateOf("停止中")
    var isAdvertising = MutableLiveData(false)

    //private val serviceIntent = Intent(context,ForegroundBeaconOutputService::class.java)

    fun googleLogin() {
        val c = count.value ?: 0
        Log.d("Button", c.toString())
        count.value = c+1
    }

    fun updateStatus(){
        beaconStatus = "発信中"
        Log.d("Button", "updateStatusだよ")
    }

    fun startBleAdvertising(peripheralServiceManager: BlePeripheralServerManager){
        Log.d("viewModel", "startBleAdvertisingを開始するよ")
        peripheralServiceManager.clear()
        peripheralServiceManager.startAdvertising(UUID.randomUUID())
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
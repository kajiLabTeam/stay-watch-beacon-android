package kajilab.togawa.staywatchbeaconandroid.broadcast

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startForegroundService
import androidx.room.Room
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.db.DBUser
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BeaconBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(application: Context, intent: Intent?) {
        Log.d("Broadcast", "何かを検出したよ")

        when (intent?.action) {

            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d("Broadcast", "ロック解除を検知とアドバタイズ開始")

                //val startService = Intent(application?.applicationContext, BlePeripheralService::class.java)
                val bleIntent = Intent(application, BlePeripheralService::class.java)
                startForegroundService(application, bleIntent)
            }

//            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
//                val bleIntent = Intent(application, BlePeripheralService::class.java)
//                val isAirplaneModeOn = intent.getBooleanExtra("state", false)
//                if(isAirplaneModeOn) {
//                    Log.d("Broadcast", "機内モードオン！")
//
//                    if(isServiceRunning(application, BlePeripheralService::class.java)){
//                        Log.d("Broadcast", "サービスは動いているよ")
//                        val targetIntent = bleIntent.apply {
//                            action = Intent.ACTION_SEND
//                            type = "text/plain"
//                            putExtra("tag", "air_on")
//                        }
//                        startForegroundService(application, targetIntent)
//                    }
//                } else {
//                    Log.d("Broadcast", "機内モードオフ！")
//
//                    if(isServiceRunning(application, BlePeripheralService::class.java)){
//                        Log.d("Broadcast", "サービスは動いているよ")
//                        val targetIntent = bleIntent.apply {
//                            action = Intent.ACTION_SEND
//                            type = "text/plain"
//                            putExtra("tag", "air_off")
//                        }
//                        startForegroundService(application, targetIntent)
//                    }
//                }
//            }

        }
    }

    private fun isServiceRunning(context: Context, serviceClass:Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        for (service in services) {
            if(serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
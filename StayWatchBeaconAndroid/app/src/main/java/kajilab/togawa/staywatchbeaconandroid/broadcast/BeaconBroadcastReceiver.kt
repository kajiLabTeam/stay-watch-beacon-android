package kajilab.togawa.staywatchbeaconandroid.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startForegroundService
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService

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

            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val bleIntent = Intent(application, BlePeripheralService::class.java)
                val isAirplaneModeOn = intent.getBooleanExtra("state", false)
                if(isAirplaneModeOn) {
                    Log.d("Broadcast", "機内モードオン！")

                    val targetIntent = bleIntent.apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra("tag", "air_on")
                    }
                    startForegroundService(application, targetIntent)
                } else {
                    Log.d("Broadcast", "機内モードオフ！")

                    val targetIntent = bleIntent.apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra("tag", "air_off")
                    }
                    startForegroundService(application, targetIntent)
                }
            }

        }
    }
}
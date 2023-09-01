package kajilab.togawa.staywatchbeaconandroid.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startForegroundService
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService

class BeaconBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(application: Context, intent: Intent?) {
        Log.d("Broadcast", "何かを検出したよ")

        when (intent?.action) {
//            Log.d("Broadcast", "なんかを検出したよ")

            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d("Broadcast", "ロック解除を検知とアドバタイズ開始")

                //val startService = Intent(application?.applicationContext, BlePeripheralService::class.java)
                val bleIntent = Intent(application, BlePeripheralService::class.java)
                startForegroundService(application, bleIntent)
            }

        }
    }
}
package kajilab.togawa.staywatchbeaconandroid.broadcast

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService

class BluetoothStateBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(application: Context, intent: Intent?) {
        when(intent?.action){

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                Log.d("Broadcast", "ブルーツースかわっったよー")
                Log.d("Broadcast", BluetoothAdapter.EXTRA_STATE)

                val bleIntent = Intent(application, BlePeripheralService::class.java)

                // val previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, STATE_OFF)

                when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)){
                    BluetoothAdapter.STATE_ON -> {
                        Log.d("Broadcast", "ONになった")
                        val targetIntent = bleIntent.apply {
                            action = Intent.ACTION_SEND
                            type = "text/bluetooth"
                            putExtra("state", "on")
                        }
                        ContextCompat.startForegroundService(application, targetIntent)
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d("Broadcast", "OFFになった")
                        val targetIntent = bleIntent.apply {
                            action = Intent.ACTION_SEND
                            type = "text/bluetooth"
                            putExtra("state", "off")
                        }
                        ContextCompat.startForegroundService(application, targetIntent)
                    }
                }
            }

        }
    }
}
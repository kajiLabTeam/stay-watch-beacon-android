package kajilab.togawa.staywatchbeaconandroid.model

import android.app.Service
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter

class ForegroundBeaconOutputService : Service() {
    companion object {
        const val CHANNEL_ID = "default"
    }

    lateinit var beaconTransmitter: BeaconTransmitter

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onStartCommand(flags: Int, startId: Int): Int {
        Log.d("Service","onStartCommand called")

        val beaconParser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)

        val uuid = "11111111-1111-1111-1111-111111111111"
        val major = "0"
        val minor = "2"

        Log.d("Service", "第二関門突破")

        val beacon = Beacon.Builder()
            .setId1(uuid)
            .setId2(major)
            .setId3(minor)
            .setManufacturer(0x004C)
            .build()

        Log.d("Service", "第三関門突破")

        beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
                Log.d("debug","Advertising成功")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.d("debug","Advertising失敗")
            }
        })

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconTransmitter.stopAdvertising()
    }
}
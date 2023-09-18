package kajilab.togawa.staywatchbeaconandroid.model

import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import androidx.room.Room
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.db.DBUser
import kajilab.togawa.staywatchbeaconandroid.observer.BaseBondingObserver
import kajilab.togawa.staywatchbeaconandroid.observer.BaseConnectionObserver
//import asia.groovelab.blesample.extension.asHexByteArray
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.observer.ServerObserver
import java.lang.Exception
import java.util.*

class BlePeripheralServerManager(private val context: Context) : BleServerManager(context) {
    companion object {
        private const val TAG = "SampleBleServerManager"
        private val serviceUUID = UUID.randomUUID()
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "アドバタイズ開始")
        }

        override fun onStartFailure(errorCode: Int){
            Log.d(TAG, "アドバタイズ失敗")
        }
    }

    val readCharacteristic = characteristic(
        UUID.randomUUID(),
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ
    ).apply {
//        val hexValue = "00"  // 16進数表現の値
//        val byteArray = hexValue.chunked(2) { it.toInt(16).toByte() }.toByteArray()
        value = byteArrayOf(0, 7)
    }
    val writeCharacteristic = characteristic(
        UUID.randomUUID(),
        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )
    val notifyCharacteristic = characteristic(
        UUID.randomUUID(),
        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ,
        cccd()
    )
    val gattService = service(serviceUUID, readCharacteristic, writeCharacteristic, notifyCharacteristic)

    private val connectionObserver = object: BaseConnectionObserver {}
    private val bondingObserver = object: BaseBondingObserver {}
    private val serverObserver = object: ServerObserver {
        override fun onDeviceConnectedToServer(device: BluetoothDevice) {
            //"Not yet implemented"
            setConnectedMangerToServer(device)
        }

        override fun onDeviceDisconnectedFromServer(device: BluetoothDevice) {
            // Not yet implemented
            Log.d(TAG, "サーバーから切断")
        }

        override fun onServerReady() {
            startAdvertising(serviceUUID)
        }
    }

    val canAdvertise: Boolean
        get() = advertiser != null

    private val advertiser: BluetoothLeAdvertiser?
        get() = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser

    init {
        setServerObserver(serverObserver)
    }

    override fun initializeServer() = listOf(gattService)

    override fun log(priority: Int, message: String) {
        Log.d(TAG, "$priority $message")
    }

    // アドバタイズ開始
    fun startAdvertising(uuid: UUID) {
        Log.d("debug", "アドバタイズを試みるよ")
        //S serviceUuid = UUID.randomUUID()
        //Log.d("serverManager", serviceUUID.toString())
        Log.d("serverManager", uuid.toString())
        val advertiseData = AdvertiseData.Builder()
            //.addServiceUuid(ParcelUuid(serviceUUID))
            .addServiceUuid(ParcelUuid(uuid))
            .build()

        val adviserSettings = AdvertiseSettings.Builder()
            //.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setAdvertiseMode(2)
            //.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setTxPowerLevel(3)
            .setTimeout(0)
            .setConnectable(false)
            .build()

        //advertiser?.startAdvertising(adviserSettings, advertiseData, advertiseCallback)
        advertiser?.startAdvertising(adviserSettings, advertiseData, advertiseCallback)
    }

    // アドバタイズ停止
    private fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
    }

    // アドバタイズするUUIDを取得
    fun getAdvertisingUUID(db: AppDatabase): UUID? {

        val dao = db.userDao()

        val dbUser: DBUser? = dao.getUserById(1)
        if(dbUser == null){
            return null
        }
        if(dbUser.uuid == null || dbUser.isAllowedAdvertising == null){
            return null
        }
        if(!dbUser.isAllowedAdvertising){
            return null
        }

        val advertisingUuid = convertUuidFromString(dbUser.uuid)
        if(advertisingUuid == null){
            return null
        }

        return advertisingUuid
    }

    // 8ebc21144abdba0db7c6ff0a0020002b: String -> 8ebc2114-4abd-ba0d-b7c6-ff0a0020002b: String
    private fun formatUuidString(strUuid: String): String {
        val formattedStr = buildString {
            append(strUuid.substring(0,8))
            append("-")
            append(strUuid.substring(8,12))
            append("-")
            append(strUuid.substring(12,16))
            append("-")
            append(strUuid.substring(16,20))
            append("-")
            append(strUuid.substring(20))
        }
        Log.d("ViewModel", "formattedUuid:$formattedStr")
        return formattedStr
    }

    // 8ebc2114-4abd-ba0d-b7c6-ff0a0020002b: String -> 8ebc2114-4abd-ba0d-b7c6-ff0a0020002b: UUID
    private fun convertUuidFromString(strUuid: String): UUID? {
        Log.d("ViewModel", "formatに出すUUID $strUuid")
        val formattedStr = formatUuidString(strUuid)
        //val formattedStr = "8ebc2114-4abd-ba0d-b7c6-ff0a0020002b"
        var resultUuid: UUID? = null
        try{
            resultUuid = UUID.fromString(formattedStr)
            Log.d("ViewModel", "uuid: $resultUuid")
        } catch(e: Exception){
            return null
        }
        return resultUuid
    }

    fun clear() {
        close()
        stopAdvertising()
    }

    private fun setConnectedMangerToServer(device: BluetoothDevice) {
        Log.d("debug", "コネクトされたよ")
    }

//    private fun sendNotificationForWriteRequest(connectedManager: SampleConnectedBleManager, value: ByteArray?) {
//        value?.let {
//            connectedManager.sendNotification(notifyCharacteristic, "ff".asHexByteArray) {
//                Log.d(TAG, "send notification")
//            }
//        }
//    }
}
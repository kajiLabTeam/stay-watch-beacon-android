package kajilab.togawa.staywatchbeaconandroid.viewModel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.Global.AIRPLANE_MODE_ON
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import kajilab.togawa.staywatchbeaconandroid.api.StayWatchClient
import kajilab.togawa.staywatchbeaconandroid.broadcast.BeaconBroadcastReceiver
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.db.DBUser
import kajilab.togawa.staywatchbeaconandroid.db.UserDao
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.model.SignInResult
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService
import kajilab.togawa.staywatchbeaconandroid.state.SignInState
import kajilab.togawa.staywatchbeaconandroid.useCase.EncryptedSharePreferencesManager
import kajilab.togawa.staywatchbeaconandroid.useCase.ServiceState
import kajilab.togawa.staywatchbeaconandroid.utils.StatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

class BeaconViewModel(): ViewModel() {
    // ここでインスタンスの初期化をする
    private val count = MutableLiveData(0)
    var beaconStatus:String by mutableStateOf("停止中")
    //var isAdvertising = MutableLiveData(false)
    var isLoading by mutableStateOf(false)

    private val statusCode = StatusCode

    var userName by mutableStateOf("")
    var email: String? by mutableStateOf(null)
    var communityName by mutableStateOf("")
    var latestSyncTime by mutableStateOf("")

    var isAdvertising by mutableStateOf(false)

    // firebaseAuth関連
    private val _state = MutableStateFlow(SignInState())

    fun startViewModel(db: AppDatabase, context: Context){
        val dao = db.userDao()
        //var user = DBUser(1,null,null,null,null,null)
        //dao.deleteUserById(1)
        val user = dao.getUserById(1)
        if(user == null){
            // 端末にユーザ情報がない場合(アプリ初起動時)
            Log.d("StartViewModel", "端末にユーザ情報なし")
            return
        }

        if(user.name == null || user.communityName == null || user.latestSyncTime == null || user.isAllowedAdvertising == null){
            // サーバに登録されていないユーザでサインインしている状態のときはemailだけ入れる
            Log.d("StartViewModel", "サーバに登録されていないユーザです")
            email = user.email
            latestSyncTime = user.latestSyncTime!!
            return
        }
        Log.d("StartViewModel", "$user")
        userName = user.name
        email = user.email
        communityName = user.communityName
        latestSyncTime = user.latestSyncTime
        isAdvertising = user.isAllowedAdvertising

        Log.d("StartViewModel", "サービス開始するかどうか")
        val serviceState = ServiceState()
        if(user.isAllowedAdvertising && !serviceState.isServiceRunning(context, BlePeripheralService::class.java)){
            Log.d("StartViewModel", "サービスが再開")
            startForegroundService(context, Intent(context, BlePeripheralService::class.java))
        }
    }

    fun onSignInResult(result: SignInResult) {
        _state.update {it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        )}
    }

    fun getColorByTheme(colorInLightTheme: Color, isDarkTheme: Boolean): Color{
        Log.d("ViewModel", "ダークモードですか $isDarkTheme")
        if(isDarkTheme){
            Log.d("ViewModel", "ダークモードだよ")
            when(colorInLightTheme){
                Color.White -> return Color.Black
                Color.Black -> return Color.White
            }
        }
        return colorInLightTheme
    }

    /**
     * 返す値：400 or 410 or 450 or null
     */
    suspend fun signInUser(gmail:String?, token:String, db:AppDatabase, context:Context, peripheralServiceManager: BlePeripheralServerManager): Number?{
        val encryptedSharedPreferencesManager = EncryptedSharePreferencesManager(context)

        Log.d("ViewModel", "トークンとメールアドレス保存するぞう")
        Log.d("ViewModel", "$gmail")
        Log.d("ViewModel", token)

        // トークンを保存
        var error = encryptedSharedPreferencesManager.storeString("TOKEN", token)
        if(error != null){
            Log.d("ViewModel", "トークンの保存に失敗しました")
            // 失敗しても処理は続行してもよいためreturnはなし
        }

        // ここから合併できそう
        val errorCode = storeUserAndStartService(context, db, peripheralServiceManager, token, gmail)
        if(errorCode != null){
            Log.d("ViewModel", "サービス開始できませんでした")
//            withContext(Dispatchers.Main){
//                Toast.makeText(context, "サインイン失敗", Toast.LENGTH_SHORT).show()
//            }
            return errorCode
        }

        email = gmail
        isAdvertising
        return null
    }

    /**
     * 同期ボタン
     * 返す値：400 or 410 or 430 or 450 or null
     */
    suspend fun syncUser(db:AppDatabase, context: Context, peripheralServiceManager: BlePeripheralServerManager): Number?{
        val dao = db.userDao()
        val encryptedSharePreferencesManager = EncryptedSharePreferencesManager(context)

        // トークンの取得
        val (token, error) = encryptedSharePreferencesManager.getString("TOKEN")
        if(error != null){
            Log.d("ViewModel", "端末からトークンを取得するのに失敗しました")
            return statusCode.UNABLE_GET_TOKEN_FROM_DEVICE
        }
        Log.d("ViewModel", "GoogleIDトークン：$token")

        // gmailの取得
        val currentUser = dao.getUserById(1)
        val gmail = currentUser.email

        val errorCode = storeUserAndStartService(context, db, peripheralServiceManager, token, gmail)
        if(errorCode != null){
            Log.d("ViewModel", "サービス開始できませんでした")
            return errorCode
        }

        return null
    }

    /**
     * 返す値：400 or 410 or 450 or Null
     */
    private suspend fun storeUserAndStartService(context:Context, db:AppDatabase, peripheralServiceManager: BlePeripheralServerManager, token: String, gmail: String?) : Number?{
        val dao = db.userDao()

        // 現在時刻の取得
        val formatter = SimpleDateFormat("yyyy-M-d H:mm")
        // UIに反映
        latestSyncTime = formatter.format(Date())

        Log.d("ViewModel", "gmailの中身は：$gmail")

        // ネットに繋がらない場合gmailはnullが入るため
        if(gmail == null){
            // ネットに繋がらない場合はサービスの開始停止、データベースの更新何もしない
            return statusCode.NO_NETWORK_CONNECTION
        }

        // サーバーからユーザ情報を取得
        Log.d("ViewModel", "GoogleIDトークンを使って滞在ウォッチサーバからユーザ取得するぞう")
        val stayWatchClient = StayWatchClient()
        val user = stayWatchClient.postPrivBeaconKeyToServer(token)
        if(user.errorMessage != null){
            if(user.errorStatus != statusCode.NO_NETWORK_CONNECTION){
                // サーバーからユーザを取得するのが失敗したときの処理
                dao.createUser(DBUser(
                    id = 1,
                    name = null,
                    uuid = null,
                    email = gmail,
                    privbeaconKey = null,
                    communityName = null,
                    latestSyncTime = latestSyncTime,
                    isAllowedAdvertising = false
                ))
            }
            // UIに反映
            email = gmail
            print(user.errorMessage)
            return user.errorStatus
        }
        Log.d("ViewModel", "ユーザ情報：" + user.data?.userName)

        // ユーザ情報を上書きするためデータベースへ保存(ユーザは一人であるためidは1固定)
        dao.createUser(DBUser(
            id = 1,
            name = user.data?.userName,
            uuid = null,
            email = gmail,
            privbeaconKey = user.data?.privbeaconKey,
            communityName = user.data?.communityName,
            latestSyncTime = latestSyncTime,
            isAllowedAdvertising = false
        ))


        // UIに反映
        if(user.data == null){
            // ユーザ情報がバックエンドにない場合(削除されたユーザの場合)
            userName = ""
            communityName = ""
            peripheralServiceManager.clear()
            isAdvertising = false
            return null
        }
        // ＝ユーザ情報がバックエンドに登録されている場合の処理＝
        // UIへ反映
        userName = user.data.userName
        communityName = user.data.communityName
        isAdvertising = true

        // ペリフェラルサービスを開始
        val intent = Intent(context, BlePeripheralService::class.java)
        context.stopService(intent)
        startForegroundService(context, intent)

        dao.updateAdvertisingAllowance(true)

        return null
    }

    suspend fun signOut(db:AppDatabase, context:Context, peripheralServiceManager: BlePeripheralServerManager){
        val dao = db.userDao()
        val encryptedSharedPreferencesManager = EncryptedSharePreferencesManager(context)

        // データベースからユーザ情報の削除
        dao.deleteUserById(1)

        // UI部分の変更を反映
        email = null
        userName = ""
        communityName = ""
        latestSyncTime = ""
        isAdvertising = false


        // ペリフェラルサービスを停止
//        peripheralServiceManager.clear()
        val intent = Intent(context, BlePeripheralService::class.java)
        context.stopService(intent)
        dao.updateAdvertisingAllowance(true)


        // 保存されているトークンを削除
        var error = encryptedSharedPreferencesManager.deleteString("TOKEN")
        if(error != null){
            Log.d("ViewModel", "トークンの削除に失敗しました")
            return
        }

    }

    /**
     * 「発信を開始する」ボタン押したとき
     * 返す値：401 or 440 or Null
     */
    fun startAdvertisingService(db: AppDatabase, peripheralServiceManager: BlePeripheralServerManager, context: Context): Number?{
        val dao = db.userDao()
        Log.d("ViewModel", "アドバタイジングサービスをスタート")

        // アドバタイジング開始
        val intent = Intent(context, BlePeripheralService::class.java)
        context.stopService(intent)
        startForegroundService(context, intent)

        // アドバタイジングの許可の有無をUIへ反映
        isAdvertising = true
        // アドバタイジングの許可の有無をデータベースへ保存
        dao.updateAdvertisingAllowance(true)

        return null
    }

    /**
     * 「発信を停止する」ボタンを押したとき
     * 返す値：Null
     */
    fun stopAdvertisingService(db: AppDatabase, peripheralServiceManager: BlePeripheralServerManager, context: Context): Number?{
        val dao = db.userDao()
        Log.d("ViewModel", "アドバタイジングサービスをストップ")

        // アドバタイジング停止
        val intent = Intent(context, BlePeripheralService::class.java)
        context.stopService(intent)

        // アドバタイジングの許可の有無をUIへ反映
        isAdvertising = false
        // UUIDをデータベースへ保存
        dao.updateAdvertisingAllowance(false)

        return null
    }

    fun isAndroidBeaconUUID(uuid: String): Boolean{
        if(uuid.length != 32 || uuid[23] != 'a'){
            return false
        }
        return true
    }


    fun testUser(){
        Log.d("ViewModel", "testUserが実行開始")
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("ViewModel", "トークンを使って滞在ウォッチサーバからユーザ取得するぞう")
            val stayWatchClient = StayWatchClient()
            val user = stayWatchClient.postPrivBeaconKeyToServer("testtokne")
            Log.d("ViewModel", "ユーザ情報：" + user.data?.userName)
            Log.d("ViewModel", "Errorメッセージ：" + user.errorMessage)
        }
        Log.d("ViewModel", "testUserが実行終了")
    }

    fun startBleAdvertising(context: Context){
        Log.d("viewModel", "startBleAdvertisingを開始するよ")

        val intent = Intent(context, BlePeripheralService::class.java)
        startForegroundService(context, intent)
    }

    fun stopBleAdvertising(context: Context){
        Log.d("viewModel", "stopBleAdvertisingを開始するよ")

        val intent = Intent(context, BlePeripheralService::class.java)
        context.stopService(intent)
    }

    fun showSetting(context: Context){
        val tmpIntent = Intent()
        tmpIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        tmpIntent.data = Uri.fromParts("package", context.packageName, null)
        tmpIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // FLAG_ACTIVITY_NEW_TASK フラグを設定
        context.startActivity(tmpIntent)
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

}
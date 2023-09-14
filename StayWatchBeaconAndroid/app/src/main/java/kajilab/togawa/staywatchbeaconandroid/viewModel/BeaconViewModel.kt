package kajilab.togawa.staywatchbeaconandroid.viewModel

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import kajilab.togawa.staywatchbeaconandroid.api.StayWatchClient
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.db.DBUser
import kajilab.togawa.staywatchbeaconandroid.db.UserDao
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.model.SignInResult
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService
import kajilab.togawa.staywatchbeaconandroid.state.SignInState
import kajilab.togawa.staywatchbeaconandroid.useCase.EncryptedSharePreferencesManager
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
    var isLoading by mutableStateOf(true)

    private val statusCode = StatusCode

    var userName by mutableStateOf("")
    var uuid by mutableStateOf("")
    var email: String? by mutableStateOf(null)
    var communityName by mutableStateOf("")
    var latestSyncTime by mutableStateOf("")

    var isAdvertising by mutableStateOf(false)

    // firebaseAuth関連
    private val _state = MutableStateFlow(SignInState())

    fun startViewModel(db: AppDatabase){
        val dao = db.userDao()
        //var user = DBUser(1,null,null,null,null,null)
        val user = dao.getUserById(1)
        if(user == null){
            // 端末にユーザ情報がない場合(アプリ初起動時)
            Log.d("StartViewModel", "端末にユーザ情報なし")
            return
        }

        if(user.name == null || user.uuid == null || user.communityName == null || user.latestSyncTime == null){
            // サーバに登録されていないユーザでサインインしている状態のときはemailだけ入れる
            email = user.email
            return
        }
        userName = user.name!!
        uuid = user.uuid!!
        email = user.email
        communityName = user.communityName!!
        latestSyncTime = user.latestSyncTime!!
    }

    fun onSignInResult(result: SignInResult) {
        _state.update {it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        )}
    }

    // 返す値：400 or 410 or Null
    suspend fun signInUser(gmail:String, token:String, db:AppDatabase, context:Context, peripheralServiceManager: BlePeripheralServerManager): Number?{
        val encryptedSharedPreferencesManager = EncryptedSharePreferencesManager(context)

        Log.d("ViewModel", "トークンとメールアドレス保存するぞう")
        Log.d("ViewModel", gmail)
        Log.d("ViewModel", token)

        // トークンを保存
        var error = encryptedSharedPreferencesManager.storeString("TOKEN", token)
        if(error != null){
            Log.d("ViewModel", "トークンの保存に失敗しました")
            // 失敗しても処理は続行してもよいためreturnはなし
        }

        // ここから合併できそう
        val errorCode = storeUserAndStartService(db, peripheralServiceManager, token, gmail)
        if(errorCode != null){
            Log.d("ViewModel", "サービス開始できませんでした")
//            withContext(Dispatchers.Main){
//                Toast.makeText(context, "サインイン失敗", Toast.LENGTH_SHORT).show()
//            }
            return errorCode
        }

        email = gmail
        return null
    }

    /**
     * 返す値：400 or 410 or 430 or null
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
        val gmail = currentUser.email.toString()

        val errorCode = storeUserAndStartService(db, peripheralServiceManager, token, gmail)
        if(errorCode != null){
            Log.d("ViewModel", "サービス開始できませんでした")
//            withContext(Dispatchers.Main){
//                Toast.makeText(context, "同期失敗", Toast.LENGTH_SHORT).show()
//            }
            return errorCode
        }

//        withContext(Dispatchers.Main){
//            Toast.makeText(context, "同期完了", Toast.LENGTH_SHORT).show()
//        }

        return null
    }

    /**
     * 返す値：400 or 410 or Null
     */
    private suspend fun storeUserAndStartService(db:AppDatabase, peripheralServiceManager: BlePeripheralServerManager, token: String, gmail: String) : Number?{
        val dao = db.userDao()

        // サーバーからユーザ情報を取得
        Log.d("ViewModel", "GoogleIDトークンを使って滞在ウォッチサーバからユーザ取得するぞう")
        val stayWatchClient = StayWatchClient()
        val user = stayWatchClient.getUserFromServer(token)
        if(user.errorMessage != null){
            // サーバーからユーザを取得するのが失敗したら終了
            print(user.errorMessage)
            return user.errorStatus
        }
        Log.d("ViewModel", "ユーザ情報：" + user.data?.userName)


        // 現在時刻の取得
        val formatter = SimpleDateFormat("yyyy-M-d H:mm")
        latestSyncTime = formatter.format(Date())

        // ユーザ情報を上書きするためデータベースへ保存(ユーザは一人であるためidは1固定)
        dao.createUser(DBUser(
            id = 1,
            name = user.data?.userName,
            uuid = user.data?.uuid,
            email = gmail,
            communityName = user.data?.communityName,
            latestSyncTime = latestSyncTime
        ))


        // UIに反映
        if(user.data == null){
            // ユーザ情報がバックエンドにない場合(削除されたユーザの場合)
            userName = ""
            uuid = ""
            communityName = ""
            peripheralServiceManager.clear()
            isAdvertising = false
            return null
        }
        // ユーザ情報がバックエンドに登録されている場合
        // UUIDをStringからUUIDの型へ変換
        val advertisingUuid = convertUuidFromString(user.data.uuid)
        if(advertisingUuid == null){
            Log.d("ViewModel", "UUIDの型変換に失敗しました")
            peripheralServiceManager.clear()
            isAdvertising = false
            return null
        }

        // UIへ反映
        userName = user.data.userName
        uuid = user.data.uuid
        communityName = user.data.communityName

        // ペリフェラルサービスを開始
        peripheralServiceManager.clear()
        peripheralServiceManager.startAdvertising(advertisingUuid)
        Log.d("ViewModel", "${advertisingUuid}をアドバタイズするよ")

        return null
    }

    suspend fun signOut(db:AppDatabase, context:Context){
        val dao = db.userDao()
        val encryptedSharedPreferencesManager = EncryptedSharePreferencesManager(context)

        // データベースからユーザ情報の削除
        dao.deleteUserById(1)

        // UI部分の変更を反映
        email = null
        userName = ""
        uuid = ""
        communityName = ""
        latestSyncTime = ""


        // ペリフェラルサービスを停止


        // 保存されているトークンを削除
        var error = encryptedSharedPreferencesManager.deleteString("TOKEN")
        if(error != null){
            Log.d("ViewModel", "トークンの削除に失敗しました")
            return
        }

    }


    fun testUser(){
        Log.d("ViewModel", "testUserが実行開始")
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("ViewModel", "トークンを使って滞在ウォッチサーバからユーザ取得するぞう")
            val stayWatchClient = StayWatchClient()
            val user = stayWatchClient.getUserFromServer("testtokne")
            Log.d("ViewModel", "ユーザ情報：" + user.data?.userName)
            Log.d("ViewModel", "Errorメッセージ：" + user.errorMessage)
        }
        Log.d("ViewModel", "testUserが実行終了")
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
package kajilab.togawa.staywatchbeaconandroid.viewModel

import android.content.Context
import android.content.Intent
import android.util.Log
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
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.model.SignInResult
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService
import kajilab.togawa.staywatchbeaconandroid.state.SignInState
import kajilab.togawa.staywatchbeaconandroid.useCase.EncryptedSharePreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

class BeaconViewModel(): ViewModel() {
    // ここでインスタンスの初期化をする
    private val count = MutableLiveData(0)
    var beaconStatus:String by mutableStateOf("停止中")
    var isAdvertising = MutableLiveData(false)

    var userName by mutableStateOf("")
    var uuid by mutableStateOf("")
    var email: String? by mutableStateOf(null)
    var communityName by mutableStateOf("")
    var latestSyncTime by mutableStateOf("")

    // firebaseAuth関連
    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()
    var isSignInSuccessful = false

    fun onSignInResult(result: SignInResult) {
        _state.update {it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        )}
    }

    fun resetState() {
        _state.update { SignInState() }
    }

    suspend fun storeUserAndToken(gmail:String, token:String, db:AppDatabase, context:Context){
        val dao = db.userDao()
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

        // トークン使って滞在ウォッチサーバからユーザ情報取得
        Log.d("ViewModel", "GoogleIDトークンを使って滞在ウォッチサーバからユーザ取得するぞう")
        val stayWatchClient = StayWatchClient()
        val user = stayWatchClient.getUserFromServer(token)
        Log.d("ViewModel", "ユーザ情報：" + user.data?.userName)

        // 現在時刻の取得
        val formatter = SimpleDateFormat("yyyy-M-d H:mm")
        latestSyncTime = formatter.format(Date())
        if(user.errorMessage != null){
            // ユーザの取得が失敗したらメールアドレスと同期時刻だけデータベースへ保存しておしまい
            dao.createUser(DBUser(
                id = 1,
                name = null,
                uuid = null,
                email = gmail,
                communityName = null,
                latestSyncTime = latestSyncTime
            ))
            print(user.errorMessage)
            return
        }

        // ユーザ情報をデータベースへ保存(ユーザは一人であるためidは1固定)
        dao.createUser(DBUser(
            id = 1,
            name = user.data?.userName,
            uuid = user.data?.uuid,
            email = gmail,
            communityName = user.data?.communityName,
            latestSyncTime = latestSyncTime
        ))

//        val dbUser = dao.getUserById(1)
//        Log.d("ViewModel", "DBの内容: $dbUser")

        // UI部分の変更を反映
        // emailはgoogleClientからのを使用
        email = gmail
        if(user.data != null){
            userName = user.data.userName
            uuid = user.data.uuid
            communityName = user.data.communityName
        }

        // ペリフェラルサービスを開始
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
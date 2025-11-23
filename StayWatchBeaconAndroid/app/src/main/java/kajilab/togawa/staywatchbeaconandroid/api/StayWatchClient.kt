package kajilab.togawa.staywatchbeaconandroid.api

import android.util.JsonToken
import android.util.Log
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.gson.Gson
import kajilab.togawa.staywatchbeaconandroid.model.StayWatchServerResult
import kajilab.togawa.staywatchbeaconandroid.model.StayWatchUser
import kajilab.togawa.staywatchbeaconandroid.model.User
import kajilab.togawa.staywatchbeaconandroid.model.UserGetResponse
import kajilab.togawa.staywatchbeaconandroid.model.UserPostResponse
import kajilab.togawa.staywatchbeaconandroid.useCase.RsaEncryptor
import kajilab.togawa.staywatchbeaconandroid.utils.BeaconID
import kajilab.togawa.staywatchbeaconandroid.utils.StatusCode
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class StayWatchClient {
    //private val url = "https://apppppp.com/jojo.json"
//    private val url = "https://staywatch-backend.kajilab.net/api/v1/check"
//    private val url = "http://192.168.101.14:8082/api/v1/users/key"
    private val url = "http://192.168.0.8:8082/api/v1/users/key"
//    private val url = "https://staywatch-backend.kajilab.net/api/v1/users/key"
    private val statusCode = StatusCode

    /**
     * 返すエラーコード：400 or 410 or 450 or null
     */
    fun getUserFromServer(googleIdToken: String): StayWatchServerResult {
        // GoogleIDトークン(GoogleIDのトークン)からFirebaseIDトークン(プロジェクト内でのトークン)を取得
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithCredential(credential)
        val firebaseUser = firebaseAuth.currentUser
        // Log.d("StayWatchClient", "firebaseAuthは $firebaseUser")
        var firebaseIdToken:String? = null
        try {
            firebaseIdToken = firebaseUser?.getIdToken(false)?.result?.token
        }catch (e: Exception){
            // googleIdトークンが無効なエラー
            return StayWatchServerResult(
                data = null,
                errorMessage = e.message.toString(),
                errorStatus = statusCode.INVALID_GOOGLE_TOKEN
            )
        }

        //val firebaseIdToken = firebaseUser?.getIdToken(false)?.result?.token
        Log.d("StayWatchClient", "firebaseAuthは $firebaseIdToken")

        // FirebaseIDトークンを用いて滞在ウォッチサーバからユーザ情報取得
        val (request, response, result) = url.httpGet()
            .header(Headers.AUTHORIZATION to "Bearer $firebaseIdToken")
            .responseJson()
        //Log.d("StayWatchClient", "リクエスト：$request")
        //Log.d("StayWatchClient", "レスポンス：$response")

        return when (result) {
            // 失敗時
            is Result.Failure -> {
                // 通信失敗エラー
                Log.d("API", "API通信失敗")
                val ex = result.getException()
                println(ex)

                val httpStatusCode = result.getException().response.statusCode

                if(httpStatusCode == 500){
                    // サーバーには繋がるがユーザ情報が見つからない時の返り値
                    StayWatchServerResult(
                        data = null,
                        errorMessage = ex.message.toString(),
                        errorStatus = statusCode.UNABLE_FIND_USER_IN_SERVER
                    )
                }else{
                    // サーバーに接続できない時の返り値
                    StayWatchServerResult(
                        data = null,
                        errorMessage = ex.message.toString(),
                        errorStatus = statusCode.NO_NETWORK_CONNECTION
                    )
                }
            }

            // 成功時
            is Result.Success -> {
                Log.d("API", "API通信成功")
                // resultからBodyの部分を取り出す
                val resultJson = result.get().obj()
                // Jsonをパースする
                val responseUser = Gson().fromJson(resultJson.toString(), UserGetResponse::class.java)
                println(responseUser)
                //Log.d("API", "ユーザ名：${responseUser}")

                // 返り値
                StayWatchServerResult(
                    data = StayWatchUser(
                        userName = responseUser.name,
                        uuid = responseUser.uuid,
                        communityName = responseUser.communityName
                    ),
                    errorMessage = null,
                    errorStatus = null
                )
            }
        }
    }

    /**
     * 返すエラーコード：400 or 410 or 450 or null
     */
    fun postPrivBeaconKeyToServer(googleIdToken: String): StayWatchServerResult {
        // ======== FirebaseAuthのトークンを取得 ========
        // GoogleIDトークン(GoogleIDのトークン)からFirebaseIDトークン(プロジェクト内でのトークン)を取得
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithCredential(credential)
        val firebaseUser = firebaseAuth.currentUser
        // Log.d("StayWatchClient", "firebaseAuthは $firebaseUser")
        Log.d("StayWatchClient", "POSTされた！！")
        var firebaseIdToken:String? = null
        try {
            firebaseIdToken = firebaseUser?.getIdToken(false)?.result?.token
        }catch (e: Exception){
            // googleIdトークンが無効なエラー
            return StayWatchServerResult(
                data = null,
                errorMessage = e.message.toString(),
                errorStatus = statusCode.INVALID_GOOGLE_TOKEN
            )
        }

        // ======== Postで送るPrivBeaconの鍵(暗号化済み)を用意 ========
        val encryptor = RsaEncryptor()
        // PrivBeaconの鍵を生成
        val privbeaconKey = encryptor.generatePrivBeaconKey()
        Log.d("StayWatchClient", "privbeaconKey(暗号化前)は $privbeaconKey")
        // PrivBeaconの鍵を暗号化
        val privbeaconKeyEncryption = encryptor.encrypt(privbeaconKey)

        //val firebaseIdToken = firebaseUser?.getIdToken(false)?.result?.token
        Log.d("StayWatchClient", "firebaseAuthは $firebaseIdToken")
        Log.d("StayWatchClient", "privbeaconKey(暗号化済み)は $privbeaconKeyEncryption")

        // ======== サーバにPrivBeaconの鍵をPostし，ユーザ情報を取得 ========
        // FirebaseIDトークンを用いて滞在ウォッチサーバからユーザ情報取得
        val (request, response, result) = url.httpPost()
            .header(Headers.AUTHORIZATION to "Bearer $firebaseIdToken")
            .jsonBody("""{"beaconId":${BeaconID.BEACON_ID_ANDROID_PRIVBEACON},"key":"$privbeaconKeyEncryption"}""")
            .responseJson()
        //Log.d("StayWatchClient", "リクエスト：$request")
        Log.d("StayWatchClient", "レスポンス：$response")

        return when (result) {
            // 失敗時
            is Result.Failure -> {
                // 通信失敗エラー
                Log.d("API", "API通信失敗")
                val ex = result.getException()
                println(ex)

                val httpStatusCode = result.getException().response.statusCode

                if(httpStatusCode == 500){
                    // サーバーには繋がるがユーザ情報が見つからない時の返り値
                    StayWatchServerResult(
                        data = null,
                        errorMessage = ex.message.toString(),
                        errorStatus = statusCode.UNABLE_FIND_USER_IN_SERVER
                    )
                }else{
                    // サーバーに接続できない時の返り値
                    StayWatchServerResult(
                        data = null,
                        errorMessage = ex.message.toString(),
                        errorStatus = statusCode.NO_NETWORK_CONNECTION
                    )
                }
            }

            // 成功時
            is Result.Success -> {
                Log.d("API", "API通信成功")
                // resultからBodyの部分を取り出す
                val resultJson = result.get().obj()
                // Jsonをパースする
                val responseUser = Gson().fromJson(resultJson.toString(), UserPostResponse::class.java)
                println(responseUser)
                //Log.d("API", "ユーザ名：${responseUser}")

                // 返り値
                StayWatchServerResult(
                    data = StayWatchUser(
                        userName = responseUser.name,
                        uuid = responseUser.uuid,
                        communityName = responseUser.communityName
                    ),
                    errorMessage = null,
                    errorStatus = null
                )
            }
        }
    }
}
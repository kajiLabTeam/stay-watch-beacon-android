package kajilab.togawa.staywatchbeaconandroid.api

import android.util.JsonToken
import android.util.Log
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.httpGet
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
    private val url = "https://staywatch-backend.kajilab.net/api/v1/check"
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
        //Log.d("StayWatchClient", "firebaseAuthは $firebaseIdToken")

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
}
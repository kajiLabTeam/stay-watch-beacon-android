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

data class TmpUser (
    val id: Int,
    val uuid: String,
    val name: String,
    val role: Int,
    val communityName: String,
        )

class StayWatchClient {
    //private val url = "https://apppppp.com/jojo.json"
    private val url = "https://go-staywatch.kajilab.tk/api/v1/check"

    fun getUserFromServer(googleIdToken: String): StayWatchServerResult {
        // GoogleIDトークン(GoogleIDのトークン)からFirebaseIDトークン(プロジェクト内でのトークン)を取得
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithCredential(credential)
        val firebaseUser = firebaseAuth.currentUser
        Log.d("StayWatchClient", "firebaseAuthは $firebaseUser")
        val firebaseIdToken = firebaseUser?.getIdToken(false)?.result?.token
        Log.d("StayWatchClient", "firebaseAuthは $firebaseIdToken")

        // FirebaseIDトークンを用いて滞在ウォッチサーバからユーザ情報取得
        val (request, response, result) = url.httpGet()
            .header(Headers.AUTHORIZATION to "Bearer $firebaseIdToken")
            .responseJson()
        Log.d("StayWatchClient", "リクエスト：$request")
        Log.d("StayWatchClient", "レスポンス：$response")

        return when (result) {
            // 失敗時
            is Result.Failure -> {
                Log.d("API", "API通信失敗")
                val ex = result.getException()
                println(ex)

                // 返り値
                StayWatchServerResult(
                    data = null,
                    errorMessage = ex.message.toString()
                )
            }

            // 成功時
            is Result.Success -> {
                Log.d("API", "API通信成功")
                // resultからBodyの部分を取り出す
                val resultJson = result.get().obj()
                // Jsonをパースする
                val responseUser = Gson().fromJson(resultJson.toString(), UserGetResponse::class.java)
                println(responseUser)
                Log.d("API", "ユーザ名：${responseUser}")

                // 返り値
                StayWatchServerResult(
                    data = StayWatchUser(
                        userName = responseUser.name,
                        uuid = responseUser.uuid,
                        communityName = responseUser.communityName
                    ),
                    errorMessage = null
                )
            }
        }
    }
}
//    fun getUser(): String {
//        url.httpGet().responseJson { request, response, result ->
//            when (result) {
//                // ステータスコード 2xx
//                is Result.Success -> {
//                    Log.d("API", "API通信成功")
//                    result.get().obj()
////                    Log.d("API", "API通信成功")
////                    val resultJson = result.get().obj()
////                    // JSONObjectをUserDetailへ変換
////                    val resultUser = Gson().fromJson(resultJson.toString(), UserDetail::class.java)
////                    Log.d("API", resultUser.name)
////                    Log.d("API", "API通信終了")
////                    resultUser
//                }
//                // ステータスコード 2xx以外(401など)
//                is Result.Failure -> {
//                    // エラー処理
//                    Log.d("API", "API通信失敗")
//                }
//            }
//        }

//class StayWatchClient {
//    private val url = "https://apppppp.com/jojo.json"
//
//    fun getUser(): JSONObject {
//        val (_, _, result) = url.httpGet().responseJson()
//        return when (result) {
//            is Result.Failure -> {
//                Log.d("API", "API通信失敗")
//                val ex = result.getException()
//                JSONObject(mapOf("message" to ex.toString()))
//            }
//            is Result.Success -> {
//                Log.d("API", "API通信成功")
//                result.get().obj()
//            }
//        }
//    }
//}
//        url.httpGet().responseJson {request, response, result ->
//            when(result) {
//                // ステータスコード 2xx
//                is Result.Success -> {
//                    Log.d("API", "API通信成功")
//                    result.get().obj()
////                    Log.d("API", "API通信成功")
////                    val resultJson = result.get().obj()
////                    // JSONObjectをUserDetailへ変換
////                    val resultUser = Gson().fromJson(resultJson.toString(), UserDetail::class.java)
////                    Log.d("API", resultUser.name)
////                    Log.d("API", "API通信終了")
////                    resultUser
//                }
//                // ステータスコード 2xx以外(401など)
//                is Result.Failure -> {
//                    // エラー処理
//                    Log.d("API", "API通信失敗")
//                }
//            }

//    suspend fun getUser(): JSONObject {
//        // コルーチンをバックグラウンドスレッドで実行する
//        return withContext(IO) {
//            // ネットワークリクエストを実行する
//            val (_, _, result) = url.httpGet().responseJson()
//
//            // レスポンスからユーザーデータを返す
//            when (result) {
//                is Result.Failure -> {
//                    val ex = result.getException()
//
//                    Log.d("API", "ユーザ取得API通信失敗")
//                    JSONObject(mapOf("message" to ex.toString()))
//                }
//                is Result.Success -> {
//                    Log.d("API", "ユーザ取得API通信成功")
//                    print(result)
//                    result.get().obj()
//                }
//            }
//        }
//      }
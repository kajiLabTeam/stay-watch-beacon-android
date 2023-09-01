package kajilab.togawa.staywatchbeaconandroid.api

import android.util.JsonToken
import android.util.Log
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import kajilab.togawa.staywatchbeaconandroid.model.User
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class UserDetail(val name: String, val stand: String)

class StayWatchClient {
    //private val url = "https://apppppp.com/jojo.json"
    private val url = "https://go-staywatch.kajilab.tk/api/v1/check"

    private val headers = hashMapOf(
        "Authorization" to "Bearer TOKEN"
    )
    private val headeras = listOf(
        Pair("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImIyZGZmNzhhMGJkZDVhMDIyMTIwNjM0OTlkNzdlZjRkZWVkMWY2NWIiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoi5oSb55-l5bel5qWt5aSn5a2m5qK256CU56m25a6kIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hL0FFZEZUcDRrZW1FOUlLM1JweEIwRHRnOGJJazFzbGZhQXBuRGh5aFVUaGU2PXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL3N0YXktd2F0Y2gtYTYxNmYiLCJhdWQiOiJzdGF5LXdhdGNoLWE2MTZmIiwiYXV0aF90aW1lIjoxNjkwNDI3NDczLCJ1c2VyX2lkIjoiSk96bEFIRXU2ZWd6aUlXY1J3MElNYnllT1RvMSIsInN1YiI6IkpPemxBSEV1NmVnemlJV2NSdzBJTWJ5ZU9UbzEiLCJpYXQiOjE2OTA0Mjc0NzQsImV4cCI6MTY5MDQzMTA3NCwiZW1haWwiOiJhaXQua2FqaWxhYkBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZmlyZWJhc2UiOnsiaWRlbnRpdGllcyI6eyJnb29nbGUuY29tIjpbIjEwNDg1ODgyMDQ5NTMxMTkzMDU3MiJdLCJlbWFpbCI6WyJhaXQua2FqaWxhYkBnbWFpbC5jb20iXX0sInNpZ25faW5fcHJvdmlkZXIiOiJnb29nbGUuY29tIn19.f1sXnOXmBxRFF8WiDOKAJJyU3byINpeaA2_gmz8mSCrGG2h-OPGq3GeMbKs5BKQYHyzJXVaUGSk_Xgl2p7ua7eXzY68C6SiUpbLZEvndURgPu3o-dTSxIqmrOlkq72ynb3J9Ow8c9rSaw4YqeeAxJ2w-5f0btAE0OQzFFUz_o3K-dJv90Z0V2YFTHHs__gEytz5-sVJR5D8rsU4DQa1niY5w2AIprOHdGdfZn2XK0mI7peI0ny-atISShL1QCstUOj2JYxkB5VMuj0_wx9hiR3SRvTu9MR_pBFby8uT-cfHvRF1nI6lszQ8vKrtC5_eRTQ_5H-xxxPeJXEMBGH4XhA")
    )

    fun getUser(): String {
        //val (request, response, result) = url.httpGet().responseString()
        val (request, response, result) = url.httpGet(headeras).responseString()

        return when (result) {
            is Result.Failure -> {
                Log.d("API", "API通信失敗")
                val ex = result.getException()
                println(ex)
                val resultUser = User("miss","missstand")
                resultUser.toString()  // 返り値
//                ex.toString()   // 返り値

            }

            is Result.Success -> {
                Log.d("API", "API通信成功")
                val resultJson = result.get()
                println(resultJson)
                //val resultUser = Gson().fromJson(resultJson, User::class.java)
                //val resultUser = User("kota","ruirui")
                resultJson  // 返り値
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
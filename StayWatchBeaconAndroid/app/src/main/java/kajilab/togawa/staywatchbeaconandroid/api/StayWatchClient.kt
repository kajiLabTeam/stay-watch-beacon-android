package kajilab.togawa.staywatchbeaconandroid.api

import android.util.JsonToken
import android.util.Log
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import kajilab.togawa.staywatchbeaconandroid.model.StayWatchServerResult
import kajilab.togawa.staywatchbeaconandroid.model.StayWatchUser
import kajilab.togawa.staywatchbeaconandroid.model.User
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

data class UserDetail(val name: String, val stand: String)

class StayWatchClient {
    //private val url = "https://apppppp.com/jojo.json"
    private val url = "https://go-staywatch.kajilab.tk/api/v1/check"
    //private val url = "http://192.168.101.11:8082/api/v1/stayers"

//    private val headers = hashMapOf(
//        "Authorization" to "Bearer TOKEN"
//    )
//    private val headeras = listOf(
//        Pair("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImM3ZTExNDEwNTlhMTliMjE4MjA5YmM1YWY3YTgxYTcyMGUzOWI1MDAiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI3NjgzMDYwMDQ2MDYtY2llMWNucW5vaG9hcWY3ZWc5dmIzM3AyZDAydDdidjguYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI3NjgzMDYwMDQ2MDYtNDdqNGlnMWExaXVxMGpsamU2Y3VkZW81aTduaWhsbmEuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDQ4NTg4MjA0OTUzMTE5MzA1NzIiLCJlbWFpbCI6ImFpdC5rYWppbGFiQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJuYW1lIjoi5oSb55-l5bel5qWt5aSn5a2m5qK256CU56m25a6kIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hL0FBY0hUdGVDVU9NQnREN1FHcnZUUjE1RDhtQUxHa2RjVkluV0ZadG9BRWozTUJOcT1zOTYtYyIsImdpdmVuX25hbWUiOiLmorbnoJTnqbblrqQiLCJmYW1pbHlfbmFtZSI6IuaEm-efpeW3pealreWkp-WtpiIsImxvY2FsZSI6ImphIiwiaWF0IjoxNjkzNzUyODEyLCJleHAiOjE2OTM3NTY0MTJ9.CvaZF8Bw2_dJOfjrpPClsyE5xKZAKdWLhJoeIno7tDr6EuopDt69a5M75OFFUciomfJzRUtX3pyaB49UrN57K8U_YWE3p3TodEa3FDH6BTQdmWGGQzn8BxO7Xeh8xTPGRG_9c8yJfArxXyRG1i3EQ_r5UJUJ_B2RDrz_T2paU-9a9lnm6QfQdQMYVomQlNQdli1fm7-JcdulcmiMd9minIdTl7Hs_N6mKqh0l4qGtulQb1WlySvg63EzEDrPxGfJi5E8VsbuwcA7XUQZviaheY2Vzl1jgcJKLXxeDAfTpNW8Ew35YgL6X7U3UxOZwtMNLak9K1EMq6Kry5-KU6aoSw")
//    )

    fun getUserFromServerWithOkHttp(token: String): StayWatchServerResult {
        val client = OkHttpClient().newBuilder()
            .build()
        var urlStr = "https://go-staywatch.kajilab.tk/api/v1/check"


        val request = Request.Builder()
            .url(urlStr)
            //.addHeader("Authorization", "Bearer $token")
            .build()

        Log.d("StayWatchClient", "リクエスト内容：$request")

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // Responseの読み出し
                val responseBody = response.body?.string().orEmpty()
                // 必要に応じてCallback
                Log.d("ビーコン情報",responseBody)
                StayWatchServerResult(
                    data = null,
                    errorMessage = null
                )
            }
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", e.toString())
                // 必要に応じてCallback
                StayWatchServerResult(
                    data = null,
                    errorMessage = e.message.toString()
                )
            }
        })
        return StayWatchServerResult(
            data = null,
            errorMessage = null
        )
    }


    fun getUserFromServer(token: String): StayWatchServerResult {
//        val headers = listOf(
//            Pair("Authorization", "Bearer $token")
//        )
        val (request, response, result) = url.httpGet()
            .header(Headers.AUTHORIZATION to "Bearer $token")
            .responseString()
        //val (request, response, result) = url.httpGet(headers).responseString()
        //val (request, response, result) = url.httpGet(listOf(Pair("Authorization", "Bearer$token"))).responseString()
        Log.d("StayWatchClient", "リクエスト：$request")
        Log.d("StayWatchClient", "レスポンス：$response")

        return when (result) {
            is Result.Failure -> {
                Log.d("API", "API通信失敗")
                val ex = result.getException()
                println(ex)
                val resultUser = User("miss","missstand")
                resultUser.toString()  // 返り値
//                ex.toString()   // 返り値
                StayWatchServerResult(
                    data = null,
                    errorMessage = ex.message.toString()
                )

            }

            is Result.Success -> {
                Log.d("API", "API通信成功")
                val resultJson = result.get()
                println(resultJson)
                //val resultUser = Gson().fromJson(resultJson, User::class.java)
                //val resultUser = User("kota","ruirui")

                StayWatchServerResult(
                    data = StayWatchUser(
                        userName = resultJson,
                        uuid = "ajifioaiefa",
                        communityName = "戸川家"
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
package kajilab.togawa.staywatchbeaconandroid.useCase

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class EncryptedSharePreferencesManager(context: Context) {

    // 暗号化、復号化のためのマスターキーを作成
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    // EncryptedSharedPreferencesのインスタンスを初期化
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "SecureContentAuth",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun storeString(key:String, str: String): Exception?{
        try {
            sharedPreferences.edit()
                .putString(key, str)
                .apply()
        }catch(e: Exception){
            // 正常に保存できなかったらエラーを返す
            return e
        }
        // 正常に保存できたら何も返さない
        return null
    }

    fun getString(key:String): Pair<String, Exception?>{
        var value = ""
        try {
            value = sharedPreferences.getString(key, "").toString()
        }catch(e: Exception){
            // うまく取得できなかったらerrorを返す
            print(e)
            return Pair("", e)
        }
        return Pair(value, null)
    }

    fun deleteString(key:String): Exception?{
        try {
            // 中身を""にすることで削除とする
            sharedPreferences.edit()
                .putString(key, "")
                .apply()
        }catch(e: Exception){
            return e
        }
        return null
     }
}
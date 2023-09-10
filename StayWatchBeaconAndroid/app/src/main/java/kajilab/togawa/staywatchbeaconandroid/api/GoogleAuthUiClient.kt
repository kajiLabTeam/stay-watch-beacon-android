package kajilab.togawa.staywatchbeaconandroid.api

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.oAuthCredential
import com.google.firebase.ktx.Firebase
import kajilab.togawa.staywatchbeaconandroid.R
import kajilab.togawa.staywatchbeaconandroid.model.SignInResult
import kajilab.togawa.staywatchbeaconandroid.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient
)  {
    private val auth = Firebase.auth

    // サインイン画面が出る
    suspend fun signIn(): IntentSender? {
//    suspend fun signIn(): String {
        Log.d("FirebaseAuth", "signIn開始")
        val result = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        } catch (e: Exception) {
            Log.d("error", "時間を開けて再度やり直してください")
            withContext(Dispatchers.Main){
                Toast.makeText(context, "通信に失敗しました", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
            if(e is CancellationException) throw e
            null
        }
        Log.d("FirebaseAuth", "signIn成功")
        return result?.pendingIntent?.intentSender
        //return "サインイン成功"
    }

    // サインイン成功時
    suspend fun getSignWithIntent(intent: Intent): SignInResult {
        Log.d("FirebaseAuth", "getSignWithIntent開始")
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        Log.d("FirebaseAuth", "Googleトークンは" + googleIdToken.toString())
        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        username = displayName,
                        email = email,
                        profilePictureUrl = photoUrl?.toString(),
                        token = googleIdToken
                    )
                },
                errorMessage = null
            )
        } catch(e: Exception) {
            e.printStackTrace()
            if(e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    fun getSignInUser(): UserData? = auth.currentUser?.run {
        Log.d("FirebaseAuth", "getSignInUser開始")
        UserData(
            userId = uid,
            username = displayName,
            email = email,
            profilePictureUrl = photoUrl?.toString(),
            token = ""
        )
    }

    suspend fun signOut() {
        try{
            oneTapClient.signOut().await()
            auth.signOut()
        } catch(e: Exception){
            e.printStackTrace()
            if(e is CancellationException) throw e
        }
    }

    // サインイン画面が出る前
    private fun buildSignInRequest(): BeginSignInRequest {
        Log.d("FirebaseAuth", "buildSignInRequest開始")
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}
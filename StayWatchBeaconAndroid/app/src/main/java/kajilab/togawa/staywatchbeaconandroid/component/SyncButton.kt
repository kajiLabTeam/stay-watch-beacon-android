package kajilab.togawa.staywatchbeaconandroid.component

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kajilab.togawa.staywatchbeaconandroid.R
import kajilab.togawa.staywatchbeaconandroid.api.GoogleAuthUiClient
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.viewModel.BeaconViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun SyncButton(googleAuthUiClient: GoogleAuthUiClient, viewModel: BeaconViewModel, db: AppDatabase, context: Context) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "sign_in"){
        composable("sign_in") {

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = { result ->
                    if(result.resultCode == ComponentActivity.RESULT_OK) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val signInResult = googleAuthUiClient.getSignWithIntent(
                                intent = result.data ?: return@launch
                            )
                            viewModel.onSignInResult(signInResult)
                            Log.d("MainActivity", "RESULT_OKだったよ")
                            Log.d("MainActivity", "signInResult: " + signInResult.toString())
                            // viewModelのメソッドへトークンとメールアドレスを渡してデータベース関連とBLEサービス開始処理を行う
                            viewModel.storeUserAndToken(signInResult.data?.email.toString(), signInResult.data?.token.toString(), db, context)
                        }
                    }

                }
            )

            Button(onClick = {
                Log.d("SyncButton", "SignInScreen開始")
                CoroutineScope(Dispatchers.IO).launch {
                    var error = viewModel.syncUser(db, context)
                    if(error != null){
                        // 同期が失敗(トークンが古い)場合サインイン画面を出す
                        val signInIntentSender = googleAuthUiClient.signIn()
                        launcher.launch(
                            IntentSenderRequest.Builder(
                                signInIntentSender ?: return@launch
                            ).build()
                        )
                    }
                    Log.d("SignInScreen", "サインインボタンが押されたよ")
                }
            },
                //colors = ButtonDefaults.buttonColors(Color(0xFFF8CC45))
                colors = ButtonDefaults.buttonColors(Color.Transparent)
            ){
                Image(
                    painter = painterResource(R.drawable.forward_circle),
                    contentDescription = null,
                )
            }
        }
    }
}
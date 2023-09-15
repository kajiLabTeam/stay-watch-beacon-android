package kajilab.togawa.staywatchbeaconandroid.component

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kajilab.togawa.staywatchbeaconandroid.R
import kajilab.togawa.staywatchbeaconandroid.api.GoogleAuthUiClient
import kajilab.togawa.staywatchbeaconandroid.db.AppDatabase
import kajilab.togawa.staywatchbeaconandroid.model.BlePeripheralServerManager
import kajilab.togawa.staywatchbeaconandroid.ui.theme.StayWatchBeaconAndroidTheme
import kajilab.togawa.staywatchbeaconandroid.viewModel.BeaconViewModel
import kajilab.togawa.staywatchbeaconandroid.service.BlePeripheralService
import kajilab.togawa.staywatchbeaconandroid.utils.StatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun BeaconView (viewModel: BeaconViewModel, googleAuthClient: GoogleAuthUiClient, peripheralServerManager: BlePeripheralServerManager, application: Context, db: AppDatabase) {
    val statusCode = StatusCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        // 上のHeader
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text(
                text = viewModel.communityName,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Button(
                    onClick = {
                        Log.d("Button", "サインアウト！")
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.signOut(db, application, peripheralServerManager)
                        }
                              },
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    border = BorderStroke(3.dp, Color(0xFFF8CC45))
                ) {
                    Text(
                        text="サインアウト",
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
                Text(
                    text= if(viewModel.email == null) "error@gmail.com" else viewModel.email!!,
                    fontSize = 12.sp
                )
            }
        }

        Divider(
            modifier = Modifier
                .padding(top = 5.dp)
                .padding(bottom = 10.dp)
            )

        // 下のエリア
//        Button(onClick = {
//        }) {
//        }

        Column (
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.15f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 発信中・停止中の四角
            AdvertiseStatusPanel(
                textStr = if(viewModel.isAdvertising) "発信中" else "停止中",
                panelColor = if(viewModel.isAdvertising) Color(0xFF007AFF) else Color(0xFFFF3B30),
                textColor = Color.White
            )

            // お試し
            //Text(beaconStatus)
            //Text(viewModel.beaconStatus)

            // ユーザ名や同期ボタン、同期時刻
            Text(
                text = viewModel.userName,
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(bottom = 15.dp)
            )
            Text(
                text = viewModel.uuid,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(bottom = 25.dp)
            )
//            Icon(
//                painter = rememberVectorPainter(image = Icons.Default.Star),
//                contentDescription = null,
//            )
            SyncButton(googleAuthUiClient = googleAuthClient, viewModel = viewModel, db = db, context = application, peripheralServiceManager = peripheralServerManager)
            Text(
                text = "最新の同期：" + viewModel.latestSyncTime,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(bottom = 15.dp)
            )
        }

        // 発信開始停止ボタン
        if(viewModel.isAdvertising){
            Button(
                onClick = {
                    Log.d("Button", "発信を停止")
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.stopAdvertisingService(db, peripheralServerManager)
                    }
                          },
                colors = ButtonDefaults.buttonColors(Color.Transparent)
            ) {
                Text(
                    text="発信を停止する",
                    color = Color.Gray
                )
            }
        } else {
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val errorCode = viewModel.startAdvertisingService(db, peripheralServerManager)
                        if(errorCode != null){
                            when (errorCode) {
                                statusCode.UNABLE_GET_USER_FROM_DATABASE -> {
                                    withContext(Dispatchers.Main){
                                        Toast.makeText(application, "発信開始失敗", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                statusCode.INVALID_UUID -> {
                                    withContext(Dispatchers.Main){
                                        Toast.makeText(application, "発信開始失敗", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                          },
                colors = ButtonDefaults.buttonColors(Color.Transparent)
            ) {
                Text(
                    text="発信を開始する",
                    color = Color.Gray
                )
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun BeaconPreview() {
//    StayWatchBeaconAndroidTheme {
//        BeaconView()
//    }
//}
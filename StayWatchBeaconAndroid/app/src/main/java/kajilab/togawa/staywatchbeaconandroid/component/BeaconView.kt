package kajilab.togawa.staywatchbeaconandroid.component

import android.content.Context
import android.content.Intent
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun BeaconView (viewModel: BeaconViewModel, googleAuthClient: GoogleAuthUiClient, peripheralServerManager: BlePeripheralServerManager, application: Context, db: AppDatabase) {

    val communityName = remember {
        mutableStateOf("梶研究室")
    }
    val userName = remember {
        mutableStateOf("kajilabkjlb")
    }
    val uuid = remember {
        mutableStateOf("e7d61ea3-f8dd-49c8-8f2f-f24f00200015")
    }
    val latestSyncTime = remember {
        mutableStateOf("2023/02/01 11:53")
    }
    val isAdvertising = remember {
        mutableStateOf(false)
    }


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
                            viewModel.signOut(db, application)
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
                textStr = if(isAdvertising.value) "発信中" else "停止中",
                panelColor = if(isAdvertising.value) Color(0xFF007AFF) else Color(0xFFFF3B30),
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
        if(isAdvertising.value){
            Button(
                onClick = {
                    Log.d("Button", "発信を停止")
                    viewModel.stopBleAdvertising(peripheralServerManager)
                    isAdvertising.value = false
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
                    Log.d("Button", "発信を開始")
                    //viewModel.updateStatus()
                    viewModel.startBleAdvertising(peripheralServerManager)
                    isAdvertising.value = true
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
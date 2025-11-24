package kajilab.togawa.staywatchbeaconandroid.component

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.Global.AIRPLANE_MODE_ON
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
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
    val isDarkTheme = isSystemInDarkTheme()

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
                fontSize = 20.sp,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Button(
                    onClick = {
                        if(viewModel.isLoading){
                            return@Button
                        }
                        viewModel.isLoading = true
                        //Log.d("Button", "サインアウト！")
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.signOut(db, application, peripheralServerManager)
                            viewModel.isLoading = false
                        }
                              },
                    colors = ButtonDefaults.buttonColors(
                        Color.Transparent,
                        contentColor = viewModel.getColorByTheme(Color.Black, isDarkTheme)
                    ),
                    border = BorderStroke(3.dp, Color(0xFFF8CC45))
                ) {
                    Text(
                        text="サインアウト",
                        fontSize = 12.sp,
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
        Column (
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.15f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // メールアドレスが滞在ウォッチサーバーに登録されていない場合
            if(viewModel.uuid == ""){
                // 発信中・停止中の四角
                AdvertiseStatusPanel(
                    textStr = "未登録",
                    panelColor = Color.Transparent,
                    textColor = Color.Red,
                    borderColor = Color.Red
                )
                // ユーザ名や同期ボタン、同期時刻
                Text(
                    text = viewModel.email + "は",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                )
                Text(
                    text = "未登録のメールアドレスです",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(bottom = 15.dp)
                )

            }
            else if(!viewModel.isAndroidBeaconUUID(viewModel.uuid)){
                // Androidビーコンとして登録されていない場合
                // 発信中・停止中の四角
                AdvertiseStatusPanel(
                    textStr = "未登録",
                    panelColor = Color.Transparent,
                    textColor = Color.Red,
                    borderColor = Color.Red
                )
                // ユーザ名や同期ボタン、同期時刻
                Text(
                    text = "Androidビーコンとして登録されていません",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(bottom = 15.dp)
                )
            }
            else if(ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED){
                // 「付近のデバイス」権限が許可されていない場合
                Button(
                    onClick = {
                        if(viewModel.isLoading){
                            return@Button
                        }
                        viewModel.isLoading = true
                        viewModel.showSetting(application)
                        viewModel.isLoading = false
                    },
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.Red
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ){
                        Text(
                            text="権限を許可してください",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text="設定画面へ",
                            color = Color.Black,
                            modifier = Modifier
                                .background(
                                    color = Color.Red,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(horizontal = 10.dp)
                                .padding(vertical = 7.dp)
                        )
                    }
                }
                // 発信中・停止中の四角
                AdvertiseStatusPanel(
                    textStr = "停止中",
                    panelColor = Color.Transparent,
                    textColor = Color.Red,
                    borderColor = Color.Red
                )
                // ユーザ名や同期ボタン、同期時刻
                Text(
                    text = "権限を許可したら下の同期ボタンを押してください",
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(bottom = 15.dp)
                )
            }
            else {
                // 発信中・停止中の四角
                AdvertiseStatusPanel(
                    textStr = if (viewModel.isAdvertising) "発信中" else "停止中",
                    panelColor = if (viewModel.isAdvertising) Color(0xFF007AFF) else Color(
                        0xFFFF3B30
                    ),
                    textColor = Color.White,
                    borderColor = Color.Transparent
                )

                // ユーザ名や同期ボタン、同期時刻
                Text(
                    text = viewModel.userName,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(bottom = 15.dp)
                )
            }
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
        if(viewModel.uuid != "" && viewModel.isAndroidBeaconUUID(viewModel.uuid) && ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED){
            if(viewModel.isAdvertising){
                Button(
                    onClick = {
                        if(viewModel.isLoading){
                            return@Button
                        }
                        viewModel.isLoading = true
                        //Log.d("Button", "発信を停止")
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.stopAdvertisingService(db, peripheralServerManager, application)
                            viewModel.isLoading = false
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
                        if(viewModel.isLoading){
                            return@Button
                        }
                        viewModel.isLoading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val errorCode = viewModel.startAdvertisingService(db, peripheralServerManager, application)
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
                            viewModel.isLoading = false
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
}

//@Preview(showBackground = true)
//@Composable
//fun BeaconPreview() {
//    StayWatchBeaconAndroidTheme {
//        BeaconView()
//    }
//}
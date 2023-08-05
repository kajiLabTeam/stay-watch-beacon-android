package kajilab.togawa.staywatchbeaconandroid.component

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kajilab.togawa.staywatchbeaconandroid.ui.theme.StayWatchBeaconAndroidTheme

@Composable
fun BeaconView (modifier: Modifier = Modifier) {
    //var communityName by remember { mutableStateOf("a")}
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
                text = communityName.value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Button(
                    onClick = { Log.d("Button", "別Googleでサインイン！")},
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    border = BorderStroke(3.dp, Color(0xFFF8CC45))
                ) {
                    Text(
                        text="別アカウントでサインイン",
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
                Text(
                    text="ait.kajilab@gmail.com",
                    fontSize = 12.sp
                )
            }
        }

        Divider(
            modifier = Modifier
                .padding(top = 5.dp)
                .padding(bottom = 10.dp)
            )

        Column (
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.15f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = userName.value
            )
            Text(
                text = uuid.value
            )
            Text(
                text = "最新の同期：" + latestSyncTime.value
            )
        }



    }
}

@Preview(showBackground = true)
@Composable
fun BeaconPreview() {
    StayWatchBeaconAndroidTheme {
        BeaconView()
    }
}
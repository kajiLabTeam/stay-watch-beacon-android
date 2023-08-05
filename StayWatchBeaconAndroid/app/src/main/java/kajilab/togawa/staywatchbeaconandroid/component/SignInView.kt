package com.example.staywatchbeaconandroid.component

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kajilab.togawa.staywatchbeaconandroid.ui.theme.StayWatchBeaconAndroidTheme

@Composable
fun SignInView(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    {
        Column(
            modifier = Modifier
                .height(350.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        )
        {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Text(
                    text = "滞在ウォッチ用ビーコン",
                    fontSize = 25.sp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                )
                Text(
                    text ="for Android",
                    fontSize = 25.sp
                )
            }
            Button(
                onClick = { Log.d("Button", "Googleでサインイン！")},
                colors = ButtonDefaults.buttonColors(Color(0xFFF8CC45))
            ) {
                Text(
                    text = "Googleアカウントでサインイン",
                    color = Color.Black,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInPreview() {
    StayWatchBeaconAndroidTheme {
        SignInView()
    }
}
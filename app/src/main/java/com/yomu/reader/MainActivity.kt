package com.yomu.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yomu.reader.ui.YomuApp
import com.yomu.reader.ui.theme.YomuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            YomuTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    YomuApp()
                }
            }
        }
    }
}

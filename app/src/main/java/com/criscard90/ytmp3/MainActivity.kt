package com.criscard90.ytmp3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.criscard90.ytmp3.ui.AppRoot
import com.criscard90.ytmp3.ui.theme.YtTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YtTheme {
                AppRoot()
            }
        }
    }
}

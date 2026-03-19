package com.violet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.violet.app.ui.theme.VioletTheme
import com.violet.app.ui.navigation.VioletNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VioletTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VioletNavHost()
                }
            }
        }
    }
}

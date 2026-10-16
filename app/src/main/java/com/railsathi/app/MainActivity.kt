package com.railsathi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.railsathi.app.ui.navigation.RailSathiAppUi
import com.railsathi.app.ui.theme.RailSathiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RailSathiTheme {
                RailSathiAppUi()
            }
        }
    }
}

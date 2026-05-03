package com.forkaton.pemob9_123140020

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.forkaton.pemob9_123140020.ui.AuditScreen
import com.forkaton.pemob9_123140020.ui.theme.Pemob9_123140020Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pemob9_123140020Theme {
                AuditScreen()
            }
        }
    }
}
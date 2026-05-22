package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize the MainViewModel using native Compose support
                val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val screenState by viewModel.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    when (screenState) {
                        "SPLASH" -> {
                            SplashScreen(
                                onTimeout = {
                                    viewModel.setScreen("ONBOARDING")
                                }
                            )
                        }
                        "ONBOARDING" -> {
                            OnboardingScreen(
                                onComplete = {
                                    viewModel.setScreen("MAIN")
                                }
                            )
                        }
                        else -> {
                            MainAppScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

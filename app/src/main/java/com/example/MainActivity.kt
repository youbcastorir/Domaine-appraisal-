package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.DomainRepository
import com.example.ui.screens.AppraisalScreen
import com.example.ui.screens.ChatbotScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DomainViewModel
import com.example.ui.viewmodel.DomainViewModelFactory
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: DomainViewModel by lazy {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DomainRepository(database.appraisalDao(), database.portfolioDao())
        ViewModelProvider(this, DomainViewModelFactory(repository))[DomainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainOrchestrator(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainOrchestrator(viewModel: DomainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { viewModel.navigateTo(Screen.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Dashboard",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Dashboard",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == Screen.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.APPRAISE,
                    onClick = { viewModel.navigateTo(Screen.APPRAISE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Appraisal",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Appraise",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == Screen.APPRAISE) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.PORTFOLIO,
                    onClick = { viewModel.navigateTo(Screen.PORTFOLIO) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Portfolio",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Portfolio",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == Screen.PORTFOLIO) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.CHAT,
                    onClick = { viewModel.navigateTo(Screen.CHAT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = "AI Coach",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "AI Advisor",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == Screen.CHAT) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                Screen.APPRAISE -> AppraisalScreen(viewModel = viewModel)
                Screen.PORTFOLIO -> PortfolioScreen(viewModel = viewModel)
                Screen.CHAT -> ChatbotScreen(viewModel = viewModel)
            }
        }
    }
}

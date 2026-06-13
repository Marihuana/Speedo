package kr.yooreka.speedo.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.dashboard.DashBoardScreen
import kr.yooreka.speedo.ui.dashboard.DashBoardViewModel
import kr.yooreka.speedo.ui.log.LogScreen
import kr.yooreka.speedo.ui.records.RecordsScreen
import kr.yooreka.speedo.ui.settings.SettingsScreen
import kr.yooreka.speedo.ui.splash.SplashScreen
import kr.yooreka.speedo.ui.theme.BackgroundBlack
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SpeedoTheme

import kr.yooreka.speedo.utils.AdManager
import javax.inject.Inject
import android.app.Activity
import kr.yooreka.speedo.ui.splash.SplashViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        adManager.loadInterstitialAd()
        
        enableEdgeToEdge()
        setContent {
            SpeedoTheme {
                MainContent(adManager)
            }
        }
    }
}

sealed class Screen(val route: String, val labelResId: Int, val icon: Int) {
    object Splash : Screen("splash", R.string.app_name, 0)
    object MainPager : Screen("main_pager", R.string.app_name, 0)
    object Monitor : Screen("monitor", R.string.tab_monitor, R.drawable.ic_monitor)
    object Records : Screen("records", R.string.tab_records, R.drawable.ic_records)
    object Settings : Screen("settings", R.string.tab_settings, R.drawable.ic_settings)
    object Log : Screen("log/{rideId}", R.string.ride_log, 0) {
        fun createRoute(rideId: Long) = "log/$rideId"
    }
}

@Composable
fun MainContent(adManager: AdManager) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            SplashScreen(onTimeout = {
                splashViewModel.onSplashFinished(context as Activity, adManager) {
                    navController.navigate(Screen.MainPager.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            })
        }
        composable(Screen.MainPager.route) {
            MainPagerScreen(navController = navController, adManager = adManager)
        }
        composable(
            route = Screen.Log.route,
            arguments = listOf(navArgument("rideId") { type = NavType.LongType })
        ) {
            LogScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPagerScreen(navController: NavController, adManager: AdManager) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            (context as? ComponentActivity)?.finish()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(context, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    val items = listOf(
        Screen.Monitor,
        Screen.Records,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundBlack,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E2530),
                contentColor = Color.White
            ) {
                items.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(painterResource(id = screen.icon), contentDescription = stringResource(id = screen.labelResId)) },
                        label = { Text(stringResource(id = screen.labelResId)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen,
                            selectedTextColor = NeonGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> {
                    val viewModel: DashBoardViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    DashBoardScreen(
                        state = uiState,
                        uiEvent = viewModel.uiEvent,
                        onRecordToggle = { viewModel.toggleRecording() },
                        onConfirmRecording = { viewModel.onConfirmRecording() },
                        onShowInterstitial = {
                            (context as? Activity)?.let { activity ->
                                adManager.showInterstitial(activity) {}
                            }
                        }
                    )
                }
                1 -> {
                    RecordsScreen(
                        onRecordClick = { rideId ->
                            navController.navigate(Screen.Log.createRoute(rideId))
                        }
                    )
                }
                2 -> {
                    SettingsScreen()
                }
            }
        }
    }
}

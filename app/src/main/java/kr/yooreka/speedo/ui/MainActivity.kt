package kr.yooreka.speedo.ui

import android.app.Activity
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import kr.yooreka.speedo.ui.splash.SplashViewModel
import kr.yooreka.speedo.ui.theme.BackgroundBlack
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SpeedoTheme
import kr.yooreka.speedo.utils.AdManager
import javax.inject.Inject

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
        modifier = Modifier.fillMaxSize(),
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
            arguments = listOf(navArgument("rideId") { type = NavType.LongType }),
        ) {
            LogScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPagerScreen(
    navController: NavController,
    adManager: AdManager,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }
    // 콜백(비 Composable)에서 쓸 문자열은 미리 읽어 둔다(LocalContextGetResourceValueCall 회피).
    val exitHint = stringResource(R.string.press_back_again_to_exit)

    BackHandler {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            (context as? ComponentActivity)?.finish()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(context, exitHint, Toast.LENGTH_SHORT).show()
        }
    }

    val items =
        listOf(
            Screen.Monitor,
            Screen.Records,
            Screen.Settings,
        )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundBlack,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E2530),
                contentColor = Color.White,
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
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                selectedTextColor = NeonGreen,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent,
                            ),
                    )
                }
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                        },
                        onAutoStopContinue = { viewModel.onAutoStopContinue() },
                        onAutoStopConfirm = { viewModel.onAutoStopConfirm() },
                        onMarkIssue = { viewModel.markDiagnosticIssue() },
                    )
                }
                1 -> {
                    RecordsScreen(
                        onRecordClick = { rideId ->
                            navController.navigate(Screen.Log.createRoute(rideId))
                        },
                    )
                }
                2 -> {
                    SettingsScreen()
                }
            }
        }
    }
}

package kr.yooreka.speedo.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.yooreka.speedo.R
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // 콜드스타트 시스템 스플래시(F-25, §4.6, AC-06). super.onCreate 직전에 설치해야 한다.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // is_first_launch(및 관련 prefs) 리딩이 끝날 때까지 시스템 스플래시를 유지해,
        // 시작 목적지가 확정되기 전에 메인 화면이 노출되는 Visual Leak/Jank 를 원천 차단한다.
        // userPreferencesFlow 는 IO 에러 시 .catch 로 메모리 캐시(fallback)를 즉시 emit 하므로
        // 조건이 무한정 유지되지 않으며, AC-06(최초 1회만 노출)을 만족한다.
        val prefsReady = AtomicBoolean(false)
        splashScreen.setKeepOnScreenCondition { !prefsReady.get() }
        lifecycleScope.launch {
            userPreferencesRepository.userPreferencesFlow.first()
            prefsReady.set(true)
        }

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

    object SafetyGuide : Screen("safety_guide", R.string.app_name, 0)

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
                splashViewModel.onSplashFinished(context as Activity, adManager) { isFirstLaunch ->
                    if (isFirstLaunch) {
                        navController.navigate(Screen.SafetyGuide.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.MainPager.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            })
        }
        composable(Screen.SafetyGuide.route) {
            SafetyGuideScreen(
                onConfirm = {
                    navController.navigate(Screen.MainPager.route) {
                        popUpTo(Screen.SafetyGuide.route) { inclusive = true }
                    }
                },
            )
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

    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundBlack,
        bottomBar = {
            if (!isLandscape) {
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
            }
        },
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {
                // 가로모드용 좌측 세로 내비게이션 바 (Figma 80:333 스펙 반영)
                Column(
                    modifier =
                        Modifier
                            .width(68.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF1E2530))
                            .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items.forEachIndexed { index, screen ->
                        val isSelected = pagerState.currentPage == index
                        val activeColor = NeonGreen
                        val inactiveColor = Color.Gray

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = screen.icon),
                                contentDescription = stringResource(id = screen.labelResId),
                                tint = if (isSelected) activeColor else inactiveColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(id = screen.labelResId),
                                color = if (isSelected) activeColor else inactiveColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(if (isLandscape) PaddingValues(0.dp) else innerPadding),
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
}

// ── SafetyGuideScreen 및 SafetyGuideViewModel (Figma 80-192 / 80-140 싱크) ──────────────────

@dagger.hilt.android.lifecycle.HiltViewModel
class SafetyGuideViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        val isFirstLaunch: StateFlow<Boolean> =
            userPreferencesRepository.userPreferencesFlow
                .map { it.isFirstLaunch }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = false,
                )

        fun confirmSafetyGuide(onComplete: () -> Unit) {
            viewModelScope.launch {
                userPreferencesRepository.updateFirstLaunch(false)
                onComplete()
            }
        }
    }

@Composable
fun SafetyGuideScreen(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SafetyGuideViewModel = hiltViewModel(),
) {
    BackHandler {}

    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // 하단 고정 카드 방식. ModalBottomSheet 는 콘텐츠(이미지+텍스트+버튼)를 다 펼치지 못해
    // 하단 '다음/시작하기' 버튼이 잘리는 문제가 있어, 콘텐츠 높이에 맞는 카드를 하단에 고정한다.
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(BackgroundBlack),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier =
                Modifier
                    // 가로모드에서는 카드가 화면 전체 폭을 채워 답답하므로 폭을 제한해 중앙 정렬한다.
                    // (widthIn 을 fillMaxWidth 앞에 두어야 최대 폭 제한이 적용된다.)
                    .then(if (isLandscape) Modifier.widthIn(max = 560.dp) else Modifier)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF1E2530)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 드래그 핸들 캡슐 인디케이터(장식, Figma 스펙 32x4)
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(color = Color(0xFF4A5568), shape = RoundedCornerShape(2.dp)),
            )
            SafetyGuideSheetContent(
                onConfirm = { viewModel.confirmSafetyGuide(onConfirm) },
                isLandscape = isLandscape,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SafetyGuideSheetContent(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == 1

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 페이지 인디케이터 — pager 밖에 고정해 항상 노출.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp),
        ) {
            repeat(2) { index ->
                val isActive = pagerState.currentPage == index
                val widthSize = if (isActive) 32.dp else 12.dp
                val color = if (isActive) NeonGreen else Color(0xFF4A5568)
                Box(
                    modifier =
                        Modifier
                            .size(width = widthSize, height = 4.dp)
                            .background(color = color, shape = RoundedCornerShape(2.dp)),
                )
            }
        }

        // 스와이프 콘텐츠(이미지+타이틀+설명)만 pager 에 둔다. 버튼은 아래에 고정.
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) { page ->
            val imageRes = if (page == 0) R.drawable.img_info_mount else R.drawable.img_info_lean
            val titleRes = if (page == 0) R.string.guide_safety_mount_title else R.string.guide_safety_title
            val messageRes = if (page == 0) R.string.guide_safety_mount_message else R.string.guide_safety_message

            if (isLandscape) {
                // 가로모드: 세로 공간이 좁고 세로형 일러스트가 잘리므로 이미지(좌) + 텍스트(우)를 나란히 배치한다.
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(184.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier
                                .weight(0.44f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(
                        modifier = Modifier.weight(0.56f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(id = titleRes),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(id = messageRes),
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(id = titleRes),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(id = messageRes),
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 동작 버튼 — pager 밖에 고정해 항상 노출. 현재 페이지에 따라 라벨/동작 분기.
        Button(
            onClick = {
                if (isLastPage) {
                    onConfirm()
                } else {
                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                }
            },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = BackgroundBlack,
                ),
            shape = RoundedCornerShape(16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
        ) {
            Text(
                text =
                    stringResource(
                        id = if (isLastPage) R.string.guide_safety_confirm else R.string.guide_safety_next,
                    ),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SafetyGuideSheetContentPreview() {
    SpeedoTheme {
        SafetyGuideSheetContent(onConfirm = {})
    }
}

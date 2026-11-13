package com.respect.community

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.respect.community.service.StreamMonitorService
import com.respect.community.ui.components.LocalRespectHazeState
import com.respect.community.ui.components.RespectHazeContainer
import com.respect.community.ui.components.respectGlassStyle
import com.respect.community.ui.screens.MediaScreen
import com.respect.community.ui.screens.PlayerScreen
import com.respect.community.ui.screens.ProfileAdminScreen
import com.respect.community.ui.screens.SearchScreen
import com.respect.community.ui.screens.SocialScreen
import com.respect.community.ui.screens.StreamsScreen
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.RespectCommunityTheme
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.VoidBlack
import com.respect.community.ui.viewmodel.CommunityViewModel
import com.respect.community.ui.viewmodel.PlayerViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class CommunityTab(val route: String, val label: String, val icon: ImageVector) {
    data object Streams : CommunityTab("streams", "البثوث", Icons.Rounded.LiveTv)
    data object Social : CommunityTab("social", "المجتمع", Icons.Rounded.Forum)
    data object Media : CommunityTab("media", "المقاطع", Icons.Rounded.Movie)
    data object Search : CommunityTab("search", "بحث", Icons.Rounded.Search)
    data object Profile : CommunityTab("profile", "حسابي", Icons.Rounded.Person)
}

class MainActivity : ComponentActivity() {

    private var pendingOAuthUri by mutableStateOf<Uri?>(null)
    private var pendingOpenSlug by mutableStateOf<String?>(null)

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        ensureBackgroundService()

        val app = application as RespectCommunityApp

        setContent {
            RespectCommunityTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RespectCommunityRoot(
                        app = app,
                        pendingOAuthUri = pendingOAuthUri,
                        onConsumedOAuthUri = { pendingOAuthUri = null },
                        pendingOpenSlug = pendingOpenSlug,
                        onConsumedOpenSlug = { pendingOpenSlug = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "respect" && data.host == "oauth") {
            pendingOAuthUri = data
        }
        val slug = intent?.getStringExtra(StreamMonitorService.EXTRA_OPEN_SLUG)
        if (!slug.isNullOrBlank()) {
            pendingOpenSlug = slug
        }
    }

    private fun ensureBackgroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val app = application as RespectCommunityApp
        lifecycleScope.launch {
            if (app.container.sessionManager.getNotificationPrefsOnce().monitoringEnabled) {
                StreamMonitorService.start(this@MainActivity)
            }
        }
    }
}

@Composable
private fun RespectCommunityRoot(
    app: RespectCommunityApp,
    pendingOAuthUri: Uri?,
    onConsumedOAuthUri: () -> Unit,
    pendingOpenSlug: String?,
    onConsumedOpenSlug: () -> Unit
) {
    val vm: CommunityViewModel = viewModel(
        factory = CommunityViewModel.Factory(app.container.repository, app.container.sessionManager)
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val streamers by vm.streamers.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val scheduleEvents by vm.scheduleEvents.collectAsStateWithLifecycle()
    val timelinePosts by vm.timelinePosts.collectAsStateWithLifecycle()
    val analyses by vm.analyses.collectAsStateWithLifecycle()
    val clips by vm.clips.collectAsStateWithLifecycle()
    val artworks by vm.artworks.collectAsStateWithLifecycle()
    val generalChat by vm.generalChat.collectAsStateWithLifecycle()
    val bannedUsers by vm.bannedUsers.collectAsStateWithLifecycle()
    val userProfile by vm.userProfile.collectAsStateWithLifecycle()
    val kickSession by vm.kickAuthSession.collectAsStateWithLifecycle()
    val notifPrefs by vm.notificationPrefs.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val isBusy by vm.isBusy.collectAsStateWithLifecycle()
    val snackbarMsg by vm.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = remember { HazeState() }
    val activityContext = LocalContext.current

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(950L)
        showSplash = false
    }

    LaunchedEffect(pendingOAuthUri) {
        if (pendingOAuthUri != null) {
            vm.handleOAuthRedirect(pendingOAuthUri)
            onConsumedOAuthUri()
        }
    }

    LaunchedEffect(pendingOpenSlug) {
        if (!pendingOpenSlug.isNullOrBlank()) {
            navController.navigate("player/$pendingOpenSlug")
            onConsumedOpenSlug()
        }
    }

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearSnackbar()
        }
    }

    val tabs = listOf(
        CommunityTab.Streams,
        CommunityTab.Social,
        CommunityTab.Media,
        CommunityTab.Search,
        CommunityTab.Profile
    )
    val showBottomNav = currentRoute != null && !currentRoute.startsWith("player/")

    CompositionLocalProvider(LocalRespectHazeState provides hazeState) {
        Box(modifier = Modifier.fillMaxSize()) {
            RespectHazeContainer(hazeState = hazeState) {
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (showBottomNav) {
                            val navShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(navShape)
                                    .hazeChild(state = hazeState, style = respectGlassStyle(true))
                                    .border(BorderStroke(1.dp, GlassBorder), navShape)
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp
                                ) {
                                    tabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(CommunityTab.Streams.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label, fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = TextPrimary,
                                                selectedTextColor = RespectPurpleBright,
                                                indicatorColor = RespectPurple.copy(alpha = 0.30f),
                                                unselectedIconColor = TextMuted,
                                                unselectedTextColor = TextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = CommunityTab.Streams.route
                        ) {
                            composable(CommunityTab.Streams.route) {
                                StreamsScreen(
                                    streamers = streamers,
                                    scheduleEvents = scheduleEvents,
                                    isAdmin = userProfile.isAdmin,
                                    isBusy = isBusy,
                                    onAddStreamer = vm::addStreamer,
                                    onRefreshAll = { vm.refreshAllStreamers(silent = false) },
                                    onToggleFavorite = vm::toggleFavorite,
                                    onToggleFollow = vm::toggleFollow,
                                    onRemoveStreamer = vm::removeStreamer,
                                    onWatchStreamer = { slug -> navController.navigate("player/$slug") },
                                    onSubmitSuggestion = vm::submitSuggestion,
                                    onSaveSchedule = vm::saveScheduleEvent,
                                    onDeleteSchedule = vm::deleteScheduleEvent
                                )
                            }

                            composable(CommunityTab.Social.route) {
                                SocialScreen(
                                    timelinePosts = timelinePosts,
                                    generalChat = generalChat,
                                    analyses = analyses,
                                    isAdmin = userProfile.isAdmin,
                                    onCreatePost = vm::createTimelinePost,
                                    onTogglePostLike = vm::togglePostLike,
                                    onDeletePost = vm::deleteTimelinePost,
                                    onBanUserOneDay = vm::banUserOneDay,
                                    onSendGeneralChat = vm::sendGeneralChat,
                                    onDeleteChatMessage = vm::deleteChatMessage,
                                    onAddAnalysis = vm::addAnalysis,
                                    onDeleteAnalysis = vm::deleteAnalysis
                                )
                            }

                            composable(CommunityTab.Media.route) {
                                MediaScreen(
                                    clips = clips,
                                    artworks = artworks,
                                    isAdmin = userProfile.isAdmin,
                                    onAddClip = vm::addClip,
                                    onToggleClipLike = vm::toggleClipLike,
                                    onDeleteClip = vm::deleteClip,
                                    onAddArt = vm::addArt,
                                    onToggleArtLike = vm::toggleArtLike,
                                    onDeleteArt = vm::deleteArt
                                )
                            }

                            composable(CommunityTab.Search.route) {
                                SearchScreen(
                                    query = searchQuery,
                                    results = searchResults,
                                    onQueryChange = vm::updateSearchQuery,
                                    onWatchStreamer = { slug -> navController.navigate("player/$slug") },
                                    onNavigateToSocial = { navController.navigate(CommunityTab.Social.route) },
                                    onNavigateToMedia = { navController.navigate(CommunityTab.Media.route) }
                                )
                            }

                            composable(CommunityTab.Profile.route) {
                                ProfileAdminScreen(
                                    profile = userProfile,
                                    kickSession = kickSession,
                                    notificationPrefs = notifPrefs,
                                    streamers = streamers,
                                    suggestions = suggestions,
                                    scheduleEvents = scheduleEvents,
                                    timelinePosts = timelinePosts,
                                    clips = clips,
                                    artworks = artworks,
                                    bannedUsers = bannedUsers,
                                    onUpdateProfile = vm::updateProfile,
                                    onKickOAuthLogin = { vm.launchKickOAuth(activityContext) },
                                    onKickLogout = vm::logoutKick,
                                    onMonitoringChanged = { enabled -> vm.setMonitoring(app, enabled) },
                                    onSoundChanged = vm::setSound,
                                    onVibrationChanged = vm::setVibration,
                                    onFavoritesOnlyChanged = vm::setFavoritesOnly,
                                    onAdminLogin = vm::loginAdmin,
                                    onAdminLogout = vm::logoutAdmin,
                                    onAddStreamer = vm::addStreamer,
                                    onRemoveStreamer = vm::removeStreamer,
                                    onApproveSuggestion = vm::approveSuggestion,
                                    onRejectSuggestion = vm::rejectSuggestion,
                                    onDeleteSuggestion = vm::deleteSuggestion,
                                    onSaveSchedule = vm::saveScheduleEvent,
                                    onDeleteSchedule = vm::deleteScheduleEvent,
                                    onDeletePost = vm::deleteTimelinePost,
                                    onBanUserOneDay = vm::banUserOneDay,
                                    onUnbanUser = vm::unbanUser,
                                    onDeleteClip = vm::deleteClip,
                                    onDeleteArt = vm::deleteArt
                                )
                            }

                            composable(
                                route = "player/{slug}",
                                arguments = listOf(navArgument("slug") { type = NavType.StringType })
                            ) { entry ->
                                val slug = entry.arguments?.getString("slug").orEmpty()
                                val playerVm: PlayerViewModel = viewModel(
                                    key = "player_$slug",
                                    factory = PlayerViewModel.Factory(app, slug, app.container.repository)
                                )
                                PlayerScreen(
                                    viewModel = playerVm,
                                    onBack = { navController.popBackStack() },
                                    onRequestKickLogin = { vm.launchKickOAuth(activityContext) }
                                )
                            }
                        }
                    }
                }
            }

            // Branded RESPECT Splash Overlay
            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(450))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VoidBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.respect_logo),
                            contentDescription = "RESPECT",
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(26.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("RESPECT COMMUNITY", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text("مجتمع ريسبكت الرسمي", color = RespectPurpleBright, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.respect.community.auth.KickOAuthManager
import com.respect.community.service.StreamMonitorService
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.LocalRespectHazeState
import com.respect.community.ui.components.RespectBrandLogo
import com.respect.community.ui.components.RespectHazeContainer
import com.respect.community.ui.components.respectGlassStyle
import com.respect.community.ui.screens.AuthGateScreen
import com.respect.community.ui.screens.CommunityHubScreen
import com.respect.community.ui.screens.PlayerScreen
import com.respect.community.ui.screens.ProfileAdminScreen
import com.respect.community.ui.screens.StatsScreen
import com.respect.community.ui.screens.StreamsScreen
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.GoldAccent
import com.respect.community.ui.theme.RespectCommunityTheme
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.theme.VoidBlack
import com.respect.community.ui.viewmodel.CommunitySearchResults
import com.respect.community.ui.viewmodel.CommunityViewModel
import com.respect.community.ui.viewmodel.PlayerViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class CommunityTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Streams : CommunityTab("streams", R.string.tab_streams, Icons.Rounded.LiveTv)
    data object Community : CommunityTab("community", R.string.tab_community, Icons.Rounded.Forum)
    data object Profile : CommunityTab("profile", R.string.tab_profile, Icons.Rounded.Person)
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
        if (data != null && (
                data.toString().startsWith(KickOAuthManager.REDIRECT_URI) ||
                    (data.scheme == "respect" && data.host == "oauth" &&
                        (data.path.isNullOrEmpty() || data.path?.startsWith("/callback") == true))
                )
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val communityUsers by vm.communityUsers.collectAsStateWithLifecycle()
    val userProfile by vm.userProfile.collectAsStateWithLifecycle()
    val kickSession by vm.kickAuthSession.collectAsStateWithLifecycle()
    val notifPrefs by vm.notificationPrefs.collectAsStateWithLifecycle()
    val remoteSettings by vm.remoteSettings.collectAsStateWithLifecycle()
    val statsRankings by vm.statsRankings.collectAsStateWithLifecycle()
    val selectedStatsPeriod by vm.selectedStatsPeriod.collectAsStateWithLifecycle()
    val hasMoreTimeline by vm.hasMoreTimeline.collectAsStateWithLifecycle()
    val isLoadingMorePosts by vm.isLoadingMorePosts.collectAsStateWithLifecycle()
    val isInitialLoading by vm.isInitialLoading.collectAsStateWithLifecycle()
    val networkError by vm.networkError.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val isBusy by vm.isBusy.collectAsStateWithLifecycle()
    val snackbarMsg by vm.snackbarMessage.collectAsStateWithLifecycle()

    val activityContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = remember { HazeState() }
    var showSearchOverlay by remember { mutableStateOf(false) }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(850L)
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
        CommunityTab.Community,
        CommunityTab.Profile
    )
    val isAuthorized = (userProfile.isFirebaseSignedIn && userProfile.isEmailVerified) || userProfile.isAdmin
    val isImmersiveRoute = currentRoute != null && (currentRoute.startsWith("player/") || currentRoute == "stats")
    val showChrome = isAuthorized && !isImmersiveRoute

    CompositionLocalProvider(LocalRespectHazeState provides hazeState) {
        Box(modifier = Modifier.fillMaxSize()) {
            RespectHazeContainer(hazeState = hazeState) {
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        if (showChrome) {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = TextPrimary
                                ),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RespectBrandLogo(size = 34.dp, cornerRadius = 10.dp)
                                        Text(
                                            text = stringResource(R.string.brand_short),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { showSearchOverlay = true }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = "بحث",
                                            tint = TextPrimary
                                        )
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (showChrome) {
                            val navShape = RoundedCornerShape(24.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .clip(navShape)
                                    .hazeChild(state = hazeState, style = respectGlassStyle(false))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), navShape)
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp
                                ) {
                                    tabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        val tabLabel = stringResource(tab.labelRes)
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(CommunityTab.Streams.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(tab.icon, contentDescription = tabLabel) },
                                            label = {
                                                Text(
                                                    tabLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = RespectPurpleBright,
                                                selectedTextColor = TextPrimary,
                                                indicatorColor = RespectPurple.copy(alpha = 0.22f),
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
                        if (!isAuthorized) {
                            AuthGateScreen(
                                profile = userProfile,
                                isBusy = isBusy,
                                onSignIn = vm::signInFirebase,
                                onSignUp = vm::signUpFirebase,
                                onResendVerification = vm::resendVerificationEmail,
                                onCheckVerification = vm::checkEmailVerification,
                                onSignOut = vm::signOutFirebase
                            )
                        } else if (remoteSettings.maintenanceMode && !userProfile.isAdmin && currentRoute != CommunityTab.Profile.route) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                HazeGlassCard(isGlowing = true) {
                                    Text("وضع الصيانة مفعل", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "يعمل فريق ريسبكت حالياً على تحديث المنصة. يمكنك الدخول لحساب الإدارة من تبويب حسابي.",
                                        color = TextPrimary
                                    )
                                }
                            }
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = CommunityTab.Streams.route
                            ) {
                                composable(CommunityTab.Streams.route) {
                                    StreamsScreen(
                                        streamers = streamers,
                                        scheduleEvents = scheduleEvents,
                                        bannerText = remoteSettings.bannerText,
                                        isAdmin = userProfile.isAdmin,
                                        isBusy = isBusy,
                                        isInitialLoading = isInitialLoading,
                                        networkError = networkError,
                                        onAddStreamer = vm::addStreamer,
                                        onRefreshAll = { vm.refreshAllStreamers(silent = false) },
                                        onRetry = vm::initialBootSync,
                                        onOpenStats = { navController.navigate("stats") },
                                        onToggleNotifying = vm::toggleNotifying,
                                        onToggleFollow = vm::toggleFollow,
                                        onRemoveStreamer = vm::removeStreamer,
                                        onWatchStreamer = { slug -> navController.navigate("player/$slug") },
                                        onSubmitSuggestion = vm::submitSuggestion,
                                        onSaveSchedule = vm::saveScheduleEvent,
                                        onDeleteSchedule = vm::deleteScheduleEvent
                                    )
                                }

                                composable(CommunityTab.Community.route) {
                                    CommunityHubScreen(
                                        timelinePosts = timelinePosts,
                                        generalChat = generalChat,
                                        analyses = analyses,
                                        clips = clips,
                                        artworks = artworks,
                                        isAdmin = userProfile.isAdmin,
                                        isInitialLoading = isInitialLoading,
                                        hasMorePosts = hasMoreTimeline,
                                        isLoadingMorePosts = isLoadingMorePosts,
                                        onLoadMorePosts = vm::loadNextTimelinePage,
                                        onUploadImage = vm::uploadImageToTelegraph,
                                        onCreatePost = vm::createTimelinePost,
                                        onTogglePostLike = vm::togglePostLike,
                                        onTogglePinPost = vm::togglePinPost,
                                        onDeletePost = vm::deleteTimelinePost,
                                        onBanUserOneDay = vm::banUserOneDay,
                                        onSendGeneralChat = vm::sendGeneralChat,
                                        onDeleteChatMessage = vm::deleteChatMessage,
                                        onAddAnalysis = vm::addAnalysis,
                                        onDeleteAnalysis = vm::deleteAnalysis,
                                        onAddClip = vm::addClip,
                                        onToggleClipLike = vm::toggleClipLike,
                                        onDeleteClip = vm::deleteClip,
                                        onAddArt = vm::addArt,
                                        onToggleArtLike = vm::toggleArtLike,
                                        onDeleteArt = vm::deleteArt
                                    )
                                }

                                composable(CommunityTab.Profile.route) {
                                    ProfileAdminScreen(
                                        profile = userProfile,
                                        kickSession = kickSession,
                                        notificationPrefs = notifPrefs,
                                        remoteSettings = remoteSettings,
                                        communityUsers = communityUsers,
                                        streamers = streamers,
                                        suggestions = suggestions,
                                        scheduleEvents = scheduleEvents,
                                        timelinePosts = timelinePosts,
                                        clips = clips,
                                        artworks = artworks,
                                        bannedUsers = bannedUsers,
                                        onUploadImage = vm::uploadImageToTelegraph,
                                        onUpdateProfile = vm::updateProfile,
                                        onFirebaseSignIn = vm::signInFirebase,
                                        onFirebaseSignUp = vm::signUpFirebase,
                                        onResendVerification = vm::resendVerificationEmail,
                                        onCheckVerification = vm::checkEmailVerification,
                                        onFirebaseSignOut = vm::signOutFirebase,
                                        onKickOAuthLogin = { vm.launchKickOAuth(activityContext) },
                                        onKickLogout = vm::logoutKick,
                                        onMonitoringChanged = { enabled -> vm.setMonitoring(app, enabled) },
                                        onSoundChanged = vm::setSound,
                                        onVibrationChanged = vm::setVibration,
                                        onFavoritesOnlyChanged = vm::setFavoritesOnly,
                                        onAdminLogin = vm::loginAdmin,
                                        onAdminLogout = vm::logoutAdmin,
                                        onUpdateUserRole = vm::updateUserRole,
                                        onSetUserBannedOrMuted = vm::setUserBannedOrMuted,
                                        onDeleteCommunityUser = vm::deleteCommunityUser,
                                        onAddGlobalStreamer = { slug, chars -> vm.addStreamer(slug, chars, isGlobal = true) },
                                        onToggleFeaturedStreamer = vm::toggleFeatured,
                                        onRemoveStreamer = vm::removeStreamer,
                                        onApproveSuggestion = vm::approveSuggestion,
                                        onRejectSuggestion = vm::rejectSuggestion,
                                        onDeleteSuggestion = vm::deleteSuggestion,
                                        onSaveSchedule = vm::saveScheduleEvent,
                                        onDeleteSchedule = vm::deleteScheduleEvent,
                                        onTogglePinPost = vm::togglePinPost,
                                        onDeletePost = vm::deleteTimelinePost,
                                        onBanUserOneDay = vm::banUserOneDay,
                                        onUnbanUser = vm::unbanUser,
                                        onDeleteClip = vm::deleteClip,
                                        onDeleteArt = vm::deleteArt,
                                        onSaveGlobalSettings = vm::saveGlobalSettings,
                                        onRefreshRemoteConfig = vm::refreshRemoteConfig,
                                        onSendBroadcast = vm::sendBroadcastNotification,
                                        onSendTargetedNotification = vm::sendTargetedNotification,
                                        onSendTestKickNotification = vm::sendTestKickNotification
                                    )
                                }

                                composable("stats") {
                                    StatsScreen(
                                        rankings = statsRankings,
                                        selectedPeriod = selectedStatsPeriod,
                                        isBusy = isBusy,
                                        onSelectPeriod = vm::setStatsPeriod,
                                        onRefreshStats = vm::refreshStatsFromKICK,
                                        onWatchStreamer = { slug -> navController.navigate("player/$slug") },
                                        onBack = { navController.popBackStack() }
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
            }

            if (showSearchOverlay) {
                UnifiedSearchDialog(
                    query = searchQuery,
                    results = searchResults,
                    onQueryChange = vm::updateSearchQuery,
                    onOpenStreamer = { slug ->
                        showSearchOverlay = false
                        navController.navigate("player/$slug")
                    },
                    onDismiss = { showSearchOverlay = false }
                )
            }

            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VoidBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RespectBrandLogo(size = 104.dp, cornerRadius = 24.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.app_name),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.brand_full),
                            color = RespectPurpleBright,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedSearchDialog(
    query: String,
    results: CommunitySearchResults,
    onQueryChange: (String) -> Unit,
    onOpenStreamer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VoidBlack.copy(alpha = 0.96f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                stringResource(R.string.search_placeholder),
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = RespectPurpleBright) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RespectPurpleBright,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "إغلاق", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (results.streamers.isNotEmpty()) {
                        item { Text("الستريمرز", color = RespectPurpleBright, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        items(results.streamers, key = { "sr_s_${it.slug}" }) { s ->
                            HazeGlassCard(modifier = Modifier.clickable { onOpenStreamer(s.slug) }) {
                                Text("${s.displayName} (@${s.slug})", color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (s.characterNames.isNotBlank()) {
                                    Text("الشخصيات: ${s.characterNames}", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    if (results.posts.isNotEmpty()) {
                        item { Text("المنشورات", color = RespectPurpleBright, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        items(results.posts, key = { "sr_p_${it.id}" }) { p ->
                            HazeGlassCard {
                                Text(p.authorName, color = RespectPurpleBright, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(p.content, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (results.clips.isNotEmpty()) {
                        item { Text("الكليبات", color = RespectPurpleBright, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        items(results.clips, key = { "sr_c_${it.id}" }) { c ->
                            HazeGlassCard {
                                Text(c.title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("@${c.streamerName}", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (results.arts.isNotEmpty()) {
                        item { Text("معرض الفن", color = RespectPurpleBright, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        items(results.arts, key = { "sr_a_${it.id}" }) { a ->
                            HazeGlassCard {
                                Text(a.title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("الفنان: ${a.artistName}", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (query.isNotBlank() && results.streamers.isEmpty() && results.posts.isEmpty() &&
                        results.clips.isEmpty() && results.arts.isEmpty()
                    ) {
                        item {
                            Surface(
                                color = RespectSurface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لا توجد نتائج مطابقة لـ \"$query\"",
                                    color = TextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

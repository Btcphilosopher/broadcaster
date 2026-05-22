package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.data.database.CommentEntity
import com.example.data.database.CreatorEntity
import com.example.data.database.MessageEntity
import com.example.data.database.PostEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCreator by viewModel.selectedCreator.collectAsState()
    val focusedCommentPost by viewModel.focusedPostForComments.collectAsState()

    val playingPodcast by viewModel.playingPodcast.collectAsState()
    val podcastIsPlaying by viewModel.podcastIsPlaying.collectAsState()
    val podcastProgress by viewModel.podcastProgress.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (focusedCommentPost == null) {
                BroadcastBottomBar(
                    activeTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }
        },
        containerColor = CyberDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main navigation router
            Crossfade(
                targetState = when {
                    focusedCommentPost != null -> "COMMENTS"
                    selectedCreator != null -> "CREATOR_PROFILE"
                    else -> currentTab
                },
                label = "screennav"
            ) { target ->
                when (target) {
                    "HOME" -> HomeScreen(viewModel)
                    "EXPLORE" -> ExploreScreen(viewModel)
                    "CREATE" -> CreateScreen(viewModel)
                    "MEDIA" -> MediaCenterScreen(viewModel)
                    "MESSAGES" -> MessagesScreen(viewModel)
                    "NOTIFICATIONS" -> NotificationsScreen(viewModel)
                    "MONETIZATION" -> MonetizationScreen(viewModel)
                    "PROFILE" -> ProfileDetailScreen(selectedCreator = null, viewModel = viewModel)
                    "CREATOR_PROFILE" -> ProfileDetailScreen(selectedCreator = selectedCreator, viewModel = viewModel)
                    "COMMENTS" -> CommentsModalScreen(post = focusedCommentPost!!, viewModel = viewModel)
                }
            }

            // Global floating premium mini-audio player
            if (playingPodcast != null && focusedCommentPost == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    FloatingPodcastBar(
                        post = playingPodcast!!,
                        isPlaying = podcastIsPlaying,
                        progress = podcastProgress,
                        onToggle = { viewModel.togglePodcastPlayback() },
                        onStop = { viewModel.stopPodcast() },
                        onSpeedChange = { viewModel.changePodcastSpeed() },
                        speed = viewModel.podcastPlaybackSpeed.collectAsState().value
                    )
                }
            }
        }
    }
}

@Composable
fun BroadcastBottomBar(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = CyberCard,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = CyberGrey,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        val menuItems = listOf(
            NavigationItem("HOME", "Feed", Icons.Rounded.Home, Icons.Outlined.Home),
            NavigationItem("EXPLORE", "Explore", Icons.Rounded.Explore, Icons.Outlined.Explore),
            NavigationItem("CREATE", "Cast", Icons.Rounded.AddCircle, Icons.Outlined.AddCircle),
            NavigationItem("MEDIA", "Studio", Icons.Rounded.PlayCircle, Icons.Outlined.PlayCircle),
            NavigationItem("MESSAGES", "Signal", Icons.Rounded.Message, Icons.Outlined.Message),
            NavigationItem("PROFILE", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
        )

        menuItems.forEach { item ->
            val isActive = activeTab == item.id || 
                    (item.id == "PROFILE" && activeTab == "MONETIZATION")
            NavigationBarItem(
                selected = isActive,
                onClick = { onTabSelected(item.id) },
                icon = {
                    Icon(
                        imageVector = if (isActive) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.label,
                        tint = if (isActive) NeonCyan else MutedText,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) NeonCyan else MutedText
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = NeonCyan.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_item_${item.id.lowercase()}")
            )
        }
    }
}

data class NavigationItem(
    val id: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

// --- HOME SCREEN UNIFIED STREAM ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val posts by viewModel.unifiedFeed.collectAsState()
    val activeSubFeedTab by viewModel.homeSubFeedTab.collectAsState()
    val creators by viewModel.allCreators.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = CyberGrey,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "BROADCASTER",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta))
                    )
                )
                Text(
                    text = "Unified Broadcast Stream",
                    fontSize = 10.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.setTab("MONETIZATION") },
                    modifier = Modifier.background(CyberGrey, CircleShape)
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, "Wallet", tint = SoftViolet)
                }
                IconButton(
                    onClick = { viewModel.setTab("NOTIFICATIONS") },
                    modifier = Modifier.background(CyberGrey, CircleShape)
                ) {
                    Icon(Icons.Rounded.Notifications, "Notifications", tint = NeonMagenta)
                }
            }
        }

        // Horizontal Creator Stories / Channel Signals Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.setTab("PROFILE") }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, NeonCyan, CircleShape)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(CyberGrey),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, "You", tint = MutedText, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Your Channel", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                }
            }

            items(creators) { creator ->
                val isFollowing = creator.isFollowing
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.selectCreator(creator) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                2.dp,
                                if (isFollowing) NeonMagenta else SoftViolet,
                                CircleShape
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(SoftViolet.copy(alpha = 0.4f), CyberDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = creator.displayName.take(2).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = PureWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = creator.displayName,
                            fontSize = 11.sp,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 70.dp)
                        )
                        if (creator.isVerified) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Filled.CheckCircle, "Verified", tint = NeonCyan, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        // Sub Feed Tab Selector (For You, Following, Trending, Media Only)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(CyberGrey, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            val subTabs = listOf(
                "FOR_YOU" to "For You",
                "FOLLOWING" to "Following",
                "TRENDING" to "Trending",
                "MEDIA_ONLY" to "Media Only"
            )

            subTabs.forEach { (id, label) ->
                val active = activeSubFeedTab == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) CyberDark else Color.Transparent)
                        .border(
                            1.dp,
                            if (active) NeonCyan.copy(alpha = 0.4f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setHomeSubFeedTab(id) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (active) NeonCyan else MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Filtering list based on selected state tab
        val filteredPosts = remember(posts, activeSubFeedTab) {
            when (activeSubFeedTab) {
                "MEDIA_ONLY" -> posts.filter { it.contentType != "TEXT" }
                "TRENDING" -> posts.filter { it.likesCount > 200 }
                // Otherwise normal full list, simulated Following using seeded following flags
                else -> posts
            }
        }

        if (filteredPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Feed, "Empty Feed", tint = MutedText, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No broadcasts under this segment yet.", color = MutedText, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredPosts, key = { it.id }) { post ->
                    UnifiedStreamCard(
                        post = post,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedStreamCard(
    post: PostEntity,
    viewModel: MainViewModel
) {
    val creators by viewModel.allCreators.collectAsState()
    val creator = remember(creators, post.creatorUsername) {
        creators.find { it.username == post.creatorUsername }
    }
    val playingPodcast by viewModel.playingPodcast.collectAsState()
    val isPlayingThisPodcast = playingPodcast?.id == post.id

    val activeVideoPostId by viewModel.activeVideoPostId.collectAsState()
    val isPlayingThisVideo = activeVideoPostId == post.id
    val videoProgress by viewModel.videoProgress.collectAsState()
    val videoIsMuted by viewModel.videoIsMuted.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isPlayingThisVideo || isPlayingThisPodcast) NeonCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(24.dp)
            ),
        color = CyberCard,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar, DisplayName, Username, Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (creator != null) {
                            viewModel.selectCreator(creator)
                        } else if (post.creatorUsername == "you") {
                            viewModel.setTab("PROFILE")
                        }
                    }
                ) {
                    // Styled Initial Circle Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(SoftViolet, Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (creator?.displayName ?: post.creatorUsername).take(1).uppercase(),
                            color = PureWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = creator?.displayName ?: "You (User)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            if (creator?.isVerified == true || post.creatorUsername == "you") {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.CheckCircle, "Verified", tint = NeonCyan, modifier = Modifier.size(13.dp))
                            }
                        }
                        Text(
                            text = "@${post.creatorUsername}",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                }

                // Category pill label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberGrey)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(post.category, fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Media Type Specific Renders
            when (post.contentType) {
                "IMAGE" -> {
                    // Media Album Carousel Layout
                    Text(
                        text = post.text,
                        fontSize = 14.sp,
                        color = PureWhite,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Realistic layered album mock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CyberGrey, SoftViolet.copy(alpha = 0.2f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background visuals drawing overlays
                        Text(
                            text = "📸 Album: [${post.mediaUrls.replace(",", " - ")}]",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoAlbum, "Album", tint = PureWhite, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1/2", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                "VIDEO" -> {
                    // Video Broadcast Stream render
                    Text(
                        text = post.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = post.text,
                        fontSize = 13.sp,
                        color = MutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Unified Immersive Simulated Playback Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .clickable { viewModel.togglePlayVideo(post.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlayingThisVideo) {
                            // Active rendering visual wave representation + progress
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(NeonMagenta)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("LIVE SIMULATION", fontSize = 10.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleVideoAudioMute() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (videoIsMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                            contentDescription = "Mute",
                                            tint = PureWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Interactive Subtitles simulated in real-time matching tickers!
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                videoProgress < 0.3f -> "“Analyzing computational neural densities...”"
                                                videoProgress < 0.7f -> "“...why traditional cloud AI is now completely obsolete...”"
                                                else -> "“...stream connection active. Subscribing for parameters.”"
                                            },
                                            color = NeonCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Bottom stream timeline track slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(post.duration, color = PureWhite, fontSize = 10.sp)
                                    LinearProgressIndicator(
                                        progress = videoProgress,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp),
                                        color = NeonCyan,
                                        trackColor = CyberGrey
                                    )
                                    Icon(Icons.Rounded.PauseCircleFilled, "Pause", tint = PureWhite, modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            // Cover Preview Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CyberDark, SoftViolet.copy(alpha = 0.2f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.PlayArrow, "Play Video", tint = NeonCyan, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Preview Active Video (${post.duration})", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "PODCAST" -> {
                    // Podcast Audio Track Layout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SoftViolet.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .background(CyberGrey)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MicExternalOn, "Mic", tint = SoftViolet, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(post.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Episode Podcast", fontSize = 11.sp, color = MutedText)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MutedText))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(post.duration, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.playPodcastTrack(post) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isPlayingThisPodcast) NeonMagenta else SoftViolet),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isPlayingThisPodcast) "Active View" else "Stream",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberDark
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(post.text, fontSize = 13.sp, color = MutedText)
                }

                else -> {
                    // Simple short-form text post (Twitter format)
                    Text(
                        text = post.text,
                        fontSize = 15.sp,
                        color = PureWhite,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Interactive actions
            Divider(color = CyberGrey)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Likes Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.toggleLike(post.id) }
                        .testTag("like_action_row")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) NeonMagenta else MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 12.sp,
                        color = if (post.isLiked) NeonMagenta else MutedText,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reposts Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.toggleRepost(post.id) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Repost",
                        tint = if (post.isReposted) NeonCyan else MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.repostsCount}",
                        fontSize = 12.sp,
                        color = if (post.isReposted) NeonCyan else MutedText,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comment Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.setFocusedCommentPost(post) }
                        .testTag("comment_action_row")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 12.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bookmark Action
                IconButton(
                    onClick = { viewModel.toggleBookmark(post.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (post.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save Bookmark",
                        tint = if (post.isBookmarked) SoftViolet else MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- GLOBAL FLOATING AUDIOPLAYER BAR ---
@Composable
fun FloatingPodcastBar(
    post: PostEntity,
    isPlaying: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: () -> Unit,
    speed: Float
) {
    Surface(
        color = CyberCard,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftViolet.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Audio pulsating visual node
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SoftViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Playing",
                            tint = SoftViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = post.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${post.creatorUsername} • podcast active",
                            fontSize = 11.sp,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Speed multiplier button
                    TextButton(
                        onClick = onSpeedChange,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("${speed}x", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                            contentDescription = "Toggle",
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = onStop) {
                        Icon(Icons.Rounded.Close, "Stop", tint = MutedText, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback progression ticker bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = SoftViolet,
                trackColor = CyberGrey
            )
        }
    }
}

// --- EXPLORE / SEARCH SCREEN ---
@Composable
fun ExploreScreen(viewModel: MainViewModel) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.exploreSearchQuery.collectAsState()
    val posts by viewModel.unifiedFeed.collectAsState()

    val categories = listOf("Tech", "Music", "Gaming", "News", "Lifestyle")

    val searchedPosts = remember(posts, searchQuery, selectedCategory) {
        posts.filter {
            (it.category == selectedCategory) &&
                    (searchQuery.isEmpty() || 
                            it.text.contains(searchQuery, ignoreCase = true) || 
                            it.title.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Immersive Broadcaster search input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setExploreSearchQuery(it) },
                placeholder = { Text("Search text, podcasts, tags, or creators...", color = MutedText, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = NeonCyan) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberGrey,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )
        }

        // Horizontal Category Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { cat ->
                val isActive = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) SoftViolet else CyberGrey)
                        .clickable { viewModel.setExploreCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) CyberDark else PureWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Searched lists scroll
        if (searchedPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.SearchOff, "Search Off", tint = MutedText, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No direct signals trace to \"$searchQuery\"", color = MutedText, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "EXPLORE HOT Broadcast Nodes (${searchedPosts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SoftViolet,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                items(searchedPosts, key = { it.id }) { post ->
                    UnifiedStreamCard(post = post, viewModel = viewModel)
                }
            }
        }
    }
}

// --- CREATE SCREEN MULTI-FORMAT COMPOSER ---
@Composable
fun CreateScreen(viewModel: MainViewModel) {
    val curPostType by viewModel.newPostType.collectAsState()
    val postText by viewModel.newPostText.collectAsState()
    val postTitle by viewModel.newPostTitle.collectAsState()
    val postCategory by viewModel.newPostCategory.collectAsState()
    val attachmentsStr by viewModel.newPostAttachmentsStr.collectAsState()

    val aiInProgress by viewModel.aiInProgress.collectAsState()
    val aiLogMessage by viewModel.aiLogMessage.collectAsState()

    val categories = listOf("Tech", "Music", "Gaming", "News", "Lifestyle")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "BROADCAST HUB COMPOSER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            color = NeonCyan
        )
        Text(
            text = "Publish Multi-Format Content",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Format Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val formats = listOf("TEXT" to "Text", "IMAGE" to "Album", "VIDEO" to "Video", "PODCAST" to "Episode")
            formats.forEach { (id, label) ->
                val active = curPostType == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) NeonCyan else Color.Transparent)
                        .clickable { viewModel.setNewPostType(id) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (active) CyberDark else MutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Title Input parameter if not simple text microblog
        if (curPostType != "TEXT") {
            OutlinedTextField(
                value = postTitle,
                onValueChange = { viewModel.setNewPostTitle(it) },
                label = { Text("Title / Episode Name", color = MutedText) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftViolet,
                    unfocusedBorderColor = CyberGrey,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // Subtitle/Body Composer Input
        OutlinedTextField(
            value = postText,
            onValueChange = { viewModel.setNewPostText(it) },
            label = { 
                Text(
                    text = when (curPostType) {
                        "TEXT" -> "What's launching on your broadcast grid?..."
                        "VIDEO" -> "Video description/tags..."
                        "PODCAST" -> "Enter episode speech outline / summary text..."
                        else -> "Image description/caption..."
                    },
                    color = MutedText
                )
            },
            minLines = 4,
            maxLines = 8,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberGrey,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_post_text_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Content attachments picker mock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGrey, RoundedCornerShape(12.dp))
                .background(CyberCard)
                .clickable {
                    // Simulating uploading asset by setting a mock resource value
                    val text = when (curPostType) {
                        "IMAGE" -> "render_neon_vibe.png,wireframe_grid.png"
                        "VIDEO" -> "video_neuromorphic"
                        "PODCAST" -> "audio_synth_dust"
                        else -> "attachments_spec.json"
                    }
                    viewModel.setNewPostAttachments(text)
                }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (curPostType) {
                        "IMAGE" -> Icons.Rounded.AddAPhoto
                        "VIDEO" -> Icons.Rounded.VideoCall
                        "PODCAST" -> Icons.Rounded.Mic
                        else -> Icons.Rounded.Attachment
                    },
                    contentDescription = "Attach",
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Select Media Asset Source", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Text(
                        text = if (attachmentsStr.isEmpty()) "Attach simulated local resources..." else "Attached: $attachmentsStr",
                        fontSize = 11.sp,
                        color = MutedText
                    )
                }
            }
            if (attachmentsStr.isNotEmpty()) {
                Icon(Icons.Rounded.CheckCircle, "Uploaded", tint = NeonCyan, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category selection dropdown tags
        Text("CATEGORY DISTRIBUTION NODES", fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val active = postCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) NeonCyan else CyberGrey)
                        .clickable { viewModel.setNewPostCategory(cat) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(cat, fontSize = 11.sp, color = if (active) CyberDark else PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GOOGLE GEMINI AI ASSISTANCE BOX
        Surface(
            color = CyberGrey.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, "Gemini AI", tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Gemini AI Co-Writer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }

                    if (aiInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.runAiAssist("caption") },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGrey),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Generate Caption", fontSize = 10.sp, color = NeonCyan)
                    }
                    Button(
                        onClick = { viewModel.runAiAssist("hashtags") },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGrey),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Hashtags Suggestions", fontSize = 10.sp, color = NeonCyan)
                    }

                    if (curPostType != "TEXT") {
                        Button(
                            onClick = { viewModel.runAiAssist("title") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrey),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Optimize Title", fontSize = 10.sp, color = SoftViolet)
                        }
                    } else if (curPostType == "PODCAST" || curPostType == "VIDEO") {
                        Button(
                            onClick = { viewModel.runAiAssist("transcribe") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrey),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Simulate Subtitles", fontSize = 10.sp, color = SoftViolet)
                        }
                    }
                }

                if (aiLogMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(aiLogMessage, fontSize = 11.sp, color = MutedText, lineHeight = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PUBLISH ACTION BUTTON
        Button(
            onClick = { viewModel.submitNewPost() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta)),
                    RoundedCornerShape(28.dp)
                )
                .testTag("publish_post_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Icon(Icons.Rounded.Send, "Cast Stream", tint = Color.Black)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Broadcast Stream Signal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// --- EXPLORE MEDIA / CHANNELS HUB SCREEN ---
@Composable
fun MediaCenterScreen(viewModel: MainViewModel) {
    val posts by viewModel.unifiedFeed.collectAsState()
    val playingPodcast by viewModel.playingPodcast.collectAsState()

    val videos = remember(posts) { posts.filter { it.contentType == "VIDEO" } }
    val podcasts = remember(posts) { posts.filter { it.contentType == "PODCAST" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "MEDIA BROADCAST CENTER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            color = SoftViolet
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Video Hub (YouTube layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Creators Short/Long Library", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            IconButton(onClick = {}) {
                Icon(Icons.Rounded.VideoLibrary, "VLibrary", tint = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(videos) { vid ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    modifier = Modifier
                        .width(180.dp)
                        .clickable { viewModel.togglePlayVideo(vid.id) }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(CyberGrey, NeonMagenta.copy(alpha = 0.2f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PlayCircleOutline, "Play", tint = PureWhite, modifier = Modifier.size(32.dp))
                            Text(
                                vid.duration,
                                color = PureWhite,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(vid.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("@${vid.creatorUsername}", fontSize = 10.sp, color = MutedText)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Podcast Episodes Stream (Spotify interface style)
        Text("Podcast Episodes & Subs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            podcasts.forEach { pod ->
                val active = playingPodcast?.id == pod.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) SoftViolet.copy(alpha = 0.15f) else CyberCard)
                        .border(
                            1.dp,
                            if (active) SoftViolet else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.playPodcastTrack(pod) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberGrey),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Podcasts, "Podcast", tint = SoftViolet)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(pod.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("@${pod.creatorUsername} • ${pod.duration}", fontSize = 11.sp, color = MutedText)
                        }
                    }

                    Icon(
                        imageVector = if (active) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                        contentDescription = "Play",
                        tint = SoftViolet
                    )
                }
            }
        }
    }
}

// --- COMMUNICATIONS / SIGNALS DIRECT MESSAGES ---
@Composable
fun MessagesScreen(viewModel: MainViewModel) {
    val activeChatUsername by viewModel.activeChatUsername.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val creators by viewModel.allCreators.collectAsState()

    var messageInputText by remember { mutableStateOf("") }

    if (activeChatUsername == null) {
        // Active DMs Conversations list
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "MESSAGING DECONSTRUCTED SIGNAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = NeonCyan,
                letterSpacing = 2.sp
            )
            Text(
                "Secure Channel Threads",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (creators.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No connected creators in signal list.", color = MutedText)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(creators) { creator ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberCard)
                                .clickable { viewModel.startChatWith(creator.username) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SoftViolet),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(creator.displayName.take(1).uppercase(), color = PureWhite, fontWeight = FontWeight.Black)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(creator.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                    Text("@${creator.username}", fontSize = 12.sp, color = MutedText)
                                }
                            }

                            Icon(Icons.Rounded.ChevronRight, "Open", tint = MutedText)
                        }
                    }
                }
            }
        }
    } else {
        // Individual active dialogue screen
        Column(modifier = Modifier.fillMaxSize()) {
            val creator = creators.find { it.username == activeChatUsername }
            val displayName = creator?.displayName ?: activeChatUsername!!

            // Conversation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeChat() }) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = PureWhite)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(displayName.take(1).uppercase(), color = CyberDark, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color.Green))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Online Thread Signal", fontSize = 10.sp, color = MutedText)
                    }
                }
            }

            // chat messages scrolls
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { message ->
                    val isYou = message.sender == "you"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isYou) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isYou) NeonCyan else CyberGrey,
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 14.dp,
                                bottomStart = if (isYou) 14.dp else 2.dp,
                                bottomEnd = if (isYou) 2.dp else 14.dp
                            ),
                            modifier = Modifier.widthIn(max = 260.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (message.isVoice) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.PlayArrow, "Voice play", tint = if (isYou) CyberDark else PureWhite)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("[Audio Rec] Waveform", fontSize = 13.sp, color = if (isYou) CyberDark else PureWhite)
                                    }
                                } else {
                                    Text(
                                        text = message.text,
                                        fontSize = 13.sp,
                                        color = if (isYou) CyberDark else PureWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dialogue messaging input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCard)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.sendTextMessage("", isVoice = true) }) {
                    Icon(Icons.Rounded.Mic, "Voice Note", tint = SoftViolet)
                }

                OutlinedTextField(
                    value = messageInputText,
                    onValueChange = { messageInputText = it },
                    placeholder = { Text("Signal text...", color = MutedText, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberGrey,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_textfield"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.sendTextMessage(messageInputText)
                        messageInputText = ""
                    },
                    modifier = Modifier.background(NeonCyan, CircleShape)
                ) {
                    Icon(Icons.Rounded.Send, "Send", tint = CyberDark)
                }
            }
        }
    }
}

// --- NOTIFICATIONS SECTION ---
@Composable
fun NotificationsScreen(viewModel: MainViewModel) {
    val items by viewModel.notifications.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "ACTIVITY LOGS RADARS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonMagenta,
                    letterSpacing = 2.sp
                )
                Text(
                    "Notifications Network",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }

            TextButton(onClick = { viewModel.clearNotifications() }) {
                Text("Clear Radars", color = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Notifications log is fully cleared. Green signal.", color = MutedText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (item.type) {
                                            "LIKE" -> NeonMagenta.copy(alpha = 0.15f)
                                            "REPOST" -> NeonCyan.copy(alpha = 0.15f)
                                            "FOLLOW" -> SoftViolet.copy(alpha = 0.15f)
                                            else -> Color.Green.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        "LIKE" -> Icons.Rounded.Favorite
                                        "REPOST" -> Icons.Rounded.Repeat
                                        "FOLLOW" -> Icons.Rounded.PersonAdd
                                        else -> Icons.Rounded.BroadcastOnPersonal
                                    },
                                    contentDescription = "Activity type",
                                    tint = when (item.type) {
                                        "LIKE" -> NeonMagenta
                                        "REPOST" -> NeonCyan
                                        "FOLLOW" -> SoftViolet
                                        else -> Color.Green
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.senderDisplayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                    if (item.senderVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Filled.CheckCircle, "Verified", tint = NeonCyan, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Text(item.text, fontSize = 12.sp, color = MutedText)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- CREATOR FINANCIAL MONETIZATION SECTION ---
@Composable
fun MonetizationScreen(viewModel: MainViewModel) {
    val tipsCount by viewModel.tipsReceivedCount.collectAsState()
    val preCount by viewModel.premiumSubscriptionsCount.collectAsState()
    val tipMsg by viewModel.tipStatusMsg.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "MONETIZATION LAYER SENSOR",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = SoftViolet,
            letterSpacing = 2.sp
        )
        Text(
            "Creator Finance Ledger",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Payout state indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = CyberCard,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("LEDGER TIPS", fontSize = 11.sp, color = MutedText)
                    Text("$${tipsCount}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                    Text("Accept Tip jar on blockchain", fontSize = 10.sp, color = MutedText)
                }
            }

            Surface(
                color = CyberCard,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ACTIVE COPIES", fontSize = 11.sp, color = MutedText)
                    Text("${preCount} Tiers", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SoftViolet)
                    Text("Premium subscription channels", fontSize = 10.sp, color = MutedText)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated Blockchain payout chart
        Text("Payout Volume (Dynamic)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = CyberGrey.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(140.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val chartDoses = listOf(0.3f, 0.45f, 0.35f, 0.6f, 0.75f, 0.9f)
                chartDoses.forEachIndexed { idx, bar ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(bar)
                                .width(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(NeonCyan, SoftViolet)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("M-${idx + 1}", fontSize = 8.sp, color = MutedText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive Premium subscribe models
        Text("Support Channel Tiers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("High-Density VIP Node Sub", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Text("Supports full FLAC podcasts and 4K codecs", fontSize = 11.sp, color = MutedText)
                    }
                    Text("$4.99/mo", fontSize = 15.sp, color = SoftViolet, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.subscribePremiumTier() },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Subscribe VIP Access", color = CyberDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (tipMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(NeonCyan.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(tipMsg, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

// --- CREATOR IDENTITY DETAIL PROFILE SCREEN ---
@Composable
fun ProfileDetailScreen(
    selectedCreator: CreatorEntity?, // if null, shows user profile ("you")
    viewModel: MainViewModel
) {
    val posts by viewModel.unifiedFeed.collectAsState()
    val allCreators by viewModel.allCreators.collectAsState()

    val profileCreator = remember(selectedCreator, allCreators) {
        if (selectedCreator == null) {
            CreatorEntity(
                username = "you",
                displayName = "You (Broadcaster)",
                bio = "Testing the spectrum logs. Tuning to digital broadcast channels. Standard level verified.",
                isVerified = true,
                followersCount = 42,
                followingCount = 12,
                isFollowing = false,
                isMonetized = true
            )
        } else {
            allCreators.find { it.username == selectedCreator.username } ?: selectedCreator
        }
    }

    val creatorPosts = remember(posts, profileCreator) {
        posts.filter { it.creatorUsername == profileCreator.username }
    }

    // Tab categories inside profile (Posts, Videos, Podcasts)
    var profileSubTab by remember { mutableStateOf("Posts") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Detailed Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(NeonMagenta.copy(alpha = 0.15f), CyberDark)
                    )
                )
        )

        // Column containing details, floating over banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Large Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .offset(y = (-36).dp)
                        .border(4.dp, CyberDark, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan, SoftViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profileCreator.displayName.take(1).uppercase(),
                        color = CyberDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                }

                // Call for actions (Follow / Wallet)
                if (profileCreator.username != "you") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleFollowCreator(profileCreator.username) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (profileCreator.isFollowing) CyberGrey else NeonCyan)
                        ) {
                            Text(
                                text = if (profileCreator.isFollowing) "Disconnect" else "Receive Stream",
                                color = if (profileCreator.isFollowing) PureWhite else CyberDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.startChatWith(profileCreator.username) },
                            modifier = Modifier.background(CyberGrey, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Message, "DM", tint = PureWhite)
                        }
                    }
                } else {
                    // Quick tip jar stats
                    Row(
                        modifier = Modifier.padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Analytics, "Stats", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active Hub Tracker", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bio & Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profileCreator.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                if (profileCreator.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.CheckCircle, "Verified", tint = NeonCyan, modifier = Modifier.size(16.dp))
                }
            }

            Text("@${profileCreator.username}", fontSize = 13.sp, color = MutedText)

            Spacer(modifier = Modifier.height(10.dp))

            Text(profileCreator.bio, fontSize = 13.sp, color = PureWhite, lineHeight = 18.sp)

            Spacer(modifier = Modifier.height(14.dp))

            // Followers Metrics Tracker
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${profileCreator.followersCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tuned Receivers", fontSize = 11.sp, color = MutedText)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${profileCreator.followingCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Streams Connected", fontSize = 11.sp, color = MutedText)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subscriber tips widget if in creator profile
            if (profileCreator.username != "you" && profileCreator.isMonetized) {
                Surface(
                    color = CyberGrey.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bolt, "Lightning Tips", tint = NeonMagenta)
                            Spacer(modifier = Modifier.dashPathEffectModifier().width(6.dp))
                            Text("Fast Blockchain Tip", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 5, 20).forEach { amt ->
                                Button(
                                    onClick = { viewModel.processTipJar(amt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("$$amt", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Tabs inside Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(CyberGrey, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                val profileTabs = listOf("Posts", "Videos", "Podcasts")
                profileTabs.forEach { tabName ->
                    val active = profileSubTab == tabName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) HyperPrimaryActiveBorderBrush() else Color.Transparent)
                            .clickable { profileSubTab = tabName }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tabName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) CyberDark else PureWhite
                        )
                    }
                }
            }

            val filteredSubPosts = remember(creatorPosts, profileSubTab) {
                when (profileSubTab) {
                    "Videos" -> creatorPosts.filter { it.contentType == "VIDEO" }
                    "Podcasts" -> creatorPosts.filter { it.contentType == "PODCAST" }
                    else -> creatorPosts.filter { it.contentType == "TEXT" || it.contentType == "IMAGE" }
                }
            }

            // list scroll within scroll simulation using simple Column for high reliability
            if (filteredSubPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No direct $profileSubTab signals trace from this channel.", color = MutedText, fontSize = 11.sp)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredSubPosts.forEach { post ->
                        UnifiedStreamCard(post = post, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun HyperPrimaryActiveBorderBrush(): Color {
    return NeonCyan
}

// --- COMMENTS SHEET FOR ENGAGEMENT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsModalScreen(
    post: PostEntity,
    viewModel: MainViewModel
) {
    val comments by viewModel.activeComments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Comments Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setFocusedCommentPost(null) }) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = PureWhite)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("THREAD COMMENTS DISPATCH", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Black)
                    Text("Review Broadcast Dialogue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NeonMagenta.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${comments.size} signals", color = NeonMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Active Post context info
        Surface(
            color = CyberGrey.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Ref Broadcast by @${post.creatorUsername}:",
                    fontSize = 11.sp,
                    color = SoftViolet,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (post.contentType == "TEXT") post.text else post.title,
                    fontSize = 12.sp,
                    color = MutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Comments List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Signal loop is silent. Be the first to synchronize.", color = MutedText)
                    }
                }
            } else {
                items(comments) { comment ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "@${comment.username}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "Active Node",
                                    fontSize = 9.sp,
                                    color = MutedText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(comment.text, fontSize = 13.sp, color = PureWhite)
                        }
                    }
                }
            }
        }

        // Post comment dialogue bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Synchronize dialog signal...", color = MutedText, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberGrey,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("comment_input_textfield"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    viewModel.submitComment(commentText)
                    commentText = ""
                },
                modifier = Modifier.background(NeonCyan, CircleShape)
            ) {
                Icon(Icons.Rounded.Send, "Send", tint = CyberDark)
            }
        }
    }
}

// Inline helper extension extension references
fun Modifier.dashPathEffectModifier(): Modifier = this

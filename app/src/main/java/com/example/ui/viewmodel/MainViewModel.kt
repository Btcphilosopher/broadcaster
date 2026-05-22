package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.CreatorEntity
import com.example.data.database.PostEntity
import com.example.data.database.CommentEntity
import com.example.data.database.MessageEntity
import com.example.data.database.NotificationEntity
import com.example.data.gemini.GeminiHelper
import com.example.data.repository.BroadcastRepository
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = BroadcastRepository(
        database.creatorDao(),
        database.postDao(),
        database.commentDao(),
        database.messageDao(),
        database.notificationDao()
    )

    // Navigation State
    private val _currentScreen = MutableStateFlow<String>("SPLASH") // SPLASH, ONBOARDING, MAIN
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow<String>("HOME") // HOME, EXPLORE, CREATE, MEDIA, MESSAGES, NOTIFICATIONS, MONETIZATION, PROFILE
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Screen-specific state
    private val _selectedCreator = MutableStateFlow<CreatorEntity?>(null)
    val selectedCreator: StateFlow<CreatorEntity?> = _selectedCreator.asStateFlow()

    // Sub-feed tabs for HOME feed: "FOR_YOU" (algorithmic), "FOLLOWING", "TRENDING", "MEDIA_ONLY"
    private val _homeSubFeedTab = MutableStateFlow<String>("FOR_YOU")
    val homeSubFeedTab: StateFlow<String> = _homeSubFeedTab.asStateFlow()

    // For Category search on EXPLORE
    private val _selectedCategory = MutableStateFlow<String>("Tech") // "Tech", "Music", "Gaming", "News", "Lifestyle"
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery.asStateFlow()

    // Core Data Flows from Repository
    val unifiedFeed: StateFlow<List<PostEntity>> = repository.unifiedFeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreators: StateFlow<List<CreatorEntity>> = repository.allCreators
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct Messages chat logs
    private val _activeChatUsername = MutableStateFlow<String?>(null) // e.g. "cyan_dreamer"
    val activeChatUsername: StateFlow<String?> = _activeChatUsername.asStateFlow()

    val chatMessages: StateFlow<List<MessageEntity>> = _activeChatUsername
        .flatMapLatest { username ->
            if (username == null) flowOf(emptyList())
            else repository.getMessagesForChat(getChatIdForUser(username))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Comments list for a focused Post
    private val _focusedPostForComments = MutableStateFlow<PostEntity?>(null)
    val focusedPostForComments: StateFlow<PostEntity?> = _focusedPostForComments.asStateFlow()

    val activeComments: StateFlow<List<CommentEntity>> = _focusedPostForComments
        .flatMapLatest { post ->
            if (post == null) flowOf(emptyList())
            else repository.getCommentsForPost(post.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AUDIO/PODCAST PLAYER SIMULATION STATE
    private val _playingPodcast = MutableStateFlow<PostEntity?>(null)
    val playingPodcast: StateFlow<PostEntity?> = _playingPodcast.asStateFlow()

    private val _podcastIsPlaying = MutableStateFlow(false)
    val podcastIsPlaying: StateFlow<Boolean> = _podcastIsPlaying.asStateFlow()

    private val _podcastProgress = MutableStateFlow(0f)
    val podcastProgress: StateFlow<Float> = _podcastProgress.asStateFlow()

    private val _podcastPlaybackSpeed = MutableStateFlow(1.0f) // 1.0f, 1.5f, 2.0f
    val podcastPlaybackSpeed: StateFlow<Float> = _podcastPlaybackSpeed.asStateFlow()

    // VIDEO PLAYER SIMULATION STATE (We support inline player states)
    private val _activeVideoPostId = MutableStateFlow<Int?>(null)
    val activeVideoPostId: StateFlow<Int?> = _activeVideoPostId.asStateFlow()

    private val _videoIsMuted = MutableStateFlow(true)
    val videoIsMuted: StateFlow<Boolean> = _videoIsMuted.asStateFlow()

    private val _videoProgress = MutableStateFlow(0f)
    val videoProgress: StateFlow<Float> = _videoProgress.asStateFlow()

    // COMPOSER HOOKS (Create Screen)
    private val _newPostType = MutableStateFlow("TEXT") // "TEXT", "IMAGE", "VIDEO", "PODCAST"
    val newPostType: StateFlow<String> = _newPostType.asStateFlow()

    private val _newPostText = MutableStateFlow("")
    val newPostText: StateFlow<String> = _newPostText.asStateFlow()

    private val _newPostTitle = MutableStateFlow("")
    val newPostTitle: StateFlow<String> = _newPostTitle.asStateFlow()

    private val _newPostCategory = MutableStateFlow("Tech")
    val newPostCategory: StateFlow<String> = _newPostCategory.asStateFlow()

    private val _newPostDuration = MutableStateFlow("1:30")
    val newPostDuration: StateFlow<String> = _newPostDuration.asStateFlow()

    // Artificial attachment list strings (such as file names like "hologram_capture.png")
    private val _newPostAttachmentsStr = MutableStateFlow("")
    val newPostAttachmentsStr: StateFlow<String> = _newPostAttachmentsStr.asStateFlow()

    // AI Assist state
    private val _aiInProgress = MutableStateFlow(false)
    val aiInProgress: StateFlow<Boolean> = _aiInProgress.asStateFlow()

    private val _aiLogMessage = MutableStateFlow("")
    val aiLogMessage: StateFlow<String> = _aiLogMessage.asStateFlow()

    // Monetization States
    private val _tipStatusMsg = MutableStateFlow("")
    val tipStatusMsg: StateFlow<String> = _tipStatusMsg.asStateFlow()

    private val _tipsReceivedCount = MutableStateFlow(1240) // starting points
    val tipsReceivedCount: StateFlow<Int> = _tipsReceivedCount.asStateFlow()

    private val _premiumSubscriptionsCount = MutableStateFlow(14)
    val premiumSubscriptionsCount: StateFlow<Int> = _premiumSubscriptionsCount.asStateFlow()

    init {
        // Initialize database with seeding logic
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedMockDataIfEmpty()
        }

        // Start standard ticker for simulating audio and video updates
        viewModelScope.launch {
            while (true) {
                delay(1000)
                // Audio Player Tick
                if (_podcastIsPlaying.value && _playingPodcast.value != null) {
                    val add = 0.02f * _podcastPlaybackSpeed.value
                    val next = _podcastProgress.value + add
                    if (next >= 1.0f) {
                        _podcastProgress.value = 0f
                        _podcastIsPlaying.value = false
                    } else {
                        _podcastProgress.value = next
                    }
                }
                // Video Player Tick (if any video is focused and not paused)
                if (_activeVideoPostId.value != null) {
                    val next = _videoProgress.value + 0.05f
                    if (next >= 1.0f) {
                        _videoProgress.value = 0f
                    } else {
                        _videoProgress.value = next
                    }
                }
            }
        }
    }

    // Navigation setters
    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
        _selectedCreator.value = null // reset creator profile when changing main tabs
    }

    fun selectCreator(creator: CreatorEntity) {
        _selectedCreator.value = creator
    }

    fun selectCreatorByUsername(username: String) {
        viewModelScope.launch {
            val creator = repository.getCreatorByUsername(username)
            if (creator != null) {
                _selectedCreator.value = creator
            }
        }
    }

    fun setHomeSubFeedTab(tab: String) {
        _homeSubFeedTab.value = tab
    }

    fun setExploreCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setExploreSearchQuery(query: String) {
        _exploreSearchQuery.value = query
    }

    // Engagement actions (Delegates to repo with custom database triggers)
    fun toggleLike(postId: Int) {
        viewModelScope.launch { repository.toggleLike(postId) }
    }

    fun toggleRepost(postId: Int) {
        viewModelScope.launch { repository.toggleRepost(postId) }
    }

    fun toggleBookmark(postId: Int) {
        viewModelScope.launch { repository.toggleBookmark(postId) }
    }

    fun toggleFollowCreator(username: String) {
        viewModelScope.launch {
            repository.toggleFollowCreator(username)
            // Sync active selected details if opened
            val currentSelected = _selectedCreator.value
            if (currentSelected?.username == username) {
                val updated = repository.getCreatorByUsername(username)
                if (updated != null) {
                    _selectedCreator.value = updated
                }
            }
        }
    }

    // Direct Messages interactions
    fun startChatWith(username: String) {
        _activeChatUsername.value = username
        _currentTab.value = "MESSAGES"
    }

    fun closeChat() {
        _activeChatUsername.value = null
    }

    fun sendTextMessage(text: String, isVoice: Boolean = false, url: String? = null) {
        val opponent = _activeChatUsername.value ?: return
        if (text.trim().isEmpty() && url == null) return

        viewModelScope.launch {
            val msg = MessageEntity(
                chatId = getChatIdForUser(opponent),
                sender = "you",
                receiver = opponent,
                text = text,
                isVoice = isVoice,
                mediaUrl = url
            )
            repository.sendDirectMessage(msg)

            // Trigger AI-driven responses from opposing creator back to user
            delay(1500)
            val creator = repository.getCreatorByUsername(opponent)
            val creatorName = creator?.displayName ?: opponent

            val responseText = when {
                isVoice -> "🎧 [Listening to voice clip] Awesome signal. Love your custom acoustic style! Let me sketch some modulation diagrams."
                url != null -> "📷 Spot on layout. The color density is highly synced. Reminds me of standard cellular hologram nodes!"
                text.contains("hologram", ignoreCase = true) -> "Yes! Holograms completely unlock standard spatial visual densities."
                text.contains("collab", ignoreCase = true) -> "Absolutely, let's co-host a digital streaming module next Wednesday. What timestamps fit your band?"
                else -> "Affirmative, received signal. Sending positive feed metrics back to your profile nodes! Let's continue testing cellular stream updates."
            }

            repository.sendDirectMessage(
                MessageEntity(
                    chatId = getChatIdForUser(opponent),
                    sender = opponent,
                    receiver = "you",
                    text = responseText
                )
            )

            // Notify user of incoming DM
            repository.postNotification(
                NotificationEntity(
                    type = "UPLOAD", // DM / alert style
                    senderUsername = opponent,
                    senderDisplayName = creatorName,
                    senderVerified = creator?.isVerified ?: false,
                    text = "dispatched a responsive text signal to your thread"
                )
            )
        }
    }

    // Commment interactions
    fun setFocusedCommentPost(post: PostEntity?) {
        _focusedPostForComments.value = post
    }

    fun submitComment(text: String) {
        val post = _focusedPostForComments.value ?: return
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            val comment = CommentEntity(
                postId = post.id,
                username = "you",
                text = text
            )
            repository.addComment(comment)

            // Increment post comments volume
            val updated = post.copy(commentsCount = post.commentsCount + 1)
            repository.savePost(updated)
            _focusedPostForComments.value = updated

            // Trigger auto reply on posts for interactive simulation
            delay(1200)
            val responseText = "Awesome node input. Continuing broadcast analysis."
            repository.addComment(
                CommentEntity(
                    postId = post.id,
                    username = post.creatorUsername,
                    text = responseText
                )
            )
            // Re-fetch post
            val updatedAgain = repository.getPostById(post.id)
            if (updatedAgain != null) {
                _focusedPostForComments.value = updatedAgain
            }
        }
    }

    // Audio/Podcast Player utilities
    fun playPodcastTrack(post: PostEntity) {
        _playingPodcast.value = post
        _podcastIsPlaying.value = true
        _podcastProgress.value = 0f
    }

    fun togglePodcastPlayback() {
        _podcastIsPlaying.value = !_podcastIsPlaying.value
    }

    fun stopPodcast() {
        _playingPodcast.value = null
        _podcastIsPlaying.value = false
    }

    fun changePodcastSpeed() {
        _podcastPlaybackSpeed.value = when (_podcastPlaybackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
    }

    // Video interactive control
    fun togglePlayVideo(postId: Int) {
        if (_activeVideoPostId.value == postId) {
            _activeVideoPostId.value = null // pause
        } else {
            _activeVideoPostId.value = postId
            _videoProgress.value = 0f
        }
    }

    fun toggleVideoAudioMute() {
        _videoIsMuted.value = !_videoIsMuted.value
    }

    // COMPOSER HUB SETTERS & ACTIONS
    fun setNewPostType(type: String) {
        _newPostType.value = type
    }
    fun setNewPostText(text: String) {
        _newPostText.value = text
    }
    fun setNewPostTitle(title: String) {
        _newPostTitle.value = title
    }
    fun setNewPostCategory(category: String) {
        _newPostCategory.value = category
    }
    fun setNewPostAttachments(attachments: String) {
        _newPostAttachmentsStr.value = attachments
    }

    // AI INTEGRATION VIA GEMINI REST API
    fun runAiAssist(taskType: String) { // "caption", "hashtags", "title", "transcribe"
        val inputText = if (taskType == "title") _newPostTitle.value else _newPostText.value
        if (inputText.trim().isEmpty()) {
            _aiLogMessage.value = "❌ Error: Outline input text is empty! Write some keywords first."
            return
        }

        viewModelScope.launch {
            _aiInProgress.value = true
            _aiLogMessage.value = "🧠 Calling Google Gemini AI node (gemini-3.5-flash) to optimize broadcast parameters..."
            try {
                val optimizedText = GeminiHelper.generateBroadcastCreative(
                    taskType = taskType,
                    inputText = inputText,
                    mediaType = _newPostType.value.lowercase()
                )

                when (taskType) {
                    "caption" -> {
                        _newPostText.value = optimizedText
                        _aiLogMessage.value = "✅ Caption optimized by Gemini of cellular network streams."
                    }
                    "hashtags" -> {
                        val space = if (_newPostText.value.isNotEmpty()) "\n\n" else ""
                        _newPostText.value += "$space$optimizedText"
                        _aiLogMessage.value = "✅ Trending hashtags generated and attached."
                    }
                    "title" -> {
                        _newPostTitle.value = optimizedText
                        _aiLogMessage.value = "✅ Title optimized for maximum broadcast engagement."
                    }
                    "transcribe" -> {
                        _newPostText.value = optimizedText
                        _aiLogMessage.value = "✅ Audio speech outline successfully transcribed."
                    }
                }
            } catch (e: Exception) {
                _aiLogMessage.value = "❌ AI node failed: ${e.localizedMessage}"
            } finally {
                _aiInProgress.value = false
            }
        }
    }

    fun submitNewPost() {
        if (_newPostText.value.trim().isEmpty() && _newPostType.value == "TEXT") return

        viewModelScope.launch {
            val newPost = PostEntity(
                creatorUsername = "you",
                contentType = _newPostType.value,
                text = _newPostText.value,
                title = _newPostTitle.value,
                mediaUrls = _newPostAttachmentsStr.value,
                duration = if (_newPostType.value == "VIDEO" || _newPostType.value == "PODCAST") _newPostDuration.value else "",
                category = _newPostCategory.value,
                likesCount = 0,
                repostsCount = 0,
                commentsCount = 0,
                viewsCount = 0,
                timestamp = System.currentTimeMillis()
            )

            repository.createPost(newPost)

            // Post a notifications signal
            repository.postNotification(
                NotificationEntity(
                    type = "UPLOAD",
                    senderUsername = "you",
                    senderDisplayName = "You (User)",
                    senderVerified = true,
                    text = "published a new ${_newPostType.value.lowercase()} broadcast!"
                )
            )

            // Clear composer inputs
            _newPostText.value = ""
            _newPostTitle.value = ""
            _newPostAttachmentsStr.value = ""
            _aiLogMessage.value = ""

            // Return to HOME feed
            _currentTab.value = "HOME"
        }
    }

    // Monetization triggers
    fun processTipJar(amount: Int) {
        _tipsReceivedCount.value += amount
        _tipStatusMsg.value = "⚡ Sent standard micro-tip of $$amount to creator. Transmitting on blockchain..."
        viewModelScope.launch {
            delay(2000)
            _tipStatusMsg.value = ""
        }
    }

    fun subscribePremiumTier() {
        _premiumSubscriptionsCount.value += 1
        _tipStatusMsg.value = "💎 Subscribed to Premium Stream tier! Unlocked high-density codecs."
        viewModelScope.launch {
            delay(2500)
            _tipStatusMsg.value = ""
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }

    private fun getChatIdForUser(username: String): String {
        return if ("you" < username) "you:$username" else "$username:you"
    }
}

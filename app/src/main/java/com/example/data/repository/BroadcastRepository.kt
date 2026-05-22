package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class BroadcastRepository(
    private val creatorDao: CreatorDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val messageDao: MessageDao,
    private val notificationDao: NotificationDao
) {
    val unifiedFeed: Flow<List<PostEntity>> = postDao.getUnifiedFeed()
    val bookmarkedFeed: Flow<List<PostEntity>> = postDao.getBookmarkedFeed()
    val notifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val allCreators: Flow<List<CreatorEntity>> = creatorDao.getAllCreators()

    fun getPostsByCreator(username: String): Flow<List<PostEntity>> = postDao.getPostsByCreator(username)
    fun getPostsByType(type: String): Flow<List<PostEntity>> = postDao.getPostsByType(type)
    fun getCommentsForPost(postId: Int): Flow<List<CommentEntity>> = commentDao.getCommentsForPost(postId)
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)
    fun getActiveChats(): Flow<List<String>> = messageDao.getActiveChats()

    suspend fun getPostById(postId: Int): PostEntity? = postDao.getPostById(postId)
    suspend fun getCreatorByUsername(username: String): CreatorEntity? = creatorDao.getCreatorByUsername(username)

    suspend fun createPost(post: PostEntity): Long = postDao.insertPost(post)
    suspend fun savePost(post: PostEntity) = postDao.updatePost(post)
    suspend fun deletePost(id: Int) = postDao.deletePost(id)

    suspend fun addComment(comment: CommentEntity) = commentDao.insertComment(comment)
    suspend fun sendDirectMessage(message: MessageEntity) = messageDao.insertMessage(message)
    suspend fun postNotification(notification: NotificationEntity) = notificationDao.insertNotification(notification)
    suspend fun clearNotifications() = notificationDao.clearAllNotifications()

    suspend fun createCreator(creator: CreatorEntity) = creatorDao.insertCreator(creator)
    suspend fun updateCreator(creator: CreatorEntity) = creatorDao.updateCreator(creator)

    suspend fun toggleLike(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        val isLiked = !post.isLiked
        val likesCount = post.likesCount + if (isLiked) 1 else -1
        postDao.updatePost(post.copy(isLiked = isLiked, likesCount = maxOf(0, likesCount)))
        
        if (isLiked) {
            val creator = getCreatorByUsername(post.creatorUsername)
            postNotification(
                NotificationEntity(
                    type = "LIKE",
                    senderUsername = "you",
                    senderDisplayName = "You (User)",
                    senderVerified = true,
                    text = "liked your post: \"${post.text.take(30)}...\"",
                    postId = post.id
                )
            )
        }
    }

    suspend fun toggleRepost(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        val isReposted = !post.isReposted
        val repostsCount = post.repostsCount + if (isReposted) 1 else -1
        postDao.updatePost(post.copy(isReposted = isReposted, repostsCount = maxOf(0, repostsCount)))

        if (isReposted) {
            postNotification(
                NotificationEntity(
                    type = "REPOST",
                    senderUsername = "you",
                    senderDisplayName = "You (User)",
                    text = "reposted a post by @${post.creatorUsername}",
                    postId = post.id
                )
            )
        }
    }

    suspend fun toggleBookmark(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        postDao.updatePost(post.copy(isBookmarked = !post.isBookmarked))
    }

    suspend fun toggleFollowCreator(username: String) {
        val creator = creatorDao.getCreatorByUsername(username) ?: return
        val isFollowing = !creator.isFollowing
        val followersCount = creator.followersCount + if (isFollowing) 1 else -1
        creatorDao.updateCreator(creator.copy(
            isFollowing = isFollowing,
            followersCount = maxOf(0, followersCount)
        ))

        if (isFollowing) {
            postNotification(
                NotificationEntity(
                    type = "FOLLOW",
                    senderUsername = creator.username,
                    senderDisplayName = creator.displayName,
                    senderVerified = creator.isVerified,
                    text = "started following you back"
                )
            )
        }
    }

    suspend fun seedMockDataIfEmpty() {
        val creators = creatorDao.getAllCreators().first()
        if (creators.isNotEmpty()) return

        // 1. Seed Creators (Verification and subscriber stats)
        val sampleCreators = listOf(
            CreatorEntity(
                username = "cyan_dreamer",
                displayName = "Nova Vibe",
                bio = "Futurist | Synthesized soundscapes & neon optics. Coding the culture stream.",
                isVerified = true,
                followersCount = 14200,
                followingCount = 380,
                isFollowing = false,
                isMonetized = true,
                avatarUrl = "nova"
            ),
            CreatorEntity(
                username = "pixel_architect",
                displayName = "ZeroOne",
                bio = "Generative visual nodes & system architecture design. Built with light.",
                isVerified = true,
                followersCount = 89000,
                followingCount = 120,
                isFollowing = true,
                isMonetized = true,
                avatarUrl = "zero"
            ),
            CreatorEntity(
                username = "ambient_node",
                displayName = "Solara",
                bio = "Acoustic loops and podcasting on digital architecture and cosmic philosophy.",
                isVerified = false,
                followersCount = 4200,
                followingCount = 1800,
                isFollowing = false,
                isMonetized = false,
                avatarUrl = "solara"
            ),
            CreatorEntity(
                username = "byte_news",
                displayName = "ByteNews Network",
                bio = "Real-time updates from the neural boundaries of tech, gaming, and design.",
                isVerified = true,
                followersCount = 230000,
                followingCount = 10,
                isFollowing = false,
                isMonetized = true,
                avatarUrl = "news"
            )
        )
        creatorDao.insertCreators(sampleCreators)

        // 2. Seed Posts (multi-format)
        val samplePosts = listOf(
            PostEntity(
                creatorUsername = "cyan_dreamer",
                contentType = "TEXT",
                text = "Autonomous algorithms are drafting custom atmospheric vapor playlists. The border between human curator and neural networks evaporates entirely tonight. #FutureVibes #DigitalAtmosphere",
                mediaUrls = "",
                category = "Music",
                likesCount = 340,
                repostsCount = 89,
                commentsCount = 18,
                viewsCount = 1500,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15 // 15 mins ago
            ),
            PostEntity(
                creatorUsername = "pixel_architect",
                contentType = "IMAGE",
                text = "Rethinking spatial layouts in expanded displays. Here's a radial grid built with pure polar coordinates and dynamic canvas matrices. Swipe to see the wireframe overlay! #DesignSystems #BrutalistUI",
                mediaUrls = "radial_grid,wireframe_grid", // multi-image (carousel) placeholders
                category = "Tech",
                likesCount = 1200,
                repostsCount = 450,
                commentsCount = 56,
                viewsCount = 14500,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2 // 2 hours ago
            ),
            PostEntity(
                creatorUsername = "byte_news",
                contentType = "VIDEO",
                title = "Neuromorphic Processing: The Next Frontier",
                text = "Breaking down custom silicon mimicking human neural density. Why cloud-centric AI might become completely obsolete by late 2026. Watch for a full system layout runthrough.",
                mediaUrls = "video_neuromorphic",
                duration = "1:42",
                category = "Tech",
                likesCount = 4800,
                repostsCount = 1900,
                commentsCount = 203,
                viewsCount = 82000,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5 // 5 hours ago
            ),
            PostEntity(
                creatorUsername = "ambient_node",
                contentType = "PODCAST",
                title = "Episode 42: Synthesizers & Space Dust",
                text = "Today we discuss the acoustics of telemetry, pulsar broadcasts, and utilizing cosmic background hums to drive structural modulations in hardware synthesis. Subscribe for weekly episodes!",
                mediaUrls = "audio_synth_dust",
                duration = "24:15",
                category = "Music",
                likesCount = 95,
                repostsCount = 18,
                commentsCount = 7,
                viewsCount = 820,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 12 // 12 hours ago
            ),
            PostEntity(
                creatorUsername = "cyan_dreamer",
                contentType = "VIDEO",
                title = "Holographic Beats Live Live",
                text = "Short clip from my live projection system in Tokyo. Live audio reactive laser streams mapping synesthetic frequency peaks.",
                mediaUrls = "video_holo_beats",
                duration = "0:30",
                category = "Entertainment", // standard lifestyle or gaming
                likesCount = 840,
                repostsCount = 230,
                commentsCount = 45,
                viewsCount = 12000,
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 // 1 day ago
            )
        )
        postDao.insertPosts(samplePosts)

        // Seed some sample comments on first post
        val firstPostId = 1
        commentDao.insertComment(CommentEntity(postId = firstPostId, username = "pixel_architect", text = "Totally agree. Listening to a synth playlist generated by a neural node right now, it is scary good."))
        commentDao.insertComment(CommentEntity(postId = firstPostId, username = "ambient_node", text = "This is the thesis behind my next episode! The aesthetics of hybrid biological/computational sound."))

        // Seed some starter notifications
        notificationDao.insertNotification(
            NotificationEntity(
                type = "LIKE",
                senderUsername = "cyan_dreamer",
                senderDisplayName = "Nova Vibe",
                senderVerified = true,
                text = "liked your recent text broadcast",
                postId = null
            )
        )
        notificationDao.insertNotification(
            NotificationEntity(
                type = "FOLLOW",
                senderUsername = "pixel_architect",
                senderDisplayName = "ZeroOne",
                senderVerified = true,
                text = "started following you",
                postId = null
            )
        )

        // Seed some direct messages
        messageDao.insertMessage(
            MessageEntity(
                chatId = "cyan_dreamer:you",
                sender = "cyan_dreamer",
                receiver = "you",
                text = "Hey! Love your channel signal. Are you open to collaborating on a generative podcast episode next week?"
            )
        )
        messageDao.insertMessage(
            MessageEntity(
                chatId = "cyan_dreamer:you",
                sender = "you",
                receiver = "cyan_dreamer",
                text = "Wow, Nova! Absolutely. I have been compiling some telemetry data that would make amazing modulation vectors."
            )
        )
    }
}

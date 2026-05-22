package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creators")
data class CreatorEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val bio: String,
    val isVerified: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isMonetized: Boolean = false,
    val avatarUrl: String = ""
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val creatorUsername: String,
    val contentType: String, // "TEXT", "IMAGE", "VIDEO", "PODCAST"
    val text: String,
    val mediaUrls: String, // comma-separated strings (e.g. "url1,url2") or empty
    val duration: String = "", // for video/audio
    val title: String = "", // for long video, podcast title
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val repostsCount: Int = 0,
    val commentsCount: Int = 0,
    val viewsCount: Int = 0,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val category: String = "Lifestyle" // "Tech", "Music", "Gaming", "News", "Lifestyle"
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val username: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: String, // format: "user1:user2" (alphabetically ordered)
    val sender: String,
    val receiver: String,
    val text: String,
    val mediaUrl: String? = null,
    val isVoice: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "LIKE", "COMMENT", "REPOST", "FOLLOW", "UPLOAD"
    val senderUsername: String,
    val senderDisplayName: String,
    val senderVerified: Boolean = false,
    val text: String,
    var postId: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

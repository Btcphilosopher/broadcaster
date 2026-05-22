package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Logo bounce & pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    LaunchedEffect(Unit) {
        delay(2300) // Animated logo reveal duration
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing broadcast emblem
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cast,
                    contentDescription = "Broadcaster Logo",
                    tint = NeonCyan,
                    modifier = Modifier
                        .size(64.dp * scale)
                        .testTag("splash_logo_icon")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BROADCASTER",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta))
                ),
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonMagenta)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NETWORK SIGNAL ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "“Post anything. Broadcast everything.”",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SoftViolet,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    val cards = listOf(
        OnboardingItem(
            title = "Unified Microblogs",
            description = "Share high-velocity text signals and post threads directly with your followers. Stay instant, fast, and expressive.",
            icon = Icons.Rounded.Chat,
            color = NeonCyan
        ),
        OnboardingItem(
            title = "Clips & Short Video",
            description = "Scroll through beautiful, auto-playing video streams. Upload high-density clips with simulated subtitle rendering.",
            icon = Icons.Rounded.PlayCircle,
            color = NeonMagenta
        ),
        OnboardingItem(
            title = "Spotify-Lite Audio player",
            description = "Publish podcasts, episodes, or music releases on-the-fly. Complete with speeds control and active equalizers.",
            icon = Icons.Rounded.GraphicEq,
            color = SoftViolet
        ),
        OnboardingItem(
            title = "Creator Monetization",
            description = "Set subscriber tiers, earn ad-revenue cuts, and accept micro-tips on the blockchain directly from your channel dashboard.",
            icon = Icons.Rounded.MonetizationOn,
            color = NeonCyan
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Text(
                text = "UNIFIED SPECTRUM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Format Sovereignty",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Carousel card display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .border(1.dp, cards[step].color.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                color = CyberCard,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(cards[step].color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cards[step].icon,
                            contentDescription = cards[step].title,
                            tint = cards[step].color,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = cards[step].title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = cards[step].description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Step Indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(height = 6.dp, width = if (i == step) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == step) NeonCyan else CyberGrey)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Actions Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("onboarding_back_button"),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(CyberGrey, CyberGrey))
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                    ) {
                        Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (step < cards.size - 1) {
                            step++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp)
                        .testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = cards[step].color)
                ) {
                    Text(
                        text = if (step == cards.size - 1) "Connect Channel" else "Analyze Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberDark
                    )
                }
            }
        }
    }
}

data class OnboardingItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

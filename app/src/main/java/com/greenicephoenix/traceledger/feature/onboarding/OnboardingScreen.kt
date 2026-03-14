package com.greenicephoenix.traceledger.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingPage — data for each slide
// ─────────────────────────────────────────────────────────────────────────────
private data class OnboardingPage(
    val icon: ImageVector,
    val iconTint: Color,
    val headline: String,
    val body: String
)

private val pages = listOf(
    OnboardingPage(
        icon      = Icons.Default.AccountBalance,
        iconTint  = NothingRed,
        headline  = "WELCOME TO\nTRACELEDGER",
        body      = "A fast, private finance tracker.\nNo cloud. No ads. Just your money."
    ),
    OnboardingPage(
        icon      = Icons.AutoMirrored.Filled.TrendingUp,
        iconTint  = Color(0xFF4CAF50),
        headline  = "TRACK EVERY\nRUPEE",
        body      = "Log expenses, income and transfers across multiple accounts in seconds."
    ),
    OnboardingPage(
        icon      = Icons.Default.BarChart,
        iconTint  = NothingRed,
        headline  = "UNDERSTAND\nYOUR SPENDING",
        body      = "Budgets, insights and charts that show exactly where your money goes each month."
    ),
    OnboardingPage(
        icon      = Icons.Default.Lock,
        iconTint  = Color(0xFFB3B3B3),
        headline  = "PRIVATE\nBY DESIGN",
        body      = "All data is stored only on your device. TraceLedger never connects to the internet."
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState  = rememberPagerState(pageCount = { pages.size })
    val scope       = rememberCoroutineScope()
    val isLastPage  = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── PAGER ─────────────────────────────────────────────────────────────
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            PageContent(page = pages[pageIndex])
        }

        // ── BOTTOM CONTROLS ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            // Dot indicators
            PagerDots(
                pageCount    = pages.size,
                currentPage  = pagerState.currentPage
            )

            // Action buttons
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Skip — hidden on last page
                if (!isLastPage) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text  = "Skip",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    Spacer(Modifier.width(64.dp))
                }

                // Next / Get Started
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    shape  = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text  = if (isLastPage) "Get Started" else "Next",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PageContent — single onboarding slide
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.Start
    ) {

        // Icon in a subtle tinted circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = page.iconTint.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = page.icon,
                contentDescription = null,
                tint               = page.iconTint,
                modifier           = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(40.dp))

        // Headline in dot-matrix font — Nothing brand feel
        Text(
            text      = page.headline,
            style     = MaterialTheme.typography.headlineLarge,
            color     = Color.White,
            lineHeight = 42.sp
        )

        Spacer(Modifier.height(20.dp))

        // Body text
        Text(
            text  = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
            lineHeight = 24.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PagerDots — animated dot indicators
// The active dot is wider (pill shape), inactive dots are small circles.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PagerDots(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage

            val width by animateDpAsState(
                targetValue   = if (isActive) 24.dp else 6.dp,
                animationSpec = tween(durationMillis = 250),
                label         = "dot_width_$index"
            )

            val color by animateColorAsState(
                targetValue   = if (isActive) NothingRed else Color.White.copy(alpha = 0.25f),
                animationSpec = tween(durationMillis = 250),
                label         = "dot_color_$index"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}
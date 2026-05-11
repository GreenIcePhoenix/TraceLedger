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
import com.greenicephoenix.traceledger.core.ui.theme.SovereignViolet
import androidx.compose.material.icons.filled.FlashOn
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

    // Page 1 — Brand / identity
    OnboardingPage(
        icon     = Icons.Default.AccountBalance,
        iconTint = SovereignViolet,
        headline = "WELCOME TO\nTRACELEDGER",
        body     = "A fast, private finance tracker for Android.\nNo cloud. No ads. No accounts. Ever."
    ),

    // Page 2 — Accounts & transactions
    OnboardingPage(
        icon     = Icons.AutoMirrored.Filled.TrendingUp,
        iconTint = SovereignViolet,
        headline = "TRACK EVERY\nRUPEE",
        body     = "Bank, wallet, cash, and credit card accounts - all in one place. Log expenses, income, and transfers in seconds. Full history with search and filters."
    ),

    // Page 3 — Budgets, statistics, recurring, templates, widget
    OnboardingPage(
        icon     = Icons.Default.BarChart,
        iconTint = SovereignViolet,
        headline = "BUDGETS &\nINSIGHTS",
        body     = "Monthly budgets with early warnings before you overspend. Charts and trends updated in real time. Recurring rules, reusable templates, and a home screen widget."
    ),

    // Page 4 — SMS detection & statement import
    OnboardingPage(
        icon     = Icons.Default.FlashOn,
        iconTint = SovereignViolet,
        headline = "CAPTURE WITHOUT\nTYPING",
        body     = "Detect bank SMS and turn them into transactions automatically. Import full statements from PDF or CSV. Review everything before it is saved - nothing happens without your approval."
    ),

    // Page 5 — Privacy promise (always last)
    OnboardingPage(
        icon     = Icons.Default.Lock,
        iconTint = SovereignViolet,
        headline = "PRIVATE\nBY DESIGN",
        body     = "All data lives only on your device. Internet required only to check updates. No analytics, no tracking, no data ever leaves your phone."
    ),
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                targetValue   = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
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
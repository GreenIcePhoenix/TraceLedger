package com.greenicephoenix.traceledger.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.greenicephoenix.traceledger.MainActivity

// ── Widget color palette (Nothing OS aesthetic — always dark) ─────────────────
// FIXED: ColorProvider in Glance 1.1.x only accepts a single Color argument.
// There is no day=/night= overload. Since our widget is intentionally always dark,
// we pass one fixed color per token.
private val WidgetBackground = ColorProvider(Color(0xFF1C1C1E))
private val TextPrimary      = ColorProvider(Color(0xFFFFFFFF))
private val TextSecondary    = ColorProvider(Color(0xFF9E9E9E))
private val TextRed          = ColorProvider(Color(0xFFE53935))  // NothingRed
private val TextGreen        = ColorProvider(Color(0xFF4CAF50))  // SuccessGreen
private val DividerColor     = ColorProvider(Color(0xFF2C2C2E))
private val PillBackground   = ColorProvider(Color(0xFF2C2C2E))

/**
 * The main Glance widget class.
 * provideGlance() is called by Android every time the widget needs to refresh.
 * It runs in a coroutine, so we can safely call suspend functions inside it.
 */
class TraceLedgerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load fresh data from the database.
        // FIXED: catch (_: Exception) — underscore discards the unused variable warning.
        val data = try {
            WidgetDataProvider.load(context)
        } catch (_: Exception) {
            WidgetDataProvider.fallback()
        }

        provideContent {
            WidgetContent(data = data, context = context)
        }
    }
}

/**
 * The Glance composable that describes the widget's visual layout.
 *
 * IMPORTANT — Glance composables use DIFFERENT types than Material3:
 *   - Use androidx.glance.layout.Alignment (not androidx.compose.ui.Alignment)
 *   - Use androidx.glance.text.TextStyle   (not androidx.compose.ui.text.TextStyle)
 *   - Use GlanceModifier                  (not Modifier)
 *   - Alignment values: Alignment.CenterVertically, Alignment.Start, Alignment.Center
 *     (NOT Alignment.Vertical.CenterVertically — that's regular Compose)
 */
@Composable
private fun WidgetContent(data: WidgetData, context: Context) {

    // Tapping the widget body → open the app on Dashboard
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    // Tapping the "+" circle → open the app and navigate to Add Transaction
    val addTransactionIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        // FIXED: Using WidgetConstants instead of MainActivity.EXTRA_NAVIGATE_TO_ADD
        // Both here and in MainActivity read the same constant from WidgetConstants.
        putExtra(WidgetConstants.EXTRA_NAVIGATE_TO_ADD, true)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .cornerRadius(16.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionStartActivity(openAppIntent)),
        contentAlignment = Alignment.TopStart  // Glance Box alignment
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Header: "TRACE" pill + month label + "+" button ─────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically  // FIXED: not Alignment.Vertical.CenterVertically
            ) {
                // App brand pill
                Box(
                    modifier = GlanceModifier
                        .background(PillBackground)
                        .cornerRadius(4.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TRACE",
                        style = TextStyle(
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Month label — fills remaining space
                Text(
                    text = data.monthLabel,
                    style = TextStyle(color = TextSecondary, fontSize = 10.sp),
                    modifier = GlanceModifier.defaultWeight()
                )

                // "+" circle button
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(PillBackground)
                        .cornerRadius(14.dp)
                        .clickable(actionStartActivity(addTransactionIntent)),
                    contentAlignment = Alignment.Center  // Glance Alignment for Box
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // ── Total Balance ────────────────────────────────────────────────
            Text(
                text = data.totalBalance,
                style = TextStyle(
                    color = if (data.isBalancePositive) TextPrimary else TextRed,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace  // Closest to DotMatrix in Glance
                )
            )
            Text(
                text = "Total Balance",
                style = TextStyle(color = TextSecondary, fontSize = 10.sp)
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Thin divider line
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor)
            ) {}

            Spacer(modifier = GlanceModifier.height(8.dp))

            // ── Monthly summary: Income | Expense | Net ──────────────────────
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                MetricColumn(
                    label     = "\u2191 Income",   // ↑ using unicode for arrow
                    value     = data.monthlyIncome,
                    color     = TextGreen,
                    modifier  = GlanceModifier.defaultWeight()
                )
                MetricColumn(
                    label     = "\u2193 Expense",  // ↓
                    value     = data.monthlyExpense,
                    color     = TextRed,
                    modifier  = GlanceModifier.defaultWeight()
                )
                MetricColumn(
                    label     = "= Net",
                    value     = data.monthlyNet,
                    color     = if (data.isNetPositive) TextGreen else TextRed,
                    modifier  = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

/** Reusable column for Income / Expense / Net metrics at the bottom. */
@Composable
private fun MetricColumn(
    label    : String,
    value    : String,
    color    : ColorProvider,
    modifier : GlanceModifier = GlanceModifier
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.Start  // FIXED: not Alignment.Horizontal.Start
    ) {
        Text(text = label, style = TextStyle(color = TextSecondary, fontSize = 9.sp))
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text     = value,
            style    = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}
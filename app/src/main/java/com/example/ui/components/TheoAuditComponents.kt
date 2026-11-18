package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.DailyVerdictReport
import com.example.ui.FinancialHealthScore
import com.example.ui.QuickMetricsSummary
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldWalletBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChanakyaLiveShieldStatusBar(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = CardWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chanakya_live_shield_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHANAKYA 24/7 ACTIVE SHIELD",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = EmeraldContainer,
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "RUNNING NON-STOP",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ChanakyaDailyVerdictCard(
    verdict: DailyVerdictReport,
    onSpeakVerdict: (String) -> Unit,
    onDiscussWithChanakya: () -> Unit
) {
    val calloutBg = if (verdict.isWarning) AlertRedContainer.copy(alpha = 0.55f) else EmeraldSoft
    val calloutBorder = if (verdict.isWarning) AlertRed.copy(alpha = 0.35f) else EmeraldContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = RoyalBlue.copy(alpha = 0.08f)
            )
            .testTag("chanakya_daily_verdict_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_chanakya_avatar),
                        contentDescription = "Chanakya AI",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, RoyalBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Chanakya's Daily Verdict",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = RoyalBlueSoft,
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, RoyalBlueContainer)
                ) {
                    Text(
                        text = "HINGLISH AUDIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                color = calloutBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, calloutBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = verdict.headline,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (verdict.isWarning) AlertRed else EmeraldGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                                append("• Aisa Kiya: ")
                            }
                            withStyle(SpanStyle(color = TextPrimary)) {
                                append(verdict.aisaKiyaLine)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (verdict.isWarning) AlertRed else EmeraldGreen)) {
                                append("• Aur Yeh Zaroori Kaam Nahi Kiya: ")
                            }
                            withStyle(SpanStyle(color = TextPrimary)) {
                                append(verdict.zarooriKaamLine)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSpeakVerdict(verdict.speechText) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("suno_chanakya_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, RoyalBlue.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to Chanakya Verdict",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Suno Chanakya",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onDiscussWithChanakya,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("discuss_with_chanakya_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = "Discuss with Chanakya AI",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Discuss with Chanakya",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun QuickMetrics3ColumnRow(
    metrics: QuickMetricsSummary,
    privacyBlur: Boolean,
    onFaltuKharcheClick: () -> Unit,
    onActiveTrialsClick: () -> Unit,
    onActiveDebtsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Faltu Kharche
        MetricTileCard(
            title = "Faltu Kharche",
            value = CurrencyFormatter.formatInr(metrics.faltuKharcheAmount, privacyBlur),
            subtitle = "Money Leaks",
            accentColor = AlertRed,
            badgeBg = AlertRedContainer,
            testTagName = "metric_faltu_kharche",
            onClick = onFaltuKharcheClick,
            modifier = Modifier.weight(1f)
        )

        // Card 2: Active Trials
        MetricTileCard(
            title = "Active Trials",
            value = "${metrics.activeTrialsCount} Trap${if (metrics.activeTrialsCount == 1) "" else "s"}",
            subtitle = "Expiring Soon",
            accentColor = GoldAmber,
            badgeBg = GoldWalletBg,
            testTagName = "metric_active_trials",
            onClick = onActiveTrialsClick,
            modifier = Modifier.weight(1f)
        )

        // Card 3: Active Debts
        MetricTileCard(
            title = "Active Debts",
            value = "${metrics.activeDebtsCount} Debit${if (metrics.activeDebtsCount == 1) "" else "s"}",
            subtitle = "Loans & Cards",
            accentColor = RoyalBlue,
            badgeBg = RoyalBlueSoft,
            testTagName = "metric_active_debts",
            onClick = onActiveDebtsClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTileCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    badgeBg: Color,
    testTagName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = TextPrimary.copy(alpha = 0.06f)
            )
            .testTag(testTagName),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun FinancialHealthGaugeCard(
    health: FinancialHealthScore,
    onSyncClick: () -> Unit,
    onUploadCibilPdfClick: () -> Unit,
    onViewCibilAuditClick: () -> Unit
) {
    val hasScore = health.score > 0
    val statusColor = when {
        !hasScore -> TextMuted
        health.score >= 760 -> EmeraldGreen
        health.score >= 680 -> RoyalBlue
        health.score >= 580 -> GoldAmber
        else -> AlertRed
    }
    val statusBg = when {
        !hasScore -> SurfaceMuted
        health.score >= 760 -> EmeraldContainer
        health.score >= 680 -> RoyalBlueContainer
        health.score >= 580 -> GoldWalletBg
        else -> AlertRedContainer
    }
    val progress = if (hasScore) ((health.score - 300f) / 600f).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = RoyalBlue.copy(alpha = 0.07f)
            )
            .testTag("financial_health_gauge_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Credit & AutoPay Health Gauge",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Live estimate from parsed SMS & Gmail (300–900)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = health.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // Semi-Circular Color-Graded Gauge Meter (Red -> Yellow/Amber -> Green)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    val strokePx = 18.dp.toPx()
                    val diameter = minOf(size.width - strokePx, (size.height - strokePx) * 2f)
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = size.height - diameter / 2f - strokePx / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    // Subtle base track
                    drawArc(
                        color = SurfaceMuted,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    if (hasScore) {
                        // Red -> Yellow -> Green full spectrum arc
                        val spectrumBrush = Brush.horizontalGradient(
                            colors = listOf(AlertRed, GoldBright, EmeraldGreen),
                            startX = topLeft.x,
                            endX = topLeft.x + diameter
                        )
                        drawArc(
                            brush = spectrumBrush,
                            startAngle = 180f,
                            sweepAngle = 180f * progress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        // Faint continuation of spectrum for reference
                        drawArc(
                            brush = spectrumBrush,
                            startAngle = 180f + (180f * progress),
                            sweepAngle = (180f * (1f - progress)).coerceAtLeast(0f),
                            useCenter = false,
                            alpha = 0.18f,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        // Needle / Score Dot on the semi-circle
                        val angleRad = PI * (1.0 - progress.toDouble())
                        val radius = diameter / 2f
                        val centerX = topLeft.x + radius
                        val centerY = topLeft.y + radius
                        val dotX = centerX + (radius * cos(angleRad)).toFloat()
                        val dotY = centerY - (radius * sin(angleRad)).toFloat()

                        drawCircle(
                            color = CardWhite,
                            radius = 12.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        drawCircle(
                            color = statusColor,
                            radius = 7.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${health.score}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = " / ${health.maxScore}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Text(
                        text = if (hasScore) "300 Min • Credit & AutoPay Score • 900 Max" else "Upload CIBIL PDF, Sync SMS or Add Entries",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = BorderLight)

            // 3 Mini Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HealthMiniStat(
                    label = "Credit Utilization",
                    value = "${health.utilizationPct}%",
                    valueColor = when {
                        !hasScore -> TextMuted
                        health.utilizationPct >= 50 -> AlertRed
                        health.utilizationPct >= 30 -> GoldAmber
                        else -> EmeraldGreen
                    },
                    modifier = Modifier.weight(1f)
                )
                HealthMiniStat(
                    label = "On-Time EMIs",
                    value = "${health.onTimeEmisPct}%",
                    valueColor = when {
                        !hasScore -> TextMuted
                        health.onTimeEmisPct < 80 -> AlertRed
                        health.onTimeEmisPct < 95 -> GoldAmber
                        else -> EmeraldGreen
                    },
                    modifier = Modifier.weight(1f)
                )
                HealthMiniStat(
                    label = "Active Mandates",
                    value = "${health.activeMandatesCount}",
                    valueColor = RoyalBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            // Live Score Drivers from SMS / Gmail Ledger
            if (health.positiveReasons.isNotEmpty() || health.negativeReasons.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    health.positiveReasons.forEach { reason ->
                        ScoreFactorBadge(
                            text = reason,
                            isPositive = true
                        )
                    }
                    health.negativeReasons.forEach { reason ->
                        ScoreFactorBadge(
                            text = reason,
                            isPositive = false
                        )
                    }
                }
            }

            // Last Official CIBIL PDF Audit Pill (if user previously uploaded a report)
            if (!health.cibilPdfFileName.isNullOrBlank()) {
                Surface(
                    onClick = onViewCibilAuditClick,
                    color = RoyalBlueSoft,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlueContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cibil_pdf_audit_badge")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = health.cibilPdfScore?.let {
                                    "Official CIBIL PDF Score: $it / 900"
                                } ?: "Official CIBIL PDF Audited",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${health.cibilPdfFileName} • Tap to review AI report",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "View →",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Primary CTA: Upload Official CIBIL PDF + Secondary Bureau/SMS/Gmail Sync
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onUploadCibilPdfClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("upload_cibil_pdf_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = "Upload Official CIBIL PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Official CIBIL PDF",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chanakya AI spots errors & boosts score",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = onSyncClick,
                        modifier = Modifier.testTag("health_gauge_sync_link")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync SMS and Gmail",
                            tint = RoyalBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sync SMS & Gmail",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreFactorBadge(
    text: String,
    isPositive: Boolean
) {
    val bg = if (isPositive) EmeraldSoft else AlertRedContainer.copy(alpha = 0.5f)
    val fg = if (isPositive) EmeraldGreen else AlertRed
    val icon = if (isPositive) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HealthMiniStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

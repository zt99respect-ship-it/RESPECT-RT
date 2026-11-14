package com.respect.community.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respect.community.R
import com.respect.community.ui.components.GlassFilterChip
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.LivePulseBadge
import com.respect.community.ui.components.RespectImage
import com.respect.community.ui.components.ShimmerCardSkeleton
import com.respect.community.ui.components.formatCompactNumber
import com.respect.community.ui.theme.CyanAccent
import com.respect.community.ui.theme.GoldAccent
import com.respect.community.ui.theme.KickEmerald
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.viewmodel.StatsPeriod
import com.respect.community.ui.viewmodel.StreamerChartStat
import java.util.Locale

@Composable
fun StatsScreen(
    rankings: List<StreamerChartStat>,
    selectedPeriod: StatsPeriod,
    isBusy: Boolean,
    onSelectPeriod: (StatsPeriod) -> Unit,
    onRefreshStats: () -> Unit,
    onWatchStreamer: (String) -> Unit,
    onBack: () -> Unit
) {
    val topPeak = rankings.maxByOrNull { it.peakViewers }
    val topHours = rankings.maxByOrNull { it.streamedHours }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HazeGlassCard(cornerRadius = 22.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Leaderboard, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(20.dp))
                                Text(
                                    text = stringResource(R.string.charts_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = stringResource(R.string.charts_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onRefreshStats, enabled = !isBusy) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = RespectPurpleBright,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "تحديث الإحصائيات", tint = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsPeriod.entries.forEach { period ->
                        GlassFilterChip(
                            label = period.label,
                            selected = selectedPeriod == period,
                            onClick = { onSelectPeriod(period) }
                        )
                    }
                }
            }
        }

        if (rankings.isEmpty() && isBusy) {
            items(4) { ShimmerCardSkeleton() }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HighlightStatCard(
                        title = "أعلى ذروة مشاهدين",
                        streamerName = topPeak?.displayName ?: "-",
                        metricValue = topPeak?.let { formatCompactNumber(it.peakViewers) } ?: "0",
                        icon = Icons.Rounded.TrendingUp,
                        accent = RespectPurpleBright,
                        modifier = Modifier.weight(1f)
                    )
                    HighlightStatCard(
                        title = "الأكثر ساعات بث",
                        streamerName = topHours?.displayName ?: "-",
                        metricValue = topHours?.let { String.format(Locale.US, "%.1f س", it.streamedHours) } ?: "0.0 س",
                        icon = Icons.Rounded.Schedule,
                        accent = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.BarChart, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(18.dp))
                    Text(
                        text = "ترتيب صناع المحتوى (${selectedPeriod.label})",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            itemsIndexed(rankings, key = { _, item -> item.slug }) { index, stat ->
                val rankColor = when (index) {
                    0 -> GoldAccent
                    1 -> Color(0xFFE5E7EB)
                    2 -> Color(0xFFCD7F32)
                    else -> TextSecondary
                }

                HazeGlassCard(
                    isGlowing = index == 0 || stat.isLiveNow,
                    onClick = { onWatchStreamer(stat.slug) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (index < 3) rankColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, if (index < 3) rankColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "#${index + 1}",
                                color = rankColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        RespectImage(
                            url = stat.avatarUrl,
                            contentDescription = stat.displayName,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (stat.isLiveNow) KickEmerald else Color.White.copy(alpha = 0.15f),
                                    CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stat.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (stat.isLiveNow) LivePulseBadge()
                            }
                            Text(
                                text = "@${stat.slug}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricPill(
                            label = "الذروة",
                            value = formatCompactNumber(stat.peakViewers),
                            icon = Icons.Rounded.Visibility,
                            accent = RespectPurpleBright,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricPill(
                            label = "المتوسط",
                            value = formatCompactNumber(stat.avgViewers),
                            icon = Icons.Rounded.TrendingUp,
                            accent = KickEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricPill(
                            label = "الساعات",
                            value = String.format(Locale.US, "%.1f", stat.streamedHours),
                            icon = Icons.Rounded.Schedule,
                            accent = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricPill(
                            label = "المتابعون",
                            value = formatCompactNumber(stat.followerCount),
                            icon = Icons.Rounded.Groups,
                            accent = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightStatCard(
    title: String,
    streamerName: String,
    metricValue: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    HazeGlassCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(title, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(metricValue, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(streamerName, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatMetricPill(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

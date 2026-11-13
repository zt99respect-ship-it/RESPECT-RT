package com.respect.community.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respect.community.ui.components.GlassFilterChip
import com.respect.community.ui.components.HazeGlassCard
import com.respect.community.ui.components.LivePulseBadge
import com.respect.community.ui.components.RespectImage
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.TextMuted
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.viewmodel.CommunitySearchResults

@Composable
fun SearchScreen(
    query: String,
    results: CommunitySearchResults,
    onQueryChange: (String) -> Unit,
    onWatchStreamer: (String) -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToMedia: () -> Unit
) {
    var category by remember { mutableIntStateOf(0) } // 0: All, 1: Streamers, 2: Posts, 3: Clips, 4: Art

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("البحث الشامل", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("ابحث عن ستريمر، منشور، كليب، أو رسمة...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = RespectPurpleBright) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RespectPurpleBright,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassFilterChip("الكل", category == 0, { category = 0 })
            GlassFilterChip("ستريمرز", category == 1, { category = 1 })
            GlassFilterChip("التايملاين", category == 2, { category = 2 })
            GlassFilterChip("كليبات", category == 3, { category = 3 })
            GlassFilterChip("فن", category == 4, { category = 4 })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank()) {
            HazeGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ابدأ الكتابة للبحث في مجتمع RESPECT", style = MaterialTheme.typography.titleMedium)
                    Text("بحث فوري في القنوات والمنشورات والكليبات ومعرض الفن", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (category == 0 || category == 1) {
                    items(results.streamers, key = { "s_${it.slug}" }) { streamer ->
                        HazeGlassCard(isGlowing = streamer.isLive, onClick = { onWatchStreamer(streamer.slug) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RespectImage(
                                    url = streamer.avatarUrl,
                                    contentDescription = streamer.displayName,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(streamer.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text("@${streamer.slug} • ${streamer.characterNames}", color = TextSecondary, fontSize = 12.sp)
                                }
                                if (streamer.isLive) LivePulseBadge() else Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = TextMuted)
                            }
                        }
                    }
                }

                if (category == 0 || category == 2) {
                    items(results.posts, key = { "p_${it.id}" }) { post ->
                        HazeGlassCard(onClick = onNavigateToSocial) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Forum, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(18.dp))
                                Text(post.authorName, color = RespectPurpleBright, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(post.content, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                        }
                    }
                }

                if (category == 0 || category == 3) {
                    items(results.clips, key = { "c_${it.id}" }) { clip ->
                        HazeGlassCard(onClick = onNavigateToMedia) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Movie, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(clip.title, style = MaterialTheme.typography.titleMedium)
                                    Text("كليب • ${clip.streamerName}", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (category == 0 || category == 4) {
                    items(results.arts, key = { "a_${it.id}" }) { art ->
                        HazeGlassCard(onClick = onNavigateToMedia) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Brush, contentDescription = null, tint = RespectPurpleBright, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(art.title, style = MaterialTheme.typography.titleMedium)
                                    Text("فن • ${art.artistName}", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

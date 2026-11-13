package com.respect.community.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.respect.community.R
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.GlassPurpleBorder
import com.respect.community.ui.theme.LiveCrimson
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectPurpleDeep
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.theme.VoidBlack
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import java.util.Locale

val LocalRespectHazeState = compositionLocalOf<HazeState?> { null }

@Composable
fun respectGlassStyle(isGlowing: Boolean = false): HazeStyle {
    val tintColor = if (isGlowing) {
        RespectPurpleDeep.copy(alpha = 0.48f)
    } else {
        RespectSurface.copy(alpha = 0.68f)
    }
    return HazeStyle(
        backgroundColor = RespectSurface,
        tint = HazeTint(color = tintColor),
        blurRadius = 22.dp,
        noiseFactor = 0.08f
    )
}

@Composable
fun RespectHazeContainer(
    hazeState: HazeState,
    parallaxY: Float = 0f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "bg_ambient")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // Blurred source backdrop captured by Haze
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            Image(
                painter = painterResource(id = R.drawable.respect_bg_shapes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = parallaxY }
                    .alpha(0.24f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RespectPurple.copy(alpha = glowAlpha),
                                RespectPurpleDeep.copy(alpha = 0.16f),
                                VoidBlack.copy(alpha = 0.92f)
                            ),
                            radius = 1200f
                        )
                    )
            )
        }

        content()
    }
}

@Composable
fun HazeGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    isGlowing: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = LocalRespectHazeState.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isGlowing) GlassPurpleBorder else GlassBorder
    val style = respectGlassStyle(isGlowing)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )

    var baseModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)

    baseModifier = if (hazeState != null) {
        baseModifier.hazeChild(state = hazeState, style = style)
    } else {
        baseModifier.background(RespectSurface.copy(alpha = 0.85f))
    }

    baseModifier = baseModifier
        .border(BorderStroke(1.dp, borderColor), shape)
        .let {
            if (onClick != null) {
                it.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            } else {
                it
            }
        }

    Column(
        modifier = baseModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun ScaleBounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = RespectPurple,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = TextPrimary
        ),
        modifier = modifier.scale(scale),
        content = content
    )
}

@Composable
fun GlassFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = if (selected) RespectPurple.copy(alpha = 0.30f) else RespectSurface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, if (selected) RespectPurpleBright else GlassBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextPrimary else TextSecondary,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun LivePulseBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = LiveCrimson.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, LiveCrimson.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(LiveCrimson)
            )
            Text(
                text = "مباشر",
                color = Color(0xFFFCA5A5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RespectImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val model: Any = when (url) {
        "res://respect_banner" -> R.drawable.respect_banner
        "res://respect_logo" -> R.drawable.respect_logo
        "res://respect_shapes" -> R.drawable.respect_bg_shapes
        "" -> R.drawable.respect_logo
        else -> url
    }
    AsyncImage(
        model = model,
        placeholder = painterResource(id = R.drawable.respect_logo),
        error = painterResource(id = R.drawable.respect_logo),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

fun formatCompactNumber(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}

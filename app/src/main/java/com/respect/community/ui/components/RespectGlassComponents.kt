package com.respect.community.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.respect.community.R
import com.respect.community.ui.theme.DeepObsidian
import com.respect.community.ui.theme.GlassBorder
import com.respect.community.ui.theme.GlassHighlightBorder
import com.respect.community.ui.theme.GlassPurpleBorder
import com.respect.community.ui.theme.LiveCrimson
import com.respect.community.ui.theme.RespectPurple
import com.respect.community.ui.theme.RespectPurpleBright
import com.respect.community.ui.theme.RespectSurface
import com.respect.community.ui.theme.RespectSurfaceElevated
import com.respect.community.ui.theme.RespectSurfaceGlass
import com.respect.community.ui.theme.TextPrimary
import com.respect.community.ui.theme.TextSecondary
import com.respect.community.ui.theme.VoidBlack
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import java.util.Locale

val LocalRespectHazeState = compositionLocalOf<HazeState?> { null }

@Composable
fun respectGlassStyle(isGlowing: Boolean = false): HazeStyle {
    val tintColor = if (isGlowing) {
        RespectSurfaceElevated.copy(alpha = 0.72f)
    } else {
        RespectSurfaceGlass.copy(alpha = 0.68f)
    }
    return HazeStyle(
        backgroundColor = RespectSurface,
        tint = HazeTint(color = tintColor),
        blurRadius = 24.dp,
        noiseFactor = 0.05f
    )
}

@Composable
fun RespectHazeContainer(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    parallaxY: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "bg_ambient")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // Frosted dark obsidian backdrop (clean, no purple overkill)
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
                    .alpha(0.08f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DeepObsidian,
                                VoidBlack,
                                DeepObsidian.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RespectPurple.copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            radius = 950f
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
    cornerRadius: Dp = 20.dp,
    isGlowing: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = LocalRespectHazeState.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isGlowing) GlassPurpleBorder else GlassHighlightBorder.copy(alpha = 0.14f)
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
        baseModifier.background(RespectSurfaceGlass)
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
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
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
    val shape = RoundedCornerShape(13.dp)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = if (selected) RespectPurple.copy(alpha = 0.24f) else RespectSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (selected) RespectPurpleBright.copy(alpha = 0.75f) else GlassBorder)
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
fun TelegraphImagePickerButton(
    onUploadImage: suspend (Context, Uri) -> Result<String>,
    onUploadedUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "رفع صورة عبر Telegraph"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                onUploadImage(context, uri).onSuccess { url ->
                    onUploadedUrl(url)
                }
                isUploading = false
            }
        }
    }

    OutlinedButton(
        onClick = {
            if (!isUploading) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        },
        enabled = !isUploading,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, GlassHighlightBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        if (isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = RespectPurpleBright
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("جاري الرفع إلى Telegraph...", color = TextSecondary, fontSize = 12.sp)
        } else {
            Icon(
                Icons.Rounded.CloudUpload,
                contentDescription = null,
                tint = RespectPurpleBright,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = TextPrimary, fontSize = 12.sp)
        }
    }
}

@Composable
fun LivePulseBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = LiveCrimson.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, LiveCrimson.copy(alpha = 0.65f))
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

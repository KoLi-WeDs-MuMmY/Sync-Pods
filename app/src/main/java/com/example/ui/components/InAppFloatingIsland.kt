package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FloatingIslandEvent
import com.example.model.IslandTone
import com.example.ui.theme.AmberLatency
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.IslandCapsuleBorder
import com.example.ui.theme.IslandCapsuleSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SignalEmerald

@Composable
fun InAppFloatingIslandHost(
  event: FloatingIslandEvent?,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onDismiss: () -> Unit,
  onOpenDualSync: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(top = 6.dp, start = 16.dp, end = 16.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    AnimatedVisibility(
      visible = event != null,
      enter = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      ) + scaleIn(initialScale = 0.8f) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { -it }) + scaleOut(targetScale = 0.85f) + fadeOut(),
    ) {
      if (event != null) {
        FloatingIslandPillCard(
          event = event,
          isExpanded = isExpanded,
          onToggleExpand = onToggleExpand,
          onDismiss = onDismiss,
          onOpenAction = onOpenDualSync,
        )
      }
    }
  }
}

@Composable
fun FloatingIslandPillCard(
  event: FloatingIslandEvent,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onDismiss: () -> Unit,
  onOpenAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val accentColor = when (event.tone) {
    IslandTone.PRIMARY -> CyanElectric
    IslandTone.EMERALD -> SignalEmerald
    IslandTone.AMBER -> AmberLatency
    IslandTone.ALERT -> CoralAlert
  }

  val icon = when (event) {
    is FloatingIslandEvent.DeviceConnected -> Icons.Rounded.BluetoothAudio
    is FloatingIslandEvent.DeviceDisconnected -> Icons.Rounded.BluetoothDisabled
    is FloatingIslandEvent.DualSyncUpdated -> Icons.Rounded.Sync
    is FloatingIslandEvent.BatteryStatus -> Icons.Rounded.BatteryChargingFull
    is FloatingIslandEvent.RadarProximityAlert -> Icons.Rounded.Radar
    is FloatingIslandEvent.EqProfileApplied -> Icons.Rounded.GraphicEq
  }

  Surface(
    modifier = modifier
      .testTag("floating_island_pill")
      .widthIn(min = 190.dp, max = 380.dp)
      .shadow(
        elevation = if (isExpanded) 20.dp else 10.dp,
        shape = RoundedCornerShape(if (isExpanded) 28.dp else 50.dp),
        ambientColor = accentColor.copy(alpha = 0.45f),
        spotColor = accentColor.copy(alpha = 0.45f),
      )
      .animateContentSize(
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        )
      )
      .pointerInput(event.id) {
        detectVerticalDragGestures { _, dragAmount ->
          if (dragAmount < -18f) {
            onDismiss()
          } else if (dragAmount > 18f && !isExpanded) {
            onToggleExpand()
          }
        }
      }
      .clickable { onToggleExpand() },
    shape = RoundedCornerShape(if (isExpanded) 28.dp else 50.dp),
    color = IslandCapsuleSurface,
    border = BorderStroke(
      width = 1.dp,
      color = if (isExpanded) accentColor.copy(alpha = 0.5f) else IslandCapsuleBorder,
    ),
  ) {
    AnimatedContent(
      targetState = isExpanded,
      transitionSpec = {
        fadeIn(tween(180)) togetherWith fadeOut(tween(150))
      },
      label = "island_expand_transition",
    ) { expanded ->
      if (!expanded) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = event.title,
              tint = accentColor,
              modifier = Modifier.size(16.dp),
            )
          }

          Text(
            text = event.title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )

          IslandEqualizerPulse(color = accentColor)

          Surface(
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.16f),
          ) {
            Text(
              text = event.badgeText,
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
              ),
              color = accentColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = icon,
                  contentDescription = event.title,
                  tint = accentColor,
                  modifier = Modifier.size(22.dp),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = event.title,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = event.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF94A3B8),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .testTag("floating_island_dismiss")
                .size(40.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss notification",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              IslandEqualizerPulse(color = accentColor, barCount = 5)
              Text(
                text = "Swipe up or tap to collapse",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
              )
            }

            if (onOpenAction != null) {
              TextButton(
                onClick = {
                  onOpenAction()
                  onDismiss()
                },
                modifier = Modifier.testTag("floating_island_action_btn"),
              ) {
                Text(
                  text = "Open Studio",
                  color = accentColor,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun IslandEqualizerPulse(
  color: Color,
  barCount: Int = 3,
) {
  val transition = rememberInfiniteTransition(label = "island_eq_bars")
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    modifier = Modifier.height(16.dp),
  ) {
    repeat(barCount) { idx ->
      val ratio by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
          animation = tween(
            durationMillis = 420 + (idx * 130),
            easing = FastOutSlowInEasing,
          ),
          repeatMode = RepeatMode.Reverse,
        ),
        label = "bar_$idx",
      )
      Box(
        modifier = Modifier
          .width(3.dp)
          .height((14f * ratio).dp)
          .clip(RoundedCornerShape(50))
          .background(color),
      )
    }
    Spacer(modifier = Modifier.width(1.dp))
  }
}

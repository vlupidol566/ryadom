package roman.alex

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ContactSupport
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onLeadingClick: () -> Unit,
    onFollowerClick: () -> Unit,
    onContactsClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val contentColor = getAppContentColor()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Рядом",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                fontSize = 18.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRegisterClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                    ) {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            contentDescription = "Профиль",
                            modifier = Modifier.size(20.dp),
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onContactsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ContactSupport,
                            contentDescription = "Поддержка",
                            modifier = Modifier.size(20.dp),
                            tint = contentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contentColor
                )
            )
        }
    ) { padding ->
        AppBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Главная иконка в стиле "Стекло"
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer { rotationZ = -5f },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = if (isDark) 0.15f else 0.5f),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Diversity3,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = contentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Кем вы хотите быть сейчас?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    LiquidGlassRoleCard(
                        title = "Стать ведущим",
                        description = "Показывайте маршрут и помогайте другим двигаться безопасно",
                        icon = Icons.Rounded.Navigation,
                        accentColor = Color(0xFF2196F3),
                        isDark = isDark,
                        onClick = onLeadingClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LiquidGlassRoleCard(
                        title = "Стать подопечным",
                        description = "Получайте подсказки и поддержку по пути",
                        icon = Icons.Rounded.PersonPinCircle,
                        accentColor = Color(0xFF4CAF50),
                        isDark = isDark,
                        onClick = onFollowerClick
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(onClick = onRegisterClick) {
                        Text(
                            text = "Открыть профиль",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = contentColor.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun LiquidGlassRoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_card_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = if (isDark) 0.1f else 0.4f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
            )
        )
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isDark) Color.White else accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val contentColor = getAppContentColor()
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = contentColor.copy(alpha = 0.6f),
                            lineHeight = 14.sp,
                            fontSize = 12.sp
                        )
                    )
                }

                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = getAppContentColor().copy(alpha = 0.3f)
                )
            }
        }
    }
}

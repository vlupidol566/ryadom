package roman.alex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ContactSupport
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    // Цвета iOS HIG
    val bgColor = MaterialTheme.colorScheme.background
    val sheetColor = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onBackground
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.6f else 0.55f)
    val separatorColor = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)

    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 300.dp,
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetContainerColor = sheetColor,
        sheetShadowElevation = 0.dp,
        sheetTonalElevation = 0.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(separatorColor.copy(alpha = 0.5f))
            )
        },
        sheetContent = {
            // ─── Sheet: выбор роли ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Кем вы сейчас?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = labelColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                )

                // Контейнер ролей — единый "grouped" блок как в iOS Settings
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF3A3A3C) else Color(0xFFFFFFFF),
                    tonalElevation = 0.dp,
                    shadowElevation = if (isDark) 0.dp else 1.dp
                ) {
                    Column {
                        RoleRow(
                            title = "Ведущий",
                            subtitle = "Вижу карту и камеру, направляю подопечного",
                            icon = Icons.Rounded.Navigation,
                            iconBg = Color(0xFF007AFF),
                            onClick = onLeadingClick
                        )

                        // Separator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(start = 60.dp)
                                .background(separatorColor.copy(alpha = 0.4f))
                        )

                        RoleRow(
                            title = "Подопечный",
                            subtitle = "Слушаю голосовые подсказки, двигаюсь",
                            icon = Icons.Rounded.PersonPinCircle,
                            iconBg = Color(0xFF34C759),
                            onClick = onFollowerClick
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Кнопка профиля внизу листа
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRegisterClick),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF3A3A3C) else Color(0xFFFFFFFF),
                    tonalElevation = 0.dp,
                    shadowElevation = if (isDark) 0.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFF8E8E93)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.ManageAccounts,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Мой профиль",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = labelColor
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = secondaryLabel
                        )
                    }
                }
            }
        },
        containerColor = bgColor
    ) { _ ->
        // ─── Фон: брендинг приложения ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            // TopBar с кнопками
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavIconButton(
                    icon = Icons.AutoMirrored.Rounded.ContactSupport,
                    label = "Контакты",
                    isDark = isDark,
                    onClick = onContactsClick
                )
                Spacer(Modifier.width(8.dp))
                NavIconButton(
                    icon = Icons.Rounded.AccountCircle,
                    label = "Профиль",
                    isDark = isDark,
                    onClick = onRegisterClick
                )
            }

            // Центральный контент
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(bottom = 200.dp),  // отступ снизу, чтобы не залезать под sheet
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App icon — iOS-стиль
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF007AFF),
                    shadowElevation = if (isDark) 0.dp else 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Diversity3,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Large Title — Apple HIG 34sp
                Text(
                    text = "Рядом",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = labelColor
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Помощник для незрячих в движении",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RoleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val labelColor = MaterialTheme.colorScheme.onSurface
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветная иконка в квадрате (как в iOS Settings)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp
                ),
                color = labelColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = secondaryLabel
            )
        }

        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun NavIconButton(
    icon: ImageVector,
    label: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

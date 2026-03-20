package roman.alex.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onDone: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var name  by remember { mutableStateOf(UserProfile.getName(context)  ?: "") }
    var phone by remember { mutableStateOf(UserProfile.getPhone(context) ?: "") }
    var notes by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // iOS color tokens
    val bgColor    = MaterialTheme.colorScheme.background
    val navColor   = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.94f)
                    else Color(0xFFF2F2F7).copy(alpha = 0.94f)
    val cardColor  = if (isDark) Color(0xFF2C2C2E) else Color.White
    val labelColor = MaterialTheme.colorScheme.onBackground
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f)
    val separatorColor = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)
    val sectionHeaderColor = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.45f)
                             else Color(0x993C3C43)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = navColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onBack)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Назад",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Назад",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Профиль",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = labelColor
                        )

                        Spacer(Modifier.weight(1f))

                        // Save action в navbar — как в iOS
                        TextButton(
                            onClick = {
                                UserProfile.save(context, name, phone)
                                saved = true
                                onDone?.invoke()
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "Готово",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = separatorColor)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // ─── Аватар ───────────────────────────────────────────────
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (name.isNotBlank()) {
                            Text(
                                text = name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = if (isDark) Color(0xFF8E8E93) else Color(0xFF6D6D72)
                            )
                        }
                    }
                }

                // Кнопка редактирования аватара
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = "Изменить фото",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Кнопка смены фото под аватаром
            TextButton(onClick = {}) {
                Text(
                    "Изменить фото",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(28.dp))

            // ─── ЛИЧНЫЕ ДАННЫЕ ────────────────────────────────────────
            IosFormSectionHeader(text = "ЛИЧНЫЕ ДАННЫЕ", color = sectionHeaderColor)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardColor,
                tonalElevation = 0.dp,
                shadowElevation = if (isDark) 0.dp else 1.dp
            ) {
                Column {
                    IosFormField(
                        label = "Имя",
                        value = name,
                        placeholder = "Не указано",
                        onValueChange = { name = it; saved = false },
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        labelColor = labelColor,
                        secondaryColor = secondaryLabel
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = separatorColor.copy(alpha = 0.6f)
                    )

                    IosFormField(
                        label = "Телефон",
                        value = phone,
                        placeholder = "+7 000 000-00-00",
                        onValueChange = { phone = it; saved = false },
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                        labelColor = labelColor,
                        secondaryColor = secondaryLabel
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ─── ЗАМЕТКИ ──────────────────────────────────────────────
            IosFormSectionHeader(text = "ЗАМЕТКИ", color = sectionHeaderColor)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardColor,
                tonalElevation = 0.dp,
                shadowElevation = if (isDark) 0.dp else 1.dp
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .padding(16.dp)
                ) {
                    BasicTextField(
                        value = notes,
                        onValueChange = { notes = it; saved = false },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = labelColor
                        ),
                        maxLines = 6,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (notes.isEmpty()) {
                                    Text(
                                        "Особенности зрения, контакты для экстренной связи…",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                        color = secondaryLabel
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }

            IosFormSectionFooter(
                text = "Заметки видны только вам и помогут ведущему лучше вас сопроводить.",
                color = sectionHeaderColor
            )

            Spacer(Modifier.height(32.dp))

            // ─── Кнопка сохранения ────────────────────────────────────
            Button(
                onClick = {
                    UserProfile.save(context, name, phone)
                    saved = true
                    onDone?.invoke()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    "Сохранить",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                )
            }

            // Статус сохранения
            if (saved) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF34C759)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Профиль сохранён",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = Color(0xFF34C759)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── iOS Form компоненты ────────────────────────────────────────────────────────

@Composable
private fun IosFormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    labelColor: Color,
    secondaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Фиксированная ширина лейбла как в iOS Settings
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            color = labelColor,
            modifier = Modifier.width(100.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                color = labelColor
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = secondaryColor
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun IosFormSectionHeader(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            letterSpacing = 0.4.sp
        ),
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun IosFormSectionFooter(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp
        ),
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp)
    )
}

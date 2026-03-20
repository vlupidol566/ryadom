package roman.alex.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    userRepository: UserRepository
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var query       by remember { mutableStateOf("") }
    var users       by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorText   by remember { mutableStateOf<String?>(null) }
    var infoText    by remember { mutableStateOf<String?>(null) }

    val myPhone = remember { UserProfile.getPhone(context) }

    val bgColor        = MaterialTheme.colorScheme.background
    val navColor       = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.94f)
                         else Color(0xFFF2F2F7).copy(alpha = 0.94f)
    val cardColor      = if (isDark) Color(0xFF2C2C2E) else Color.White
    val labelColor     = MaterialTheme.colorScheme.onBackground
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f)
    val separatorColor = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)

    LaunchedEffect(Unit) {
        isLoading = true
        val result = userRepository.getUsers(null)
        isLoading = false
        result.onSuccess { users = it }.onFailure { errorText = it.message }
    }

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
                            "Найти ведущего",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = labelColor
                        )

                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.width(80.dp))
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // iOS Search field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF3A3A3C) else Color(0x1A787880)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = secondaryLabel
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = labelColor
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        "Имя или номер телефона",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                        color = secondaryLabel
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .background(secondaryLabel.copy(alpha = 0.5f), CircleShape)
                                .clickable { query = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(10.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search button
            Button(
                onClick = {
                    if (isLoading) return@Button
                    isLoading = true
                    errorText = null
                    scope.launch {
                        val result = userRepository.getUsers(query.ifBlank { null })
                        isLoading = false
                        result.onSuccess { users = it }.onFailure {
                            errorText = it.message ?: "Ошибка поиска"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        "Найти ведущих",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    )
                }
            }

            // Error
            errorText?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF3B30).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
            }

            // Info
            infoText?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF34C759).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFF34C759)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (users.isNotEmpty()) {
                Text(
                    text = "ДОСТУПНЫЕ ВЕДУЩИЕ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = secondaryLabel,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor,
                    tonalElevation = 0.dp,
                    shadowElevation = if (isDark) 0.dp else 1.dp
                ) {
                    LazyColumn {
                        itemsIndexed(users) { index, user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    tonalElevation = 0.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.name.take(1).uppercase().ifEmpty { "?" },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 17.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name.ifBlank { "Без имени" },
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                        color = labelColor
                                    )
                                    if (user.phone.isNotBlank()) {
                                        Text(
                                            text = user.phone,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                            color = secondaryLabel
                                        )
                                    }
                                }

                                // "Позвать" button
                                TextButton(
                                    onClick = {
                                        if (myPhone.isNullOrBlank()) {
                                            errorText = "Сначала укажите номер телефона в профиле"
                                            return@TextButton
                                        }
                                        if (isLoading) return@TextButton
                                        isLoading = true
                                        errorText = null
                                        infoText = null
                                        scope.launch {
                                            val note = "Подопечный $myPhone просит помощи у ${user.name}"
                                            val result = userRepository.sendHelpRequest(
                                                fromPhone = myPhone,
                                                note = note
                                            )
                                            isLoading = false
                                            result.onSuccess {
                                                infoText = "Запрос отправлен"
                                            }.onFailure {
                                                errorText = it.message ?: "Не удалось отправить"
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        "Позвать",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (index < users.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    thickness = 0.5.dp,
                                    color = separatorColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            } else if (!isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.PersonSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = secondaryLabel
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Нет доступных ведущих",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = secondaryLabel,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

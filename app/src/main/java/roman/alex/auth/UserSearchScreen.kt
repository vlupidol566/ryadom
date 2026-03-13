package roman.alex.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import roman.alex.AppBackground
import roman.alex.getAppContentColor
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    userRepository: UserRepository
) {
    val isDark = isSystemInDarkTheme()
    val contentColor = getAppContentColor()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var infoText by remember { mutableStateOf<String?>(null) }

    val myPhone = remember { UserProfile.getPhone(context) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = userRepository.getUsers(null)
        isLoading = false
        result.onSuccess { users = it }.onFailure { errorText = it.message }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Найти ведущего",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                    },
                    label = { Text("Поиск по имени или телефону") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.PersonSearch,
                            contentDescription = null,
                            tint = contentColor
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = contentColor.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Найти ведущих")
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = Color(0xFFE53935),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (infoText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = infoText ?: "",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users) { user ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // Здесь в будущем можно отправить help-request конкретному пользователю
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = if (isDark) 0.12f else 0.9f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = contentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = user.phone,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = contentColor.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                TextButton(onClick = {
                                    if (myPhone.isNullOrBlank()) {
                                        errorText = "Сначала зарегистрируйтесь, чтобы указать свой номер"
                                        return@TextButton
                                    }
                                    if (isLoading) return@TextButton
                                    isLoading = true
                                    errorText = null
                                    infoText = null
                                    scope.launch {
                                        val note = "Подопечный ${myPhone} просит помощи у ${user.name}"
                                        val result = userRepository.sendHelpRequest(
                                            fromPhone = myPhone,
                                            note = note
                                        )
                                        isLoading = false
                                        result.onSuccess {
                                            infoText = "Запрос помощи отправлен"
                                        }.onFailure {
                                            errorText = it.message ?: "Не удалось отправить запрос помощи"
                                        }
                                    }
                                }) {
                                    Text("Позвать на помощь")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


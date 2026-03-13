package roman.alex.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import roman.alex.AppBackground
import roman.alex.getAppContentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBack: () -> Unit,
    onAddContact: () -> Unit,
    repository: ContactsRepository,
    onNavigateToHome: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val contacts = remember { mutableStateListOf<Contact>() }
    val isDark = isSystemInDarkTheme()
    val contentColor = getAppContentColor()

    LaunchedEffect(Unit) {
        contacts.clear()
        contacts.addAll(repository.getAll())
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = if (isDark) 0.1f else 0.4f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.ContactPage,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = contentColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Контакты",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                fontSize = 16.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                            .then(Modifier.semantics { contentDescription = "Назад" })
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = contentColor)
                    }
                },
                actions = {
                    onNavigateToHome?.let { onHome ->
                        IconButton(
                            onClick = onHome,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                                .then(Modifier.semantics { contentDescription = "На главный экран" })
                        ) {
                            Icon(Icons.Rounded.Home, contentDescription = "На главный экран", tint = contentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = onAddContact,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.4f))
                            .then(Modifier.semantics { contentDescription = "Добавить контакт" })
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Добавить контакт", tint = contentColor, modifier = Modifier.size(20.dp))
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
                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White.copy(alpha = if (isDark) 0.1f else 0.4f),
                                border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))))
                            ) {
                                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Пока нет контактов",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                "Нажмите «+», чтобы создать первый контакт",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = contentColor.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            ContactRow(
                                contact = contact,
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${contact.phone}")
                                    }
                                    context.startActivity(intent)
                                },
                                isDark = isDark,
                                contentColor = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onCall: () -> Unit,
    isDark: Boolean,
    contentColor: Color
) {
    val callDesc = "Позвонить ${contact.name}, ${contact.phone}"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Контакт ${contact.name}, телефон ${contact.phone}. Нажмите чтобы позвонить."
            }
            .clickable(onClick = onCall),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = if (isDark) 0.1f else 0.4f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f)))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2196F3).copy(alpha = if (isDark) 0.3f else 0.2f),
                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = contentColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name.ifBlank { "Без имени" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        fontSize = 17.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.phone.isNotBlank()) {
                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Rounded.Call,
                contentDescription = callDesc,
                modifier = Modifier.size(28.dp),
                tint = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    repository: ContactsRepository
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val contentColor = getAppContentColor()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        "Новый контакт",
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
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = contentColor)
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = if (isDark) 0.15f else 0.5f),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f)))
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        androidx.compose.material3.OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Имя", color = contentColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Поле ввода имени контакта" },
                            singleLine = true,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = contentColor.copy(alpha = 0.3f),
                                focusedLabelColor = contentColor,
                                unfocusedLabelColor = contentColor.copy(alpha = 0.7f),
                                cursorColor = contentColor,
                                focusedTextColor = contentColor,
                                unfocusedTextColor = contentColor
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Телефон", color = contentColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Поле ввода номера телефона" },
                            singleLine = true,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = contentColor.copy(alpha = 0.3f),
                                focusedLabelColor = contentColor,
                                unfocusedLabelColor = contentColor.copy(alpha = 0.7f),
                                cursorColor = contentColor,
                                focusedTextColor = contentColor,
                                unfocusedTextColor = contentColor
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                if (name.isNotBlank() || phone.isNotBlank()) {
                                    repository.add(name, phone)
                                    onSaved()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .semantics { contentDescription = "Сохранить контакт" },
                            shape = RoundedCornerShape(20.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            enabled = name.isNotBlank() || phone.isNotBlank()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сохранить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

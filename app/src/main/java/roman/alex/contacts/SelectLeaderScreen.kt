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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
fun SelectLeaderScreen(
    onBack: () -> Unit,
    onSelectLeader: (Contact) -> Unit,
    repository: ContactsRepository
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
                                    Icons.Rounded.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = contentColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Выберите ведущего",
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
                                "Нет контактов",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                "Добавьте контакт в «Контакты» на главном экране",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = contentColor.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            "Нажмите на контакт для подключения к ведущему",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = contentColor.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(contacts, key = { it.id }) { contact ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = "Ведущий ${contact.name}. Нажмите чтобы подключиться."
                                        }
                                        .clickable(onClick = { onSelectLeader(contact) }),
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
                                                modifier = Modifier.size(52.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(26.dp),
                                                    tint = contentColor
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name.ifBlank { "Без имени" },
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = contentColor,
                                                    fontSize = 16.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (contact.phone.isNotBlank()) {
                                                Text(
                                                    text = contact.phone,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = contentColor.copy(alpha = 0.7f),
                                                        fontSize = 14.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${contact.phone}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.semantics { contentDescription = "Позвонить ${contact.name}" }
                                        ) {
                                            Icon(
                                                Icons.Rounded.Call,
                                                contentDescription = "Позвонить",
                                                tint = contentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

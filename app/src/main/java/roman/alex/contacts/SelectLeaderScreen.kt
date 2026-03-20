package roman.alex.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val bgColor        = MaterialTheme.colorScheme.background
    val navColor       = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.94f)
                         else Color(0xFFF2F2F7).copy(alpha = 0.94f)
    val cardColor      = if (isDark) Color(0xFF2C2C2E) else Color.White
    val labelColor     = MaterialTheme.colorScheme.onBackground
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f)
    val separatorColor = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)

    LaunchedEffect(Unit) {
        contacts.clear()
        contacts.addAll(repository.getAll())
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
                            "Выбрать ведущего",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = labelColor
                        )

                        Spacer(Modifier.weight(1f))

                        // Spacer to balance title
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
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.PersonOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = secondaryLabel
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Нет контактов",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = labelColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Добавьте контакт в разделе «Контакты»",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = secondaryLabel
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "ВЕДУЩИЕ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = secondaryLabel,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                Text(
                    text = "Нажмите на контакт для подключения",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = secondaryLabel,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor,
                    tonalElevation = 0.dp,
                    shadowElevation = if (isDark) 0.dp else 1.dp
                ) {
                    LazyColumn {
                        itemsIndexed(contacts, key = { _, c -> c.id }) { index, contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectLeader(contact) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Ведущий ${contact.name}. Нажмите чтобы подключиться."
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF007AFF).copy(alpha = 0.12f),
                                    tonalElevation = 0.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (contact.name.isNotBlank()) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 17.sp
                                                ),
                                                color = Color(0xFF007AFF)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Rounded.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp),
                                                tint = Color(0xFF007AFF)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name.ifBlank { "Без имени" },
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                        color = labelColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (contact.phone.isNotBlank()) {
                                        Text(
                                            text = contact.phone,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                            color = secondaryLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Call button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Color(0xFF34C759).copy(alpha = 0.12f)
                                        )
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${contact.phone}")
                                            }
                                            context.startActivity(intent)
                                        }
                                        .semantics { contentDescription = "Позвонить ${contact.name}" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Call,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = Color(0xFF34C759)
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = secondaryLabel
                                )
                            }

                            if (index < contacts.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    thickness = 0.5.dp,
                                    color = separatorColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

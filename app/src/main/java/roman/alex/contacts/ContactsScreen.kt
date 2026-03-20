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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Экран списка контактов ────────────────────────────────────────────────────

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

    // iOS color tokens
    val bgColor       = MaterialTheme.colorScheme.background
    val navColor      = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.94f)
                        else Color(0xFFF2F2F7).copy(alpha = 0.94f)
    val cardColor     = if (isDark) Color(0xFF2C2C2E) else Color.White
    val labelColor    = MaterialTheme.colorScheme.onBackground
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
                            "Контакты",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = labelColor
                        )

                        Spacer(Modifier.weight(1f))

                        // Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            onNavigateToHome?.let { onHome ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                                        )
                                        .clickable(onClick = onHome)
                                        .semantics { contentDescription = "На главный экран" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Home,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                                    )
                                    .clickable(onClick = onAddContact)
                                    .semantics { contentDescription = "Добавить контакт" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
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
                .padding(horizontal = 16.dp)
        ) {
            if (contacts.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            "Контактов нет",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = labelColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Нажмите «+» чтобы добавить первый контакт",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = secondaryLabel
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(20.dp))

                // Section header
                Text(
                    text = "${contacts.size} ${pluralContacts(contacts.size)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = secondaryLabel,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                // Grouped list card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor,
                    tonalElevation = 0.dp,
                    shadowElevation = if (isDark) 0.dp else 1.dp
                ) {
                    LazyColumn {
                        itemsIndexed(contacts, key = { _, c -> c.id }) { index, contact ->
                            ContactRow(
                                contact = contact,
                                isDark = isDark,
                                labelColor = labelColor,
                                secondaryColor = secondaryLabel,
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${contact.phone}")
                                    }
                                    context.startActivity(intent)
                                }
                            )
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

@Composable
private fun ContactRow(
    contact: Contact,
    isDark: Boolean,
    labelColor: Color,
    secondaryColor: Color,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCall)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Контакт ${contact.name}, ${contact.phone}. Нажмите чтобы позвонить."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
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
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
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
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Call icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    Color(0xFF34C759).copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Call,
                contentDescription = "Позвонить",
                modifier = Modifier.size(17.dp),
                tint = Color(0xFF34C759)
            )
        }
    }
}

private fun pluralContacts(n: Int): String = when {
    n % 100 in 11..19 -> "контактов"
    n % 10 == 1 -> "контакт"
    n % 10 in 2..4 -> "контакта"
    else -> "контактов"
}

// ─── Экран добавления контакта ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    repository: ContactsRepository
) {
    var name  by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()

    val bgColor        = MaterialTheme.colorScheme.background
    val navColor       = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.94f)
                         else Color(0xFFF2F2F7).copy(alpha = 0.94f)
    val cardColor      = if (isDark) Color(0xFF2C2C2E) else Color.White
    val labelColor     = MaterialTheme.colorScheme.onBackground
    val secondaryLabel = labelColor.copy(alpha = if (isDark) 0.55f else 0.5f)
    val separatorColor = if (isDark) Color(0xFF48484A) else Color(0xFFC6C6C8)
    val sectionHeaderColor = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.45f)
                             else Color(0x993C3C43)

    val canSave = name.isNotBlank() || phone.isNotBlank()

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
                        // Cancel
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                "Отмена",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Новый контакт",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = labelColor
                        )

                        Spacer(Modifier.weight(1f))

                        // Save
                        TextButton(
                            onClick = {
                                if (canSave) {
                                    repository.add(name, phone)
                                    onSaved()
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp),
                            enabled = canSave
                        ) {
                            Text(
                                "Готово",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = if (canSave) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Avatar placeholder
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

            Spacer(Modifier.height(28.dp))

            // Section header
            Text(
                text = "ДАННЫЕ КОНТАКТА",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    letterSpacing = 0.4.sp
                ),
                color = sectionHeaderColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp)
            )

            // Form card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardColor,
                tonalElevation = 0.dp,
                shadowElevation = if (isDark) 0.dp else 1.dp
            ) {
                Column {
                    // Имя
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Имя",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = labelColor,
                            modifier = Modifier.width(100.dp)
                        )
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                color = labelColor
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Box {
                                    if (name.isEmpty()) {
                                        Text(
                                            "Не указано",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                            color = secondaryLabel
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = separatorColor.copy(alpha = 0.6f)
                    )

                    // Телефон
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Телефон",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            color = labelColor,
                            modifier = Modifier.width(100.dp)
                        )
                        BasicTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                color = labelColor
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Box {
                                    if (phone.isEmpty()) {
                                        Text(
                                            "+7 000 000-00-00",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                            color = secondaryLabel
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    if (canSave) {
                        repository.add(name, phone)
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Сохранить контакт" },
                shape = RoundedCornerShape(14.dp),
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
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
        }
    }
}

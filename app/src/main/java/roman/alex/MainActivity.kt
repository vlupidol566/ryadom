package roman.alex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.yandex.mapkit.MapKitFactory
import roman.alex.auth.ProfileScreen
import roman.alex.auth.UserProfile
import roman.alex.auth.UserRepository
import roman.alex.auth.UserSearchScreen
import roman.alex.contacts.AddContactScreen
import roman.alex.contacts.ContactsRepository
import roman.alex.contacts.ContactsScreen
import roman.alex.contacts.SelectLeaderScreen
import roman.alex.ui.theme.РоманАлександровTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Яндекс.Карты (используем константу из конфига)
        MapKitFactory.setApiKey(SignalingConfig.YANDEX_MAPS_API_KEY)
        MapKitFactory.initialize(this)
        // TransportFactory инициализируется автоматически через MapKitFactory — отдельный вызов не нужен
        
        enableEdgeToEdge()
        setContent {
            РоманАлександровTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    val context = LocalContext.current
    val contactsRepository = remember { ContactsRepository(context.applicationContext) }
    val userRepository = remember {
        UserRepository(
            baseUrl = "http://176.99.158.181:3010",
            apiToken = "roman_alex_8f3a2b1c9d4e5f6a7b8c9d0e1f2a3b4c5d"
        )
    }
    var selectedLeaderName by remember { mutableStateOf<String?>(null) }

    // Проверяем, есть ли уже профиль (телефон)
    val hasProfile by remember {
        mutableStateOf(UserProfile.getPhone(context.applicationContext)?.isNotBlank() == true)
    }

    // Общее состояние маршрута между ведущим и подопечным
    var sharedDistanceToTurn by remember { mutableStateOf("500м") }
    var sharedNextInstruction by remember { mutableStateOf("Поверните налево") }

    if (!permissionsState.allPermissionsGranted) {
        PermissionsRationaleScreen(
            onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() }
        )
    } else {
        val startDestination = if (hasProfile) "role_selection" else "profile"

        NavHost(navController = navController, startDestination = startDestination) {
            composable("role_selection") {
                RoleSelectionScreen(
                    onLeadingClick = { navController.navigate("leading") },
                    onFollowerClick = { navController.navigate("user_search") },
                    onContactsClick = { navController.navigate("contacts") },
                    onRegisterClick = { navController.navigate("profile") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate("role_selection") {
                            popUpTo("profile") { inclusive = true }
                        }
                    }
                )
            }
            composable("leading") {
                LeadingScreen(
                    onBack = { navController.popBackStack() },
                    onCallFollower = { navController.navigate("contacts") },
                    onRouteUpdated = { distance, instruction ->
                        sharedDistanceToTurn = distance
                        sharedNextInstruction = instruction
                    }
                )
            }
            composable("user_search") {
                UserSearchScreen(
                    onBack = { navController.popBackStack() },
                    userRepository = userRepository
                )
            }
            composable("select_leader") {
                SelectLeaderScreen(
                    onBack = { navController.popBackStack() },
                    onSelectLeader = { contact ->
                        selectedLeaderName = contact.name.ifBlank { contact.phone }.ifBlank { null }
                        navController.navigate("follower") {
                            popUpTo("select_leader") { inclusive = true }
                        }
                    },
                    repository = contactsRepository
                )
            }
            composable("follower") {
                FollowerScreen(
                    onBack = {
                        selectedLeaderName = null
                        navController.popBackStack()
                    },
                    onCallLeading = { navController.navigate("contacts") },
                    leaderName = selectedLeaderName,
                    distanceToTurn = sharedDistanceToTurn,
                    nextInstruction = sharedNextInstruction
                )
            }
            composable("contacts") {
                ContactsScreen(
                    onBack = { navController.popBackStack() },
                    onAddContact = { navController.navigate("add_contact") },
                    repository = contactsRepository,
                    onNavigateToHome = { navController.popBackStack("role_selection", false) }
                )
            }
            composable("add_contact") {
                AddContactScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    repository = contactsRepository
                )
            }
        }
    }
}

@Composable
private fun PermissionsRationaleScreen(
    onRequestPermissions: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = "Чтобы приложение работало корректно, нужны доступ к камере, микрофону и геолокации.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.Button(onClick = onRequestPermissions) {
                androidx.compose.material3.Text("Разрешить доступ")
            }
        }
    }
}

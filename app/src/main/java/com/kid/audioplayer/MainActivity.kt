package com.kid.audioplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kid.audioplayer.catalog.CatalogScreen
import com.kid.audioplayer.catalog.CatalogViewModel
import com.kid.audioplayer.library.LibraryScreen
import com.kid.audioplayer.library.LibraryViewModel
import com.kid.audioplayer.player.MEDIA_ID
import com.kid.audioplayer.player.MiniPlayerBar
import com.kid.audioplayer.player.OPEN_MEDIA
import com.kid.audioplayer.player.PlayerScreen
import com.kid.audioplayer.player.PlayerViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow

private const val CATALOG_ROUTE = "catalog/{bookId}"
private const val PLAYER_ROUTE = "player/{bookId}/{unit}"

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitToPreviousPage(): ExitTransition =
    when (initialState.destination.route) {
        CATALOG_ROUTE -> slideOutHorizontally(targetOffsetX = { it })
        PLAYER_ROUTE -> slideOutVertically(targetOffsetY = { it })
        else -> ExitTransition.None
    }

class MainActivity : ComponentActivity() {
    private val requestedMedia = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) acceptMediaIntent(intent)
        setContent {
            val context = LocalContext.current
            val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            MaterialTheme(colorScheme = colors) {
                Surface { AudioPlayerNavigation(application as AudioPlayerApp) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptMediaIntent(intent)
    }

    private fun acceptMediaIntent(intent: Intent) {
        if (intent.action != OPEN_MEDIA) return
        val id = intent.getStringExtra(MEDIA_ID) ?: return
        val bookId = id.substringBefore('/')
        val unit = id.substringAfter('/', "").toIntOrNull() ?: return
        val canonicalId = runCatching { UUID.fromString(bookId).toString() }.getOrNull()
        if (canonicalId == bookId && unit >= 0) requestedMedia.value = "$bookId/$unit"
    }

    @Composable
    private fun AudioPlayerNavigation(app: AudioPlayerApp) {
        val navigation = rememberNavController()
        val currentEntry by navigation.currentBackStackEntryAsState()
        val visibleEntries by navigation.visibleEntries.collectAsStateWithLifecycle()
        val library: LibraryViewModel = viewModel(factory = viewModelFactory {
            initializer { LibraryViewModel(app.books, app.playback) }
        })
        val mediaId by requestedMedia.collectAsStateWithLifecycle()
        LaunchedEffect(mediaId) {
            val requested = mediaId ?: return@LaunchedEffect
            requestedMedia.value = null
            val id = requested.substringBefore('/')
            val unit = requested.substringAfter('/', "").toIntOrNull() ?: return@LaunchedEffect
            if (navigation.currentBackStackEntry?.arguments?.let {
                    it.getString("bookId") == id && it.getString("unit")?.toIntOrNull() == unit
                } == true) return@LaunchedEffect
            navigation.popBackStack("library", inclusive = false)
            navigation.navigate("catalog/$id")
            navigation.navigate("player/$id/$unit")
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentEntry?.destination?.route != PLAYER_ROUTE &&
                    visibleEntries.none { it.destination.route == PLAYER_ROUTE }
                ) {
                    MiniPlayerBar(app.playback, app.books) { navigation.navigate("player/$it") }
                }
            },
        ) { padding ->
            AudioPlayerDestinations(navigation, app, library, Modifier.padding(padding).consumeWindowInsets(padding))
        }
    }

    @Composable
    private fun AudioPlayerDestinations(
        navigation: NavHostController,
        app: AudioPlayerApp,
        library: LibraryViewModel,
        modifier: Modifier,
    ) {
        NavHost(
            navigation,
            startDestination = "library",
            modifier = modifier,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popExitTransition = { exitToPreviousPage() },
            predictivePopEnterTransition = { _ -> EnterTransition.None },
            predictivePopExitTransition = { _ -> exitToPreviousPage() },
        ) {
            composable("library") { LibraryScreen(library) { navigation.navigate("catalog/$it") } }
            composable(
                CATALOG_ROUTE,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                popEnterTransition = { EnterTransition.None },
            ) { entry ->
                val id = checkNotNull(entry.arguments?.getString("bookId"))
                val catalog: CatalogViewModel = viewModel(factory = viewModelFactory {
                    initializer { CatalogViewModel(app.books, id) }
                })
                CatalogScreen(catalog, onBack = { navigation.popBackStack() }) {
                    navigation.navigate("player/$id/$it")
                }
            }
            composable(
                PLAYER_ROUTE,
                enterTransition = { slideInVertically(initialOffsetY = { it }) },
            ) { entry ->
                val id = checkNotNull(entry.arguments?.getString("bookId"))
                val unit = checkNotNull(entry.arguments?.getString("unit")).toInt()
                val player: PlayerViewModel = viewModel(factory = viewModelFactory {
                    initializer { PlayerViewModel(app.books, app.playback, id, unit) }
                })
                PlayerScreen(player) { navigation.popBackStack() }
            }
        }
    }
}

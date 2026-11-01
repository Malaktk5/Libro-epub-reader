package app.libro.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.libro.reader.LibroApp
import app.libro.reader.ui.reader.ReaderScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val SHELF = "shelf"
    const val SETTINGS = "settings"
    const val READER = "reader/{id}"
    fun reader(id: String) = "reader/$id"
}

@Composable
fun LibroNav(app: LibroApp) {
    val nav = rememberNavController()
    val start = remember { if (app.prefs.current.onboarded) Routes.SHELF else Routes.ONBOARDING }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(app.prefs) {
                app.prefs.update { it.copy(onboarded = true) }
                nav.navigate(Routes.SHELF) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            }
        }
        composable(Routes.SHELF) {
            ShelfScreen(
                app = app,
                onOpen = { nav.navigate(Routes.reader(it)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(app.prefs, onBack = { nav.popBackStack() })
        }
        composable(Routes.READER, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ReaderScreen(app = app, bookId = id, onClose = { nav.popBackStack() })
        }
    }
}

package filey.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import filey.app.feature.archive.ArchiveScreen
import filey.app.feature.browser.BrowserScreen
import filey.app.feature.editor.EditorScreen
import filey.app.feature.player.AudioPlayerScreen
import filey.app.feature.player.VideoPlayerScreen
import filey.app.feature.viewer.ImageViewerScreen
import filey.app.feature.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "browser"
    ) {
        // ── Browser ──
        composable("browser") {
            BrowserScreen(
                onNavigateToImage = { path ->
                    navController.navigate("image_viewer/${Uri.encode(path)}")
                },
                onNavigateToVideo = { path ->
                    navController.navigate("video_player/${Uri.encode(path)}")
                },
                onNavigateToAudio = { path ->
                    navController.navigate("audio_player/${Uri.encode(path)}")
                },
                onNavigateToEditor = { path ->
                    navController.navigate("editor/${Uri.encode(path)}")
                },
                onNavigateToArchive = { path ->
                    navController.navigate("archive/${Uri.encode(path)}")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // ── Image Viewer ──
        composable(
            route = "image_viewer/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            ImageViewerScreen(
                path = path,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Video Player ──
        composable(
            route = "video_player/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            VideoPlayerScreen(
                path = path,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Audio Player ──
        composable(
            route = "audio_player/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            AudioPlayerScreen(
                path = path,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Text Editor ──
        composable(
            route = "editor/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            EditorScreen(
                path = path,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Archive ──
        composable(
            route = "archive/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            ArchiveScreen(
                path = path,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Settings ──
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

package filey.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import filey.app.feature.dashboard.DashboardScreen
import filey.app.feature.analyzer.StorageAnalyzerScreen
import filey.app.feature.trash.TrashScreen
import filey.app.feature.duplicates.DuplicateFinderScreen
import filey.app.feature.server.ServerScreen
import filey.app.feature.organizer.ui.OrganizerScreen
import filey.app.feature.settings.SettingsScreen
import filey.app.feature.dashboard.DashboardScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        // ── Dashboard ──
        composable("dashboard") {
            DashboardScreen(
                onCategoryClick = { category ->
                    navController.navigate("category/${category.name}")
                },
                onBrowseFiles = {
                    navController.navigate("browser")
                },
                onNavigateToAnalyzer = {
                    navController.navigate("analyzer")
                },
                onNavigateToDuplicates = {
                    navController.navigate("duplicates")
                },
                onNavigateToOrganizer = {
                    navController.navigate("organizer")
                },
                onNavigateToTrash = {
                    navController.navigate("trash")
                },
                onNavigateToServer = {
                    navController.navigate("server")
                }
            )
        }

        // ── Organizer ──
        composable("organizer") {
            OrganizerScreen(onNavigateBack = { navController.popBackStack() })
        }


        // ── Analyzer ──
        composable("analyzer") {
            StorageAnalyzerScreen(onBack = { navController.popBackStack() })
        }

        // ── Trash ──
        composable("trash") {
            TrashScreen(onBack = { navController.popBackStack() })
        }

        // ── Duplicate Finder ──
        composable("duplicates") {
            DuplicateFinderScreen(onBack = { navController.popBackStack() })
        }

        // ── Share Server ──
        composable("server") {
            ServerScreen(onBack = { navController.popBackStack() })
        }

        // ── Category View ──
        composable(
            route = "category/{categoryName}",
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val catName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val category = FileCategory.valueOf(catName)
            val viewModel: BrowserViewModel = viewModel(factory = BrowserViewModel.Factory)
            
            LaunchedEffect(category) {
                viewModel.loadCategory(category)
            }

            BrowserScreen(
                viewModel = viewModel,
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
                },
                onNavigateToDashboard = {
                    navController.popBackStack("dashboard", false)
                },
                onNavigateToTrash = {
                    navController.navigate("trash")
                },
                onNavigateToServer = {
                    navController.navigate("server")
                }
            )
        }

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
                },
                onNavigateToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("browser") { inclusive = true }
                    }
                },
                onNavigateToTrash = {
                    navController.navigate("trash")
                },
                onNavigateToServer = {
                    navController.navigate("server")
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

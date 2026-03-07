package filey.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import filey.app.core.model.FileCategory
import filey.app.feature.archive.ArchiveScreen
import filey.app.feature.browser.BrowserScreen
import filey.app.feature.browser.BrowserViewModel
import filey.app.feature.editor.EditorScreen
import filey.app.feature.player.AudioPlayerScreen
import filey.app.feature.player.VideoPlayerScreen
import filey.app.feature.viewer.ImageViewerScreen
import filey.app.feature.settings.SettingsScreen
import filey.app.feature.dashboard.DashboardScreen
import filey.app.feature.analyzer.StorageAnalyzerScreen
import filey.app.feature.trash.TrashScreen
import filey.app.feature.duplicates.ui.DuplicatesScreen
import filey.app.feature.server.ServerScreen
import filey.app.feature.organizer.ui.OrganizerScreen
import filey.app.feature.vault.ui.VaultScreen
import filey.app.feature.viewer.PdfViewerScreen
import filey.app.feature.search.semantic.presentation.SemanticSearchScreen
import filey.app.feature.search.semantic.presentation.SearchViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                onCategoryClick = { category -> navController.navigate("category/${category.name}") },
                onBrowseFiles = { navController.navigate("browser") },
                onNavigateToAnalyzer = { navController.navigate("analyzer") },
                onNavigateToDuplicates = { navController.navigate("duplicates") },
                onNavigateToOrganizer = { navController.navigate("organizer") },
                onNavigateToTrash = { navController.navigate("trash") },
                onNavigateToServer = { navController.navigate("server") },
                onNavigateToSearch = { navController.navigate("semantic_search") }
            )
        }

        composable("semantic_search") {
            val context = LocalContext.current
            val viewModel: SearchViewModel = viewModel(
                factory = SearchViewModel.createFactory(context)
            )
            SemanticSearchScreen(
                viewModel = viewModel,
                onResultClick = { path ->
                    navController.navigate("browser")
                }
            )
        }

        composable("duplicates") { DuplicatesScreen(onBack = { navController.popBackStack() }) }
        composable("vault") { VaultScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = "pdf_viewer/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            PdfViewerScreen(path = path, onBack = { navController.popBackStack() })
        }

        composable("organizer") { OrganizerScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("analyzer") { StorageAnalyzerScreen(onBack = { navController.popBackStack() }) }
        composable("trash") { TrashScreen(onBack = { navController.popBackStack() }) }
        composable("server") { ServerScreen(onBack = { navController.popBackStack() }) }

        composable(
            route = "category/{categoryName}",
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val catName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val category = try { FileCategory.valueOf(catName) } catch (_: Exception) { FileCategory.DOCUMENTS }
            val viewModel: BrowserViewModel = viewModel(factory = BrowserViewModel.Factory)
            
            LaunchedEffect(category) { viewModel.loadCategory(category) }

            BrowserScreen(
                viewModel = viewModel,
                onNavigateToImage = { path -> navController.navigate("image_viewer/${Uri.encode(path)}") },
                onNavigateToVideo = { path -> navController.navigate("video_player/${Uri.encode(path)}") },
                onNavigateToAudio = { path -> navController.navigate("audio_player/${Uri.encode(path)}") },
                onNavigateToEditor = { path -> navController.navigate("editor/${Uri.encode(path)}") },
                onNavigateToArchive = { path -> navController.navigate("archive/${Uri.encode(path)}") },
                onNavigateToPdf = { path -> navController.navigate("pdf_viewer/${Uri.encode(path)}") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDashboard = { navController.popBackStack("dashboard", false) },
                onNavigateToTrash = { navController.navigate("trash") },
                onNavigateToServer = { navController.navigate("server") }
            )
        }

        composable("browser") {
            BrowserScreen(
                onNavigateToImage = { path -> navController.navigate("image_viewer/${Uri.encode(path)}") },
                onNavigateToVideo = { path -> navController.navigate("video_player/${Uri.encode(path)}") },
                onNavigateToAudio = { path -> navController.navigate("audio_player/${Uri.encode(path)}") },
                onNavigateToEditor = { path -> navController.navigate("editor/${Uri.encode(path)}") },
                onNavigateToArchive = { path -> navController.navigate("archive/${Uri.encode(path)}") },
                onNavigateToPdf = { path -> navController.navigate("pdf_viewer/${Uri.encode(path)}") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDashboard = { navController.navigate("dashboard") { popUpTo("browser") { inclusive = true } } },
                onNavigateToTrash = { navController.navigate("trash") },
                onNavigateToServer = { navController.navigate("server") }
            )
        }

        composable(
            route = "image_viewer/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            ImageViewerScreen(path = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "video_player/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            VideoPlayerScreen(path = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "audio_player/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            AudioPlayerScreen(path = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "editor/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            EditorScreen(path = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "archive/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            ArchiveScreen(path = path, onBack = { navController.popBackStack() })
        }

        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}

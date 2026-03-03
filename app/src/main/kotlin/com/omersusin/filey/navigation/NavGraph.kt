package com.omersusin.filey.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omersusin.filey.feature.browser.BrowserScreen
import com.omersusin.filey.feature.viewer.ImageViewerScreen
import com.omersusin.filey.feature.player.VideoPlayerScreen
import com.omersusin.filey.feature.player.AudioPlayerScreen
import com.omersusin.filey.feature.editor.EditorScreen
import com.omersusin.filey.feature.archive.ArchiveScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val BROWSER = "browser"
    const val IMAGE_VIEWER = "image_viewer"
    const val VIDEO_PLAYER = "video_player"
    const val AUDIO_PLAYER = "audio_player"
    const val EDITOR = "editor"
    const val ARCHIVE = "archive"
}

fun String.encode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
fun String.decode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.BROWSER) {

        composable(Routes.BROWSER) {
            BrowserScreen(
                onNavigate = { path ->
                    navController.navigate("${Routes.BROWSER}/${path.encode()}")
                },
                onOpenImage = { path ->
                    navController.navigate("${Routes.IMAGE_VIEWER}/${path.encode()}")
                },
                onOpenVideo = { path ->
                    navController.navigate("${Routes.VIDEO_PLAYER}/${path.encode()}")
                },
                onOpenAudio = { path ->
                    navController.navigate("${Routes.AUDIO_PLAYER}/${path.encode()}")
                },
                onOpenText = { path ->
                    navController.navigate("${Routes.EDITOR}/${path.encode()}")
                },
                onOpenArchive = { path ->
                    navController.navigate("${Routes.ARCHIVE}/${path.encode()}")
                }
            )
        }

        composable(
            route = "${Routes.BROWSER}/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")?.decode() ?: "/storage/emulated/0"
            BrowserScreen(
                initialPath = path,
                onNavigate = { p ->
                    navController.navigate("${Routes.BROWSER}/${p.encode()}")
                },
                onOpenImage = { p ->
                    navController.navigate("${Routes.IMAGE_VIEWER}/${p.encode()}")
                },
                onOpenVideo = { p ->
                    navController.navigate("${Routes.VIDEO_PLAYER}/${p.encode()}")
                },
                onOpenAudio = { p ->
                    navController.navigate("${Routes.AUDIO_PLAYER}/${p.encode()}")
                },
                onOpenText = { p ->
                    navController.navigate("${Routes.EDITOR}/${p.encode()}")
                },
                onOpenArchive = { p ->
                    navController.navigate("${Routes.ARCHIVE}/${p.encode()}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.IMAGE_VIEWER}/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")?.decode() ?: ""
            ImageViewerScreen(filePath = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.VIDEO_PLAYER}/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")?.decode() ?: ""
            VideoPlayerScreen(filePath = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.AUDIO_PLAYER}/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")?.decode() ?: ""
            AudioPlayerScreen(filePath = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.EDITOR}/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")?.decode() ?: ""
            EditorScreen(filePath = path, onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.ARCHIVE}/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")?.decode() ?: ""
            ArchiveScreen(filePath = path, onBack = { navController.popBackStack() })
        }
    }
}

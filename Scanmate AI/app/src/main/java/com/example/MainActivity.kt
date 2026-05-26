package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.SettingsRepository
import com.example.data.ThemeMode
import com.example.ui.navigation.Routes
import com.example.ui.screens.AiScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PageEditorScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.QrScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignatureScreen
import com.example.ui.screens.ZipScreen
import com.example.ui.theme.ScanMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

            ScanMateTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                onNavigateToCamera = { navController.navigate(Routes.CAMERA_SCAN) },
                                onNavigateToDoc = { id -> navController.navigate(Routes.docDetail(id)) },
                                onNavigateToQr = { navController.navigate(Routes.QR_TOOLS) },
                                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                                onNavigateToAi = { navController.navigate(Routes.AI_ASSISTANT) },
                                onNavigateToZip = { navController.navigate(Routes.ZIP_TOOLS) },
                                onNavigateToFiles = { navController.navigate(Routes.FILE_MANAGER) },
                                onNavigateToPdfTools = { navController.navigate(Routes.PDF_TOOLS) }
                            )
                        }
                        composable(Routes.CAMERA_SCAN) {
                            CameraScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onScanFinished = { id ->
                                    navController.popBackStack()
                                    navController.navigate(Routes.docDetail(id))
                                }
                            )
                        }
                        composable(Routes.DOC_DETAIL) { backStackEntry ->
                            val docIdStr = backStackEntry.arguments?.getString("docId") ?: "0"
                            DocumentDetailScreen(
                                docId = docIdStr.toLongOrNull() ?: 0L,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPageEditor = { documentId, pageId -> navController.navigate(Routes.pageEditor(documentId, pageId)) },
                                onNavigateToSignature = { documentId -> navController.navigate(Routes.signature(documentId)) }
                            )
                        }

                        composable(Routes.PAGE_EDITOR) { backStackEntry ->
                            val docIdStr = backStackEntry.arguments?.getString("docId") ?: "0"
                            val pageIdStr = backStackEntry.arguments?.getString("pageId") ?: "0"
                            PageEditorScreen(
                                docId = docIdStr.toLongOrNull() ?: 0L,
                                pageId = pageIdStr.toLongOrNull() ?: 0L,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.PDF_TOOLS) {
                            PdfToolsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Routes.SIGNATURE) { backStackEntry ->
                            val docIdStr = backStackEntry.arguments?.getString("docId") ?: "0"
                            SignatureScreen(
                                docId = docIdStr.toLongOrNull() ?: 0L,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.QR_TOOLS) {
                            QrScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onOpenCameraScanner = { navController.navigate(Routes.QR_SCANNER) }
                            )
                        }
                        composable(Routes.QR_SCANNER) {
                            QrScannerScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Routes.AI_ASSISTANT) {
                            AiScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Routes.ZIP_TOOLS) {
                            ZipScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Routes.FILE_MANAGER) {
                            FileManagerScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

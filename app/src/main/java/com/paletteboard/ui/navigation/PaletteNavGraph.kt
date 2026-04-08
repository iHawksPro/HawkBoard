package com.paletteboard.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyPressAnimationPreset
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.ui.screen.DashboardScreen
import com.paletteboard.ui.screen.GestureSettingsScreen
import com.paletteboard.ui.screen.ImportExportScreen
import com.paletteboard.ui.screen.KeyboardSettingsScreen
import com.paletteboard.ui.screen.SoundHapticSettingsScreen
import com.paletteboard.ui.screen.ThemeBuilderScreen
import com.paletteboard.ui.screen.ThemeLibraryScreen
import com.paletteboard.ui.screen.ToolbarCustomizationScreen
import com.paletteboard.ui.state.MainUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PaletteNavGraph(
    uiState: MainUiState,
    isKeyboardEnabled: Boolean,
    isKeyboardSelected: Boolean,
    onEnableKeyboard: () -> Unit,
    onUseHawkBoardKeyboard: () -> Unit,
    onShowKeyboardPicker: () -> Unit,
    onApplyTheme: (String) -> Unit,
    onEditTheme: (Theme) -> Unit,
    onDuplicateTheme: (Theme) -> Unit,
    onDeleteTheme: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onResetDraft: () -> Unit,
    onRandomizeDraft: () -> Unit,
    onDraftNameChanged: (String) -> Unit,
    onCornerRadiusChanged: (Float) -> Unit,
    onSpacingChanged: (Float) -> Unit,
    onLabelSizeChanged: (Float) -> Unit,
    onPrimaryColorChanged: (Long) -> Unit,
    onFunctionColorChanged: (Long) -> Unit,
    onBackgroundColorChanged: (Long) -> Unit,
    onTrailColorChanged: (Long) -> Unit,
    onKeyPressAnimationChanged: (KeyPressAnimationPreset) -> Unit,
    onKeyboardTransitionAnimationChanged: (KeyboardTransitionPreset) -> Unit,
    onAnimationDurationChanged: (Int) -> Unit,
    onKeyboardHeightScaleChanged: (Float) -> Unit,
    onNumberRowChanged: (Boolean) -> Unit,
    onGlideTypingChanged: (Boolean) -> Unit,
    onGestureSensitivityChanged: (Float) -> Unit,
    onToolbarActionEnabledChanged: (ToolbarAction, Boolean) -> Unit,
    onSoundPackChanged: (String) -> Unit,
    onHapticProfileChanged: (String) -> Unit,
    onImportBufferChanged: (String) -> Unit,
    onImportTheme: () -> Unit,
    onExportActiveTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val screenHeader = screenHeaderForRoute(currentRoute)
    val topLevelDestinations = listOf(
        TopLevelDestination(Destination.Dashboard.route, "Home", Icons.Rounded.Home),
        TopLevelDestination(Destination.Themes.route, "Themes", Icons.Rounded.Palette),
        TopLevelDestination(Destination.Builder.route, "Builder", Icons.Rounded.Tune),
        TopLevelDestination(Destination.Settings.route, "Settings", Icons.Rounded.Settings),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    title = {
                        Column {
                            Text(
                                text = screenHeader.title,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = screenHeader.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                    ) {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Destination.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                composable(Destination.Dashboard.route) {
                    DashboardScreen(
                        uiState = uiState,
                        isKeyboardEnabled = isKeyboardEnabled,
                        isKeyboardSelected = isKeyboardSelected,
                        onEnableKeyboard = onEnableKeyboard,
                        onUseHawkBoardKeyboard = onUseHawkBoardKeyboard,
                        onShowKeyboardPicker = onShowKeyboardPicker,
                        onApplyTheme = onApplyTheme,
                        onOpenThemeLibrary = { navController.navigate(Destination.Themes.route) },
                        onOpenThemeBuilder = { navController.navigate(Destination.Builder.route) },
                        onOpenImportExport = { navController.navigate(Destination.ImportExport.route) },
                    )
                }
                composable(Destination.Themes.route) {
                    ThemeLibraryScreen(
                        uiState = uiState,
                        onApplyTheme = onApplyTheme,
                        onEditTheme = {
                            onEditTheme(it)
                            navController.navigate(Destination.Builder.route)
                        },
                        onDuplicateTheme = {
                            onDuplicateTheme(it)
                            navController.navigate(Destination.Builder.route)
                        },
                        onDeleteTheme = onDeleteTheme,
                        onOpenImportExport = { navController.navigate(Destination.ImportExport.route) },
                    )
                }
                composable(Destination.Builder.route) {
                    ThemeBuilderScreen(
                        uiState = uiState,
                        onNameChanged = onDraftNameChanged,
                        onCornerRadiusChanged = onCornerRadiusChanged,
                        onSpacingChanged = onSpacingChanged,
                        onLabelSizeChanged = onLabelSizeChanged,
                        onPrimaryColorChanged = onPrimaryColorChanged,
                        onFunctionColorChanged = onFunctionColorChanged,
                        onBackgroundColorChanged = onBackgroundColorChanged,
                        onTrailColorChanged = onTrailColorChanged,
                        onKeyPressAnimationChanged = onKeyPressAnimationChanged,
                        onKeyboardTransitionAnimationChanged = onKeyboardTransitionAnimationChanged,
                        onAnimationDurationChanged = onAnimationDurationChanged,
                        onSaveTheme = onSaveDraft,
                        onReset = onResetDraft,
                        onRandomize = onRandomizeDraft,
                    )
                }
                composable(Destination.Settings.route) {
                    KeyboardSettingsScreen(
                        uiState = uiState,
                        onKeyboardHeightScaleChanged = onKeyboardHeightScaleChanged,
                        onNumberRowChanged = onNumberRowChanged,
                        onGlideTypingChanged = onGlideTypingChanged,
                        onOpenGestureSettings = { navController.navigate(Destination.Gesture.route) },
                        onOpenToolbarSettings = { navController.navigate(Destination.Toolbar.route) },
                        onOpenSoundSettings = { navController.navigate(Destination.Sound.route) },
                        onOpenImportExport = { navController.navigate(Destination.ImportExport.route) },
                    )
                }
                composable(Destination.Gesture.route) {
                    GestureSettingsScreen(
                        uiState = uiState,
                        onGlideTypingChanged = onGlideTypingChanged,
                        onGestureSensitivityChanged = onGestureSensitivityChanged,
                    )
                }
                composable(Destination.Toolbar.route) {
                    ToolbarCustomizationScreen(
                        uiState = uiState,
                        onActionEnabledChanged = onToolbarActionEnabledChanged,
                    )
                }
                composable(Destination.Sound.route) {
                    SoundHapticSettingsScreen(
                        uiState = uiState,
                        onSoundPackChanged = onSoundPackChanged,
                        onHapticProfileChanged = onHapticProfileChanged,
                    )
                }
                composable(Destination.ImportExport.route) {
                    ImportExportScreen(
                        uiState = uiState,
                        onImportBufferChanged = onImportBufferChanged,
                        onImportTheme = onImportTheme,
                        onExportActiveTheme = onExportActiveTheme,
                    )
                }
            }
        }
    }
}

private fun screenHeaderForRoute(route: String?): ScreenHeader = when (route) {
    Destination.Themes.route -> ScreenHeader("Theme Library", "Presets, custom themes, and quick swapping")
    Destination.Builder.route -> ScreenHeader("Theme Builder", "Live editing for color, layout, and animation")
    Destination.Settings.route -> ScreenHeader("Keyboard Settings", "Tune behavior, toolbar, and feedback")
    Destination.Gesture.route -> ScreenHeader("Gesture Settings", "First-class glide typing controls")
    Destination.Toolbar.route -> ScreenHeader("Toolbar", "Choose the shortcuts above the keys")
    Destination.Sound.route -> ScreenHeader("Sound and Haptics", "Feedback profiles for every typing style")
    Destination.ImportExport.route -> ScreenHeader("Import and Export", "Move and share theme JSON locally")
    else -> ScreenHeader("Hawk Board", "Deep keyboard customization with local-first control")
}

private data class ScreenHeader(
    val title: String,
    val subtitle: String,
)

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private sealed class Destination(val route: String) {
    data object Dashboard : Destination("dashboard")
    data object Themes : Destination("themes")
    data object Builder : Destination("builder")
    data object Settings : Destination("settings")
    data object Gesture : Destination("gesture")
    data object Toolbar : Destination("toolbar")
    data object Sound : Destination("sound")
    data object ImportExport : Destination("import_export")
}

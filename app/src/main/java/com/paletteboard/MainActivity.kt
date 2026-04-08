package com.paletteboard

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paletteboard.app.PaletteBoardApp
import com.paletteboard.ime.PaletteInputMethodService
import com.paletteboard.ui.designsystem.PaletteBoardTheme
import com.paletteboard.ui.main.MainViewModel
import com.paletteboard.ui.main.MainViewModelFactory
import com.paletteboard.ui.navigation.PaletteNavGraph

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PaletteBoardApp).container)
    }

    private var keyboardStatus by mutableStateOf(KeyboardStatus())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaletteBoardTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                PaletteNavGraph(
                    uiState = uiState,
                    isKeyboardEnabled = keyboardStatus.isEnabled,
                    isKeyboardSelected = keyboardStatus.isSelected,
                    onEnableKeyboard = {
                        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    onUseHawkBoardKeyboard = { useHawkBoardKeyboard() },
                    onShowKeyboardPicker = {
                        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
                    },
                    onApplyTheme = viewModel::applyTheme,
                    onEditTheme = viewModel::editTheme,
                    onDuplicateTheme = viewModel::duplicateTheme,
                    onDeleteTheme = viewModel::deleteTheme,
                    onSaveDraft = viewModel::saveDraftTheme,
                    onResetDraft = viewModel::resetDraftFromActiveTheme,
                    onRandomizeDraft = viewModel::randomizeDraft,
                    onDraftNameChanged = viewModel::updateDraftName,
                    onCornerRadiusChanged = viewModel::updateDraftCornerRadius,
                    onSpacingChanged = viewModel::updateDraftKeySpacing,
                    onLabelSizeChanged = viewModel::updateDraftLabelSize,
                    onPrimaryColorChanged = viewModel::updateDraftPrimaryColor,
                    onFunctionColorChanged = viewModel::updateDraftFunctionColor,
                    onBackgroundColorChanged = viewModel::updateDraftBackgroundColor,
                    onTrailColorChanged = viewModel::updateDraftTrailColor,
                    onKeyPressAnimationChanged = viewModel::updateDraftKeyPressAnimation,
                    onKeyboardTransitionAnimationChanged = viewModel::updateDraftKeyboardTransitionAnimation,
                    onAnimationDurationChanged = viewModel::updateDraftAnimationDuration,
                    onKeyboardHeightScaleChanged = viewModel::updateKeyboardHeightScale,
                    onNumberRowChanged = viewModel::setNumberRowEnabled,
                    onGlideTypingChanged = viewModel::setGlideTypingEnabled,
                    onGestureSensitivityChanged = viewModel::setGestureSensitivity,
                    onToolbarActionEnabledChanged = viewModel::setToolbarActionEnabled,
                    onSoundPackChanged = viewModel::setSoundPackId,
                    onHapticProfileChanged = viewModel::setHapticProfileId,
                    onImportBufferChanged = viewModel::updateImportBuffer,
                    onImportTheme = viewModel::importThemeFromBuffer,
                    onExportActiveTheme = viewModel::exportTheme,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKeyboardStatus()
    }

    private fun useHawkBoardKeyboard() {
        if (!keyboardStatus.isEnabled) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            return
        }
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }

    private fun refreshKeyboardStatus() {
        val imm = getSystemService(InputMethodManager::class.java)
        val componentName = ComponentName(this, PaletteInputMethodService::class.java)
        val shortId = componentName.flattenToShortString()
        val longId = componentName.flattenToString()
        val selectedId = Secure.getString(contentResolver, Secure.DEFAULT_INPUT_METHOD)
        val isEnabled = imm?.enabledInputMethodList?.any { info ->
            info.id == shortId || info.id == longId
        } == true
        val isSelected = selectedId == shortId || selectedId == longId
        keyboardStatus = KeyboardStatus(
            isEnabled = isEnabled,
            isSelected = isSelected,
        )
    }
}

private data class KeyboardStatus(
    val isEnabled: Boolean = false,
    val isSelected: Boolean = false,
)

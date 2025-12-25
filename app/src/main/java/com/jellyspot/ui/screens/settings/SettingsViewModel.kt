package com.jellyspot.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.jellyspot.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel()

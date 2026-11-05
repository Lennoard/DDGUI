package com.androidvip.ddgui.presentation.di

import androidx.preference.PreferenceManager
import com.androidvip.ddgui.presentation.disclaimer.DisclaimerViewModel
import com.androidvip.ddgui.presentation.files.FilePickerViewModel
import com.androidvip.ddgui.presentation.main.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    viewModelOf(::MainViewModel)
    viewModelOf(::FilePickerViewModel)
    viewModelOf(::DisclaimerViewModel)
}

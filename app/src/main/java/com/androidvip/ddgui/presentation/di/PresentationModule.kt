package com.androidvip.ddgui.presentation.di

import com.androidvip.ddgui.presentation.disclaimer.DisclaimerViewModel
import com.androidvip.ddgui.presentation.files.FilePickerViewModel
import com.androidvip.ddgui.presentation.main.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::FilePickerViewModel)
    viewModelOf(::DisclaimerViewModel)
}

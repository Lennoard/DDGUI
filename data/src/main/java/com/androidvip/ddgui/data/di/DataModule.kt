package com.androidvip.ddgui.data.di

import androidx.preference.PreferenceManager
import com.androidvip.ddgui.data.dd.LibsuDdRepository
import com.androidvip.ddgui.data.dd.LibsuRootShellDataSource
import com.androidvip.ddgui.data.dd.RootShellDataSource
import com.androidvip.ddgui.data.files.RootFileDataSource
import com.androidvip.ddgui.data.files.RootFileRepositoryImpl
import com.androidvip.ddgui.data.files.SuFileDataSource
import com.androidvip.ddgui.data.settings.SharedPreferencesDestructiveOperationSettingsRepository
import com.androidvip.ddgui.domain.dd.DdRepository
import com.androidvip.ddgui.domain.files.RootFileRepository
import com.androidvip.ddgui.domain.settings.DestructiveOperationSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
    single<CoroutineDispatcher>(named(IO_DISPATCHER)) { Dispatchers.IO }
    single<DestructiveOperationSettingsRepository> {
        SharedPreferencesDestructiveOperationSettingsRepository(get())
    }
    single<RootShellDataSource> { LibsuRootShellDataSource(get(named(IO_DISPATCHER))) }
    single<DdRepository> { LibsuDdRepository(get()) }
    single<RootFileDataSource> { SuFileDataSource() }
    single<RootFileRepository> {
        RootFileRepositoryImpl(get(), get(named(IO_DISPATCHER)))
    }
}

private const val IO_DISPATCHER = "ioDispatcher"

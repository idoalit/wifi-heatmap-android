package id.klaras.wifilogger.di

import androidx.room.Room
import id.klaras.wifilogger.data.WifiLoggerDatabase
import id.klaras.wifilogger.data.repository.FloorPlanRepository
import id.klaras.wifilogger.data.repository.WifiLogRepository
import id.klaras.wifilogger.viewmodel.FloorPlanViewModel
import id.klaras.wifilogger.viewmodel.HeatmapViewModel
import id.klaras.wifilogger.viewmodel.LogHistoryViewModel
import id.klaras.wifilogger.viewmodel.WifiLogViewModel
import id.klaras.wifilogger.viewmodel.WifiScannerViewModel
import id.klaras.wifilogger.wifi.WifiScannerHelper
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            WifiLoggerDatabase::class.java,
            WifiLoggerDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    
    single { get<WifiLoggerDatabase>().floorPlanDao() }
    single { get<WifiLoggerDatabase>().wifiLogDao() }
}

val repositoryModule = module {
    single { FloorPlanRepository(get()) }
    single { WifiLogRepository(get()) }
}

val wifiModule = module {
    single { WifiScannerHelper(androidContext()) }
}

val viewModelModule = module {
    viewModel { FloorPlanViewModel(get()) }
    viewModel { WifiLogViewModel(get()) }
    viewModel { WifiScannerViewModel(get(), get()) }
    viewModel { LogHistoryViewModel(get(), get()) }
    viewModel { HeatmapViewModel(get(), get()) }
}

val appModules = listOf(databaseModule, repositoryModule, wifiModule, viewModelModule)

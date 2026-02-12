package id.klaras.wifilogger

import android.app.Application
import id.klaras.wifilogger.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class WifiLoggerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@WifiLoggerApplication)
            modules(appModules)
        }
    }
}

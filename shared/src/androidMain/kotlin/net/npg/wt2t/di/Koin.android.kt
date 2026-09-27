package net.npg.wt2t.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import net.npg.wt2t.data.storage.AndroidDatabaseStorage
import net.npg.wt2t.data.storage.DatabaseStorage
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.domain.service.AndroidDatabaseFileService
import net.npg.wt2t.domain.service.AndroidReportService
import net.npg.wt2t.domain.service.DatabaseFileService
import net.npg.wt2t.domain.service.ReportService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<ReportService> { AndroidReportService(get(), get(named(IO_DISPATCHER_QUALIFIER))) }
    single<DatabaseFileService> { AndroidDatabaseFileService(get(), get(named(IO_DISPATCHER_QUALIFIER))) }
    single<DatabaseStorage> { AndroidDatabaseStorage(get<Context>()) }
    single<SqlDriver> { get<DatabaseStorage>().openDriver() }
    single { WorkTimeDatabase(get()) }
}

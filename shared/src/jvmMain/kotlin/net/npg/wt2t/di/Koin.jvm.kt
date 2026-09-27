package net.npg.wt2t.di

import app.cash.sqldelight.db.SqlDriver
import net.npg.wt2t.data.storage.DatabaseStorage
import net.npg.wt2t.data.storage.JvmDatabaseStorage
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.domain.service.DatabaseFileService
import net.npg.wt2t.domain.service.JvmDatabaseFileService
import net.npg.wt2t.domain.service.JvmReportService
import net.npg.wt2t.domain.service.ReportService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<ReportService> { JvmReportService(get(named(IO_DISPATCHER_QUALIFIER))) }
    single<DatabaseFileService> { JvmDatabaseFileService(get(named(IO_DISPATCHER_QUALIFIER))) }
    single<DatabaseStorage> { JvmDatabaseStorage(JvmDatabaseConfiguration.resolveDatabaseFile()) }
    single<SqlDriver> { get<DatabaseStorage>().openDriver() }
    single { WorkTimeDatabase(get()) }
}

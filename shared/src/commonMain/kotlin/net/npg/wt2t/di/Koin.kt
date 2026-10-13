package net.npg.wt2t.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.npg.wt2t.data.repository.*
import net.npg.wt2t.domain.service.CloudStorageService
import net.npg.wt2t.domain.service.DummyCloudStorageService
import net.npg.wt2t.domain.service.SBOMService
import net.npg.wt2t.domain.usecase.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

expect val platformModule: Module

/**
 * Initializes Koin with the shared module, platform module, and an optional app module.
 * 
 * @param appModule An optional module to include in the Koin context.
 */
fun initKoin(appModule: Module = module {}) = startKoin {
    modules(sharedModule, platformModule, appModule)
}

val sharedModule = module {
    single<Clock> { Clock.System }
    single<CoroutineDispatcher>(named(IO_DISPATCHER_QUALIFIER)) { Dispatchers.IO }

    single<ProjectRepository> {
        SqlDelightProjectRepository(get(), get(named(IO_DISPATCHER_QUALIFIER)))
    }
    single<TaskRepository> {
        SqlDelightTaskRepository(get(), get(named(IO_DISPATCHER_QUALIFIER)))
    }
    single<TimeRepository> {
        SqlDelightTimeRepository(get(), get(named(IO_DISPATCHER_QUALIFIER)))
    }
    single<DailyWorkTimeRepository> {
        SqlDelightDailyWorkTimeRepository(get(), get(named(IO_DISPATCHER_QUALIFIER)))
    }
    single<DayRepository> {
        SqlDelightDayRepository(get(), get(named(IO_DISPATCHER_QUALIFIER)))
    }
    single<ConfigRepository> {
        SqlDelightConfigRepository(get(), get(named(IO_DISPATCHER_QUALIFIER)))
    }

    single { DatabaseExportRepository(get(), get(named(IO_DISPATCHER_QUALIFIER))) }
    single {
        DatabaseManagementRepository(get(), get(), get(named(IO_DISPATCHER_QUALIFIER)))
    }

    singleOf(::ConfigurationUseCase)
    singleOf(::ProjectManagementUseCase)
    singleOf(::BookingUseCase)
    singleOf(::DayEditingUseCase)
    singleOf(::EndOfDayUseCase)
    singleOf(::ReportingUseCase)
    singleOf(::BackupUseCase)
    single { SBOMService(get(named(IO_DISPATCHER_QUALIFIER))) }
    single<CloudStorageService> { DummyCloudStorageService() }
}

/** Koin qualifier for dispatchers that execute blocking I/O. */
const val IO_DISPATCHER_QUALIFIER = "ioDispatcher"

package net.npg.wt2t.ui.viewmodel

import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException

/** Logs an exception handled at a UI event or coroutine boundary. */
internal fun logBoundaryException(exception: Throwable, message: String) {
    if (exception is CancellationException) throw exception
    Logger.e(exception) { message }
}

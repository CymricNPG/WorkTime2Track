package net.npg.wt2t.domain.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import worktime2track.shared.generated.resources.Res

/**
 * A library component presented from the application's software bill of materials.
 */
data class SBOMComponent(
    val name: String,
    val version: String,
    val license: String,
    val packageUrl: String,
    val isDirect: Boolean,
)

/**
 * Loads and parses the CycloneDX JSON SBOM bundled with the application.
 */
class SBOMService internal constructor(
    private val dispatcher: CoroutineDispatcher,
    private val loadResource: suspend () -> ByteArray,
) {
    /** Creates a service that reads the SBOM bundled with the application. */
    constructor(dispatcher: CoroutineDispatcher) : this(
        dispatcher = dispatcher,
        loadResource = { Res.readBytes(SBOM_RESOURCE_PATH) },
    )

    /**
     * Returns all external components in the bundled SBOM.
     */
    suspend fun loadComponents(): List<SBOMComponent> = withContext(dispatcher) {
        parseCycloneDxJson(loadResource())
    }
}

private const val SBOM_RESOURCE_PATH = "files/sbom.json"

package net.npg.wt2t.domain.service

import org.cyclonedx.model.Component
import org.cyclonedx.model.LicenseChoice
import org.cyclonedx.parsers.JsonParser

/** Parses cyclone dx json. */
internal fun parseCycloneDxJson(json: ByteArray): List<SBOMComponent> {
    val bom = JsonParser().parse(json)
    val directReferences = bom.dependencies.orEmpty()
        .filter { dependency -> dependency.ref?.contains(PROJECT_PATH_QUALIFIER) == true }
        .flatMap { dependency -> dependency.dependencies.orEmpty() }
        .mapNotNullTo(mutableSetOf()) { dependency -> dependency.ref }

    return bom.components.orEmpty()
        .mapNotNull { component -> component.toSBOMComponent(directReferences) }
        .sortedWith(
            compareBy<SBOMComponent> { component -> component.name.lowercase() }
                .thenBy(SBOMComponent::version),
        )
}

/** Converts this CycloneDX component to a displayable SBOM component. */
private fun Component.toSBOMComponent(directReferences: Set<String>): SBOMComponent? {
    val componentPackageUrl = purl ?: return null
    if (PROJECT_PATH_QUALIFIER in componentPackageUrl || componentPackageUrl.isPomPackage()) {
        return null
    }

    return SBOMComponent(
        name = name.orEmpty(),
        version = version.orEmpty(),
        license = licenses.toDisplayValue(),
        packageUrl = componentPackageUrl,
        isDirect = bomRef in directReferences,
    )
}

/** Returns whether this package URL describes a Maven POM artifact. */
private fun String.isPomPackage(): Boolean = substringAfter('?', "")
    .substringBefore('#')
    .split('&')
    .any { qualifier -> qualifier.equals(POM_TYPE_QUALIFIER, ignoreCase = true) }

/** Returns a user-facing value for this license choice. */
private fun LicenseChoice?.toDisplayValue(): String {

    val items = this?.items ?: emptyList()

    val identifiers = items
        .map { item -> item.license }
        .filterNotNull()
        .map { license -> license.id ?: license.name }
        .filterNotNull()
        .toList();

    val expressionValues = items
        .map { item -> item.expression }
        .filterNotNull()
        .map { expression -> expression.value }
        .filterNotNull()
        .toList();

    return (identifiers + expressionValues).distinct().joinToString()
}

private const val PROJECT_PATH_QUALIFIER = "project_path="
private const val POM_TYPE_QUALIFIER = "type=pom"
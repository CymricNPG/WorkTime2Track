package net.npg.wt2t

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import net.npg.wt2t.ui.viewmodel.BookingViewModel
import kotlin.test.Test

class PresentationArchitectureTest {
    @Test
    fun `presentation code does not access low-level storage`() {
        val productionClasses = ClassFileImporter().importUrl(
            requireNotNull(BookingViewModel::class.java.protectionDomain.codeSource.location),
        )

        noClasses()
            .that()
            .resideInAnyPackage("net.npg.wt2t.ui..", "net.npg.wt2t")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "net.npg.wt2t.data.repository..",
                "net.npg.wt2t.data.storage..",
                "net.npg.wt2t.db..",
            )
            .check(productionClasses)
    }
}

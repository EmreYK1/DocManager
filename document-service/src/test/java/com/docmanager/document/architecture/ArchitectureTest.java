package com.docmanager.document.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Hält die Schichten ein: controller/messaging → service → repository → entity. */
@AnalyzeClasses(packages = "com.docmanager.document", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule entryPointsUseServices = noClasses()
            .that()
            .resideInAnyPackage("..controller..", "..messaging..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule servicesDoNotKnowTheWebLayer = noClasses()
            .that()
            .resideInAPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..dto..", "..mapper..");

    @ArchTest
    static final ArchRule entitiesAreIndependent = noClasses()
            .that()
            .resideInAPackage("..entity..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..service..", "..repository..", "..controller..", "..dto..", "..messaging..");
}

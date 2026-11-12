package com.steelstory;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {
    @Test
    void verifiesBoundariesWithoutScaffoldingGameplayModules() {
        System.setProperty("spring.modulith.detection-strategy", "explicitly-annotated");
        try {
            ApplicationModules modules = ApplicationModules.of(SteelstoryApplication.class);
            modules.verify();
            assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                    .containsExactly("identity");
        } finally {
            System.clearProperty("spring.modulith.detection-strategy");
        }
    }

    @Test
    void domainIsPureAndIdentityHasNoGameplayOrBootstrapDependencies() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.steelstory");
        noClasses().that().resideInAPackage("..identity.internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "java.sql..", "tools.jackson..",
                        "..identity.internal.application..", "..identity.internal.infrastructure..",
                        "..identity.internal.web..")
                .check(classes);
        noClasses().that().resideInAPackage("..identity..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..character..", "..economy..", "..bootstrap..")
                .check(classes);
        noClasses().that().resideOutsideOfPackage("..identity..")
                .should().dependOnClassesThat().resideInAPackage("..identity.internal..")
                .check(classes);
    }
}

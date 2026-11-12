package com.steelstory;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {
    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.steelstory");

    @Test
    void verifiesBoundariesWithoutScaffoldingGameplayModules() {
        System.setProperty("spring.modulith.detection-strategy", "explicitly-annotated");
        try {
            ApplicationModules modules = ApplicationModules.of(SteelstoryApplication.class);
            modules.verify();
            assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                    .containsExactlyInAnyOrder("identity", "character");
        } finally {
            System.clearProperty("spring.modulith.detection-strategy");
        }
    }

    @Test
    void domainIsPureAndIdentityHasNoGameplayOrBootstrapDependencies() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.steelstory");
        noClasses().that().resideInAPackage("..internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "java.sql..", "tools.jackson..",
                        "..internal.application..", "..internal.infrastructure..", "..internal.web..")
                .check(classes);
        noClasses().that().resideInAPackage("..identity..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..character..", "..economy..", "..bootstrap..")
                .check(classes);
        noClasses().that().resideOutsideOfPackage("..identity..")
                .should().dependOnClassesThat().resideInAPackage("..identity.internal..")
                .check(classes);
    }

    @Test
    void characterInternalsStayPrivate() {
        fields().that().areDeclaredInClassesThat()
                .haveFullyQualifiedName("com.steelstory.character.internal.domain.PlayerCharacter")
                .should().bePrivate().andShould().beFinal()
                .check(PRODUCTION_CLASSES);
        noClasses().that().resideOutsideOfPackage("..character..")
                .should().dependOnClassesThat().resideInAPackage("..character.internal..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void httpDtosAreTransportOnlyAndApplicationAndDomainCannotDependOnWeb() {
        classes().that().resideInAPackage("..web.dto..")
                .should().haveSimpleNameEndingWith("Dto")
                .check(PRODUCTION_CLASSES);
        noClasses().that().resideInAPackage("..web.dto..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..internal.application..", "..internal.domain..", "..internal.infrastructure..", "..identity.api..")
                .check(PRODUCTION_CLASSES);
        noClasses().that().resideInAnyPackage("..internal.application..", "..internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..web..", "org.springframework.web..", "jakarta.servlet..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void restEndpointsUseRequestDtosAndTypedResponseEntities() {
        for (JavaClass controller : PRODUCTION_CLASSES) {
            if (!controller.isAnnotatedWith(RestController.class)) {
                continue;
            }

            for (Method method : controller.reflect().getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }

                assertThat(method.getReturnType()).as("%s return type", method).isEqualTo(ResponseEntity.class);
                assertThat(method.getGenericReturnType()).as("%s response type", method)
                        .isInstanceOf(ParameterizedType.class);
                ParameterizedType responseEntityType = (ParameterizedType) method.getGenericReturnType();
                Type bodyType = responseEntityType.getActualTypeArguments()[0];
                assertThat(bodyType).as("%s response body", method).isInstanceOf(Class.class);
                Class<?> responseType = (Class<?>) bodyType;
                if (responseType != Void.class) {
                    assertThat(responseType.getSimpleName()).endsWith("ResponseDto");
                    assertThat(responseType.getPackageName()).endsWith(".web.dto");
                }

                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(RequestBody.class)) {
                        assertThat(parameter.getType().getSimpleName()).endsWith("RequestDto");
                        assertThat(parameter.getType().getPackageName()).endsWith(".web.dto");
                    }
                }
            }
        }
    }
}

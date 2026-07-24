package com.javaacademy.platform;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String ROOT = "com.javaacademy.platform";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void no_top_level_layer_packages() {
        ArchRule rule = noClasses()
                .should()
                .resideInAPackage(ROOT + ".controllers..")
                .orShould()
                .resideInAPackage(ROOT + ".services..")
                .orShould()
                .resideInAPackage(ROOT + ".models..")
                .orShould()
                .resideInAPackage(ROOT + ".repositories..")
                .because(
                        "packages must be organised by feature (auth, catalog, progress, sandbox, ai, interview, common, config), not by layer");

        rule.check(classes);
    }

    @Test
    void all_classes_reside_in_allowed_packages() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage(ROOT + "..")
                .and()
                .doNotHaveSimpleName("PlatformApplication")
                .and()
                .doNotHaveSimpleName("package-info")
                .should()
                .resideInAnyPackage(
                        ROOT + ".auth..",
                        ROOT + ".catalog..",
                        ROOT + ".progress..",
                        ROOT + ".sandbox..",
                        ROOT + ".ai..",
                        ROOT + ".interview..",
                        ROOT + ".common..",
                        ROOT + ".config..")
                .because("every class except PlatformApplication must live in a feature package per §3");

        rule.check(classes);
    }

    @Test
    void controller_methods_must_not_return_entities() {
        DescribedPredicate<JavaClass> isEntity = DescribedPredicate.describe(
                "annotated with @Entity", type -> type.isAnnotatedWith("jakarta.persistence.Entity"));

        ArchRule rule = noMethods()
                .that()
                .areDeclaredInClassesThat()
                .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should()
                .haveRawReturnType(isEntity)
                .because(
                        "controllers must return DTOs, never JPA entities — an entity leak exposes internal fields like password_hash");

        rule.check(classes);
    }
}

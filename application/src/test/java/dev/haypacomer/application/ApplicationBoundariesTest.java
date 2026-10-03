package dev.haypacomer.application;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ApplicationBoundariesTest {

  private static final JavaClasses CLASSES = new ClassFileImporter().importClasspath();

  @Test
  void applicationDoesNotDependOnFrameworks() {
    noClasses()
        .that()
        .resideInAPackage("dev.haypacomer.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta..",
            "javax..",
            "org.hibernate..",
            "java.sql..",
            "org.flywaydb..",
            "io.lettuce..",
            "redis.clients..",
            "io.jsonwebtoken..")
        .because("application must stay framework-free")
        .check(CLASSES);
  }
}

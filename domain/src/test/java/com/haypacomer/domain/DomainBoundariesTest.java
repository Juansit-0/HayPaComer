package com.haypacomer.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class DomainBoundariesTest {

  private static final JavaClasses CLASSES = new ClassFileImporter().importClasspath();

  @Test
  void domainDoesNotDependOnFrameworks() {
    noClasses()
        .that()
        .resideInAPackage("com.haypacomer.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta..", "javax..", "org.hibernate..")
        .because("domain must stay pure Java")
        .check(CLASSES);
  }
}

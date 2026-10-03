package dev.haypacomer.application;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

class ApplicationArchitectureTest {

  private static final JavaClasses PRODUCTION =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("dev.haypacomer");

  private static final DescribedPredicate<JavaClass> USE_CASES =
      new DescribedPredicate<>("use case classes") {
        @Override
        public boolean test(JavaClass type) {
          return type.getPackageName().startsWith("dev.haypacomer.application.")
              && !type.getPackageName().startsWith("dev.haypacomer.application.port")
              && type.getModifiers().contains(JavaModifier.PUBLIC)
              && type.getModifiers().contains(JavaModifier.FINAL)
              && !type.isRecord()
              && !type.isEnum()
              && !type.isInterface()
              && !type.isAssignableTo(Throwable.class)
              && !type.getSimpleName().equals("OpaqueTokens")
              && !type.isNestedClass();
        }
      };

  private static final ArchCondition<JavaClass> HAVE_AT_MOST_ONE_PUBLIC_METHOD =
      new ArchCondition<>("have at most one public method") {
        @Override
        public void check(JavaClass type, ConditionEvents events) {
          long publicMethods =
              type.getMethods().stream()
                  .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                  .filter(method -> !method.getModifiers().contains(JavaModifier.STATIC))
                  .count();
          if (publicMethods > 1) {
            events.add(
                SimpleConditionEvent.violated(
                    type, type.getName() + " exposes " + publicMethods + " public methods"));
          }
        }
      };

  @Test
  void everyUseCaseHasASinglePublicMethod() {
    classes().that(USE_CASES).should(HAVE_AT_MOST_ONE_PUBLIC_METHOD).check(PRODUCTION);
  }

  @Test
  void portsAreInterfaces() {
    classes()
        .that()
        .resideInAPackage("dev.haypacomer.application.port..")
        .should()
        .beInterfaces()
        .check(PRODUCTION);
  }

  @Test
  void applicationNeverDependsOnAdaptersOrWeb() {
    noClasses()
        .that()
        .resideInAPackage("dev.haypacomer.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "dev.haypacomer.persistence..",
            "dev.haypacomer.notifications..",
            "dev.haypacomer.sensors..",
            "dev.haypacomer.ai..",
            "dev.haypacomer.agent..",
            "dev.haypacomer.web..")
        .check(PRODUCTION);
  }

  @Test
  void domainNeverDependsOnApplication() {
    noClasses()
        .that()
        .resideInAPackage("dev.haypacomer.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("dev.haypacomer.application..")
        .check(PRODUCTION);
  }
}

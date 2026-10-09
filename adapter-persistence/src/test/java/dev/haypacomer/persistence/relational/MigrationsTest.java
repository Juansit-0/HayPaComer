package dev.haypacomer.persistence.relational;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class MigrationsTest extends PostgresTestSupport {

  @Test
  void appliesAllMigrationsAndSeedsReferenceData() {
    JdbcClient jdbc = JdbcClient.create(dataSource);

    assertEquals(26, appliedMigrations);
    assertEquals(14, jdbc.sql("SELECT count(*) FROM allergens").query(Integer.class).single());
    assertEquals(24, seededFoods);
    assertEquals(24, pricedFoods);
    assertEquals(2, soySauceAllergens);
    assertEquals(11, seededSubstitutionRules);
    assertEquals(5, seededTemplates);
    assertEquals(9, seededTemplateSteps);
  }
}

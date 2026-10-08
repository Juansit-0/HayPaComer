package dev.haypacomer.web.planning;

import dev.haypacomer.application.planning.ListRecipeTemplates;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecipeTemplateController {

  private final ListRecipeTemplates listRecipeTemplates;

  public RecipeTemplateController(ListRecipeTemplates listRecipeTemplates) {
    this.listRecipeTemplates = listRecipeTemplates;
  }

  @GetMapping("/api/v1/recipe-templates")
  List<PlanningController.RecipeResponse> templates() {
    return listRecipeTemplates.list().stream()
        .map(PlanningController.RecipeResponse::from)
        .toList();
  }
}

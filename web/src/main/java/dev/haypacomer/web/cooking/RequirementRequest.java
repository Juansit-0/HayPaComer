package dev.haypacomer.web.cooking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record RequirementRequest(
    @NotBlank String food,
    @DecimalMin(value = "0", inclusive = false) BigDecimal grams,
    String quantity,
    Boolean optional) {}

package dev.haypacomer.web.cooking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record StepRequest(
    @NotBlank @Size(max = 500) String instruction,
    @Positive Long timerSeconds,
    @Valid RequirementRequest weigh) {}

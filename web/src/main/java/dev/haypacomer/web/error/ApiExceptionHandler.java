package dev.haypacomer.web.error;

import dev.haypacomer.application.auth.EmailAlreadyRegisteredException;
import dev.haypacomer.application.auth.InvalidCredentialsException;
import dev.haypacomer.application.auth.InvalidPasswordException;
import dev.haypacomer.application.auth.InvalidTokenException;
import dev.haypacomer.application.auth.TooManyLoginAttemptsException;
import dev.haypacomer.application.auth.UserNotFoundException;
import dev.haypacomer.application.coldchain.FridgeNotFoundException;
import dev.haypacomer.application.device.DeviceNotFoundException;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.household.InvitationEmailMismatchException;
import dev.haypacomer.application.inventory.FoodItemNotFoundException;
import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.inventory.NothingToUndoException;
import dev.haypacomer.application.inventory.PermissionRequiredException;
import dev.haypacomer.application.inventory.SnapshotNotFoundException;
import dev.haypacomer.application.scale.NoRecentSampleException;
import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.quantity.InvalidQuantityException;
import dev.haypacomer.domain.quantity.UnconvertibleQuantityException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(InvalidCredentialsException.class)
  ProblemDetail invalidCredentials(InvalidCredentialsException exception) {
    return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", exception.getMessage());
  }

  @ExceptionHandler(TooManyLoginAttemptsException.class)
  ProblemDetail tooManyAttempts(TooManyLoginAttemptsException exception) {
    return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts", exception.getMessage());
  }

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  ProblemDetail emailTaken(EmailAlreadyRegisteredException exception) {
    return problem(HttpStatus.CONFLICT, "Email already registered", exception.getMessage());
  }

  @ExceptionHandler(InvalidPasswordException.class)
  ProblemDetail invalidPassword(InvalidPasswordException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid password", exception.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  ProblemDetail userNotFound(UserNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(InvalidTokenException.class)
  ProblemDetail invalidToken(InvalidTokenException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid or expired link", exception.getMessage());
  }

  @ExceptionHandler(InvitationEmailMismatchException.class)
  ProblemDetail invitationMismatch(InvitationEmailMismatchException exception) {
    return problem(HttpStatus.FORBIDDEN, "Invitation not for this account", exception.getMessage());
  }

  @ExceptionHandler(NoRecentSampleException.class)
  ProblemDetail noRecentSample(NoRecentSampleException exception) {
    return problem(HttpStatus.CONFLICT, "Scale offline", exception.getMessage());
  }

  @ExceptionHandler(MalformedSensorPayloadException.class)
  ProblemDetail malformedPayload(MalformedSensorPayloadException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Malformed sensor payload", exception.getMessage());
  }

  @ExceptionHandler(FridgeNotFoundException.class)
  ProblemDetail fridgeNotFound(FridgeNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(DeviceNotFoundException.class)
  ProblemDetail deviceNotFound(DeviceNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(HouseholdNotFoundException.class)
  ProblemDetail householdNotFound(HouseholdNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(FoodItemNotFoundException.class)
  ProblemDetail foodItemNotFound(FoodItemNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(FoodNotInCatalogException.class)
  ProblemDetail foodNotInCatalog(FoodNotInCatalogException exception) {
    return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Unknown food", exception.getMessage());
  }

  @ExceptionHandler(InvalidQuantityException.class)
  ProblemDetail invalidQuantity(InvalidQuantityException exception) {
    return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Unreadable quantity", exception.getMessage());
  }

  @ExceptionHandler(UnconvertibleQuantityException.class)
  ProblemDetail unconvertibleQuantity(UnconvertibleQuantityException exception) {
    return problem(
        HttpStatus.UNPROCESSABLE_CONTENT, "Quantity cannot be weighed", exception.getMessage());
  }

  @ExceptionHandler(NothingToUndoException.class)
  ProblemDetail nothingToUndo(NothingToUndoException exception) {
    return problem(HttpStatus.CONFLICT, "Nothing to undo", exception.getMessage());
  }

  @ExceptionHandler(SnapshotNotFoundException.class)
  ProblemDetail snapshotNotFound(SnapshotNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(PermissionRequiredException.class)
  ProblemDetail permissionRequired(PermissionRequiredException exception) {
    return problem(HttpStatus.FORBIDDEN, "Permission required", exception.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail conflict(IllegalStateException exception) {
    return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail accessDenied(AccessDeniedException exception) {
    return problem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalidInput(IllegalArgumentException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
  }

  private static ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }
}

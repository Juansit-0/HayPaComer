package dev.haypacomer.web.error;

import dev.haypacomer.agent.chat.ConversationNotFoundException;
import dev.haypacomer.agent.confirm.ConfirmationRefusedException;
import dev.haypacomer.application.agent.AgentRunNotFoundException;
import dev.haypacomer.application.agent.ConfirmationExpiredException;
import dev.haypacomer.application.agent.ConfirmationNotFoundException;
import dev.haypacomer.application.ai.AiRateLimitExceededException;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
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
import dev.haypacomer.application.market.MarketBudgetNotSetException;
import dev.haypacomer.application.notification.NotificationNotFoundException;
import dev.haypacomer.application.planning.RecipeNotFoundException;
import dev.haypacomer.application.planning.WeeklyPlanNotFoundException;
import dev.haypacomer.application.scale.NoRecentSampleException;
import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.application.session.CookingSessionNotFoundException;
import dev.haypacomer.application.session.SessionAlreadyActiveException;
import dev.haypacomer.domain.fridge.FridgeBusyException;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.quantity.InvalidQuantityException;
import dev.haypacomer.domain.quantity.UnconvertibleQuantityException;
import dev.haypacomer.domain.session.IllegalSessionTransitionException;
import dev.haypacomer.web.security.ProblemTypes;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail invalidBody(MethodArgumentNotValidException exception) {
    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST, "Invalid request", "Some fields are missing or out of range");
    Map<String, String> errors = new TreeMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(
            error ->
                errors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage()));
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadableBody(HttpMessageNotReadableException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request body is not valid JSON");
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ProblemDetail invalidCredentials(InvalidCredentialsException exception) {
    return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", exception.getMessage());
  }

  @ExceptionHandler(TooManyLoginAttemptsException.class)
  ProblemDetail tooManyAttempts(TooManyLoginAttemptsException exception) {
    return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts", exception.getMessage());
  }

  @ExceptionHandler(RecipeNotFoundException.class)
  ProblemDetail recipeNotFound(RecipeNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(WeeklyPlanNotFoundException.class)
  ProblemDetail planNotFound(WeeklyPlanNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(NotificationNotFoundException.class)
  ProblemDetail notificationNotFound(NotificationNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(FridgeBusyException.class)
  ProblemDetail fridgeBusy(FridgeBusyException exception) {
    return problem(HttpStatus.CONFLICT, "Fridge in use", exception.getMessage());
  }

  @ExceptionHandler(AiRateLimitExceededException.class)
  ProblemDetail aiRateLimit(AiRateLimitExceededException exception) {
    return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many requests", exception.getMessage());
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

  @ExceptionHandler(CookingSessionNotFoundException.class)
  ProblemDetail sessionNotFound(CookingSessionNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(SessionAlreadyActiveException.class)
  ProblemDetail sessionAlreadyActive(SessionAlreadyActiveException exception) {
    ProblemDetail problem = problem(HttpStatus.CONFLICT, "Already cooking", exception.getMessage());
    problem.setProperty("activeSessionId", exception.active().value());
    return problem;
  }

  @ExceptionHandler(IllegalSessionTransitionException.class)
  ProblemDetail illegalTransition(IllegalSessionTransitionException exception) {
    return problem(HttpStatus.CONFLICT, "Invalid session step", exception.getMessage());
  }

  @ExceptionHandler(NothingToUndoException.class)
  ProblemDetail nothingToUndo(NothingToUndoException exception) {
    return problem(HttpStatus.CONFLICT, "Nothing to undo", exception.getMessage());
  }

  @ExceptionHandler(SnapshotNotFoundException.class)
  ProblemDetail snapshotNotFound(SnapshotNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessException.class})
  ResponseEntity<ProblemDetail> dataUnavailable(DataAccessException exception) {
    ProblemDetail problem =
        problem(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service unavailable",
            "The kitchen data is not reachable right now; measured stock is never shown from an"
                + " old copy. Try again in a few seconds.");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header(HttpHeaders.RETRY_AFTER, "5")
        .body(problem);
  }

  @ExceptionHandler(MarketBudgetNotSetException.class)
  ProblemDetail budgetNotSet(MarketBudgetNotSetException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(PhotoReadingUnavailableException.class)
  ProblemDetail photoUnavailable(PhotoReadingUnavailableException exception) {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE, "Photo reading unavailable", exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail tooLarge(MaxUploadSizeExceededException exception) {
    return problem(HttpStatus.CONTENT_TOO_LARGE, "Photo too large", "Send a photo of at most 4 MB");
  }

  @ExceptionHandler(ConversationNotFoundException.class)
  ProblemDetail conversationNotFound(ConversationNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(AgentRunNotFoundException.class)
  ProblemDetail agentRunNotFound(AgentRunNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(ConfirmationNotFoundException.class)
  ProblemDetail confirmationNotFound(ConfirmationNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
  }

  @ExceptionHandler(ConfirmationExpiredException.class)
  ProblemDetail confirmationExpired(ConfirmationExpiredException exception) {
    return problem(HttpStatus.GONE, "Confirmation expired", exception.getMessage());
  }

  @ExceptionHandler(ConfirmationRefusedException.class)
  ProblemDetail confirmationRefused(ConfirmationRefusedException exception) {
    return problem(HttpStatus.CONFLICT, "Confirmation refused", exception.getMessage());
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
    problem.setType(ProblemTypes.of(title));
    return problem;
  }
}

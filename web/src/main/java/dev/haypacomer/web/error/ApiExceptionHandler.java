package dev.haypacomer.web.error;

import dev.haypacomer.application.auth.EmailAlreadyRegisteredException;
import dev.haypacomer.application.auth.InvalidCredentialsException;
import dev.haypacomer.application.auth.InvalidPasswordException;
import dev.haypacomer.application.auth.TooManyLoginAttemptsException;
import dev.haypacomer.application.auth.UserNotFoundException;
import dev.haypacomer.domain.household.AccessDeniedException;
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

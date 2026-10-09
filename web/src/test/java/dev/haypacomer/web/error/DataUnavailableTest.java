package dev.haypacomer.web.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class DataUnavailableTest {

  @Test
  void anUnreachableDatabaseIsAnHonest503WithRetryAfter() {
    ResponseEntity<ProblemDetail> response =
        new ApiExceptionHandler()
            .dataUnavailable(new DataAccessResourceFailureException("connection refused"));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals("5", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    assertEquals("Service unavailable", response.getBody().getTitle());
    assertTrue(response.getBody().getDetail().contains("never shown from an old copy"));
  }
}

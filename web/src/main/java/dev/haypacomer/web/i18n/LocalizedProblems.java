package dev.haypacomer.web.i18n;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class LocalizedProblems implements ResponseBodyAdvice<Object> {

  private final ObjectProvider<Localizer> localizer;

  public LocalizedProblems(ObjectProvider<Localizer> localizer) {
    this.localizer = localizer;
  }

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    Localizer available = localizer.getIfAvailable();
    if (available != null && body instanceof ProblemDetail problem) {
      problem.setTitle(available.message(problem.getTitle()));
      problem.setDetail(available.message(problem.getDetail()));
    }
    return body;
  }
}

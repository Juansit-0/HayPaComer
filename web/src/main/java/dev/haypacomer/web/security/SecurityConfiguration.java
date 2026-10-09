package dev.haypacomer.web.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import dev.haypacomer.application.device.AuthenticateDevice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

  @Bean
  @Order(1)
  SecurityFilterChain deviceFilterChain(HttpSecurity http, AuthenticateDevice authenticateDevice)
      throws Exception {
    return http.securityMatcher("/api/v1/device/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(
            new DeviceKeyAuthenticationFilter(authenticateDevice),
            AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(requests -> requests.anyRequest().hasRole("DEVICE"))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(ProblemSecurityResponses.unauthorized("X-Device-Key"))
                    .accessDeniedHandler(ProblemSecurityResponses.forbidden()))
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/docs",
                        "/docs/**",
                        "/swagger-ui/**",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/api/v1/i18n",
                        "/api/v1/i18n/*",
                        "/",
                        "/index.html",
                        "/app/**",
                        "/brand/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(ProblemSecurityResponses.unauthorized("Bearer"))
                    .accessDeniedHandler(ProblemSecurityResponses.forbidden()))
        .oauth2ResourceServer(
            server ->
                server
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(ProblemSecurityResponses.unauthorized("Bearer"))
                    .accessDeniedHandler(ProblemSecurityResponses.forbidden()))
        .build();
  }

  @Bean
  JwtEncoder jwtEncoder(JwtProperties properties) {
    return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(properties.signingKey()));
  }

  @Bean
  JwtDecoder jwtDecoder(JwtProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(properties.signingKey())
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
    return decoder;
  }
}

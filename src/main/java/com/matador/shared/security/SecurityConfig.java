package com.matador.shared.security;

import com.matador.shared.security.jwt.JwtAuthenticationFilter;
import com.matador.shared.security.jwt.JwtProperties;
import com.matador.shared.security.jwt.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.HttpStatus;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt cost factor 12 per BACKEND.md §5.1.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }

    /** Inbound provider webhooks: no Spring auth, signatures verified in controllers. */
    @Bean
    @Order(1)
    public SecurityFilterChain webhookChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/webhooks/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Customer API: stateless JWT bearer authentication. */
    @Bean
    @Order(2)
    public SecurityFilterChain customerChain(HttpSecurity http, JwtService jwtService)
        throws Exception {
        http.securityMatcher("/api/customer/**", "/jwks.json")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers("/jwks.json", "/api/customer/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer/zones")
                        .permitAll()
                        .anyRequest()
                        .hasRole(Role.CUSTOMER.name()))
            .exceptionHandling(
                e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Admin API: session-cookie authentication with CSRF protection. */
    @Bean
    @Order(3)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/admin/**")
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers("/api/admin/auth/login")
                        .permitAll()
                        .anyRequest()
                        .authenticated());
        return http.build();
    }

    /** Everything else: docs, health, public zone lookups. */
    @Bean
    @Order(4)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info",
                            "/api/zones/**",
                            "/error")
                        .permitAll()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }
}

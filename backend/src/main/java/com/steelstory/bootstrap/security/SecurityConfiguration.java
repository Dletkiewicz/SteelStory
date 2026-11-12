package com.steelstory.bootstrap.security;

import com.steelstory.identity.api.AccountAccess;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
        return new ProviderManager(authenticationProviders);
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, AccountAccess accountAccess,
            ObjectMapper objectMapper,
            CsrfTokenRepository csrfTokenRepository, SecurityContextRepository securityContextRepository,
            @Value("${springdoc.api-docs.enabled:false}") boolean docsEnabled) throws Exception {
        SecurityProblemWriter securityProblemWriter = new SecurityProblemWriter(objectMapper);

        httpSecurity.authorizeHttpRequests(authorize -> {
            authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/status", "/actuator/health",
                            "/api/v1/auth/csrf").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                            "/api/v1/auth/logout").permitAll();
            if (docsEnabled) {
                authorize.requestMatchers(HttpMethod.GET, "/v3/api-docs").permitAll();
            }

            authorize.anyRequest().authenticated();
        });

        httpSecurity.csrf(config -> config.csrfTokenRepository(csrfTokenRepository));
        httpSecurity.securityContext(config -> config
                .securityContextRepository(securityContextRepository)
                .requireExplicitSave(true));
        httpSecurity.requestCache(config -> config.disable());
        httpSecurity.formLogin(config -> config.disable());
        httpSecurity.httpBasic(config -> config.disable());
        // Explicit JSON endpoints invoke the standard authentication/logout strategies.
        httpSecurity.logout(config -> config.disable());
        httpSecurity.exceptionHandling(config -> config
                .authenticationEntryPoint((request, response, exception) ->
                        securityProblemWriter.writeUnauthenticated(request, response))
                .accessDeniedHandler((request, response, exception) ->
                        securityProblemWriter.write(request, response, HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED", "Access denied or CSRF token is missing or invalid.")));
        httpSecurity.addFilterBefore(
                new IdentityRequestLimitFilter(securityProblemWriter), SecurityContextHolderFilter.class);
        httpSecurity.addFilterBefore(
                new ActiveAccountFilter(accountAccess, securityProblemWriter), AuthorizationFilter.class);

        return httpSecurity.build();
    }
}

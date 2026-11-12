package com.steelstory.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthentication {
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler securityContextLogoutHandler;
    private final CsrfLogoutHandler csrfLogoutHandler;

    public SessionAuthentication(SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) {
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.securityContextLogoutHandler = new SecurityContextLogoutHandler();
        this.securityContextLogoutHandler.setSecurityContextRepository(securityContextRepository);
        this.csrfLogoutHandler = new CsrfLogoutHandler(csrfTokenRepository);
    }

    public void signIn(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    public void signOut(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        csrfLogoutHandler.logout(request, response, authentication);
        securityContextLogoutHandler.logout(request, response, authentication);
    }
}

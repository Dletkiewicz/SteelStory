package com.steelstory.bootstrap.security;

import com.steelstory.identity.api.AccountAccess;
import com.steelstory.identity.api.AccountId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

final class ActiveAccountFilter extends OncePerRequestFilter {
    private final AccountAccess accountAccess;
    private final SecurityProblemWriter securityProblemWriter;

    ActiveAccountFilter(AccountAccess accountAccess, SecurityProblemWriter securityProblemWriter) {
        this.accountAccess = accountAccess;
        this.securityProblemWriter = securityProblemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return request.getMethod().equals("GET")
                && (path.equals("/api/v1/status") || path.equals("/actuator/health") || path.equals("/v3/api-docs"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            boolean active;
            try {
                active = accountAccess.isActive(new AccountId(UUID.fromString(authentication.getName())));
            } catch (IllegalArgumentException exception) {
                active = false;
            }

            if (!active) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                securityProblemWriter.writeUnauthenticated(request, response);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

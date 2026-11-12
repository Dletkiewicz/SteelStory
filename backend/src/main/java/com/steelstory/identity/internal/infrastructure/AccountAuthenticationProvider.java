package com.steelstory.identity.internal.infrastructure;

import com.steelstory.identity.internal.application.IdentityService;
import com.steelstory.identity.internal.application.InvalidCredentialsException;
import com.steelstory.identity.internal.domain.Account;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AccountAuthenticationProvider implements AuthenticationProvider {
    private final IdentityService identityService;

    public AccountAuthenticationProvider(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        try {
            Account account = identityService.authenticate(
                    authentication.getName(), (String) authentication.getCredentials());

            // Persist only a stable account UUID, no credentials, email or domain aggregate.
            return UsernamePasswordAuthenticationToken.authenticated(
                    account.id().value().toString(), null, List.of());
        } catch (InvalidCredentialsException exception) {
            throw new BadCredentialsException(InvalidCredentialsException.MESSAGE);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

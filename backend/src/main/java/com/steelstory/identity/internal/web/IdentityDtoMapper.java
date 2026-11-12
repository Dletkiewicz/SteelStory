package com.steelstory.identity.internal.web;

import com.steelstory.identity.internal.application.RegisterAccountCommand;
import com.steelstory.identity.internal.domain.Account;
import com.steelstory.identity.internal.web.dto.AccountResponseDto;
import com.steelstory.identity.internal.web.dto.CredentialsRequestDto;
import com.steelstory.identity.internal.web.dto.CsrfResponseDto;
import com.steelstory.identity.internal.web.dto.LoginRequestDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;

final class IdentityDtoMapper {
    private IdentityDtoMapper() {
    }

    static RegisterAccountCommand toRegisterAccountCommand(CredentialsRequestDto request) {
        return new RegisterAccountCommand(request.email(), request.password());
    }

    static UsernamePasswordAuthenticationToken toAuthentication(LoginRequestDto request) {
        return UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password());
    }

    static AccountResponseDto toAccountResponseDto(Account account) {
        return new AccountResponseDto(
                account.id().value(),
                account.email().value(),
                account.status().name(),
                account.createdAt(),
                account.updatedAt());
    }

    static CsrfResponseDto toCsrfResponseDto(CsrfToken csrfToken) {
        return new CsrfResponseDto(csrfToken.getHeaderName(), csrfToken.getToken());
    }
}

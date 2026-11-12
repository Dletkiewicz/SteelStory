package com.steelstory.identity.internal.web;

import com.steelstory.identity.api.AccountId;
import com.steelstory.identity.internal.application.IdentityService;
import com.steelstory.identity.internal.application.RegisterAccountCommand;
import com.steelstory.identity.internal.domain.Account;
import com.steelstory.identity.internal.web.dto.AccountResponseDto;
import com.steelstory.identity.internal.web.dto.CredentialsRequestDto;
import com.steelstory.identity.internal.web.dto.CsrfResponseDto;
import com.steelstory.identity.internal.web.dto.IdentityProblemResponseDto;
import com.steelstory.identity.internal.web.dto.LoginRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@ApiResponse(responseCode = "400", description = "Invalid input",
        content = @Content(mediaType = "application/problem+json",
                schema = @Schema(implementation = IdentityProblemResponseDto.class)))
@ApiResponse(responseCode = "401", description = "Authentication required or invalid credentials",
        content = @Content(mediaType = "application/problem+json",
                schema = @Schema(implementation = IdentityProblemResponseDto.class)))
@ApiResponse(responseCode = "403", description = "Access denied or missing/invalid CSRF token",
        content = @Content(mediaType = "application/problem+json",
                schema = @Schema(implementation = IdentityProblemResponseDto.class)))
public class AuthController {
    private final IdentityService identityService;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthentication sessionAuthentication;

    public AuthController(IdentityService identityService, AuthenticationManager authenticationManager,
            SessionAuthentication sessionAuthentication) {
        this.identityService = identityService;
        this.authenticationManager = authenticationManager;
        this.sessionAuthentication = sessionAuthentication;
    }

    @Operation(operationId = "registerAccount", summary = "Register an account without logging in",
            description = "Requires the CSRF token and its session cookie. Body limited to 4096 bytes.")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "Token from getCsrfToken", schema = @Schema(type = "string"))
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "409", description = "Canonical email already registered",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = IdentityProblemResponseDto.class)))
    @ApiResponse(responseCode = "413", description = "Body exceeds 4096 bytes",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = IdentityProblemResponseDto.class)))
    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AccountResponseDto> register(@RequestBody CredentialsRequestDto request) {
        RegisterAccountCommand command = IdentityDtoMapper.toRegisterAccountCommand(request);
        Account account = identityService.register(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(IdentityDtoMapper.toAccountResponseDto(account));
    }

    @Operation(operationId = "login", summary = "Authenticate and rotate the session ID",
            description = "Requires CSRF and its session cookie. Fetch a fresh CSRF token after success. "
                    + "Body limit 4096 bytes. Missing/empty passwords, invalid Unicode, more than 128 code points "
                    + "or invalid emails return 400 before account lookup. Valid input with incorrect credentials, "
                    + "an unknown email or a disabled account returns the same 401 response.")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "Token from getCsrfToken", schema = @Schema(type = "string"))
    @ApiResponse(responseCode = "200", description = "Authenticated account; rotated SESSION cookie")
    @ApiResponse(responseCode = "413", description = "Body exceeds 4096 bytes",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = IdentityProblemResponseDto.class)))
    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AccountResponseDto> login(
            @RequestBody LoginRequestDto credentials, HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = IdentityDtoMapper.toAuthentication(credentials);
        Authentication result = authenticationManager.authenticate(authentication);
        Account account = identityService.currentAccount(new AccountId(UUID.fromString(result.getName())));

        sessionAuthentication.signIn(result, request, response);

        return ResponseEntity.ok(IdentityDtoMapper.toAccountResponseDto(account));
    }

    @Operation(operationId = "getCurrentAccount", summary = "Get the active account identified by the session",
            security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponse(responseCode = "200", description = "Current account")
    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<AccountResponseDto> me(Authentication principal) {
        AccountId accountId = new AccountId(UUID.fromString(principal.getName()));
        Account account = identityService.currentAccount(accountId);

        return ResponseEntity.ok(IdentityDtoMapper.toAccountResponseDto(account));
    }

    @Operation(operationId = "logout", summary = "Invalidate the session",
            description = "Requires CSRF, including for an anonymous session. "
                    + "Fetch a fresh token before the next mutation.")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "Token from getCsrfToken", schema = @Schema(type = "string"))
    @ApiResponse(responseCode = "413", description = "Body exceeds 4096 bytes",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = IdentityProblemResponseDto.class)))
    @ApiResponse(responseCode = "204", description = "Session invalidated", content = @Content)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication principal, HttpServletRequest request, HttpServletResponse response) {
        sessionAuthentication.signOut(principal, request, response);

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "getCsrfToken", summary = "Obtain a CSRF token bound to the session",
            description = "Retain the SESSION cookie and send token in X-CSRF-TOKEN. "
                    + "Repeat after login/logout. Do not cache.")
    @ApiResponse(responseCode = "200", description = "CSRF header name and token")
    @GetMapping(value = "/csrf", produces = "application/json")
    public ResponseEntity<CsrfResponseDto> csrf(@Parameter(hidden = true) CsrfToken token) {
        return ResponseEntity.ok(IdentityDtoMapper.toCsrfResponseDto(token));
    }
}
